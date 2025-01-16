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
package toniarts.openkeeper.view;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.collision.CollisionResults;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import de.lessvoid.nifty.controls.Label;
import de.lessvoid.nifty.elements.Element;
import toniarts.openkeeper.utils.Point;
import java.util.HashSet;
import java.util.Set;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.console.ConsoleState;
import toniarts.openkeeper.game.controller.KeeperSpellCastValidator;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.state.AbstractPauseAwareState;
import toniarts.openkeeper.game.state.CheatState;
import toniarts.openkeeper.game.state.GameClientState;
import toniarts.openkeeper.game.state.PlayerScreenController;
import toniarts.openkeeper.game.state.PlayerState;
import toniarts.openkeeper.gui.CursorFactory;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.PlayerInteractionState.InteractionState;
import toniarts.openkeeper.view.PlayerInteractionState.InteractionState.Type;
import toniarts.openkeeper.view.control.IEntityViewControl;
import toniarts.openkeeper.view.selection.SelectionArea;
import toniarts.openkeeper.view.selection.SelectionHandler;
import toniarts.openkeeper.view.text.TextParser;

/**
 * State for managing player interactions in the world. Heavily drawn from
 * Philip Willuweit's AgentKeeper code <p.willuweit@gmx.de>.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 * @author ArchDemon
 */
// TODO: States, now only selection
public abstract class PlayerInteractionState extends AbstractPauseAwareState {

    private static final int SPELL_POSSESSION_ID = 2;
    private static final float CURSOR_UPDATE_INTERVAL = 0.25f;

    private Main app;
    private GameClientState gameClientState;
    private AssetManager assetManager;
    private AppStateManager stateManager;
    private InputManager inputManager;

    private final Player player;
    private final KwdFile kwdFile;
    private final EntityData entityData;
    private final IMapInformation mapInformation;
    private final TextParser textParser;
    private SelectionHandler selectionHandler;
    private final Vector2f mousePosition = new Vector2f(Vector2f.ZERO);
    private final InteractionState interactionState = new InteractionState();
    private float timeFromLastUpdate = CURSOR_UPDATE_INTERVAL;
    private Element view;

    private boolean isOnGui = false;
    private boolean isTaggable = false;
    private boolean isOnMap = false;
    private boolean isInteractable = false;

    private RawInputListener inputListener;
    private boolean inputListenerAdded = false;
    private final Set<Integer> keys = new HashSet<>();
    private IEntityViewControl interactiveControl;
    private Label tooltip;
    private KeeperHandState keeperHandState;
    private PlayerEntityViewState playerEntityViewState;

    public PlayerInteractionState(Player player, KwdFile kwdFile, EntityData entityData,
            IMapInformation mapInformation, TextParser textParser) {
        this.player = player;
        this.kwdFile = kwdFile;
        this.entityData = entityData;
        this.mapInformation = mapInformation;
        this.textParser = textParser;

        // The input
        initializeInput();
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);
        this.app = (Main) app;
        assetManager = this.app.getAssetManager();
        this.stateManager = this.app.getStateManager();
        inputManager = this.app.getInputManager();
        gameClientState = this.stateManager.getState(GameClientState.class);

        PlayerScreenController psc = this.stateManager.getState(PlayerState.class).getScreen();
        this.view = psc.getGuiConstraint();
        this.tooltip = psc.getTooltip();

        // Init the keeper hand
        keeperHandState = new KeeperHandState((int) gameClientState.getLevelVariable(MiscType.MAX_NUMBER_OF_THINGS_IN_HAND), kwdFile, entityData, player.getPlayerId()) {

            @Override
            protected void updateCursor() {
                PlayerInteractionState.this.updateCursor();
            }

        };
        this.stateManager.attach(keeperHandState);

        // Init handler
        selectionHandler = new SelectionHandler(this.app) {
            @Override
            public boolean isVisible() {
                if (isTaggable || selectionHandler.isActive()) {
                    return true;
                }

                if (!isOnMap) {
                    return false;
                }

                switch (interactionState.getType()) {
                    case NONE:
                        return (keeperHandState.getItem() != null);
                    case SELL:
                    case ROOM:
                    case DOOR:
                    case TRAP:
                        return true;
                }

                return false;
            }

            @Override
            protected SelectionHandler.ColorIndicator getColorIndicator() {
                Point p;
                if (selectionHandler.isActive()) {
                    p = WorldUtils.vectorToPoint(selectionHandler.getSelectionArea().getRealStart());
                } else {
                    p = WorldUtils.vectorToPoint(selectionHandler.getPointedTilePosition());
                }
                if (interactionState.getType() == Type.NONE && keeperHandState.getItem() != null) {
                    IMapTileInformation tile = gameClientState.getMapClientService().getMapData().getTile(p);
                    if (tile != null) {
                        IEntityViewControl.DroppableStatus status = keeperHandState.getItem().getDroppableStatus(tile, gameClientState.getMapClientService().getTerrain(tile), player.getPlayerId());
                        return (status != IEntityViewControl.DroppableStatus.NOT_DROPPABLE ? ColorIndicator.BLUE : ColorIndicator.RED);
                    }
                    return ColorIndicator.RED;
                }
                if (interactionState.getType() == Type.SELL) {
                    return ColorIndicator.RED;
                } else if (interactionState.getType() == Type.ROOM
                        && !(gameClientState.getMapClientService().isTaggable(p)
                        || (gameClientState.getMapClientService().isBuildable(p, player.getPlayerId(), (short) interactionState.getItemId())
                        && isPlayerAffordToBuild(player, gameClientState.getLevelData().getRoomById(interactionState.getItemId()))))) {
                    return ColorIndicator.RED;
                }
                return ColorIndicator.BLUE;
            }

            private boolean isPlayerAffordToBuild(Player player, Room room) {
                int playerMoney = gameClientState.getPlayer(player.getPlayerId()).getGold();
                if (playerMoney == 0) {
                    return false;
                }
                int buildablePlots = 0;
                for (int x = (int) Math.max(0, selectionHandler.getSelectionArea().getStart().x); x < Math.min(gameClientState.getMapClientService().getMapData().getWidth(), selectionHandler.getSelectionArea().getEnd().x + 1); x++) {
                    for (int y = (int) Math.max(0, selectionHandler.getSelectionArea().getStart().y); y < Math.min(gameClientState.getMapClientService().getMapData().getHeight(), selectionHandler.getSelectionArea().getEnd().y + 1); y++) {
                        Point p = new Point(x, y);

                        if (gameClientState.getMapClientService().isBuildable(p, player.getPlayerId(), room.getId())) {
                            buildablePlots++;
                        }

                        // See the gold amount
                        if (playerMoney < buildablePlots * room.getCost()) {
                            return false;
                        }
                    }
                }
                return true;
            }
        };

        if (!gameClientState.isMultiplayer()) {
            CheatState cheatState = new CheatState(app) {

                @Override
                public void onSuccess(CheatState.CheatType cheat) {
                    gameClientState.getGameClientService().triggerCheat(cheat);
                }
            };
            this.stateManager.attach(cheatState);
        }

        // Add listener
        if (isEnabled()) {
            setEnabled(true);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        if (enabled && !inputListenerAdded) {
            app.getInputManager().addRawInputListener(inputListener);
            inputListenerAdded = true;
        } else if (!enabled && inputListenerAdded) {
            app.getInputManager().removeRawInputListener(inputListener);
            inputListenerAdded = false;
            keys.clear();
        }
    }

    @Override
    public void cleanup() {
        playerEntityViewState = null;
        this.stateManager.detach(keeperHandState);
        keeperHandState = null;
        app.getInputManager().removeRawInputListener(inputListener);
        keys.clear();
        selectionHandler.cleanup();
        CheatState cheatState = this.stateManager.getState(CheatState.class);
        if (cheatState != null) {
            this.stateManager.detach(cheatState);
        }

        super.cleanup();
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);

        if (!isInitialized() || !isEnabled()) {
            return;
        }

        selectionHandler.update(mousePosition);
        if (isOnMap && !isOnGui && !isTaggable) {
            updateInteractiveObjectOnCursor();
        }

//        updateStateFlags();
        timeFromLastUpdate += tpf;

        // Update the cursor, the camera might have moved, a creature might have slipped by us... etc.
        if (timeFromLastUpdate > CURSOR_UPDATE_INTERVAL) {
            //updateCursor();
            updateStateFlags();
            timeFromLastUpdate = 0;
        }
    }

    @Override
    public boolean isPauseable() {
        return true;
    }

    /**
     * Set the interaction state for the keeper
     *
     * @param type interaction type
     * @param id object id, i.e. build state requires the room id
     */
    public void setInteractionState(Type type, int id) {
        interactionState.setState(type, id);

        // Call the update
        onInteractionStateChange(interactionState);
    }

    /**
     * Get the current interaction state
     *
     * @return current interaction
     */
    public InteractionState getInteractionState() {
        return interactionState;
    }

    private boolean isCursorOnGUI() {
        int height = app.getContext().getSettings().getHeight();

        if (view.isVisible() && view.isMouseInsideElement((int) mousePosition.x, height - (int) mousePosition.y)) {
            for (Element e : view.getChildren()) {
                if (e.isVisible() && e.isMouseInsideElement((int) mousePosition.x, height - (int) mousePosition.y)) {
                    return true;
                }
            }
            return false;
        }

        return true;
    }

    /**
     * Set the state flags according to what user can do. Rather clumsy. Fix if
     * you can.
     *
     * @return has the state being changed
     *
     */
    private void updateStateFlags() {
        boolean changed = false;

        boolean value = isCursorOnGUI();
        if (isOnGui != value) {
            isOnGui = value;
            changed = true;
        }

        value = isOnMap();
        if (isOnMap != value) {
            isOnMap = value;
            changed = true;
        }

        value = isTaggable();
        if (isTaggable != value) {
            isTaggable = value;
            changed = true;
        }

        value = isInteractable();
        if (isInteractable != value) {
            isInteractable = value;
            changed = true;
        }

        if (changed) {
            updateCursor();
        }
    }

    private boolean isInteractable() {
        if (isOnGui || !isOnMap || isTaggable) {
            setInteractiveControl(null);
        }

        Point p = null;
        if (interactiveControl != null) {

            // Maybe a kinda hack, but set the tooltip here
            tooltip.setText(interactiveControl.getTooltip(player.getPlayerId()));
            interactiveControl.onHover(player.getPlayerId());
        } else if (isOnMap) {

            // Tile tooltip then
            p = selectionHandler.getPointedTileIndex();
            IMapTileInformation tile = mapInformation.getMapData().getTile(p);
            if (tile != null) {
                Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
                if (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)) {
                    //RoomInstance roomInstance = getWorldHandler().getMapLoader().getRoomCoordinates().get(new Point((int) p.x, (int) p.y));
                    //GenericRoom room = getWorldHandler().getMapLoader().getRoomActuals().get(roomInstance);
                    //tooltip.setText(room.getTooltip(player.getPlayerId()));
                    tooltip.setText("");
                } else {
                    tooltip.setText(textParser.parseText(Utils.getMainTextResourceBundle().getString(Integer.toString(terrain.getTooltipStringId())), tile));
                }
            } else {
                tooltip.setText("");
            }
        }

        // If debug, show tile coordinate
        if (Main.isDebug() && (interactiveControl != null || isOnMap)) {
            StringBuilder sb = new StringBuilder();
            if (interactiveControl != null) {
                p = WorldUtils.vectorToPoint(((AbstractControl) interactiveControl).getSpatial().getWorldTranslation());
            }
            sb.append("(");
            sb.append(p.x + 1);  // 1-based coordinates
            sb.append(", ");
            sb.append(p.y + 1);  // 1-based coordinates
            sb.append("): ");
            sb.append(tooltip.getText());
            tooltip.setText(sb.toString());
        }

        return (interactiveControl != null);
    }

    private void updateInteractiveObjectOnCursor() {

        // See if we hit a creature/object
        CollisionResults results = new CollisionResults();

        // Convert screen click to 3D position
        Vector3f click3d = app.getCamera().getWorldCoordinates(
                new Vector2f(mousePosition.x, mousePosition.y), 0f);
        Vector3f dir = app.getCamera().getWorldCoordinates(
                new Vector2f(mousePosition.x, mousePosition.y), 1f).subtractLocal(click3d);

        // Aim the ray from the mouse spot forwards
        Ray ray = new Ray(click3d, dir);

        // Collect intersections between ray and all nodes in results list
        if (playerEntityViewState == null) {
            playerEntityViewState = stateManager.getState(Short.toString(player.getPlayerId()), PlayerEntityViewState.class);
        }
        playerEntityViewState.getRoot().collideWith(ray, results);

        // See the results so we see what is going on
        Node object;
        for (int i = 0; i < results.size(); i++) {

            // TODO: Now just creature control, but all interaction objects
            object = results.getCollision(i).getGeometry().getParent().getParent();
            IEntityViewControl control = object.getControl(IEntityViewControl.class);
            if (control != null) {
                setInteractiveControl(control);
                return;
            }
        }
        setInteractiveControl(null);
    }

    private void setInteractiveControl(IEntityViewControl interactiveControl) {

        // If it is the same, don't do anything
        if (interactiveControl != null && interactiveControl.equals(this.interactiveControl)) {
            return;
        }

        // Changed
        if (this.interactiveControl != null) {
            this.interactiveControl.onHoverEnd(player.getPlayerId());
        }
        this.interactiveControl = interactiveControl;
        if (this.interactiveControl != null) {
            this.interactiveControl.onHoverStart(player.getPlayerId());
        }
    }

    private boolean isTaggable() {
        if (isOnGui || !isOnMap) {
            return false;
        }
        Point p = selectionHandler.getPointedTileIndex();
        return (interactionState.getType() == Type.ROOM
                || interactionState.getType() == Type.NONE)
                && isOnMap && gameClientState.getMapClientService().isTaggable(p);
    }

    private boolean isOnMap() {
        if (isOnGui) {
            return false;
        }
        Point p = selectionHandler.getPointedTileIndex();
        return gameClientState.getMapClientService().getMapData().getTile(p) != null;
    }

    protected void updateCursor() {
        keeperHandState.setVisible(false);
        if (Main.getUserSettings().getBoolean(Settings.Setting.USE_CURSORS)) {
            if (isOnGui || isInteractable || interactionState.getType() == Type.SPELL) {
                inputManager.setMouseCursor(CursorFactory.getCursor(CursorFactory.CursorType.POINTER, assetManager));
            } else if (selectionHandler.isActive() && isTaggable) {
                inputManager.setMouseCursor(CursorFactory.getCursor(CursorFactory.CursorType.HOLD_PICKAXE_TAGGING, assetManager));
            } else if (isTaggable) {
                inputManager.setMouseCursor(CursorFactory.getCursor(CursorFactory.CursorType.HOLD_PICKAXE, assetManager));
            } else if (keeperHandState.getItem() != null) {

                // Keeper hand item
                inputManager.setMouseCursor(CursorFactory.getCursor(keeperHandState.getItem().getInHandCursor(), assetManager));
                keeperHandState.setVisible(true);
            } else {
                inputManager.setMouseCursor(CursorFactory.getCursor(CursorFactory.CursorType.IDLE, assetManager));
            }
        } else if (keeperHandState.getItem() != null) {
            keeperHandState.setVisible(true);
        }
    }

    private void initializeInput() {
        inputListener = new RawInputListener() {
            @Override
            public void beginInput() {
            }

            @Override
            public void endInput() {
            }

            @Override
            public void onJoyAxisEvent(JoyAxisEvent evt) {
            }

            @Override
            public void onJoyButtonEvent(JoyButtonEvent evt) {
            }

            @Override
            public void onMouseMotionEvent(MouseMotionEvent evt) {
                mousePosition.set(evt.getX(), evt.getY());
                keeperHandState.setPosition(evt.getX(), evt.getY());

                timeFromLastUpdate = 0;
                updateStateFlags();
                //updateCursor();
            }

            @Override
            public void onMouseButtonEvent(MouseButtonEvent evt) {
//                timeFromLastUpdate = 0;
                if (isOnGui || !isOnMap) {
                    return;
                }

                if (evt.getButtonIndex() == MouseInput.BUTTON_LEFT) {

                    if (evt.isPressed()) {
                        if (interactionState.getType() == Type.SPELL) {
                            castSpell(kwdFile.getKeeperSpellById(interactionState.getItemId()), interactiveControl, selectionHandler.getPointedTileIndex(), selectionHandler.getActualPointedPosition());
                            //TODO correct interactiveControl.isPickable
                            /*if (interactiveControl != null && interactionState.getItemId() == SPELL_POSSESSION_ID
                                    && interactiveControl.isPickable(player.getPlayerId())) {
                                CreatureControl cc = interactiveControl.getSpatial().getControl(CreatureControl.class);
                                if (cc != null) {
                                    onPossession(cc);
                                    // Reset the state
                                    // TODO disable selection box
                                    setInteractionState(Type.NONE, 0);
                                }
                            }*/
                        } else if (interactionState.getType() == Type.TRAP) {
                            //TODO put trap
                        } else if (interactionState.getType() == Type.DOOR) {
                            //TODO put door
                        } else if (interactionState.getType() == Type.NONE
                                && interactiveControl != null && !keeperHandState.isFull()
                                && interactiveControl.isPickable(player.getPlayerId())) {
                            pickupObject(interactiveControl);
                        } else if (interactionState.getType() == Type.NONE
                                && interactiveControl != null
                                && interactiveControl.isInteractable(player.getPlayerId())) {
                            interactiveControl.interact(player.getPlayerId());
                        } else {

                            // Selection stuff
                            if (selectionHandler.isVisible()) {
                                selectionHandler.setActive(true);
                            }

                            // I suppose we are tagging
                            if (isTaggable) {
                                updateCursor();
                                // The tagging sound is positional and played against the cursor change, not the action itself
                                Point pos = selectionHandler.getPointedTileIndex();
//                                getWorldHandler().playSoundAtTile(pos, GlobalCategory.HAND, GlobalType.HAND_TAG);
                            }
                        }

                    } else if (evt.isReleased() && selectionHandler.isActive()) {
                        SelectionArea selectionArea = selectionHandler.getSelectionArea();
                        if (interactionState.getType() == Type.NONE
                                || (interactionState.getType() == Type.ROOM
                                && gameClientState.getMapClientService().isTaggable(WorldUtils.vectorToPoint(selectionArea.getRealStart())))) {

                            // Determine if this is a select/deselect by the starting tile's status
                            boolean select = !gameClientState.getMapClientService().isSelected(WorldUtils.vectorToPoint(selectionArea.getRealStart()), player.getPlayerId());
                            gameClientState.getGameClientService().selectTiles(selectionArea.getStart(), selectionArea.getEnd(), select);
                        } else if (interactionState.getType() == Type.ROOM
                                && gameClientState.getMapClientService().isBuildable(WorldUtils.vectorToPoint(selectionArea.getRealStart()), player.getPlayerId(), (short) interactionState.getItemId())) {
                            gameClientState.getGameClientService().build(selectionArea.getStart(), selectionArea.getEnd(), (short) interactionState.getItemId());
                        } else if (interactionState.getType() == Type.SELL) {
                            gameClientState.getGameClientService().sell(selectionArea.getStart(), selectionArea.getEnd());
                        }

                        selectionHandler.setActive(false);
                        updateCursor();
                    }
                } else if (evt.getButtonIndex() == MouseInput.BUTTON_RIGHT && evt.isReleased()) {

                    Point p = selectionHandler.getPointedTileIndex();
                    if (interactionState.getType() == Type.NONE) {

                        // Drop
                        IEntityViewControl entityViewControl = keeperHandState.getItem();
                        if (entityViewControl != null) {
                            IMapTileInformation mapTile = gameClientState.getMapClientService().getMapData().getTile(p);
                            if (entityViewControl.getDroppableStatus(mapTile, gameClientState.getMapClientService().getTerrain(mapTile), player.getPlayerId()) != IEntityViewControl.DroppableStatus.NOT_DROPPABLE) {
                                gameClientState.getGameClientService().drop(entityViewControl.getEntityId(), p, selectionHandler.getActualPointedPosition(), interactiveControl != null ? interactiveControl.getEntityId() : null);
                            }
                            //MapTile tile = gameClientState.getMapClientService().getMapData().getTile(p);
//                            IEntityControl.DroppableStatus status = keeperHand.peek().getDroppableStatus(tile, player.getPlayerId());
//                            if (status != IEntityControl.DroppableStatus.NOT_DROPPABLE) {
//
//                                // Drop & update cursor
//                                keeperHand.pop().drop(tile, selectionHandler.getActualPointedPosition(), interactiveControl);
//                                updateCursor();
//                            }
                        } else if (interactiveControl != null && interactiveControl.isInteractable(player.getPlayerId())) {
//                            getWorldHandler().playSoundAtTile(p, GlobalCategory.HAND, GlobalType.HAND_SLAP);
                            gameClientState.getGameClientService().interact(interactiveControl.getEntityId());
                            interactiveControl.interact(player.getPlayerId());
                        } else if (Main.isDebug()) {
                            // taggable -> "dig"
//                            if (getWorldHandler().isTaggable(p.x, p.y)) {
//                                getWorldHandler().digTile(p.x, p.y);
//                            } // ownable -> "claim"
//                            else if (getWorldHandler().isClaimable(p.x, p.y, player.getPlayerId())) {
//                                getWorldHandler().claimTile(p.x, p.y, player.getPlayerId());
//                            }
                        }
                    }

                    // Reset the state
                    setInteractionState(Type.NONE, 0);
                    updateCursor();

                    selectionHandler.setActive(false);

                } else if (evt.getButtonIndex() == MouseInput.BUTTON_MIDDLE && evt.isReleased()) {
//                    if (Main.isDebug()) {
//                        Point p = selectionHandler.getPointedTileIndex();
//                        getWorldHandler().claimTile(p.x, p.y, player.getPlayerId());
//                    }
                }
            }

            @Override
            public void onKeyEvent(KeyInputEvent evt) {

                // See the CTRL + ALT
                switch (evt.getKeyCode()) {
                    case KeyInput.KEY_LCONTROL:
                    case KeyInput.KEY_RCONTROL:
                        if (evt.isPressed()) {
                            keys.add(KeyInput.KEY_LCONTROL);
                            keys.add(KeyInput.KEY_RCONTROL);
                        } else {
                            keys.remove(KeyInput.KEY_LCONTROL);
                            keys.remove(KeyInput.KEY_RCONTROL);
                        }
                        break;

                    case KeyInput.KEY_LMENU:
                    case KeyInput.KEY_RMENU:
                        if (evt.isPressed()) {
                            keys.add(KeyInput.KEY_LMENU);
                            keys.add(KeyInput.KEY_RMENU);
                        } else {
                            keys.remove(KeyInput.KEY_LMENU);
                            keys.remove(KeyInput.KEY_RMENU);
                        }
                        break;
                }

                if (evt.isPressed()) {
                    if (evt.getKeyCode() == KeyInput.KEY_C && keys.contains(KeyInput.KEY_LCONTROL) && keys.contains(KeyInput.KEY_LMENU)) {
                        CheatState cheat = stateManager.getState(CheatState.class);
                        if (cheat != null && !cheat.isEnabled()) {
                            cheat.setEnabled(true);
                        }
                    } else if (evt.getKeyCode() == ConsoleState.KEY && Main.isDebug()) {
                        stateManager.getState(ConsoleState.class).setEnabled(true);
                    } else if (evt.getKeyCode() == (Integer) Settings.Setting.TOGGLE_PLAYER_INFORMATION.getDefaultValue()) {
                        Element stats = view.findElementById("statistics");
                        if (stats != null) {
                            if (stats.isVisible()) {
                                stats.hide();
                            } else {
                                stats.show();
                            }
                        }
                    }
                }

            }

            @Override
            public void onTouchEvent(TouchEvent evt) {
            }

        };
    }

    /**
     * Cast a keeper spell
     *
     * @param keeperSpell the spell
     * @param object pointed object, can be null
     * @param tile the tile index
     * @param position the actual cursor position
     * @return true if spell can be cast
     */
    private boolean castSpell(KeeperSpell keeperSpell, IEntityViewControl object, Point tile, Vector2f position) {
        if (!canCastSpell(keeperSpell, object, tile)) {
            return false;
        }
        gameClientState.getGameClientService().castKeeperSpell(keeperSpell.getId(), object != null ? object.getEntityId() : null, tile, position);

        return true;
    }

    private boolean canCastSpell(KeeperSpell keeperSpell, IEntityViewControl object, Point tile) {
        return KeeperSpellCastValidator.isValidCast(keeperSpell, kwdFile, mapInformation, mapInformation.getMapData().getTile(tile), gameClientState.getPlayer(), entityData, object != null ? object.getEntityId() : null);
    }

    /**
     * Picks up an object, places it in Keeper's hand
     *
     * @param object the object to pickup
     * @return picked or not
     */
    public boolean pickupObject(IEntityViewControl object) {
        if (object == null || keeperHandState.isFull() || !object.isPickable(player.getPlayerId())) {
            return false;
        }

        //keeperHand.push(object.pickUp(player.getPlayerId()));
        gameClientState.getGameClientService().pickUp(object.getEntityId());
        updateCursor();
        return true;
    }

    /**
     * Checks if the Keeper hand is full
     *
     * @return is keeper hand full
     */
    public boolean isKeeperHandFull() {
        return keeperHandState.isFull();
    }

    /**
     * A callback for changing the interaction state
     *
     * @param interactionState new state
     */
    protected abstract void onInteractionStateChange(InteractionState interactionState);

    protected abstract void onPossession(EntityId creature);

    public static class InteractionState {

        public enum Type {

            NONE, ROOM, SELL, SPELL, TRAP, DOOR, STUFF_IN_HAND
        }

        private int itemId = 0;
        private Type type = Type.NONE;

        public InteractionState() {
            itemId = 0;
            type = Type.NONE;
        }

        public InteractionState(Type type, int itemId) {
            this.type = type;
            this.itemId = itemId;
        }

        public InteractionState(Type type) {
            this(type, 0);
        }

        protected void setState(Type type, int itemId) {
            this.type = type;
            this.itemId = itemId;

            //PlayerInteractionState.this.onInteractionStateChange(this);
        }

        protected void setState(Type type) {
            setState(type, 0);
        }

        public Type getType() {
            return type;
        }

        public int getItemId() {
            return itemId;
        }
    }
}
