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
package toniarts.openkeeper.world.room;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.LoopMode;
import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.game.data.Level;
import toniarts.openkeeper.game.data.Level.LevelType;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.FullMoon;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.map.WallSection;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.effect.EffectManagerState;
import toniarts.openkeeper.world.object.ObjectLoader;
import toniarts.openkeeper.world.room.control.FrontEndLevelControl;

/**
 * Loads up a hero gate, front end edition. Main menu. Most of the objects are
 * listed in the objects, but I don't see how they help<br>
 * TODO: Effect on the gem holder & lightning, controls for the level selection
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Deprecated
public class HeroGateFrontEnd extends GenericRoom {

    private static final short OBJECT_BANNER_ONE_ID = 89;
    //private static final int OBJECT_CANDLE_STICK_ID = 110;  // empty resource?
    //private static final int OBJECT_ARROW_ID = 115;  // empty resource?
    private static final short OBJECT_GEM_HOLDER_ID = 131;
    private static final short OBJECT_CHAIN_ID = 132;
    private static final short OBJECT_BANNER_TWO_ID = 134;
    private static final short OBJECT_BANNER_THREE_ID = 135;
    private static final short OBJECT_BANNER_FOUR_ID = 136;

    public HeroGateFrontEnd(AssetManager assetManager, RoomInstance roomInstance, ObjectLoader objectLoader, WorldState worldState, EffectManagerState effectManager) {
        super(assetManager, roomInstance, objectLoader, worldState, effectManager);
    }

    /**
     * Load an object asset
     *
     * @param model the model string
     * @param assetManager the asset manager instance
     * @param start starting point
     * @param p the current point
     * @param randomizeAnimation randomize object animation (speed and start
     * time)
     * @return reseted and ready to go model (also animated if there is a such
     * option)
     */
    private Spatial loadObject(String model, AssetManager assetManager, Point start, Point p, boolean randomizeAnimation) {
        Node object = (Node) AssetUtils.loadModel(assetManager, model, null, false, true);

        // Reset
        moveSpatial(object, start, p);
        object.move(0, MapLoader.FLOOR_HEIGHT, 0);
        animate(object, randomizeAnimation);

        return object;
    }

    private Spatial loadObject(short objectId, AssetManager assetManager, Point start,
            Point p, boolean randomizeAnimation) {

        Spatial object =  objectLoader.load(assetManager, 0, 0, objectId, roomInstance.getOwnerId());
        moveSpatial(object, start, p);
        animate(object, randomizeAnimation);

        return object;
    }

    private void animate(Spatial object, final boolean randomizeAnimation) {
        // Animate
        object.breadthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                AnimControl animControl = spatial.getControl(AnimControl.class);
                if (animControl != null) {
                    AnimChannel channel = animControl.createChannel();
                    channel.setAnim("anim");
                    channel.setLoopMode(LoopMode.Loop);
                    if (randomizeAnimation) {
                        channel.setSpeed(FastMath.nextRandomInt(6, 10) / 10f);
                        channel.setTime(FastMath.nextRandomFloat() * channel.getAnimMaxTime());
                    }

                    // Don't batch animated objects, seems not to work
                    object.setBatchHint(Spatial.BatchHint.Never);
                }
            }
        });
    }

    /**
     * Adds two candles to the tile, one to each side
     *
     * @param n node to attach to
     * @param assetManager the asset manager instance
     * @param start starting point for the room
     * @param p this tile coordinate
     */
    private void addCandles(Node n, AssetManager assetManager, Point start, Point p) {

        // The "candles"
        n.attachChild(loadObject(OBJECT_CHAIN_ID, assetManager, start, p, true).move(-1f, 0, 0));
        float yAngle = -FastMath.PI;
        n.attachChild(loadObject(OBJECT_CHAIN_ID, assetManager, start, p, true)
                .rotate(0, yAngle, 0).move(1f, 0, 1f));
    }

    /**
     * Creates and attach the level node, creates a control for it
     *
     * @param map node to attach to
     * @param type level type
     * @param level level number
     * @param variation variation, like level "a" etc.
     * @param assetManager the asset manager instance
     * @param start starting point for the room
     * @param p this tile coordinate
     * @param randomizeAnimation randomize object animation (speed and start
     * time)
     */
    private void attachAndCreateLevel(Node map, LevelType type, int levelnumber, String variation,
            AssetManager assetManager, Point start, Point p, boolean randomizeAnimation) {

        String objName = "3dmap_level";
        if (type.equals(LevelType.Secret)) {
            objName = "Secret_Level";
        }

        Spatial lvl = loadObject(objName + levelnumber + (variation == null ? "" : variation),
                assetManager, start, p, randomizeAnimation);
        lvl.addControl(new FrontEndLevelControl(new Level(type, levelnumber, variation), assetManager));
        lvl.setBatchHint(Spatial.BatchHint.Never);
        map.attachChild(lvl);
    }

    @Override
    protected BatchNode constructFloor() {
        BatchNode root = new BatchNode();
        // The front end hero gate

        // Contruct the tiles
        int i = 1;
        Point start = roomInstance.getCoordinates().get(0);
        for (Point p : roomInstance.getCoordinates()) {
            Spatial tile = AssetUtils.loadModel(assetManager, roomInstance.getRoom().getCompleteResource().getName() + i, null, false, true);

            // Reset
            moveSpatial(tile, start, p);

            root.attachChild(tile);

            // Add some objects according to the tile number
            switch (i) {
                case 2:
                    root.attachChild(loadObject(OBJECT_GEM_HOLDER_ID, assetManager, start, p, false));
                    // The light beams, I dunno, there are maybe several of these here
                    // FIXME beams is a visual effect of gem holder ???
                    float yAngle = -FastMath.PI;
                    root.attachChild(loadObject("3dfe_beams", assetManager, start, p, true)
                            .rotate(0, yAngle, 0).move(0, 0.4f, 0.1f));
                    // TODO: Add a point/spot light here

                    // Banners
                    root.attachChild(loadObject(OBJECT_BANNER_ONE_ID, assetManager, start, p, true));
                    root.attachChild(loadObject(OBJECT_BANNER_TWO_ID, assetManager, start, p, true));
                    // The "candles"
                    addCandles(root, assetManager, start, p);
                    break;
                case 5:
                    // Banners
                    root.attachChild(loadObject(OBJECT_BANNER_THREE_ID, assetManager, start, p, true));
                    root.attachChild(loadObject(OBJECT_BANNER_FOUR_ID, assetManager, start, p, true));
                    // The "candles"
                    addCandles(root, assetManager, start, p);
                    break;
                case 8:
                    // Banners
                    root.attachChild(loadObject(OBJECT_BANNER_ONE_ID, assetManager, start, p, true));
                    root.attachChild(loadObject(OBJECT_BANNER_TWO_ID, assetManager, start, p, true));
                    // The "candles"
                    addCandles(root, assetManager, start, p);
                    break;
                case 11:
                    // Banners
                    root.attachChild(loadObject(OBJECT_BANNER_ONE_ID, assetManager, start, p, true));
                    root.attachChild(loadObject(OBJECT_BANNER_TWO_ID, assetManager, start, p, true));
                    // The "candles"
                    addCandles(root, assetManager, start, p);
                    // Map
                    Node map = new Node("Map");
                    for (int x = 1; x < 21; x++) {
                        switch (x) {
                            case 6:
                                attachAndCreateLevel(map, LevelType.Level, x, "a", assetManager, start, p, false);
                                map.attachChild(loadObject("3dmaplevel" + x + "a" + "_arrows", assetManager, start, p, false));
                                attachAndCreateLevel(map, LevelType.Level, x, "b", assetManager, start, p, false);
                                map.attachChild(loadObject("3dmaplevel" + x + "b" + "_arrows", assetManager, start, p, false));
                                break;
                            case 11:
                                attachAndCreateLevel(map, LevelType.Level, x, "a", assetManager, start, p, false);
                                map.attachChild(loadObject("3dmaplevel" + x + "a" + "_arrows", assetManager, start, p, false));
                                attachAndCreateLevel(map, LevelType.Level, x, "b", assetManager, start, p, false);
                                map.attachChild(loadObject("3dmaplevel" + x + "b" + "_arrows", assetManager, start, p, false));
                                attachAndCreateLevel(map, LevelType.Level, x, "c", assetManager, start, p, false);
                                map.attachChild(loadObject("3dmaplevel" + x + "c" + "_arrows", assetManager, start, p, false));
                                break;
                            case 15:
                                attachAndCreateLevel(map, LevelType.Level, x, "a", assetManager, start, p, false);
                                map.attachChild(loadObject("3dmaplevel" + x + "a" + "_arrows", assetManager, start, p, false));
                                attachAndCreateLevel(map, LevelType.Level, x, "b", assetManager, start, p, false);
                                map.attachChild(loadObject("3dmaplevel" + x + "b" + "_arrows", assetManager, start, p, false));
                                break;
                            default:
                                attachAndCreateLevel(map, LevelType.Level, x, null, assetManager, start, p, false);
                                map.attachChild(loadObject("3dmaplevel" + x + "_arrows", assetManager, start, p, false));
                        }
                    }   // Secret levels
                    for (int x = 1; x < 6; x++) {
                        if (x == 5 && !FullMoon.isFullMoon()) {
                            // don't show full moon level
                            continue;
                        }
                        attachAndCreateLevel(map, LevelType.Secret, x, null, assetManager, start, p, false);
                    }   // The map base
                    map.attachChild(loadObject("3dmap_level21", assetManager, start, p, false));
                    // Add the map node
                    root.attachChild(map);
                    break;
                default:
                    break;
            }

            i++;
        }

        // Set the transform and scale to our scale and 0 the transform
        AssetUtils.translateToTile(root, start);

        return root;
    }

    @Override
    public Spatial getWallSpatial(Point start, WallSection.WallDirection direction) {
        return null;
    }
}
