/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.openkeeper.game.state.session;

import com.jme3.app.state.AppStateManager;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.util.SafeArrayList;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import toniarts.openkeeper.utils.Point;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.game.state.CheatState;
import toniarts.openkeeper.game.state.GameClientState;
import toniarts.openkeeper.game.state.GameServerState;
import toniarts.openkeeper.game.state.lobby.ClientInfo;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.TriggerAction;
import toniarts.openkeeper.utils.PathUtils;
import toniarts.openkeeper.utils.Utils;

/**
 * Local game session, a virtual server
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class LocalGameSession implements GameSessionServerService, GameSessionClientService {
    
    private static final Logger logger = System.getLogger(LocalGameSession.class.getName());
    private static final short PLAYER_ID = Player.KEEPER1_ID;

    private boolean playerInTransition = false;
    private final EntityData entityData = new DefaultEntityData();
    private final SafeArrayList<GameSessionListener> listeners = new SafeArrayList<>(GameSessionListener.class);
    private final SafeArrayList<GameSessionServiceListener> serverListeners = new SafeArrayList<>(GameSessionServiceListener.class);

    public LocalGameSession() {

    }

    /**
     * Creates and starts a local game session with given level and default
     * players
     *
     * @param kwdFile map as KWD file
     * @param campaign whether to start this level as a campaign level
     * @param stateManager state manager instance for setting up the game
     */
    public static void createLocalGame(KwdFile kwdFile, boolean campaign, AppStateManager stateManager, Main app) {
        createLocalGame(kwdFile, stateManager, campaign, app);
    }

    /**
     * Creates and starts a local game session with given level and default
     * players
     *
     * @param level the level to load
     * @param campaign whether to start this level as a campaign level
     * @param stateManager state manager instance for setting up the game
     * @param app the main app
     * @throws java.io.IOException Problem with the map file
     */
    public static void createLocalGame(String level, boolean campaign, AppStateManager stateManager, Main app) throws IOException {

        // Try to load the file
        Path mapFile = Paths.get(PathUtils.getRealFileName(Main.getDkIIFolder() + PathUtils.DKII_MAPS_FOLDER, level + ".kwd"));
        if (!Files.exists(mapFile)) {
            throw new FileNotFoundException(mapFile.toString());
        }
        KwdFile kwdFile = new KwdFile(Main.getDkIIFolder(), mapFile);

        createLocalGame(kwdFile, stateManager, campaign, app);
    }

    private static void createLocalGame(KwdFile kwdFile, AppStateManager stateManager, boolean campaign, Main app) {

        // Player and server
        LocalGameSession gameSession = new LocalGameSession();
        Keeper keeper = new Keeper(false, PLAYER_ID);
        ClientInfo clientInfo = new ClientInfo(0, null, 0);
        clientInfo.setName(Utils.getMainTextResourceBundle().getString("58"));
        clientInfo.setKeeper(keeper);
        clientInfo.setReady(true);

        // The client
        GameClientState gameClientState = new GameClientState(kwdFile, PLAYER_ID, List.of(clientInfo), gameSession, app);
        stateManager.attach(gameClientState);

        // The game server
        GameServerState gameServerState = new GameServerState(kwdFile, campaign ? null : List.of(keeper), campaign, gameSession);
        stateManager.attach(gameServerState);
    }

    @Override
    public EntityData getEntityData() {
        return entityData;
    }

    @Override
    public void sendGameData(Collection<Keeper> players) {
        BinaryExporter exporter = BinaryExporter.getInstance();
        BinaryImporter importer = BinaryImporter.getInstance();

        // Clone the data so it really is different as in normal multiplayer it is
        for (GameSessionListener listener : listeners.getArray()) {
            List<Keeper> copiedPlayers = new ArrayList<>(players.size());
            for (Keeper player : players) {
                try (ByteArrayOutputStream playerStream = new ByteArrayOutputStream()) {
                    exporter.save(player, playerStream);
                    copiedPlayers.add((Keeper) importer.load(playerStream.toByteArray()));
                } catch (IOException ex) {
                    logger.log(Level.ERROR, "Failed to serialize the players!", ex);
                }
            }

            listener.onGameDataLoaded(copiedPlayers);
        }
    }

    @Override
    public void startGame() {
        for (GameSessionListener listener : listeners.getArray()) {
            listener.onGameStarted();
        }

        // Start the simulation
        for (GameSessionServiceListener listener : serverListeners.getArray()) {
            listener.onStartGame();
        }
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
    public void setWidescreen(boolean enable, short playerId) {
        if (playerId == PLAYER_ID) {
            for (GameSessionListener listener : listeners.getArray()) {
                listener.onSetWidescreen(enable);
            }
        }
    }

    @Override
    public void playSpeech(int speechId, boolean showText, boolean introduction, int pathId, short playerId) {
        if (playerId == PLAYER_ID) {
            for (GameSessionListener listener : listeners.getArray()) {
                listener.onPlaySpeech(speechId, showText, introduction, pathId);
            }
        }
    }

    @Override
    public void doTransition(short pathId, Vector3f start, short playerId) {
        if (playerId == PLAYER_ID) {
            playerInTransition = true;
            for (GameSessionListener listener : listeners.getArray()) {
                listener.onDoTransition(pathId, start);
            }
        }
    }

    @Override
    public void flashButton(TriggerAction.MakeType buttonType, short targetId, TriggerAction.ButtonType targetButtonType, boolean enabled, int time, short playerId) {
        if (playerId == PLAYER_ID) {
            for (GameSessionListener listener : listeners.getArray()) {
                listener.onFlashButton(buttonType, targetId, targetButtonType, enabled, time);
            }
        }
    }

    @Override
    public void rotateViewAroundPoint(Vector3f point, boolean relative, int angle, int time, short playerId) {
        if (playerId == PLAYER_ID) {
            for (GameSessionListener listener : listeners.getArray()) {
                listener.onRotateViewAroundPoint(point, relative, angle, time);
            }
        }
    }

    @Override
    public void showMessage(int textId, short playerId) {
        if (playerId == PLAYER_ID) {
            for (GameSessionListener listener : listeners.getArray()) {
                listener.onShowMessage(textId);
            }
        }
    }

    @Override
    public void zoomViewToPoint(Vector3f point, short playerId) {
        if (playerId == PLAYER_ID) {
            for (GameSessionListener listener : listeners.getArray()) {
                listener.onZoomViewToPoint(point);
            }
        }
    }

    @Override
    public void zoomViewToEntity(EntityId entityId, short playerId) {
        if (playerId == PLAYER_ID) {
            for (GameSessionListener listener : listeners.getArray()) {
                listener.onZoomViewToEntity(entityId);
            }
        }
    }

    @Override
    public void showUnitFlower(EntityId entityId, int interval, short playerId) {
        if (playerId == PLAYER_ID) {
            for (GameSessionListener listener : listeners.getArray()) {
                listener.onShowUnitFlower(entityId, interval);
            }
        }
    }

    @Override
    public void setGamePaused(boolean paused) {
        for (GameSessionListener listener : listeners.getArray()) {
            if (paused) {
                listener.onGamePaused();
            } else {
                listener.onGameResumed();
            }
        }
    }

    @Override
    public void flashTiles(List<Point> points, boolean enabled, short playerId) {
        if (playerId == PLAYER_ID) {
            for (GameSessionListener listener : listeners.getArray()) {
                listener.onTileFlash(points, enabled, playerId);
            }
        }
    }

    @Override
    public void loadComplete() {
        for (GameSessionListener listener : listeners.getArray()) {
            listener.onLoadComplete(PLAYER_ID);
        }

        // Only one player, start the game once everything ready
        startGame();
    }

    @Override
    public void loadStatus(float progress) {
//        for (GameSessionListener listener : listeners.getArray()) {
//            listener.onLoadStatusUpdate(progress, PLAYER_ID);
//        }
    }

    @Override
    public void selectTiles(Vector2f start, Vector2f end, boolean select) {
        for (GameSessionServiceListener listener : serverListeners.getArray()) {
            listener.onSelectTiles(start, end, select, PLAYER_ID);
        }
    }

    @Override
    public void build(Vector2f start, Vector2f end, short roomId) {
        for (GameSessionServiceListener listener : serverListeners.getArray()) {
            listener.onBuild(start, end, roomId, PLAYER_ID);
        }
    }

    @Override
    public void sell(Vector2f start, Vector2f end) {
        for (GameSessionServiceListener listener : serverListeners.getArray()) {
            listener.onSell(start, end, PLAYER_ID);
        }
    }

    @Override
    public void markReady() {
        // We don't care really, locally if the client is started before the server, everything is fine
    }

    @Override
    public void interact(EntityId entity) {
        for (GameSessionServiceListener listener : serverListeners.getArray()) {
            listener.onInteract(entity, PLAYER_ID);
        }
    }

    @Override
    public void pickUp(EntityId entity) {
        for (GameSessionServiceListener listener : serverListeners.getArray()) {
            listener.onPickUp(entity, PLAYER_ID);
        }
    }

    @Override
    public void drop(EntityId entity, Point tile, Vector2f coordinates, EntityId dropOnEntity) {
        for (GameSessionServiceListener listener : serverListeners.getArray()) {
            listener.onDrop(entity, tile, coordinates, dropOnEntity, PLAYER_ID);
        }
    }

    @Override
    public void getGold(int amount) {
        for (GameSessionServiceListener listener : serverListeners.getArray()) {
            listener.onGetGold(amount, PLAYER_ID);
        }
    }

    @Override
    public void transitionEnd() {
        playerInTransition = false;
        for (GameSessionServiceListener listener : serverListeners.getArray()) {
            listener.onTransitionEnd(PLAYER_ID);
        }
    }

    @Override
    public boolean isInTransition() {
        return playerInTransition;
    }

    @Override
    public void pauseGame() {
        for (GameSessionServiceListener listener : serverListeners.getArray()) {
            listener.onPauseRequest(PLAYER_ID);
        }
    }

    @Override
    public void resumeGame() {
        for (GameSessionServiceListener listener : serverListeners.getArray()) {
            listener.onResumeRequest(PLAYER_ID);
        }
    }

    @Override
    public void exitGame() {
        for (GameSessionServiceListener listener : serverListeners.getArray()) {
            listener.onExitGame(PLAYER_ID);
        }
    }

    @Override
    public void triggerCheat(CheatState.CheatType cheat) {
        for (GameSessionServiceListener listener : serverListeners.getArray()) {
            listener.onCheatTriggered(cheat, PLAYER_ID);
        }
    }

    @Override
    public void castKeeperSpell(short keeperSpellId, EntityId target, Point tile, Vector2f position) {
        for (GameSessionServiceListener listener : serverListeners.getArray()) {
            listener.onCastKeeperSpell(keeperSpellId, target, tile, position, PLAYER_ID);
        }
    }

    @Override
    public void placeDoor(short doorId, Point tile) {
        for (GameSessionServiceListener listener : serverListeners.getArray()) {
            listener.onPlaceDoor(doorId, tile, PLAYER_ID);
        }
    }

    @Override
    public void placeTrap(short trapId, Point tile) {
        for (GameSessionServiceListener listener : serverListeners.getArray()) {
            listener.onPlaceTrap(trapId, tile, PLAYER_ID);
        }
    }

    @Override
    public void updateTiles(List<Point> updatedTiles) {
        for (GameSessionListener listener : listeners.getArray()) {
            listener.onTilesChange(updatedTiles);
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
    public void onGoldChange(short keeperId, int gold) {
        for (GameSessionListener listener : listeners.getArray()) {
            listener.onGoldChange(keeperId, gold);
        }
    }

    @Override
    public void onManaChange(short keeperId, int mana, int manaLoose, int manaGain) {
        for (GameSessionListener listener : listeners.getArray()) {
            listener.onManaChange(keeperId, mana, manaLoose, manaGain);
        }
    }

    @Override
    public void onBuild(short keeperId, List<Point> tiles) {
        for (GameSessionListener listener : listeners.getArray()) {
            listener.onBuild(keeperId, tiles);
        }
    }

    @Override
    public void onSold(short keeperId, List<Point> tiles) {
        for (GameSessionListener listener : listeners.getArray()) {
            listener.onSold(keeperId, tiles);
        }
    }

    @Override
    public void onEntityAdded(short keeperId, ResearchableEntity researchableEntity) {
        for (GameSessionListener listener : listeners.getArray()) {
            listener.onEntityAdded(keeperId, researchableEntity);
        }
    }

    @Override
    public void onEntityRemoved(short keeperId, ResearchableEntity researchableEntity) {
        for (GameSessionListener listener : listeners.getArray()) {
            listener.onEntityRemoved(keeperId, researchableEntity);
        }
    }

    @Override
    public void onResearchStatusChanged(short keeperId, ResearchableEntity researchableEntity) {
        for (GameSessionListener listener : listeners.getArray()) {
            listener.onResearchStatusChanged(keeperId, researchableEntity);
        }
    }

    @Override
    public void setPossession(EntityId target, short playerId) {
        if (playerId == PLAYER_ID) {
            for (GameSessionListener listener : listeners.getArray()) {
                listener.setPossession(target);
            }
        }
    }

}
