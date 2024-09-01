/*
 * Copyright (C) 2014-2015 OpenKeeper
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
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import de.lessvoid.nifty.Nifty;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.console.ConsoleState;
import toniarts.openkeeper.game.controller.player.PlayerCreatureControl;
import toniarts.openkeeper.game.controller.player.PlayerGoldControl;
import toniarts.openkeeper.game.controller.player.PlayerRoomControl;
import toniarts.openkeeper.game.controller.player.PlayerSpellControl;
import toniarts.openkeeper.game.controller.player.PlayerStatsControl;
import toniarts.openkeeper.game.data.GameResult;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.game.listener.PlayerListener;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.TriggerAction;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.PlayerCameraState;
import toniarts.openkeeper.view.PlayerInteractionState;
import toniarts.openkeeper.view.PlayerInteractionState.InteractionState;
import toniarts.openkeeper.view.PossessionCameraState;
import toniarts.openkeeper.view.PossessionInteractionState;
import toniarts.openkeeper.view.SystemMessageState;
import toniarts.openkeeper.view.control.EntityViewControl;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.room.GenericRoom;
import toniarts.openkeeper.world.room.RoomInstance;

/**
 * The player state! GUI, camera, etc. Player interactions
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class PlayerState extends AbstractAppState implements PlayerListener {

    protected final Main app;

    protected AssetManager assetManager;
    protected AppStateManager stateManager;

    private final short playerId;
    private final KwdFile kwdFile;
    private final EntityData entityData;

    private boolean paused = false;

    private final List<AbstractPauseAwareState> appStates = new ArrayList<>();
    protected PlayerInteractionState interactionState;
    private PossessionInteractionState possessionState;
    protected PlayerCameraState cameraState;
    private PossessionCameraState possessionCameraState;

    private boolean transitionEnd = true;
    private PlayerScreenController screen;

    public PlayerState(int playerId, KwdFile kwdFile, EntityData entityData, boolean enabled, Main app) {
        this.playerId = (short) playerId;
        this.kwdFile = kwdFile;
        this.entityData = entityData;
        this.app = app;

        screen = new PlayerScreenController(this, app.getNifty());
        app.getNifty().registerScreenController(screen);
        app.getNifty().addXml(new ByteArrayInputStream(app.getGameUiXml()));

        super.setEnabled(enabled);
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);

        assetManager = this.app.getAssetManager();
        this.stateManager = stateManager;
    }

    @Override
    public void cleanup() {

        // Detach states
        for (AbstractAppState state : appStates) {
            stateManager.detach(state);
        }
        appStates.clear();
        interactionState = null;
        possessionState = null;
        cameraState = null;
        possessionCameraState = null;


        // Disassemble Nifty
        screen.cleanup();
        Nifty nifty = app.getNifty();
        nifty.unregisterScreenController(screen);
        for (String screenId : nifty.getAllScreensName()) {
            if (screen.equals(nifty.getScreen(screenId).getScreenController())) {
                nifty.removeScreen(screenId);
            }
        }
        screen = null;

        super.cleanup();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        if (!isInitialized()) {
            return;
        }

        if (enabled) {

            // Get the game state
            final GameClientState gameState = stateManager.getState(GameClientState.class);
            screen.initHud(gameState.getLevelData().getGameLevel().getTextTableId().getLevelDictFile(), entityData);

            // Cursor
            app.getInputManager().setCursorVisible(true);

            // Set the pause state
            paused = false;
            screen.setPause(paused);

            appStates.add(new ConsoleState());

            // Create app states
            Player player = gameState.getLevelData().getPlayer(playerId);

            possessionCameraState = new PossessionCameraState(false);
            possessionState = new PossessionInteractionState(false) {
                @Override
                protected void onExit() {
                    // Enable states
                    for (AbstractAppState state : appStates) {
                        if (state instanceof PossessionInteractionState
                                || state instanceof PossessionCameraState) {
                            continue;
                        }
                        state.setEnabled(true);
                    }

                    screen.goToScreen(PlayerScreenController.SCREEN_HUD_ID);
                }

                @Override
                protected void onActionChange(PossessionInteractionState.Action action) {
                    PlayerState.this.screen.updatePossessionSelectedItem(action);
                }
            };
            appStates.add(possessionCameraState);
            appStates.add(possessionState);

            cameraState = new PlayerCameraState(player);
            interactionState = new PlayerInteractionState(player, kwdFile, entityData, gameState.getMapClientService(), gameState.getTextParser()) {
                @Override
                protected void onInteractionStateChange(InteractionState interactionState) {
                    PlayerState.this.screen.updateSelectedItem(interactionState);
                }

                @Override
                protected void onPossession(EntityId entityId) {
                    // Disable states
                    for (AbstractAppState state : appStates) {
                        if (state instanceof PossessionInteractionState
                                || state instanceof PossessionCameraState) {
                            continue;
                        }
                        state.setEnabled(false);
                    }
                    // Enable state
                    possessionState.setTarget(entityId);
                    possessionState.setEnabled(true);

                    screen.goToScreen(PlayerScreenController.SCREEN_POSSESSION_ID);
                }
            };
            appStates.add(cameraState);
            appStates.add(interactionState);
            // FIXME add to appStates or directly to stateManager
            appStates.add(new SystemMessageState(screen.getMessages()));
            // Load the state
            for (AbstractAppState state : appStates) {
                stateManager.attach(state);
            }
        } else {

            // Detach states
            for (AbstractAppState state : appStates) {
                stateManager.detach(state);
            }

            appStates.clear();
            screen.cleanup();
            screen.goToScreen(PlayerScreenController.SCREEN_EMPTY_ID);
        }
    }

    @Override
    public void update(float tpf) {
        screen.update(tpf);
    }

    public KwdFile getKwdFile() {
        return kwdFile;
    }

    public EntityData getEntityData() {
        return entityData;
    }

    public PlayerScreenController getScreen() {
        return screen;
    }

    /**
     * Get this player
     *
     * @return this player
     */
    public Keeper getPlayer() {
        GameClientState gs = stateManager.getState(GameClientState.class);
        if (gs != null) {
            return gs.getPlayer(playerId);
        }
        return null;
    }

    public PlayerStatsControl getStatsControl() {
        Keeper keeper = getPlayer();
//        if (keeper != null) {
//            return keeper.getStatsControl();
//        }
        return null;
    }

    public PlayerGoldControl getGoldControl() {
        Keeper keeper = getPlayer();
//        if (keeper != null) {
//            return keeper.getGoldControl();
//        }
        return null;
    }

    public PlayerCreatureControl getCreatureControl() {
        Keeper keeper = getPlayer();
//        if (keeper != null) {
//            return keeper.getCreatureControl();
//        }
        return null;
    }

    public PlayerRoomControl getRoomControl() {
        Keeper keeper = getPlayer();
//        if (keeper != null) {
//            return keeper.getRoomControl();
//        }
        return null;
    }

    public PlayerSpellControl getSpellControl() {
        Keeper keeper = getPlayer();
//        if (keeper != null) {
//            return keeper.getSpellControl();
//        }
        return null;
    }

    public void setTransitionEnd(boolean value) {
        transitionEnd = value;
    }

    public boolean isTransitionEnd() {
        return transitionEnd;
    }

    public void setWideScreen(boolean enable) {
        if (interactionState.isInitialized()) {
            interactionState.setEnabled(!enable);
        }

        if (enable) {
            screen.goToScreen(PlayerScreenController.SCREEN_CINEMATIC_ID);
        } else {
            screen.goToScreen(PlayerScreenController.SCREEN_HUD_ID);
        }
    }

    public void setText(int textId, boolean introduction, int pathId) {
        screen.setCinematicText(textId, introduction, pathId);
    }

    public void setGeneralText(int textId) {
        screen.setCinematicText(textId);
    }

    public void flashButton(TriggerAction.MakeType buttonType, short targetId, TriggerAction.ButtonType targetButtonType, boolean enabled, int time) {
        // TODO make flash button
    }

    protected List<ResearchableEntity> getAvailableRoomsToBuild() {

        // TODO: cache, or something, maybe add the listeners here
        Keeper keeper = getPlayer();
        return keeper.getAvailableRooms();
    }

    protected List<ResearchableEntity> getAvailableKeeperSpells() {

        // TODO: cache, or something, maybe add the listeners here
        Keeper keeper = getPlayer();
        return keeper.getAvailableSpells();
    }

    protected List<ResearchableEntity> getAvailableDoors() {
        // TODO: cache, or something, maybe add the listeners here
        Keeper keeper = getPlayer();
        return keeper.getAvailableDoors();
    }

    protected List<ResearchableEntity> getAvailableTraps() {
        // TODO: cache, or something, maybe add the listeners here
        Keeper keeper = getPlayer();
        return keeper.getAvailableTraps();
    }

    public void setPaused(boolean paused) {

        // Pause / unpause
        // TODO: We should give the client to these states to self register to the client service listener...
        // But PlayerState is persistent.. I don't want to have the reference here
        if (paused) {
            stateManager.getState(GameClientState.class).getGameClientService().pauseGame();
        } else {
            stateManager.getState(GameClientState.class).getGameClientService().resumeGame();
        }
    }

    public boolean isPaused() {
        return paused;
    }

    public void onPaused(boolean paused) {

        // Pause state
        this.paused = paused;

        // Delegate to the screen
        screen.onPaused(paused);

        for (AbstractPauseAwareState state : appStates) {
            if (state.isPauseable()) {
                state.setEnabled(!paused);
            }
        }
    }

    public void quitToMainMenu() {

        // Detach game and enable main menu
        stateManager.getState(GameClientState.class).detach();
        stateManager.detach(this);
        stateManager.getState(MainMenuState.class).setEnabled(true);
    }

    public void quitToDebriefing() {
        // TODO copy results of game from GameState
        GameState gs = stateManager.getState(GameState.class);
        GameResult result = gs.getGameResult();
        gs.detach();
        setEnabled(false);
        stateManager.getState(MainMenuState.class).doDebriefing(result);
    }

    public void quitToOS() {
        app.stop();
    }

    public void zoomToDungeon() {
        cameraState.setCameraLookAt(stateManager.getState(GameClientState.class).getPlayer(playerId).getDungeonHeartLocation());
    }

    /**
     * Zoom to entity
     *
     * @param entityId the entity to zoom to
     * @param animate whether to animate the transition
     */
    public void zoomToEntity(EntityId entityId, boolean animate) {
        Position position = entityData.getComponent(entityId, Position.class);
        if (position != null) {
            zoomToPosition(position.position, animate);
        }
    }

    /**
     * Zoom to position
     *
     * @param position the position to zoom to
     * @param animate whether to animate the transition
     */
    public void zoomToPosition(Vector3f position, boolean animate) {
        if (animate) {
            cameraState.zoomToPoint(position);
        } else {
            cameraState.setCameraLookAt(position);
        }
    }

    /**
     * Pick up an entity
     *
     * @param entityId entity
     */
    public void pickUpEntity(EntityId entityId) {

        // TODO: We should really have some sort of static etc. service that we can just ask these for entityId, not to pass a view controller
        // They can be used with convenience methods from the view controller yes, but so that all will have access, shared with game logic, so the rules are the same (canPickup etc.)
        interactionState.pickupObject(new EntityViewControl(entityId, entityData, app, null, assetManager, null) {
            @Override
            protected ArtResource getAnimationData(Object state) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });
    }

    public short getPlayerId() {
        return playerId;
    }

    protected Creature getPossessionCreature() {
        return null/*possessionState.getTarget().getCreature()*/;
    }

    protected InteractionState getInteractionState() {
        return interactionState.getInteractionState();
    }

    public void rotateViewAroundPoint(Vector3f point, boolean relative, int angle, int time) {
        cameraState.rotateAroundPoint(point, relative, angle, time);
    }

    public void showMessage(int textId) {
        stateManager.getState(SystemMessageState.class).addMessage(SystemMessageState.MessageType.INFO, String.format("${level.%d}", textId));
    }

    public void zoomToPoint(Vector3f point) {
        cameraState.zoomToPoint(point);
    }

    protected void grabGold(int amount) {
        if (!interactionState.isKeeperHandFull()) {
            stateManager.getState(GameClientState.class).getGameClientService().getGold(amount);
        }
    }

    /**
     * End the game for the player
     *
     * @param win did we win or not
     */
    protected void endGame(boolean win) {

        // Disable us to get rid of all interaction
        setEnabled(false);
        stateManager.attach(new EndGameState(this));
    }

    String getTooltipText(String text) {
        int dungeonHealth = 0;
        int paydayCost = 0;
        MapLoader mapLoader = stateManager.getState(WorldState.class).getMapLoader();
        for (Map.Entry<RoomInstance, GenericRoom> en : mapLoader.getRoomActuals().entrySet()) {
            RoomInstance key = en.getKey();
            GenericRoom value = en.getValue();
            if (value.isDungeonHeart() && key.getOwnerId() == playerId) {
                dungeonHealth = key.getHealthPercentage();
                break;
            }
        }
        // TODO payday cost calculator
        return text.replaceAll("%19%", String.valueOf(dungeonHealth))
                .replaceAll("%20", String.valueOf(paydayCost));
    }

    @Override
    public void onGoldChange(short keeperId, int gold) {
        screen.setGold(gold);
    }

    @Override
    public void onManaChange(short keeperId, int mana, int manaLoose, int manaGain) {
        screen.setMana(mana, manaLoose, manaGain);
    }

    @Override
    public void onBuild(short keeperId, List<Point> tiles) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onSold(short keeperId, List<Point> tiles) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onEntityAdded(short keeperId, ResearchableEntity researchableEntity) {
        populateTab(researchableEntity);
    }

    private void populateTab(ResearchableEntity researchableEntity) {
        switch (researchableEntity.getResearchableType()) {
            case SPELL: {
                screen.populateSpellTab();
                break;
            }
            case TRAP:
            case DOOR: {
                screen.populateManufactureTab();
                break;
            }
            case ROOM: {
                screen.populateRoomTab();
                break;
            }
        }
    }

    @Override
    public void onEntityRemoved(short keeperId, ResearchableEntity researchableEntity) {
        populateTab(researchableEntity);
    }

    @Override
    public void onResearchStatusChanged(short keeperId, ResearchableEntity researchableEntity) {
        screen.updateEntityResearch(researchableEntity);
    }

    void setPossession(EntityId target) {

    }

}
