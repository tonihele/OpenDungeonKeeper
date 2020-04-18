/*
 * Copyright (C) 2014-2017 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.game.network.game;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.network.Filters;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.service.AbstractHostedConnectionService;
import com.jme3.network.service.HostedServiceManager;
import com.jme3.network.service.rmi.RmiHostedService;
import com.jme3.network.service.rmi.RmiRegistry;
import com.jme3.util.SafeArrayList;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.server.EntityDataHostedService;
import java.awt.Point;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.game.map.MapData;
import toniarts.openkeeper.game.map.MapTile;
import toniarts.openkeeper.game.network.NetworkConstants;
import toniarts.openkeeper.game.network.message.GameData;
import toniarts.openkeeper.game.network.message.GameLoadProgressData;
import toniarts.openkeeper.game.network.streaming.StreamingHostedService;
import toniarts.openkeeper.game.state.CheatState;
import toniarts.openkeeper.game.state.lobby.ClientInfo;
import toniarts.openkeeper.game.state.session.GameSession;
import toniarts.openkeeper.game.state.session.GameSessionListener;
import toniarts.openkeeper.game.state.session.GameSessionServerService;
import toniarts.openkeeper.game.state.session.GameSessionServiceListener;
import toniarts.openkeeper.tools.convert.map.TriggerAction;
import toniarts.openkeeper.utils.GameLoop;
import toniarts.openkeeper.view.selection.SelectionArea;

/**
 * Game server hosts lobby service for the game clients.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class GameHostedService extends AbstractHostedConnectionService implements GameSessionServerService {

    /**
     * Someone is listening on the other end, for that we need a message type
     */
    public enum MessageType {
        GAME_DATA,
        GAME_LOAD_PROGRESS
    }

    private static final Logger LOGGER = Logger.getLogger(GameHostedService.class.getName());

    private boolean readyToLoad = false;
    private final Object loadLock = new Object();
    private static final String ATTRIBUTE_SESSION = "game.session";
    private final Map<ClientInfo, GameSessionImpl> players = new ConcurrentHashMap<>(4, 0.75f, 5);
    private final Map<HostedConnection, ClientInfo> playersByConnection = new ConcurrentHashMap<>(4, 0.75f, 5);
    private final Map<ClientInfo, Boolean> playersInTransition = new ConcurrentHashMap<>(4, 0.75f, 5);
    private final SafeArrayList<GameSessionServiceListener> serverListeners = new SafeArrayList<>(GameSessionServiceListener.class);
    private RmiHostedService rmiService;
    private ScheduledExecutorService entityUpdater;

    /**
     * Creates a new lobby service that will use the default reliable channel
     * for reliable communication.
     */
    public GameHostedService() {
        super(false);
    }

    private GameSessionImpl getGameSession(HostedConnection conn) {
        return conn.getAttribute(ATTRIBUTE_SESSION);
    }

    @Override
    protected void onInitialize(HostedServiceManager s) {

        // Grab the RMI service so we can easily use it later
        this.rmiService = getService(RmiHostedService.class);
        if (rmiService == null) {
            throw new RuntimeException(getClass().getName() + " requires an RMI service.");
        }

        // Listen for other client progresses
        getServer().addMessageListener(new ServerMessageListener());
    }

    @Override
    public void terminate(HostedServiceManager serviceManager) {
        super.terminate(serviceManager);
        if (entityUpdater != null) {
            entityUpdater.shutdownNow();
            try {
                entityUpdater.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, "Failed to wait for the entity updater to shutdown!", ex);
            }
        }
    }

    /**
     * Starts hosting the chat services on the specified connection using a
     * generated player name.
     */
    public void startHostingOnConnection(HostedConnection conn, ClientInfo clientInfo) {
        LOGGER.log(Level.FINER, "startHostingOnConnection({0})", conn);

        GameSessionImpl session = new GameSessionImpl(conn, clientInfo);
        players.put(clientInfo, session);
        playersByConnection.put(conn, clientInfo);
        playersInTransition.put(clientInfo, false);

        // Expose the session as an RMI resource to the client
        RmiRegistry rmi = rmiService.getRmiRegistry(conn);
        rmi.share(NetworkConstants.GAME_CHANNEL, session, GameSession.class);
    }

    @Override
    public void startHostingOnConnection(HostedConnection conn) {
        throw new UnsupportedOperationException("You need to supply the clientInfo!");
    }

    @Override
    public void stopHostingOnConnection(HostedConnection conn) {
        LOGGER.log(Level.FINER, "stopHostingOnConnection({0})", conn);
        GameSessionImpl player = getGameSession(conn);
        if (player != null) {

            // Then we are still hosting on the connection... it's
            // possible that stopHostingOnConnection() is called more than
            // once for a particular connection since some other game code
            // may call it and it will also be called during connection shutdown.
            conn.setAttribute(ATTRIBUTE_SESSION, null);

            // Remove player session from the active sessions list
            players.remove(player.clientInfo);
            playersByConnection.remove(conn);
            playersInTransition.remove(player.clientInfo);
        }
    }

    @Override
    public void addGameSessionServiceListener(GameSessionServiceListener l) {
        serverListeners.add(l);
    }

    @Override
    public void removeGameSessionServiceListener(GameSessionServiceListener l) {
        serverListeners.remove(l);
    }

    @Override
    public EntityData getEntityData() {
        return getServiceManager().getService(EntityDataHostedService.class).getEntityData();
    }

    @Override
    public void sendGameData(Collection<Keeper> players, MapData mapData) {
        Thread thread = new Thread(() -> {

            if (!readyToLoad) {
                synchronized (loadLock) {
                    if (!readyToLoad) {
                        try {
                            loadLock.wait();
                        } catch (InterruptedException ex) {
                            LOGGER.log(Level.SEVERE, "Game data sender interrupted!", ex);
                            return;
                        }
                    }
                }
            }

            // We must block this until all clients are ready to receive
            try {

                // Data is too big, stream the data
                getServiceManager().getService(StreamingHostedService.class).sendData(MessageType.GAME_DATA.ordinal(), new GameData(new ArrayList<>(players), mapData), null);
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Failed to send the game data to clients!", ex);
            }

        }, "GameDataSender");
        thread.start();
    }

    @Override
    public void startGame() {
        for (GameSessionImpl gameSession : players.values()) {
            gameSession.onGameStarted();
        }

        // Hmm, for now this, update the entities
        entityUpdater = Executors.newSingleThreadScheduledExecutor((Runnable r) -> new Thread(r, "EntityDataUpdater"));
        entityUpdater.scheduleAtFixedRate(() -> {
            getServiceManager().getService(EntityDataHostedService.class).sendUpdates();
        }, 0, GameLoop.INTERVAL_FPS_60, TimeUnit.NANOSECONDS);
    }

    @Override
    public void updateTiles(List<MapTile> updatedTiles) {
        for (GameSessionImpl gameSession : players.values()) {
            gameSession.onTilesChange(updatedTiles);
        }
    }

    @Override
    public void setWidescreen(boolean enable, short playerId) {
        for (Map.Entry<ClientInfo, GameSessionImpl> gameSession : players.entrySet()) {
            if (gameSession.getKey().getKeeper().getId() == playerId) {
                gameSession.getValue().onSetWidescreen(enable);
                break;
            }
        }
    }

    @Override
    public void playSpeech(int speechId, boolean showText, boolean introduction, int pathId, short playerId) {
        for (Map.Entry<ClientInfo, GameSessionImpl> gameSession : players.entrySet()) {
            if (gameSession.getKey().getKeeper().getId() == playerId) {
                gameSession.getValue().onPlaySpeech(speechId, showText, introduction, pathId);
                break;
            }
        }
    }

    @Override
    public void doTransition(short pathId, Vector3f start, short playerId) {
        for (Map.Entry<ClientInfo, GameSessionImpl> gameSession : players.entrySet()) {
            if (gameSession.getKey().getKeeper().getId() == playerId) {
                gameSession.getValue().onDoTransition(pathId, start);
                break;
            }
        }
    }

    @Override
    public void flashButton(TriggerAction.MakeType buttonType, short targetId, TriggerAction.ButtonType targetButtonType, boolean enabled, int time, short playerId) {
        for (Map.Entry<ClientInfo, GameSessionImpl> gameSession : players.entrySet()) {
            if (gameSession.getKey().getKeeper().getId() == playerId) {
                gameSession.getValue().onFlashButton(buttonType, targetId, targetButtonType, enabled, time);
                break;
            }
        }
    }

    @Override
    public void rotateViewAroundPoint(Vector3f point, boolean relative, int angle, int time, short playerId) {
        for (Map.Entry<ClientInfo, GameSessionImpl> gameSession : players.entrySet()) {
            if (gameSession.getKey().getKeeper().getId() == playerId) {
                gameSession.getValue().onRotateViewAroundPoint(point, relative, angle, time);
                break;
            }
        }
    }

    @Override
    public void showMessage(int textId, short playerId) {
        for (Map.Entry<ClientInfo, GameSessionImpl> gameSession : players.entrySet()) {
            if (gameSession.getKey().getKeeper().getId() == playerId) {
                gameSession.getValue().onShowMessage(textId);
                break;
            }
        }
    }

    @Override
    public void zoomViewToPoint(Vector3f point, short playerId) {
        for (Map.Entry<ClientInfo, GameSessionImpl> gameSession : players.entrySet()) {
            if (gameSession.getKey().getKeeper().getId() == playerId) {
                gameSession.getValue().onZoomViewToPoint(point);
                break;
            }
        }
    }

    @Override
    public void zoomViewToEntity(EntityId entityId, short playerId) {
        for (Map.Entry<ClientInfo, GameSessionImpl> gameSession : players.entrySet()) {
            if (gameSession.getKey().getKeeper().getId() == playerId) {
                gameSession.getValue().onZoomViewToEntity(entityId);
                break;
            }
        }
    }

    @Override
    public void showUnitFlower(EntityId entityId, int interval, short playerId) {
        for (Map.Entry<ClientInfo, GameSessionImpl> gameSession : players.entrySet()) {
            if (gameSession.getKey().getKeeper().getId() == playerId) {
                gameSession.getValue().onShowUnitFlower(entityId, interval);
                break;
            }
        }
    }


    @Override
    public void setGamePaused(boolean paused) {
        for (GameSessionImpl gameSession : players.values()) {
            if (paused) {
                gameSession.onGamePaused();
            } else {
                gameSession.onGameResumed();
            }
        }
    }

    @Override
    public void flashTiles(List<Point> points, boolean enabled, short playerId) {
        for (Map.Entry<ClientInfo, GameSessionImpl> gameSession : players.entrySet()) {
            if (gameSession.getKey().getKeeper().getId() == playerId) {
                gameSession.getValue().onTileFlash(points, enabled, playerId);
                break;
            }
        }
    }

    @Override
    public boolean isInTransition() {
        for (boolean inTransition : playersInTransition.values()) {
            if (inTransition) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onGoldChange(short keeperId, int gold) {
        for (GameSessionImpl gameSession : players.values()) {
            gameSession.onGoldChange(keeperId, gold);
        }
    }

    @Override
    public void onManaChange(short keeperId, int mana, int manaLoose, int manaGain) {
        for (GameSessionImpl gameSession : players.values()) {
            gameSession.onManaChange(keeperId, mana, manaLoose, manaGain);
        }
    }

    @Override
    public void onBuild(short keeperId, List<MapTile> tiles) {
        for (GameSessionImpl gameSession : players.values()) {
            gameSession.onBuild(keeperId, tiles);
        }
    }

    @Override
    public void onSold(short keeperId, List<MapTile> tiles) {
        for (GameSessionImpl gameSession : players.values()) {
            gameSession.onSold(keeperId, tiles);
        }
    }

    @Override
    public void onEntityAdded(short keeperId, ResearchableEntity researchableEntity) {
        for (GameSessionImpl gameSession : players.values()) {
            gameSession.onEntityAdded(keeperId, researchableEntity);
        }
    }

    @Override
    public void onEntityRemoved(short keeperId, ResearchableEntity researchableEntity) {
        for (GameSessionImpl gameSession : players.values()) {
            gameSession.onEntityRemoved(keeperId, researchableEntity);
        }
    }

    @Override
    public void onResearchStatusChanged(short keeperId, ResearchableEntity researchableEntity) {
        for (GameSessionImpl gameSession : players.values()) {
            gameSession.onResearchStatusChanged(keeperId, researchableEntity);
        }
    }

    private class ServerMessageListener implements MessageListener<HostedConnection> {

        public ServerMessageListener() {
        }

        @Override
        public void messageReceived(HostedConnection source, Message message) {
            ClientInfo clientInfo = playersByConnection.get(source);

            if (message instanceof GameLoadProgressData) {
                GameLoadProgressData data = (GameLoadProgressData) message;
                LOGGER.log(Level.FINEST, "onLoadStatus({0},{1})", new Object[]{data.getProgress(), clientInfo.getKeeper().getId()});

                clientInfo.setLoadingProgress(data.getProgress());

                // Send this with UDP messages, otherwise this gets totally blocked and all connections fail
                getServer().broadcast(Filters.notEqualTo(source), new GameLoadProgressData(clientInfo.getKeeper().getId(), data.getProgress()));
            }
        }
    }

    /**
     * The connection-specific 'host' for the GameSession. For convenience this
     * also implements the GameSessionListener. Since the methods don't collide
     * at all it's convenient for our other code not to have to worry about the
     * internal delegate.
     */
    private class GameSessionImpl implements GameSession, GameSessionListener {

        private final HostedConnection conn;
        private final ClientInfo clientInfo;
        private GameSessionListener callback;

        public GameSessionImpl(HostedConnection conn, ClientInfo clientInfo) {
            this.conn = conn;
            this.clientInfo = clientInfo;

            // Note: at this point we won't be able to look up the callback
            // because we haven't received the client's RMI shared objects yet.
        }

        protected GameSessionListener getCallback() {
            if (callback == null) {
                RmiRegistry rmi = rmiService.getRmiRegistry(conn);
                callback = rmi.getRemoteObject(GameSessionListener.class);
                if (callback == null) {
                    throw new RuntimeException("Unable to locate client callback for GameSessionListener");
                }
            }
            return callback;
        }

        @Override
        public void loadComplete() {
            clientInfo.setLoaded(true);
            boolean allLoaded = true;
            for (GameSessionImpl gameSession : players.values()) {
                if (!gameSession.clientInfo.isLoaded()) {
                    allLoaded = false;
                }
                gameSession.onLoadComplete(clientInfo.getKeeper().getId());
            }

            // Start the game if everyone has loaded
            if (allLoaded) {
                startGame();
            }
        }

        @Override
        public void loadStatus(float progress) {
            // This is dealt with messaging
        }

        @Override
        public void onGameDataLoaded(Collection<Keeper> players, MapData mapData) {

            // We send this as a streamed message, to all, super big
        }

        @Override
        public void onGameStarted() {
            getCallback().onGameStarted();
        }

        @Override
        public void onLoadComplete(short keeperId) {
            getCallback().onLoadComplete(keeperId);
        }

        @Override
        public void onLoadStatusUpdate(float progress, short keeperId) {
            getCallback().onLoadStatusUpdate(progress, keeperId);
        }

        @Override
        public void onTilesChange(List<MapTile> updatedTiles) {
            getCallback().onTilesChange(updatedTiles);
        }

        @Override
        public void markReady() {
            clientInfo.setReadyToLoad(true);

            for (GameSessionImpl gameSession : players.values()) {
                if (!gameSession.clientInfo.isReadyToLoad()) {
                    return;
                }
            }

            // Let the game loader continue
            readyToLoad = true;
            synchronized (loadLock) {
                loadLock.notifyAll();
            }
        }

        @Override
        public void selectTiles(SelectionArea area) {
            for (GameSessionServiceListener listener : serverListeners.getArray()) {
                listener.onSelectTiles(area, clientInfo.getKeeper().getId());
            }
        }

        @Override
        public void build(SelectionArea area, short roomId) {
            for (GameSessionServiceListener listener : serverListeners.getArray()) {
                listener.onBuild(area, roomId, clientInfo.getKeeper().getId());
            }
        }

        @Override
        public void sell(SelectionArea area) {
            for (GameSessionServiceListener listener : serverListeners.getArray()) {
                listener.onSell(area, clientInfo.getKeeper().getId());
            }
        }

        @Override
        public void interact(EntityId entity) {
            for (GameSessionServiceListener listener : serverListeners.getArray()) {
                listener.onInteract(entity, clientInfo.getKeeper().getId());
            }
        }

        @Override
        public void pickUp(EntityId entity) {
            for (GameSessionServiceListener listener : serverListeners.getArray()) {
                listener.onPickUp(entity, clientInfo.getKeeper().getId());
            }
        }

        @Override
        public void drop(EntityId entity, Point tile, Vector2f coordinates, EntityId dropOnEntity) {
            for (GameSessionServiceListener listener : serverListeners.getArray()) {
                listener.onDrop(entity, tile, coordinates, dropOnEntity, clientInfo.getKeeper().getId());
            }
        }

        @Override
        public void getGold(int amount) {
            for (GameSessionServiceListener listener : serverListeners.getArray()) {
                listener.onGetGold(amount, clientInfo.getKeeper().getId());
            }
        }

        @Override
        public void transitionEnd() {
            playersInTransition.put(clientInfo, false);
            for (GameSessionServiceListener listener : serverListeners.getArray()) {
                listener.onTransitionEnd(clientInfo.getKeeper().getId());
            }
        }

        @Override
        public void pauseGame() {
            for (GameSessionServiceListener listener : serverListeners.getArray()) {
                listener.onPauseRequest(clientInfo.getKeeper().getId());
            }
        }

        @Override
        public void resumeGame() {
            for (GameSessionServiceListener listener : serverListeners.getArray()) {
                listener.onResumeRequest(clientInfo.getKeeper().getId());
            }
        }

        @Override
        public void exitGame() {
            for (GameSessionServiceListener listener : serverListeners.getArray()) {
                listener.onExitGame(clientInfo.getKeeper().getId());
            }
        }

        @Override
        public void triggerCheat(CheatState.CheatType cheat) {
            for (GameSessionServiceListener listener : serverListeners.getArray()) {
                listener.onCheatTriggered(cheat, clientInfo.getKeeper().getId());
            }
        }

        @Override
        public EntityData getEntityData() {
            return null; // Cached on client...
        }

        @Override
        public void onGoldChange(short keeperId, int gold) {
            getCallback().onGoldChange(keeperId, gold);
        }

        @Override
        public void onManaChange(short keeperId, int mana, int manaLoose, int manaGain) {
            getCallback().onManaChange(keeperId, mana, manaLoose, manaGain);
        }

        @Override
        public void onBuild(short keeperId, List<MapTile> tiles) {
            getCallback().onBuild(keeperId, tiles);
        }

        @Override
        public void onSold(short keeperId, List<MapTile> tiles) {
            getCallback().onSold(keeperId, tiles);
        }

        @Override
        public void onGamePaused() {
            getCallback().onGamePaused();
        }

        @Override
        public void onGameResumed() {
            getCallback().onGameResumed();
        }

        @Override
        public void onSetWidescreen(boolean enable) {
            getCallback().onSetWidescreen(enable);
        }

        @Override
        public void onPlaySpeech(int speechId, boolean showText, boolean introduction, int pathId) {
            getCallback().onPlaySpeech(speechId, showText, introduction, pathId);
        }

        @Override
        public void onDoTransition(short pathId, Vector3f start) {
            playersInTransition.put(clientInfo, true);
            getCallback().onDoTransition(pathId, start);
        }

        @Override
        public void onFlashButton(TriggerAction.MakeType buttonType, short targetId, TriggerAction.ButtonType targetButtonType, boolean enabled, int time) {
            getCallback().onFlashButton(buttonType, targetId, targetButtonType, enabled, time);
        }

        @Override
        public void onRotateViewAroundPoint(Vector3f point, boolean relative, int angle, int time) {
            getCallback().onRotateViewAroundPoint(point, relative, angle, time);
        }

        @Override
        public void onShowMessage(int textId) {
            getCallback().onShowMessage(textId);
        }

        @Override
        public void onZoomViewToPoint(Vector3f point) {
            getCallback().onZoomViewToPoint(point);
        }

        @Override
        public void onTileFlash(List<Point> points, boolean enabled, short keeperId) {
            getCallback().onTileFlash(points, enabled, keeperId);
        }

        @Override
        public void onZoomViewToEntity(EntityId entityId) {
            getCallback().onZoomViewToEntity(entityId);
        }

        @Override
        public void onShowUnitFlower(EntityId entityId, int interval) {
            getCallback().onShowUnitFlower(entityId, interval);
        }

        @Override
        public void onEntityAdded(short keeperId, ResearchableEntity researchableEntity) {
            getCallback().onEntityAdded(keeperId, researchableEntity);
        }

        @Override
        public void onEntityRemoved(short keeperId, ResearchableEntity researchableEntity) {
            getCallback().onEntityRemoved(keeperId, researchableEntity);
        }

        @Override
        public void onResearchStatusChanged(short keeperId, ResearchableEntity researchableEntity) {
            getCallback().onResearchStatusChanged(keeperId, researchableEntity);
        }

    }
}
