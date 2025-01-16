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
import com.jme3.cinematic.events.CinematicEvent;
import com.jme3.cinematic.events.CinematicEventListener;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.utils.Point;
import java.util.HashSet;
import java.util.Set;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.cinematics.Cinematic;
import toniarts.openkeeper.game.control.Container;
import toniarts.openkeeper.game.control.IContainer;
import toniarts.openkeeper.game.data.ActionPoint;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.data.Settings.Setting;
import toniarts.openkeeper.game.state.AbstractPauseAwareState;
import toniarts.openkeeper.game.state.GameClientState;
import toniarts.openkeeper.game.state.PlayerState;
import toniarts.openkeeper.game.state.SoundState;
import toniarts.openkeeper.tools.convert.map.GameMap;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.camera.PlayerCameraControl;
import toniarts.openkeeper.view.camera.PlayerCameraRotateControl;

/**
 * The player camera state. Listens for camera movement inputs.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class PlayerCameraState extends AbstractPauseAwareState implements ActionListener, AnalogListener {

    private Main app;
    private AppStateManager stateManager;
    private InputManager inputManager;

    private final IContainer container;
    private PlayerCamera camera;
    private Camera storedCamera;
    private final Player player;

    private final Set<Integer> keys = new HashSet<>();

    // Extra keys
    private static final float ZOOM_MOUSE = 0.08f;
    private static final float ZOOM_SPEED = 10f;
    private static final float MOVE_SPEED = 10f;
    private static final float ROTATION_SPEED = 4f;
    private static final String CAMERA_MOUSE_ZOOM_IN = "CAMERA_MOUSE_ZOOM_IN";
    private static final String CAMERA_MOUSE_ZOOM_OUT = "CAMERA_MOUSE_ZOOM_OUT";
    private static final String SPECIAL_KEY_CONTROL = "SPECIAL_KEY_CONTROL";
    private static final String SPECIAL_KEY_ALT = "SPECIAL_KEY_ALT";
    private static final String SPECIAL_KEY_SHIFT = "SPECIAL_KEY_SHIFT";
    // User set keys
    private static final String[] MAPPINGS = new String[]{
        Settings.Setting.CAMERA_DOWN.name(),
        Settings.Setting.CAMERA_LEFT.name(),
        Settings.Setting.CAMERA_RIGHT.name(),
        //Settings.Setting.CAMERA_ROTATE.name(),
        Settings.Setting.CAMERA_ROTATE_LEFT.name(),
        Settings.Setting.CAMERA_ROTATE_RIGHT.name(),
        Settings.Setting.CAMERA_UP.name(),
        Settings.Setting.CAMERA_ZOOM_IN.name(),
        Settings.Setting.CAMERA_ZOOM_OUT.name(),
        Setting.PITCH_CAMERA_UP.name(),
        Setting.PITCH_CAMERA_DOWN.name(),
        Setting.ROLL_CAMERA_LEFT.name(),
        Setting.ROLL_CAMERA_RIGHT.name(),
        Setting.YAW_CAMERA_LEFT.name(),
        Setting.YAW_CAMERA_RIGHT.name(),
        Setting.USE_ATTACK.name(),
        CAMERA_MOUSE_ZOOM_IN,
        CAMERA_MOUSE_ZOOM_OUT,
        SPECIAL_KEY_CONTROL,
        SPECIAL_KEY_ALT,
        SPECIAL_KEY_SHIFT};

    public PlayerCameraState(Player player) {
        this.player = player;
        this.container = new Container();
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);
        this.app = (Main) app;
        this.stateManager = this.app.getStateManager();
        inputManager = this.app.getInputManager();

        // The camera
        camera = new PlayerCamera(app.getCamera(), getCameraPresets());
        camera.setLimit(getCameraMapLimit());
        loadCameraStartLocation();
        // Add listener
        if (isEnabled()) {
            setEnabled(true);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        if (enabled) {
            /*
             if (storedCamera != null) {
             Camera cam = app.getCamera();
             cam.setLocation(storedCamera.getLocation().clone());
             cam.setRotation(storedCamera.getRotation().clone());
             //cam.setAxes(storedCamera.getLeft(), storedCamera.getUp(), storedCamera.getDirection());
             //camera.setZoomValue(storedCamera.getLocation().y);
             camera.setLookAt(cam.getLocation());
             //cameraRestore();
             }*/
            // The controls
            registerInput();
        } else {
            cameraStore();
            unregisterInput();
        }
    }

    public PlayerCamera getCamera() {
        return camera;
    }

    private Vector2f getCameraMapLimit() {
        GameMap gm = this.stateManager.getState(GameClientState.class).getLevelData().getMap();
        return new Vector2f(gm.getWidth(), gm.getHeight());
    }

    private Thing.Camera getCameraPresets() {
        GameClientState gs = this.stateManager.getState(GameClientState.class);
        Thing.Camera result = null;

        for (Thing.Camera thing : gs.getLevelData().getThings(Thing.Camera.class)) {
            if (thing.getId() == Thing.Camera.ID_GAME) {
                result = thing;
                break;
            }
        }

        return result;
    }

    /**
     * Load the initial main menu camera position
     */
    private void loadCameraStartLocation() {
        Vector3f location = WorldUtils.pointToVector3f(player.getStartingCameraX(),
                player.getStartingCameraY());
        camera.setLookAt(location);
    }

    public void cameraStore() {
        storedCamera = app.getCamera().clone();
    }

    public void cameraRestore() {
        Camera cam = app.getCamera();
        //cam.setAxes(storedCamera.getLeft(), storedCamera.getUp(), storedCamera.getDirection());
        //cam.setRotation(storedCamera.getRotation());
        cam.setFrame(storedCamera.getLocation(), storedCamera.getRotation());
        cam.setFrustum(storedCamera.getFrustumNear(), storedCamera.getFrustumFar(), storedCamera.getFrustumLeft(),
                storedCamera.getFrustumRight(), storedCamera.getFrustumTop(), storedCamera.getFrustumBottom());
        //cam.setLocation(storedCamera.getLocation());
    }

    public void setCameraLookAt(ActionPoint point) {
        Vector3f location = WorldUtils.ActionPointToVector3f(point);
        camera.setLookAt(location);
    }

    public void setCameraLookAt(Point point) {
        Vector3f location = WorldUtils.pointToVector3f(point);
        camera.setLookAt(location);
    }

    public void setCameraLookAt(Spatial spatial) {
        camera.setLookAt(spatial.getWorldTranslation());
    }

    public void setCameraLookAt(Vector3f position) {
        camera.setLookAt(position);
    }

    public void doTransition(int sweepFileId, final ActionPoint point) {
        this.doTransition(sweepFileId, WorldUtils.ActionPointToVector3f(point), null);
    }

    public void doTransition(int sweepFileId, final Vector3f start, final CinematicEventListener listener) {
        String sweepFile = "EnginePath" + sweepFileId;

        // Do cinematic transition
        Cinematic c = new Cinematic(app, sweepFile, start);
        c.addListener(new CinematicEventListener() {
            @Override
            public void onPlay(CinematicEvent cinematic) {
                PlayerCameraState.this.cameraStore();
                stateManager.getState(PlayerState.class).setWideScreen(true);
                inputManager.setCursorVisible(false);
            }

            @Override
            public void onPause(CinematicEvent cinematic) {
            }

            @Override
            public void onStop(CinematicEvent cinematic) {
                stateManager.getState(PlayerState.class).setTransitionEnd(true);
                stateManager.getState(PlayerState.class).setWideScreen(false);
                inputManager.setCursorVisible(true);
                PlayerCameraState.this.cameraRestore();
            }
        });
        if (listener != null) {
            c.addListener(listener);
        }
        // GuiEvent ce = new GuiEvent(app.getNifty(), PlayerState.CINEMATIC_SCREEN_ID);
        // c.addCinematicEvent(0, ce);
        // SoundEvent se = new SoundEvent(sweepFile);
        // c.addCinematicEvent(0, se);
        stateManager.getState(PlayerState.class).setTransitionEnd(false);
        stateManager.attach(c);
        c.play();
    }

    public void zoomToPoint(Vector3f point) {
        container.addControl(new PlayerCameraControl(camera, point));
    }

    public void rotateAroundPoint(Vector3f point, boolean relative, int angle, int time) {
        container.addControl(new PlayerCameraRotateControl(camera, relative, angle, time));
    }

    @Override
    public void cleanup() {

        // Unregister controls
        unregisterInput();

        super.cleanup();
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);

        // Update the container
        container.update(tpf);

        // Update audio listener position
        app.getListener().setLocation(app.getCamera().getLocation());
        app.getListener().setRotation(app.getCamera().getRotation());
    }

    private void addKeyMapping(Setting s) {
        inputManager.addMapping(s.name(), new KeyTrigger(Main.getUserSettings().getInteger(s)));
    }

    private void registerInput() {

        // Add the keys
        addKeyMapping(Setting.CAMERA_UP);
        addKeyMapping(Setting.CAMERA_DOWN);
        addKeyMapping(Setting.CAMERA_LEFT);
        addKeyMapping(Setting.CAMERA_RIGHT);
        //addMapping(Setting.CAMERA_ROTATE);
        addKeyMapping(Setting.CAMERA_ROTATE_LEFT);
        addKeyMapping(Setting.CAMERA_ROTATE_RIGHT);

        addKeyMapping(Setting.CAMERA_ZOOM_IN);
        addKeyMapping(Setting.CAMERA_ZOOM_OUT);

        addKeyMapping(Setting.YAW_CAMERA_LEFT);
        addKeyMapping(Setting.YAW_CAMERA_RIGHT);

        addKeyMapping(Setting.PITCH_CAMERA_DOWN);
        addKeyMapping(Setting.PITCH_CAMERA_UP);

        addKeyMapping(Setting.ROLL_CAMERA_LEFT);
        addKeyMapping(Setting.ROLL_CAMERA_RIGHT);

        addKeyMapping(Setting.USE_ATTACK);

        inputManager.addMapping(SPECIAL_KEY_ALT, new KeyTrigger(KeyInput.KEY_LMENU), new KeyTrigger(KeyInput.KEY_RMENU));
        inputManager.addMapping(SPECIAL_KEY_CONTROL, new KeyTrigger(KeyInput.KEY_LCONTROL), new KeyTrigger(KeyInput.KEY_RCONTROL));
        inputManager.addMapping(SPECIAL_KEY_SHIFT, new KeyTrigger(KeyInput.KEY_LSHIFT), new KeyTrigger(KeyInput.KEY_RSHIFT));
        // Extra
        inputManager.addMapping(CAMERA_MOUSE_ZOOM_IN, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        inputManager.addMapping(CAMERA_MOUSE_ZOOM_OUT, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));

        inputManager.addListener(this, MAPPINGS);
    }

    private void unregisterInput() {
        for (String s : MAPPINGS) {
            inputManager.deleteMapping(s);
        }

        inputManager.removeListener(this);
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!isEnabled()) {
            return;
        }

        if (name.equals(Setting.USE_ATTACK.name())) {
            if (isPressed) {
                Cinematic transition = stateManager.getState(Cinematic.class);
                if (transition != null) {
                    stateManager.getState(SoundState.class).stopSpeech();
                    transition.stop();
                }
            }
            return;
        }

        switch (name) {
            case SPECIAL_KEY_CONTROL:
                if (isPressed) {
                    keys.add(KeyInput.KEY_LCONTROL);
                    keys.add(KeyInput.KEY_RCONTROL);
                } else {
                    keys.remove(KeyInput.KEY_LCONTROL);
                    keys.remove(KeyInput.KEY_RCONTROL);
                }
                break;

            case SPECIAL_KEY_ALT:
                if (isPressed) {
                    keys.add(KeyInput.KEY_LMENU);
                    keys.add(KeyInput.KEY_RMENU);
                } else {
                    keys.remove(KeyInput.KEY_LMENU);
                    keys.remove(KeyInput.KEY_RMENU);
                }
                break;

            case SPECIAL_KEY_SHIFT:
                if (isPressed) {
                    keys.add(KeyInput.KEY_LSHIFT);
                    keys.add(KeyInput.KEY_RSHIFT);
                } else {
                    keys.remove(KeyInput.KEY_LSHIFT);
                    keys.remove(KeyInput.KEY_RSHIFT);
                }
                break;
        }
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        if (!isEnabled()) {
            return;
        }

        if (name.equals(CAMERA_MOUSE_ZOOM_IN) || isCombinationPressed(name, Setting.CAMERA_ZOOM_IN)) {
            if (name.equals(CAMERA_MOUSE_ZOOM_IN)) {
                value = Main.getUserSettings().getFloat(Setting.MOUSE_SENSITIVITY) * ZOOM_MOUSE;
                if (Main.getUserSettings().getBoolean(Setting.MOUSE_INVERT)) {
                    value = -value;
                }
            }
            camera.zoom(value * ZOOM_SPEED);

        } else if (name.equals(CAMERA_MOUSE_ZOOM_OUT) || isCombinationPressed(name, Setting.CAMERA_ZOOM_OUT)) {
            if (name.equals(CAMERA_MOUSE_ZOOM_OUT)) {
                value = Main.getUserSettings().getFloat(Setting.MOUSE_SENSITIVITY) * ZOOM_MOUSE;
                if (Main.getUserSettings().getBoolean(Setting.MOUSE_INVERT)) {
                    value = -value;
                }
            }
            camera.zoom(-value * ZOOM_SPEED);

        } else if (isCombinationPressed(name, Setting.CAMERA_UP)) {
            camera.move(0, value * MOVE_SPEED);
        } else if (isCombinationPressed(name, Setting.CAMERA_DOWN)) {
            camera.move(0, -value * MOVE_SPEED);
        } else if (isCombinationPressed(name, Setting.CAMERA_LEFT)) {
            camera.move(value * MOVE_SPEED, 0);
        } else if (isCombinationPressed(name, Setting.CAMERA_RIGHT)) {
            camera.move(-value * MOVE_SPEED, 0);
        } else if (isCombinationPressed(name, Setting.CAMERA_ROTATE_LEFT)) {
            camera.rotateAround(-value * ROTATION_SPEED);
        } else if (isCombinationPressed(name, Setting.CAMERA_ROTATE_RIGHT)) {
            camera.rotateAround(value * ROTATION_SPEED);
        } else if (isCombinationPressed(name, Setting.YAW_CAMERA_LEFT)) {
            camera.rotateAround(-value * ROTATION_SPEED);
        } else if (isCombinationPressed(name, Setting.YAW_CAMERA_RIGHT)) {
            camera.rotateAround(value * ROTATION_SPEED);
        } else if (isCombinationPressed(name, Setting.PITCH_CAMERA_UP)) {
            camera.pith(-value * ROTATION_SPEED);
        } else if (isCombinationPressed(name, Setting.PITCH_CAMERA_DOWN)) {
            camera.pith(value * ROTATION_SPEED);
        } else if (isCombinationPressed(name, Setting.ROLL_CAMERA_LEFT)) {
            camera.roll(-value * ROTATION_SPEED);
        } else if (isCombinationPressed(name, Setting.ROLL_CAMERA_RIGHT)) {
            camera.roll(value * ROTATION_SPEED);
        }
    }

    private boolean isCombinationPressed(String name, Setting s) {
        if (name.equals(s.name())) {
            // FIXME use user settings
            if (s.getSpecialKey() == null && keys.isEmpty()) {
                return true;
            } else if (keys.contains(s.getSpecialKey())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isPauseable() {
        return false;
    }
}
