/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.cinematics;

import com.jme3.animation.LoopMode;
import com.jme3.asset.AssetManager;
import com.jme3.cinematic.MotionPath;
import com.jme3.cinematic.events.MotionEvent;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Node;
import com.jme3.scene.control.CameraControl.ControlDirection;
import java.awt.Point;
import java.io.File;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import toniarts.opendungeonkeeper.tools.convert.AssetsConverter;
import toniarts.opendungeonkeeper.tools.convert.map.loader.MapLoader;

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
    private static final Logger logger = Logger.getLogger(Cinematic.class.getName());
    private static final boolean IS_DEBUG = true;
    private static final String CAMERA_NAME = "Motion cam";

    /**
     * Creates a new cinematic ready for consumption
     *
     * @param assetManager asset manager instance
     * @param cam the camera to use
     * @param start starting map coordinates, zero based
     * @param cameraSweepFile the camera sweep file name that is the basis for
     * this animation (without the extension)
     * @param scene scene node to attach to
     */
    public Cinematic(AssetManager assetManager, Camera cam, Point start, String cameraSweepFile, Node scene) {
        super(scene);
        this.assetManager = assetManager;

        // Load up the camera sweep file
        Object obj = assetManager.loadAsset(AssetsConverter.PATHS_FOLDER.concat("\\").replaceAll(Pattern.quote(File.separator), "/").concat(cameraSweepFile.concat(".").concat(CameraSweepDataLoader.CAMERA_SWEEP_DATA_FILE_EXTENSION)));
        if (obj == null || !(obj instanceof CameraSweepData)) {
            String msg = "Failed to load the camera sweep file " + cameraSweepFile + "!";
            logger.severe(msg);
            throw new RuntimeException(msg);
        }

        // Initialize
        initializeCinematic((CameraSweepData) obj, scene, cam, start);

        // Set the camera as a first step
        activateCamera(0, CAMERA_NAME);
    }

    /**
     * Creates the actual cinematic
     *
     * @param cameraSweepData the camera sweep data
     * @param scene the scene to attach to
     */
    private void initializeCinematic(final CameraSweepData cameraSweepData, Node scene, Camera cam, Point start) {
        final CameraNode camNode = bindCamera(CAMERA_NAME, cam);
        camNode.setControlDir(ControlDirection.SpatialToCamera);
        final MotionPath path = new MotionPath();
        path.setCycle(false);

        // The waypoints
        final Vector3f startLocation = new Vector3f((start.x - 0.5f) * MapLoader.TILE_WIDTH, (start.y - 0.5f) * MapLoader.TILE_HEIGHT, 0);
        for (CameraSweepDataEntry entry : cameraSweepData.getEntries()) {
            path.addWayPoint(entry.getPosition().multLocal(MapLoader.TILE_WIDTH).addLocal(startLocation));
        }
        path.setCurveTension(0);
        if (IS_DEBUG) {
            path.enableDebugShape(assetManager, scene);
        }

        final MotionEvent cameraMotionControl = new MotionEvent(camNode, path) {
            @Override
            public void update(float tpf) {
                super.update(tpf);

                // Rotate
                float progress = getCurrentValue();
                int startIndex = getCurrentWayPoint();
                int endIndex = startIndex + 1;

                // Get the rotation at previous (or current) waypoint
                Quaternion q1 = new Quaternion();
                CameraSweepDataEntry entry = cameraSweepData.getEntries().get(startIndex);
                q1.fromAxes(new Vector3f(entry.getLeft().x, entry.getLeft().z, entry.getLeft().y), new Vector3f(entry.getDirection().x, entry.getDirection().z, entry.getDirection().y), new Vector3f(entry.getUp().x, entry.getUp().z, entry.getUp().y));

                // If we are not on the last waypoint, interpolate the rotation between waypoints
                if (endIndex < cameraSweepData.getEntries().size()) {
                    Quaternion q2 = new Quaternion();
                    entry = cameraSweepData.getEntries().get(endIndex);
                    q2.fromAxes(new Vector3f(entry.getLeft().x, entry.getLeft().z, entry.getLeft().y), new Vector3f(entry.getDirection().x, entry.getDirection().z, entry.getDirection().y), new Vector3f(entry.getUp().x, entry.getUp().z, entry.getUp().y));

                    q1.slerp(q2, progress);
                }

                Quaternion quat = new Quaternion();
                quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 0, -1)); // Make it rotate normal
//                    q1.inverseLocal();

                setRotation(q1);
//                    camNode.rotate(quat);
//
//                    quat = new Quaternion();
//                    quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(1, 0, 0)); // Make it rotate normal
//                    camNode.rotate(quat);

//                    camNode.getCamera().setFrustumNear(1000);
            }
        };
        cameraMotionControl.setLoopMode(LoopMode.DontLoop);
        cameraMotionControl.setInitialDuration(cameraSweepData.getEntries().size() / getFramesPerSecond());
        cameraMotionControl.setLookAt(Vector3f.ZERO, Vector3f.ZERO);
        cameraMotionControl.setDirectionType(MotionEvent.Direction.Rotation);

        // Add us
        addCinematicEvent(0, cameraMotionControl);

        // Set duration of the whole animation
        setInitialDuration(cameraSweepData.getEntries().size() / getFramesPerSecond());
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
