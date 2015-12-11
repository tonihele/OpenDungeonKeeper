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
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.state.AbstractPauseAwareState;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.view.selection.SelectionHandler;

/**
 *
 * @author ArchDemon
 */

public class PossessionCameraState  extends AbstractPauseAwareState implements ActionListener, AnalogListener {
    private Main app;
    //private final GameState gameState;
    //private Node rootNode;
    //private AssetManager assetManager;
    //private AppStateManager stateManager;
    private InputManager inputManager;
    //private ViewPort viewPort;
    private final Thing.KeeperCreature target;
    private final Creature creature;

    //private boolean actionIsPressed = false;
    //private boolean cancelIsPressed = false;
    //private boolean startSet = false;
    public Vector2f mousePosition = Vector2f.ZERO;
    
    private PossessionCamera camera;
    //private Integer specialKey = null;
    
    private static final String POSSESSION = "POSSESSION_";
    
    private static final String CAMERA_VIEW_LEFT = "CAMERA_VIEW_LEFT";
    private static final String CAMERA_VIEW_UP = "CAMERA_VIEW_UP";
    private static final String CAMERA_VIEW_RIGHT = "CAMERA_VIEW_RIGHT";
    private static final String CAMERA_VIEW_DOWN = "CAMERA_VIEW_DOWN";
    
    private static final String SPECIAL_KEY_CONTROL = "SPECIAL_KEY_CONTROL";
    private static final String SPECIAL_KEY_ALT = "SPECIAL_KEY_ALT";
    private static final String SPECIAL_KEY_SHIFT = "SPECIAL_KEY_SHIFT";
    
    private static String[] mappings = new String[]{
        // view
        CAMERA_VIEW_LEFT,
        CAMERA_VIEW_UP,
        CAMERA_VIEW_RIGHT,
        CAMERA_VIEW_DOWN,
        // movement
        POSSESSION + Settings.Setting.CAMERA_UP.name(),
        POSSESSION + Settings.Setting.CAMERA_DOWN.name(),
        POSSESSION + Settings.Setting.CAMERA_LEFT.name(),
        POSSESSION + Settings.Setting.CAMERA_RIGHT.name(),
        Settings.Setting.POSSESSED_RUN.name(),
        Settings.Setting.POSSESSED_CREEP.name(),        
        // attack
        Settings.Setting.POSSESSED_SELECT_MELEE.name(),
        Settings.Setting.POSSESSED_SELECT_SPELL_1.name(),
        Settings.Setting.POSSESSED_SELECT_SPELL_2.name(),
        Settings.Setting.POSSESSED_SELECT_SPELL_3.name(),
        Settings.Setting.POSSESSED_SELECT_ABILITY_1.name(),
        Settings.Setting.POSSESSED_SELECT_ABILITY_2.name(),
        // group
        Settings.Setting.POSSESSED_SELECT_GROUP.name(),
        Settings.Setting.POSSESSED_REMOVE_FROM_GROUP.name(),
        
        Settings.Setting.POSSESSED_PICK_LOCK_OR_DISARM.name(),
        // special
        //SPECIAL_KEY_CONTROL,
        //SPECIAL_KEY_ALT,
        //SPECIAL_KEY_SHIFT,
    };
    
    private static final Logger logger = Logger.getLogger(PossessionCameraState.class.getName());    
    
    public PossessionCameraState(Thing.KeeperCreature target, KwdFile kwd) {
        this.target = target;
        this.creature = kwd.getCreature(this.target.getCreatureId());
    }
    
    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);
        this.app = (Main) app;
        //rootNode = this.app.getRootNode();
        //assetManager = this.app.getAssetManager();
        //this.stateManager = this.app.getStateManager();
        inputManager = this.app.getInputManager();
        //viewPort = this.app.getViewPort();
        
        // The camera
        if (camera == null) {            
            camera = new PossessionCamera(app.getCamera(), creature.getSpeed(), creature.getFirstPersonOscillateScale());
        }        
        loadCameraStartLocation();
        // The controls
        registerInput();        
    }
    
    /**
     * Load the initial camera position
     */
    private void loadCameraStartLocation() {
        Vector3f startLocation = new Vector3f(target.getPosX(), target.getPosZ(), target.getPosY());
        Camera cam = app.getCamera();        
        cam.setLocation(startLocation.addLocal(0, creature.getEyeHeight(), 0));
        //cam.setFrustumPerspective(45, cam.getWidth() / cam.getHeight(), 0.1f, creature.getDistanceCanSee() * 10);
        cam.setAxes(Vector3f.UNIT_X, Vector3f.UNIT_Y, Vector3f.UNIT_Z);
    }
    
    private void registerInput() {

        // Add the keys
        Settings settings = app.getUserSettings();
        inputManager.addMapping(POSSESSION + Settings.Setting.CAMERA_UP.name(), new KeyTrigger(settings.getSettingInteger(Settings.Setting.CAMERA_UP)));
        inputManager.addMapping(POSSESSION + Settings.Setting.CAMERA_DOWN.name(), new KeyTrigger(settings.getSettingInteger(Settings.Setting.CAMERA_DOWN)));
        inputManager.addMapping(POSSESSION + Settings.Setting.CAMERA_LEFT.name(), new KeyTrigger(settings.getSettingInteger(Settings.Setting.CAMERA_LEFT)));
        inputManager.addMapping(POSSESSION + Settings.Setting.CAMERA_RIGHT.name(), new KeyTrigger(settings.getSettingInteger(Settings.Setting.CAMERA_RIGHT)));
        
        inputManager.addMapping(Settings.Setting.POSSESSED_RUN.name(), new KeyTrigger(settings.getSettingInteger(Settings.Setting.POSSESSED_RUN)));
        inputManager.addMapping(Settings.Setting.POSSESSED_CREEP.name(), new KeyTrigger(settings.getSettingInteger(Settings.Setting.POSSESSED_CREEP)));
        
        inputManager.addMapping(CAMERA_VIEW_LEFT, new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping(CAMERA_VIEW_RIGHT, new MouseAxisTrigger(MouseInput.AXIS_X, false));        
        inputManager.addMapping(CAMERA_VIEW_UP, new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        inputManager.addMapping(CAMERA_VIEW_DOWN, new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        
        //inputManager.addMapping(SPECIAL_KEY_ALT, new KeyTrigger(KeyInput.KEY_LMENU), new KeyTrigger(KeyInput.KEY_RMENU));
        //inputManager.addMapping(SPECIAL_KEY_CONTROL, new KeyTrigger(KeyInput.KEY_LCONTROL), new KeyTrigger(KeyInput.KEY_RCONTROL));
        //inputManager.addMapping(SPECIAL_KEY_SHIFT, new KeyTrigger(KeyInput.KEY_LSHIFT), new KeyTrigger(KeyInput.KEY_RSHIFT));
        
        inputManager.addListener(this, mappings);
    }
    
    @Override
    public boolean isPauseable() {
        return false;
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!isEnabled()) {
            return;
        }
        
        if (name.equals(Settings.Setting.POSSESSED_RUN.name())) {         
            if (isPressed) {
                camera.setSpeed(creature.getRunSpeed());
            } else {
                camera.setSpeed(creature.getSpeed());
            }
        } else if (name.equals(Settings.Setting.POSSESSED_CREEP.name())) {
            if (isPressed) {
                camera.setSpeed(creature.getShuffleSpeed());
            } else {
                camera.setSpeed(creature.getSpeed());
            }
        }
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        if (!isEnabled()) {
            return;
        }
        
        if (name.equals(CAMERA_VIEW_LEFT)) {
             camera.rotate(value, true);
        } else if (name.equals(CAMERA_VIEW_RIGHT)) {
            camera.rotate(-value, true);
        } else if (name.equals(CAMERA_VIEW_UP)) {
            camera.rotate(value, false);
        } else if (name.equals(CAMERA_VIEW_DOWN)) {
            camera.rotate(-value, false);
        }

        if (name.equals(POSSESSION + Settings.Setting.CAMERA_UP.name())) {
            camera.move(value, false);
        } else if (name.equals(POSSESSION + Settings.Setting.CAMERA_DOWN.name())) {
            camera.move(-value, false);        
        } else if (name.equals(POSSESSION + Settings.Setting.CAMERA_LEFT.name())) {
            camera.move(value, true);
        } else if (name.equals(POSSESSION + Settings.Setting.CAMERA_RIGHT.name())) {
            camera.move(-value, true);
        } 
    }
    
    private void unregisterInput() {
        for (String s : mappings) {
            inputManager.deleteMapping(s);
        }
        inputManager.removeListener(this);
    }
    
    @Override
    public void cleanup() {

        // Unregister controls
        unregisterInput();

        super.cleanup();
    }

    @Override
    public void update(float tpf) {
        // Update audio listener position
        app.getListener().setLocation(app.getCamera().getLocation());
        app.getListener().setRotation(app.getCamera().getRotation());
    }
    
}