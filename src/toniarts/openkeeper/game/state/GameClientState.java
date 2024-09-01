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
package toniarts.openkeeper.game.state;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.cinematic.events.CinematicEvent;
import com.jme3.cinematic.events.CinematicEventListener;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.PlayerController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.game.data.ResearchableType;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.state.loading.IPlayerLoadingProgress;
import toniarts.openkeeper.game.state.loading.MultiplayerLoadingState;
import toniarts.openkeeper.game.state.loading.SingleBarLoadingState;
import toniarts.openkeeper.game.state.lobby.ClientInfo;
import toniarts.openkeeper.game.state.session.GameSessionClientService;
import toniarts.openkeeper.game.state.session.GameSessionListener;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.TriggerAction;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.view.PlayerCameraState;
import toniarts.openkeeper.view.PlayerEntityViewState;
import toniarts.openkeeper.view.PlayerMapViewState;
import toniarts.openkeeper.view.SystemMessageState;
import toniarts.openkeeper.view.text.TextParser;
import toniarts.openkeeper.view.text.TextParserService;

/**
 * The game client state
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class GameClientState extends AbstractPauseAwareState {
    
    private static final Logger logger = System.getLogger(GameClientState.class.getName());

    private final Main app;

    private AppStateManager stateManager;

    private final KwdFile kwdFile;

    private final Map<Short, Keeper> players = new TreeMap<>();
    private final Map<Short, IPlayerController> playerControllers = new TreeMap<>();
    private final Object loadingObject = new Object();
    private volatile boolean gameStarted = false;

    private final Short playerId;
    private final boolean multiplayer;
    private IPlayerLoadingProgress loadingState;
    private final GameSessionClientService gameClientService;
    private final GameSessionListenerImpl gameSessionListener = new GameSessionListenerImpl();
    private IMapInformation mapInformation;
    private PlayerState playerState;

    private PlayerMapViewState playerMapViewState;
    private PlayerEntityViewState playerModelViewState;
    private TextParser textParser;

    /**
     * Single use game states
     *
     * @param level the level to load
     * @param playerId our player ID
     * @param players players participating in this game
     * @param gameClientService client services
     * @param app the main application
     */
    public GameClientState(KwdFile level, Short playerId, List<ClientInfo> players, GameSessionClientService gameClientService, Main app) {
        this.kwdFile = level;
        this.gameClientService = gameClientService;
        this.playerId = playerId;
        this.app = (Main) app;

        // Set multiplayer
        int humanPlayers = 0;
        for (ClientInfo clientInfo : players) {
            if (!clientInfo.getKeeper().isAi()) {
                humanPlayers++;
                if (humanPlayers > 1) {
                    break;
                }
            }
        }
        multiplayer = (humanPlayers > 1);

        // Create the loading state
        loadingState = createLoadingState(app);

        // Add the listener
        gameClientService.addGameSessionListener(gameSessionListener);

        // Tell that we are ready to start receiving game data
        gameClientService.markReady();
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        this.stateManager = stateManager;

        // Attach the loading state finally
        if (!gameStarted) {
            synchronized (loadingObject) {
                if (!gameStarted) {
                    stateManager.attach(loadingState);
                }
            }
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
    }

    private void detachRelatedAppStates() {
        stateManager.detach(playerModelViewState);
        stateManager.detach(playerMapViewState);
        stateManager.detach(playerState);
    }

    /**
     * If you are getting rid of the game state, use this so that all the
     * related states are detached on the same render loop. Otherwise the app
     * might crash.
     */
    public void detach() {

        // If we are stuck loading
        synchronized (loadingObject) {
            loadingObject.notifyAll();
        }

        stateManager.detach(this);
        detachRelatedAppStates();

        playerState = null;
        playerMapViewState = null;
        playerModelViewState = null;
    }

    @Override
    public void cleanup() {

        // Signal our exit & close all the connections
        gameClientService.exitGame();
        ConnectionState cs = stateManager.getState(ConnectionState.class);
        if (cs != null) {
            cs.disconnect();
        }

        // Detach
        detach();

        super.cleanup();
    }

    @Override
    public boolean isPauseable() {
        return true;
    }

    public boolean isMultiplayer() {
        return multiplayer;
    }

    private IPlayerLoadingProgress createLoadingState(Main app) {

        // Create the appropriate loaging screen
        IPlayerLoadingProgress loader;
        if (isMultiplayer()) {
            loader = new MultiplayerLoadingState(app, "Multiplayer") {

                @Override
                public void onLoad() {
                    onGameLoad();
                }

                @Override
                public void onLoadComplete() {
                    onGameLoadComplete();
                }
            };
        } else {
            loader = new SingleBarLoadingState(app, "Singleplayer") {

                @Override
                public void onLoad() {
                    onGameLoad();
                }

                @Override
                public void onLoadComplete() {
                    onGameLoadComplete();
                }
            };
        }
        return loader;
    }

    private void onGameLoadComplete() {

        // Set initialized
        GameClientState.this.initialized = true;

        // Set the processors
        GameClientState.this.app.setViewProcessors();
    }

    private void onGameLoad() {
        try {

            // The game is actually loaded elsewhere but for the visuals we need this
            if (!gameStarted) {
                synchronized (loadingObject) {
                    if (!gameStarted) {
                        loadingObject.wait();
                    }
                }
            }

        } catch (Exception e) {
            logger.log(Level.ERROR, "Failed to load the game!", e);
        }
    }

    /**
     * Get the level raw data file
     *
     * @return the KWD
     */
    public KwdFile getLevelData() {
        return kwdFile;
    }

    public Keeper getPlayer(short playerId) {
        return players.get(playerId);
    }

    public Keeper getPlayer() {
        return players.get(playerId);
    }

    public Collection<Keeper> getPlayers() {
        return players.values();
    }

    /**
     * Get level variable value
     *
     * @param variable the variable type
     * @return variable value
     */
    public float getLevelVariable(Variable.MiscVariable.MiscType variable) {
        // TODO: player is able to change these, so need a wrapper and store these to GameState
        return kwdFile.getVariables().get(variable).getValue();
    }

    /**
     * Gets a text parser that can fill up the parameters in translations
     *
     * @return a text parser
     */
    public TextParser getTextParser() {
        return textParser;
    }

    private class GameSessionListenerImpl implements GameSessionListener {

        private final Object mapDataLoadingObject = new Object();
        private volatile boolean mapDataLoaded = false;

        @Override
        public void onGameDataLoaded(Collection<Keeper> players) {

            // This might take awhile, don't block
            Thread loadingThread = new Thread(() -> {

                // Now we have the game data, start loading the map
                kwdFile.load();
                AssetUtils.prewarmAssets(kwdFile, app.getAssetManager(), app);
                for (Keeper keeper : players) {
                    keeper.setPlayer(kwdFile.getPlayer(keeper.getId()));
                    GameClientState.this.players.put(keeper.getId(), keeper);
                    GameClientState.this.playerControllers.put(keeper.getId(), new PlayerController(kwdFile, keeper, kwdFile.getImp(), gameClientService.getEntityData(), kwdFile.getVariables()));
                }

                // Create player state
                playerState = new PlayerState(playerId, kwdFile, gameClientService.getEntityData(), false, app);

                playerMapViewState = new PlayerMapViewState(app, kwdFile, app.getAssetManager(), players, gameClientService.getEntityData(), playerId,
                        () -> {
                            synchronized (mapDataLoadingObject) {
                                mapDataLoaded = true;
                                mapDataLoadingObject.notifyAll();
                            }
                        }) {

                    private float lastProgress = 0;

                    @Override
                    protected void updateProgress(float progress) {

                        // Update ourselves
                        onLoadStatusUpdate(progress, playerId);

                        if (progress - lastProgress >= 0.01f) {
                            gameClientService.loadStatus(progress);
                            lastProgress = progress;
                        }
                    }
                };
                mapInformation = playerMapViewState.getMapInformation();
                textParser = new TextParserService(mapInformation);
                playerModelViewState = new PlayerEntityViewState(kwdFile, app.getAssetManager(), gameClientService.getEntityData(), playerId, textParser, app.getRootNode());

                // Attach the states
                stateManager.attach(playerState);
                stateManager.attach(playerMapViewState);
                stateManager.attach(playerModelViewState);

                // Wait until loaded
                if (!mapDataLoaded) {
                    synchronized (mapDataLoadingObject) {
                        if (!mapDataLoaded) {
                            try {
                                mapDataLoadingObject.wait();
                            } catch (InterruptedException ex) {
                                System.getLogger(GameClientState.class.getName()).log(Level.ERROR, "Map data loading interrupted!", ex);
                            }
                        }
                    }
                }

                // Loaded up, send our ready signal on the next frame to ensure all the states are attached
                app.enqueue(() -> {

                    // Prewarm the whole scene
                    app.getRenderManager().preloadScene(app.getRootNode());

                    // Signal our readiness
                    gameClientService.loadComplete();
                });

            }, "GameDataClientLoader");
            loadingThread.start();
        }

        @Override
        public void onLoadComplete(short keeperId) {
            loadingState.setProgress(1f, keeperId);
        }

        @Override
        public void onLoadStatusUpdate(float progress, short keeperId) {
            loadingState.setProgress(progress, keeperId);
        }

        @Override
        public void onGameStarted() {

            // Release loading state from memory
            loadingState = null;

            stateManager.getState(SoundState.class).setKwdFile(kwdFile);
            stateManager.getState(SoundState.class).setEnabled(true);

            app.enqueue(() -> {
                playerState.setEnabled(true);

                // Release the lock and enter to the game phase
                synchronized (loadingObject) {
                    gameStarted = true;
                    loadingObject.notifyAll();
                }
            });
        }

        @Override
        public void onTilesChange(List<Point> updatedTiles) {
            //mapInformation.setTiles(updatedTiles);
            //playerMapViewState.onTilesChange(updatedTiles);
        }

        @Override
        public void onGoldChange(short keeperId, int gold) {
            getPlayer(keeperId).setGold(gold);

            // FIXME: See in what thread we are
            if (playerState != null && playerState.getPlayerId() == keeperId) {
                app.enqueue(() -> {
                    playerState.onGoldChange(keeperId, gold);
                });
            }
        }

        @Override
        public void onManaChange(short keeperId, int mana, int manaLoose, int manaGain) {
            Keeper keeper = getPlayer(keeperId);
            keeper.setMana(mana);
            keeper.setManaGain(manaGain);
            keeper.setManaLoose(manaLoose);

            // FIXME: See in what thread we are
            if (playerState != null && playerState.getPlayerId() == keeperId) {
                app.enqueue(() -> {
                    playerState.onManaChange(keeperId, mana, manaLoose, manaGain);
                });
            }
        }

        @Override
        public void onBuild(short keeperId, List<Point> tiles) {
            playerMapViewState.onBuild(keeperId, tiles);
        }

        @Override
        public void onSold(short keeperId, List<Point> tiles) {
            playerMapViewState.onSold(keeperId, tiles);
        }

        @Override
        public void onGamePaused() {
            app.enqueue(() -> {
                playerState.onPaused(true);
            });
        }

        @Override
        public void onGameResumed() {
            app.enqueue(() -> {
                playerState.onPaused(false);
            });
        }

        @Override
        public void onSetWidescreen(boolean enable) {
            playerState.setWideScreen(enable);
        }

        @Override
        public void onPlaySpeech(int speechId, boolean showText, boolean introduction, int pathId) {

            // TODO: Refactor these, we don't maybe want this logic here, borderline visuals
            stateManager.getState(SoundState.class).attachLevelSpeech(speechId, () -> {
                stateManager.getState(SystemMessageState.class).addMessage(SystemMessageState.MessageType.INFO, String.format("${level.%d}", speechId - 1));
                if (showText) {
                    playerState.setText(speechId, introduction, pathId);
                }
            });
        }

        @Override
        public void onDoTransition(short pathId, Vector3f start) {

            // TODO: Refactor
            stateManager.getState(PlayerCameraState.class).doTransition(pathId, start, new CinematicEventListener() {
                @Override
                public void onPlay(CinematicEvent cinematic) {

                }

                @Override
                public void onPause(CinematicEvent cinematic) {

                }

                @Override
                public void onStop(CinematicEvent cinematic) {
                    gameClientService.transitionEnd();
                }
            });
        }

        @Override
        public void onFlashButton(TriggerAction.MakeType buttonType, short targetId, TriggerAction.ButtonType targetButtonType, boolean enabled, int time) {
            playerState.flashButton(buttonType, targetId, targetButtonType, enabled, time);
        }

        @Override
        public void onRotateViewAroundPoint(Vector3f point, boolean relative, int angle, int time) {
            playerState.rotateViewAroundPoint(point, relative, angle, time);
        }

        @Override
        public void onShowMessage(int textId) {
            playerState.showMessage(textId);
        }

        @Override
        public void onZoomViewToPoint(Vector3f point) {
            playerState.zoomToPoint(point);
        }

        @Override
        public void onTileFlash(List<Point> points, boolean enabled, short keeperId) {
            playerMapViewState.onTileFlash(points, enabled, keeperId);
        }

        @Override
        public void onZoomViewToEntity(EntityId entityId) {
            playerState.zoomToEntity(entityId, true);
        }

        @Override
        public void onShowUnitFlower(EntityId entityId, int interval) {
            playerModelViewState.showUnitFlower(entityId, interval);
        }

        @Override
        public void onEntityAdded(short keeperId, ResearchableEntity researchableEntity) {
            setResearchableEntity(keeperId, researchableEntity, () -> {
                playerState.onEntityAdded(keeperId, researchableEntity);
                });
        }

        private void setResearchableEntity(short keeperId, ResearchableEntity researchableEntity, Runnable notifier) {
            Keeper keeper = getPlayer(keeperId);
            setResearchableEntity(researchableEntity, getResearchableEntitiesList(keeper, researchableEntity));

            // FIXME: See in what thread we are
            if (notifier != null && playerState != null && playerState.getPlayerId() == keeperId) {
                app.enqueue(notifier);
            }
        }

        @Override
        public void onEntityRemoved(short keeperId, ResearchableEntity researchableEntity) {
            setResearchableEntity(keeperId, researchableEntity, () -> {
                playerState.onEntityRemoved(keeperId, researchableEntity);
                });
        }

        @Override
        public void onResearchStatusChanged(short keeperId, ResearchableEntity researchableEntity) {
            setResearchableEntity(keeperId, researchableEntity, () -> {
                playerState.onResearchStatusChanged(keeperId, researchableEntity);
                });
        }

        private List<ResearchableEntity> getResearchableEntitiesList(Keeper keeper, ResearchableEntity researchableEntity) {
            List<ResearchableEntity> researchableEntities = null;
            switch (researchableEntity.getResearchableType()) {
                case SPELL: {
                    researchableEntities = keeper.getAvailableSpells();
                    break;
                }
                case DOOR: {
                    researchableEntities = keeper.getAvailableDoors();
                    break;
                }
                case ROOM: {
                    researchableEntities = keeper.getAvailableRooms();
                    break;
                }
                case TRAP: {
                    researchableEntities = keeper.getAvailableTraps();
                    break;
                }
            }

            return researchableEntities;
        }

        private <T extends ResearchableEntity> void setResearchableEntity(T researchableEntity, List<T> researchableEntities) {
            int index = Collections.binarySearch(researchableEntities, researchableEntity, (ResearchableEntity o1, ResearchableEntity o2) -> {
                return getResearchableEntityType(kwdFile, o1.getResearchableType(), o1.getId()).compareTo(getResearchableEntityType(kwdFile, o2.getResearchableType(), o2.getId()));
            });
            if (index < 0) {
                researchableEntities.add(~index, researchableEntity);
            } else if (index >= 0) {
                researchableEntities.set(index, researchableEntity);
            }
        }

        private Comparable getResearchableEntityType(KwdFile kwdFile, ResearchableType researchableType, short typeId) {
            switch (researchableType) {
                case DOOR: {
                    return kwdFile.getDoorById(typeId);
                }
                case ROOM: {
                    return kwdFile.getRoomById(typeId);
                }
                case SPELL: {
                    return kwdFile.getKeeperSpellById(typeId);
                }
                case TRAP: {
                    return kwdFile.getTrapById(typeId);
                }
            }

            return null;
        }

        @Override
        public void setPossession(EntityId target) {
            playerState.setPossession(target);
        }
    }

    public IMapInformation getMapClientService() {
        return mapInformation;
    }

    public GameSessionClientService getGameClientService() {
        return gameClientService;
    }

}
