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
package toniarts.openkeeper.view.map.construction;

import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.utils.AssetUtils;

/**
 * Constructs 5 by 5 "rotated" buildings. As far as I know, only Dungeon Heart
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class FiveByFiveRotatedConstructor extends RoomConstructor {

    public FiveByFiveRotatedConstructor(AssetManager assetManager, RoomInstance roomInstance) {
        super(assetManager, roomInstance);
    }

    @Override
    protected BatchNode constructFloor() {
        BatchNode root = new BatchNode();

        // 5 by 5
        // DHeart Piece[1-20]Exp.j3o
        //Point start = roomInstance.getCoordinates().get(0);
        ArtResource artResource = roomInstance.getRoom().getCompleteResource();
        String resource = (roomInstance.isDestroyed()) ? "Dungeon_Destroyed" : artResource.getName();
        for (Point p : roomInstance.getCoordinates()) {

            // There are just 4 different pieces
            int x = p.x - start.x;
            int y = p.y - start.y;
            Spatial tile;
            float yAngle = 0;
            int piece = -1;
            // Corners
            if (x == 0 && y == 0) { // Top left corner
                piece = 3;
                yAngle = FastMath.HALF_PI;
            } else if (x == 4 && y == 0) { // Top right corner
                piece = 3;
            } else if (x == 0 && y == 4) { // Lower left corner
                piece = 3;
                yAngle = -FastMath.PI;
            } else if (x == 4 && y == 4) { // Lower right corner
                piece = 3;
                yAngle = -FastMath.HALF_PI;
            } // Outer layer sides
            else if (x == 0) { // Left side
                piece = 2;
                yAngle = -FastMath.PI;
            } else if (x == 4) { // Right side
                piece = 2;
            } else if (y == 0) { // Top side
                piece = 2;
                yAngle = FastMath.HALF_PI;
            } else if (y == 4) { // Bottom side
                piece = 2;
                yAngle = -FastMath.HALF_PI;
            } // The inner ring, corners
            else if (x == 1 && y == 1) { // Top left
                piece = 0;
            } else if (x == 3 && y == 1) { // Top right
                piece = 0;
                yAngle = -FastMath.HALF_PI;
            } else if (x == 1 && y == 3) { // Bottom left
                piece = 0;
                yAngle = FastMath.HALF_PI;
            } else if (x == 3 && y == 3) { // Bottom right
                piece = 0;
                yAngle = -FastMath.PI;
            } // The inner ring, sides
            else if (x == 1) { // Left
                piece = 1;
                yAngle = FastMath.HALF_PI;
            } else if (x == 3) { // Right
                piece = 1;
                yAngle = -FastMath.HALF_PI;
            } else if (y == 1) { // Top
                piece = 1;
            } else if (y == 3) { // Bottom
                piece = 1;
                yAngle = -FastMath.PI;
            }

            if (piece != -1) {
                tile = loadModel(resource + piece, artResource);
                moveSpatial(tile, start, p);
                if (yAngle != 0) {
                    tile.rotate(0, yAngle, 0);
                }
                root.attachChild(tile);
            }

            // Only observed 5 by 5 is the Dungeon Heart, its object list is empty, so I just hard code these here
            // The center pieces
//            if (x == 2 && y == 2) {
//
//                if (destroyed) {
//                    tile = loadModel(resource + 4);
//                    moveSpatial(tile, start, p);
//                    root.attachChild(tile);
//
//                    // The steps between the arches
//                    tile = loadModel(resource + 5);
//                    moveSpatial(tile, start, p);
//                    root.attachChild(tile);
//
//                } else {
//                    // The arches
//                    tile = objectLoader.load(assetManager, 0, 0, OBJECT_ARCHES_ID, getRoomInstance().getOwnerId());
//                    tile.move(0, -MapViewController.FLOOR_HEIGHT, 0);
//                    moveSpatial(tile, start, p);
//                    root.attachChild(tile);
//
//                    // The steps between the arches
//                    tile = objectLoader.load(assetManager, 0, 0, OBJECT_BIG_STEPS_ID, getRoomInstance().getOwnerId());
//                    tile.move(0, -MapViewController.FLOOR_HEIGHT, 0);
//                    moveSpatial(tile, start, p);
//                    root.attachChild(tile);
//
//                    tile = objectLoader.load(assetManager, 0, 0, OBJECT_BIG_STEPS_ID, getRoomInstance().getOwnerId());
//                    tile.move(0, -MapViewController.FLOOR_HEIGHT, 0);
//                    moveSpatial(tile, start, p);
//                    tile.rotate(0, -FastMath.TWO_PI / 3, 0);
//                    root.attachChild(tile);
//
//                    tile = objectLoader.load(assetManager, 0, 0, OBJECT_BIG_STEPS_ID, getRoomInstance().getOwnerId());
//                    tile.move(0, -MapViewController.FLOOR_HEIGHT, 0);
//                    moveSpatial(tile, start, p);
//                    tile.rotate(0, FastMath.TWO_PI / 3, 0);
//                    root.attachChild(tile);
//
//                    // The alfa & omega! The heart, TODO: use object loader once it is in decent condition, this after all is a real object
//                    if (centreDecay == -1) {
//                        tile = objectLoader.load(assetManager, 0, 0, OBJECT_HEART_ID, getRoomInstance().getOwnerId());
//                        tile.move(0, -MapViewController.FLOOR_HEIGHT, 0);
//                    } else {
//                        tile = loadModel("DungeonCentre_DECAY" + centreDecay);
//                    }
//                    moveSpatial(tile, start, p);
//
//                    // Animate
//                    tile.breadthFirstTraversal(new SceneGraphVisitor() {
//                        @Override
//                        public void visit(Spatial spatial) {
//                            AnimControl animControl = spatial.getControl(AnimControl.class);
//                            if (animControl != null) {
//                                AnimChannel channel = animControl.createChannel();
//                                channel.setAnim(KmfModelLoader.DUMMY_ANIM_CLIP_NAME);
//                                AnimationLoader.setLoopModeOnChannel(spatial, channel);
//
//                                // Don't batch animated objects, seems not to work
//                                spatial.setBatchHint(Spatial.BatchHint.Never);
//                            }
//                        }
//                    });
//
//                    root.attachChild(tile.move(0, MapViewController.TILE_HEIGHT / 4, 0));
//
//                    for (Integer id : roomInstance.getRoom().getEffects()) {
//                        if (id > 0) {
//                            effectManager.load(root, new Vector3f(p.x - start.x, MapViewController.UNDERFLOOR_HEIGHT, p.y - start.y), id, true);
//                        }
//                    }
//
//                    if (!created) {
//                        created = true;
//
//                        tile = objectLoader.load(assetManager, 0, 0, OBJECT_PLUG_ID, getRoomInstance().getOwnerId());
//                        tile.move(0, -MapViewController.FLOOR_HEIGHT, 0);
//                        tile.setName("plug");
//                        tile.setBatchHint(Spatial.BatchHint.Never);
//                        tile.rotate(0, FastMath.QUARTER_PI, 0);
//                        moveSpatial(tile, start, p);
//                        root.attachChild(tile.move(0, MapViewController.FLOOR_HEIGHT, 0));
//
//                        Node plug = getPlug();
//                        plug.setName("plug_decay");
//                        plug.setCullHint(Spatial.CullHint.Always);
//                        plug.setBatchHint(Spatial.BatchHint.Never);
//                        root.attachChild(plug.move(x, MapViewController.FLOOR_HEIGHT, y));
//
//                        root.addControl(new PlugControl());
//                    }
//                }
//            }
        }

        // Set the transform and scale to our scale and 0 the transform
        AssetUtils.translateToTile(root, start);
        //root.scale(MapViewController.TILE_WIDTH, MapViewController.TILE_HEIGHT, MapViewController.TILE_WIDTH);

        return root;
    }

}
