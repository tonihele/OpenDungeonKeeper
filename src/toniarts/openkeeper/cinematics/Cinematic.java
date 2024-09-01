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
package toniarts.openkeeper.cinematics;

import com.jme3.animation.LoopMode;
import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.audio.Listener;
import com.jme3.cinematic.MotionPath;
import com.jme3.cinematic.PlayState;
import com.jme3.cinematic.events.CinematicEvent;
import com.jme3.cinematic.events.CinematicEventListener;
import com.jme3.cinematic.events.MotionEvent;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Node;
import com.jme3.scene.control.CameraControl.ControlDirection;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Our wrapper on JME cinematic class, produces ready cinematics from camera
 * sweep files.<br>
 * This extends the JME's own Cinematic, so adding effects etc. is as easy as
 * possible.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Cinematic extends com.jme3.cinematic.Cinematic {

    private final AssetManager assetManager;
    private static final boolean IS_DEBUG = false;
    private static final String CAMERA_NAME = "Motion cam";
    private final CameraSweepData cameraSweepData;
    private final Camera cam;
    private final Vector3f start;
    private final Listener audioListener;
    private CameraNode camNode;

    /**
     * Creates a new cinematic ready for consumption
     *
     * @param assetManager asset manager instance
     * @param cam the camera to use
     * @param audioListener the audio listener position
     * @param start starting map coordinates, zero based
     * @param cameraSweepFile the camera sweep file name that is the basis for
     * this animation (without the extension)
     * @param scene scene node to attach to
     * @param stateManager the state manager
     */
    public Cinematic(final AssetManager assetManager, Camera cam, Listener audioListener,
            Point start, String cameraSweepFile, Node scene, AppStateManager stateManager) {

        this(assetManager, cam, audioListener, WorldUtils.pointToVector3f(start), cameraSweepFile, scene, stateManager);
    }

    public Cinematic(final Main app, String cameraSweepFile, final Vector3f start) {

        this(app.getAssetManager(), app.getCamera(), app.getListener(),
                start.addLocal(0, WorldUtils.FLOOR_HEIGHT, 0),
                cameraSweepFile, app.getRootNode(), app.getStateManager());
    }

    /**
     * Creates a new cinematic ready for consumption
     *
     * @param assetManager asset manager instance
     * @param cam the camera to use
     * @param audioListener the audio listener position
     * @param start starting location, zero based
     * @param cameraSweepFile the camera sweep file name that is the basis for
     * this animation (without the extension)
     * @param scene scene node to attach to
     * @param stateManager the state manager
     */
    public Cinematic(final AssetManager assetManager, final Camera cam, Listener audioListener,
            final Vector3f start, String cameraSweepFile, Node scene, final AppStateManager stateManager) {

        super(scene);

        this.assetManager = assetManager;
        this.audioListener = audioListener;
        this.start = start;
        this.cam = cam;

        // Load up the camera sweep file
        cameraSweepData = AssetUtils.loadCameraSweep(assetManager, cameraSweepFile);

        // Add the listener, it is critical that this listener is executed before any custom ones
        addListener(new CinematicEventListener() {
            private boolean executed = false;

            @Override
            public void onPlay(CinematicEvent cinematic) {
            }

            @Override
            public void onPause(CinematicEvent cinematic) {
            }

            @Override
            public void onStop(CinematicEvent cinematic) {

                if (!executed) {
                    executed = true;

                    // We never reach the final point
                    CameraSweepDataEntry entry = cameraSweepData.getEntries().get(cameraSweepData.getEntries().size() - 1);
                    applyCameraSweepEntry(cam, start, entry, audioListener);

                    // Detach
                    scene.detachChild(camNode);

                    // Remove us, this will cause stack over flow loop without the executed flag
                    // Dirty but, working in a way
                    stateManager.detach(Cinematic.this);
                }
            }
        });
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {

        // Initialize
        initializeCinematic(getScene(), cam, start);

        // Set the camera as a first step
        activateCamera(0, CAMERA_NAME);

        super.initialize(stateManager, app);
    }


    /**
     * Creates the actual cinematic
     *
     * @param scene the scene to attach to
     */
    private void initializeCinematic(final Node scene, final Camera cam, final Vector3f startLocation) {
        camNode = bindCamera(CAMERA_NAME, cam);
        camNode.setControlDir(ControlDirection.SpatialToCamera);
        final MotionPath path = new MotionPath();
        path.setCycle(false);

        // The waypoints
        for (CameraSweepDataEntry entry : cameraSweepData.getEntries()) {
            path.addWayPoint(entry.getPosition().mult(WorldUtils.TILE_WIDTH).addLocal(startLocation));
        }
        //path.setCurveTension(0.5f);
        if (IS_DEBUG) {
            path.enableDebugShape(assetManager, scene);
        }

        final MotionEvent cameraMotionControl = new MotionEvent(camNode, path) {
            @Override
            public void update(float tpf) {
                super.update(tpf);

                if (getPlayState() == PlayState.Playing) {

                    // Rotate
                    float progress = getCurrentValue();
                    int startIndex = getCurrentWayPoint();

                    // Get the rotation at previous (or current) waypoint
                    CameraSweepDataEntry entry = cameraSweepData.getEntries().get(startIndex);
                    Quaternion q1 = new Quaternion(entry.getRotation());

                    // If we are not on the last waypoint, interpolate the rotation between waypoints
                    CameraSweepDataEntry entryNext = cameraSweepData.getEntries().get(startIndex + 1);
                    Quaternion q2 = entryNext.getRotation();

                    q1.slerp(q2, progress);

                    // Set the rotation
                    setRotation(q1);

                    // Set the near & FOV
                    //float near = FastMath.interpolateLinear(progress, entry.getNear(), entryNext.getNear()) / 4096;
                    float fov = FastMath.interpolateLinear(progress, entry.getFov(), entryNext.getFov());
                    cam.setFrustumPerspective(fov, (float) cam.getWidth() / cam.getHeight(), 0.1f, 100f);

                    // Update audio listener position
                    updateAudioListenerPosition(audioListener, cam);
                }
            }

        };
        cameraMotionControl.setLoopMode(LoopMode.DontLoop);
        cameraMotionControl.setInitialDuration(cameraSweepData.getEntries().size() / getFramesPerSecond());
        cameraMotionControl.setDirectionType(MotionEvent.Direction.Rotation);

        // Add us
        addCinematicEvent(0, cameraMotionControl);

        // Set duration of the whole animation
        setInitialDuration(cameraSweepData.getEntries().size() / getFramesPerSecond());
    }

    /**
     * Apply the camera sweep entry data to the given camera
     *
     * @param cam the camera
     * @param startLocation the start location for the path
     * @param entry the entry to apply to
     * @param audioListener the audio listener position
     */
    public static void applyCameraSweepEntry(final Camera cam, final Vector3f startLocation,
            final CameraSweepDataEntry entry, Listener audioListener) {

        // Set Position
        cam.setLocation(startLocation.add(entry.getPosition().mult(WorldUtils.TILE_WIDTH)));

        // Set the rotation
        cam.setRotation(entry.getRotation());

        // FIXME: Near should be the very minimun needed, how to use the original near info
        // Set the near & FOV
        //cam.setFrustumNear(entry.getNear() / 4096f);
        cam.setFrustumPerspective(entry.getFov(), (float) cam.getWidth() / cam.getHeight(), 0.1f, 100f);

        // Update audio listener position
        updateAudioListenerPosition(audioListener, cam);
    }

    private static void updateAudioListenerPosition(final Listener audioListener, final Camera cam) {
        audioListener.setLocation(cam.getLocation());
        audioListener.setRotation(cam.getRotation());
    }

    /**
     * Animation speed, FPS
     *
     * @return FPS
     */
    protected float getFramesPerSecond() {
        return 30f;
    }
}
