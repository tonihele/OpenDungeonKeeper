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
import com.jme3.network.Client;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.service.AbstractClientService;
import com.jme3.network.service.ClientServiceManager;
import com.jme3.network.service.rmi.RmiClientService;
import com.jme3.util.SafeArrayList;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.client.EntityDataClientService;
import java.awt.Point;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.game.map.MapData;
import toniarts.openkeeper.game.map.MapTile;
import toniarts.openkeeper.game.network.NetworkConstants;
import toniarts.openkeeper.game.network.message.GameData;
import toniarts.openkeeper.game.network.message.GameLoadProgressData;
import toniarts.openkeeper.game.network.streaming.StreamedMessageListener;
import toniarts.openkeeper.game.network.streaming.StreamingClientService;
import toniarts.openkeeper.game.state.CheatState;
import toniarts.openkeeper.game.state.session.GameSession;
import toniarts.openkeeper.game.state.session.GameSessionClientService;
import toniarts.openkeeper.game.state.session.GameSessionListener;
import toniarts.openkeeper.tools.convert.map.TriggerAction;
import toniarts.openkeeper.view.selection.SelectionArea;

/**
 * Client side service for the game lobby services
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class GameClientService extends AbstractClientService
        implements GameSessionClientService {

    private static final Logger LOGGER = Logger.getLogger(GameClientService.class.getName());

    private RmiClientService rmiService;
    private GameSession delegate;

    private final GameSessionCallback sessionCallback = new GameSessionCallback();
    private final SafeArrayList<GameSessionListener> listeners = new SafeArrayList<>(GameSessionListener.class);

    @Override
    public void loadComplete() {
        getDelegate().loadComplete();
    }

    @Override
    public void loadStatus(float progress) {

        // By UDP messages
        //getDelegate().loadStatus(progress);
        getClient().send(new GameLoadProgressData((short) 0, progress));
    }

    @Override
    public void addGameSessionListener(GameSessionListener l) {
        listeners.add(l);
    }

    @Override
    public void removeGameSessionListener(GameSessionListener l) {
        listeners.remove(l);
    }

    @Override
    protected void onInitialize(ClientServiceManager s) {
        LOGGER.log(Level.FINER, "onInitialize({0})", s);
        this.rmiService = getService(RmiClientService.class);
        if (rmiService == null) {
            throw new RuntimeException("GameClientService requires RMI service");
        }
        LOGGER.finer("Sharing session callback.");
        rmiService.share(NetworkConstants.GAME_CHANNEL, sessionCallback, GameSessionListener.class);

        // Listen for the streaming messages
        s.getService(StreamingClientService.class).addListener(GameHostedService.MessageType.GAME_DATA.ordinal(), (StreamedMessageListener<GameData>) (GameData data) -> {

            LOGGER.log(Level.FINEST, "onGameDataLoaded({0})", new Object[]{data});
            for (GameSessionListener l : listeners.getArray()) {
                l.onGameDataLoaded(data.getPlayers(), data.getMapData());
            }

        });

        // Listen for other client progresses
        getClient().addMessageListener(new ClientMessageListener());
    }

    /**
     * Called during connection setup once the server-side services have been
     * initialized for this connection and any shared objects, etc. should be
     * available.
     */
    @Override
    public void start() {
        LOGGER.finer("start()");
        super.start();
    }

    private GameSession getDelegate() {
        // We look up the delegate lazily to make the service more
        // flexible.  This way we don't have to know anything about the
        // connection lifecycle and can simply report an error if the
        // game is doing something screwy.
        if (delegate == null) {
            // Look it up
            this.delegate = rmiService.getRemoteObject(GameSession.class);
            LOGGER.log(Level.FINER, "delegate:{0}", delegate);
            if (delegate == null) {
                throw new RuntimeException("No game session found");
            }
        }
        return delegate;
    }

    @Override
    public void selectTiles(SelectionArea area) {
        getDelegate().selectTiles(area);
    }

    @Override
    public void build(SelectionArea area, short roomId) {
        getDelegate().build(area, roomId);
    }

    @Override
    public void sell(SelectionArea area) {
        getDelegate().sell(area);
    }

    @Override
    public void markReady() {
        getDelegate().markReady();
    }

    @Override
    public EntityData getEntityData() {
        return getService(EntityDataClientService.class).getEntityData();
    }

    @Override
    public void interact(EntityId entity) {
        getDelegate().interact(entity);
    }

    @Override
    public void pickUp(EntityId entity) {
        getDelegate().pickUp(entity);
    }

    @Override
    public void drop(EntityId entity, Point tile, Vector2f coordinates, EntityId dropOnEntity) {
        getDelegate().drop(entity, tile, coordinates, dropOnEntity);
    }

    @Override
    public void getGold(int amount) {
        getDelegate().getGold(amount);
    }

    @Override
    public void transitionEnd() {
        getDelegate().transitionEnd();
    }

    @Override
    public void pauseGame() {
        getDelegate().pauseGame();
    }

    @Override
    public void resumeGame() {
        getDelegate().resumeGame();
    }

    @Override
    public void exitGame() {

        // Connection might already be lost
        if (getClient().isConnected()) {
            getDelegate().exitGame();
        }
    }

    @Override
    public void triggerCheat(CheatState.CheatType cheat) {
        getDelegate().triggerCheat(cheat);
    }

    private class ClientMessageListener implements MessageListener<Client> {

        public ClientMessageListener() {
        }

        @Override
        public void messageReceived(Client source, Message message) {
            if (message instanceof GameLoadProgressData) {
                GameLoadProgressData data = (GameLoadProgressData) message;
                LOGGER.log(Level.FINEST, "onLoadStatusUpdate({0},{1})", new Object[]{data.getProgress(), data.getKeeperId()});
                for (GameSessionListener l : listeners.getArray()) {
                    l.onLoadStatusUpdate(data.getProgress(), data.getKeeperId());
                }
            }
        }
    }

    /**
     * Shared with the server over RMI so that it can notify us about account
     * related stuff.
     */
    private class GameSessionCallback implements GameSessionListener {

        @Override
        public void onGameDataLoaded(Collection<Keeper> players, MapData mapData) {

            // This is dealt with streaming
//            logger.log(Level.FINEST, "onGameDataLoaded({0})", new Object[]{mapData});
//            for (GameSessionListener l : listeners) {
//                l.onGameDataLoaded(mapData);
//            }
        }

        @Override
        public void onGameStarted() {
            LOGGER.log(Level.FINEST, "onGameStarted()");
            for (GameSessionListener l : listeners.getArray()) {
                l.onGameStarted();
            }
        }

        @Override
        public void onLoadComplete(short keeperId) {
            LOGGER.log(Level.FINEST, "onLoadComplete({0})", new Object[]{keeperId});
            for (GameSessionListener l : listeners.getArray()) {
                l.onLoadComplete(keeperId);
            }
        }

        @Override
        public void onLoadStatusUpdate(float progress, short keeperId) {
//            logger.log(Level.FINEST, "onLoadStatusUpdate({0},{1})", new Object[]{progress, keeperId});
//            for (GameSessionListener l : listeners.getArray()) {
//                l.onLoadStatusUpdate(progress, keeperId);
//            }
        }

        @Override
        public void onTilesChange(List<MapTile> updatedTiles) {
            for (GameSessionListener l : listeners.getArray()) {
                l.onTilesChange(updatedTiles);
            }
        }

        @Override
        public void onGoldChange(short keeperId, int gold) {
            for (GameSessionListener l : listeners.getArray()) {
                l.onGoldChange(keeperId, gold);
            }
        }

        @Override
        public void onManaChange(short keeperId, int mana, int manaLoose, int manaGain) {
            for (GameSessionListener l : listeners.getArray()) {
                l.onManaChange(keeperId, mana, manaLoose, manaGain);
            }
        }

        @Override
        public void onBuild(short keeperId, List<MapTile> tiles) {
            for (GameSessionListener l : listeners.getArray()) {
                l.onBuild(keeperId, tiles);
            }
        }

        @Override
        public void onSold(short keeperId, List<MapTile> tiles) {
            for (GameSessionListener l : listeners.getArray()) {
                l.onSold(keeperId, tiles);
            }
        }

        @Override
        public void onGamePaused() {
            for (GameSessionListener l : listeners.getArray()) {
                l.onGamePaused();
            }
        }

        @Override
        public void onGameResumed() {
            for (GameSessionListener l : listeners.getArray()) {
                l.onGameResumed();
            }
        }

        @Override
        public void onSetWidescreen(boolean enable) {
            for (GameSessionListener l : listeners.getArray()) {
                l.onSetWidescreen(enable);
            }
        }

        @Override
        public void onPlaySpeech(int speechId, boolean showText, boolean introduction, int pathId) {
            for (GameSessionListener l : listeners.getArray()) {
                l.onPlaySpeech(speechId, showText, introduction, pathId);
            }
        }

        @Override
        public void onDoTransition(short pathId, Vector3f start) {
            for (GameSessionListener l : listeners.getArray()) {
                l.onDoTransition(pathId, start);
            }
        }

        @Override
        public void onFlashButton(TriggerAction.MakeType buttonType, short targetId, TriggerAction.ButtonType targetButtonType, boolean enabled, int time) {
            for (GameSessionListener l : listeners.getArray()) {
                l.onFlashButton(buttonType, targetId, targetButtonType, enabled, time);
            }
        }

        @Override
        public void onRotateViewAroundPoint(Vector3f point, boolean relative, int angle, int time) {
            for (GameSessionListener l : listeners.getArray()) {
                l.onRotateViewAroundPoint(point, relative, angle, time);
            }
        }

        @Override
        public void onShowMessage(int textId) {
            for (GameSessionListener l : listeners.getArray()) {
                l.onShowMessage(textId);
            }
        }

        @Override
        public void onZoomViewToPoint(Vector3f point) {
            for (GameSessionListener l : listeners.getArray()) {
                l.onZoomViewToPoint(point);
            }
        }

        @Override
        public void onTileFlash(List<Point> points, boolean enabled, short keeperId) {
            for (GameSessionListener l : listeners.getArray()) {
                l.onTileFlash(points, enabled, keeperId);
            }
        }

        @Override
        public void onZoomViewToEntity(EntityId entityId) {
            for (GameSessionListener l : listeners.getArray()) {
                l.onZoomViewToEntity(entityId);
            }
        }

        @Override
        public void onShowUnitFlower(EntityId entityId, int interval) {
            for (GameSessionListener l : listeners.getArray()) {
                l.onShowUnitFlower(entityId, interval);
            }
        }

        @Override
        public void onEntityAdded(short keeperId, ResearchableEntity researchableEntity) {
            for (GameSessionListener l : listeners.getArray()) {
                l.onEntityAdded(keeperId, researchableEntity);
            }
        }

        @Override
        public void onEntityRemoved(short keeperId, ResearchableEntity researchableEntity) {
            for (GameSessionListener l : listeners.getArray()) {
                l.onEntityRemoved(keeperId, researchableEntity);
            }
        }

        @Override
        public void onResearchStatusChanged(short keeperId, ResearchableEntity researchableEntity) {
            for (GameSessionListener l : listeners.getArray()) {
                l.onResearchStatusChanged(keeperId, researchableEntity);
            }
        }
    }
}
