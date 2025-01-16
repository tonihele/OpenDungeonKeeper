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

import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.view.map.WallSection;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.effect.EffectManagerState;
import toniarts.openkeeper.world.object.ObjectLoader;

/**
 *
 * @author ArchDemon
 */
@Deprecated
public class HeroGate extends GenericRoom {

    public HeroGate(AssetManager assetManager, RoomInstance roomInstance, ObjectLoader objectLoader, WorldState worldState, EffectManagerState effectManager) {
        super(assetManager, roomInstance, objectLoader, worldState, effectManager);
    }

    @Override
    protected BatchNode constructFloor() {
        BatchNode root = new BatchNode();
        // Contruct the tiles
        Point start = roomInstance.getCoordinates().get(0);
        String modelName = roomInstance.getRoom().getCompleteResource().getName();
        for (Point p : roomInstance.getCoordinates()) {
            Spatial tile;
            int piece = 2;
            float yAngle = 0;
            // Figure out which peace by seeing the neighbours
            boolean N = roomInstance.hasCoordinate(new Point(p.x, p.y - 1));
            boolean E = roomInstance.hasCoordinate(new Point(p.x + 1, p.y));
            boolean S = roomInstance.hasCoordinate(new Point(p.x, p.y + 1));
            boolean W = roomInstance.hasCoordinate(new Point(p.x - 1, p.y));

            if (!N && E && W) {
                piece = 1;
            } else if (!S && !E && !W) {
                tile = AssetUtils.loadModel(assetManager, roomInstance.getRoom().getCapResource().getName(), null, false, true);
                moveSpatial(tile, start, p);
                root.attachChild(tile);
                piece = 9;
            } else if (!W) {
                piece = 3;
            } else if (!E) {
                piece = 3;
                yAngle = - 2 * FastMath.HALF_PI;
            }

            tile = AssetUtils.loadModel(assetManager, modelName + piece, null, false, true);
            // Reset
            moveSpatial(tile, start, p);
            if (yAngle != 0) {
                tile.rotate(0, yAngle, 0);
            }
            // Set the shadows
            //tile.setShadowMode(RenderQueue.ShadowMode.Receive);

            root.attachChild(tile);
        }

        // Set the transform and scale to our scale and 0 the transform
        AssetUtils.translateToTile(root, start);

        return root;
    }

    @Override
    protected BatchNode constructWall() {
        BatchNode root = new BatchNode();
        // Get the wall points
        Point start = roomInstance.getCoordinates().get(0);
        String modelName = roomInstance.getRoom().getCompleteResource().getName();
        int south = 0;
        /*for (WallSection section : roomInstance.getWallSections()) {
            int i = 0;
            for (Point p : section.getCoordinates()) {
                int piece;

                Spatial part;
                float yAngle = 0;
                switch (section.getDirection()) {
                    case SOUTH:
                        piece = (i == 1) ? 5 : 7;
                        break;

                    case EAST:
                        if (section.getCoordinates().size() == 1) {
                            piece = 6; // outside gate
                        } else {
                            piece = 7;
                            yAngle = FastMath.HALF_PI;
                        }
                        break;

                    case WEST:
                        // FIXME if gate skip walls ???
                        if (section.getCoordinates().size() == 1) {
                            piece = 6; // inside gate
                            yAngle = FastMath.PI;
                        } else {
                            piece = 7;
                            yAngle = -FastMath.HALF_PI;
                        }
                        break;

                    default:
                        // WallSection.WallDirection.NORTH
                        // FIXME looks good, but ... ugly code
                        if (south == 0) {
                            piece = 4;
                        } else if (south == 1) {
                            piece = 8;
                        } else {
                            continue; // gate but we build it on EAST and WEST
                        }
                        south++;
                        yAngle = FastMath.PI;
                        break;
                }

                i++;
                part = AssetUtils.loadModel(assetManager, modelName + piece, null, false, true);
                if (yAngle != 0) {
                    part.rotate(0, yAngle, 0);
                }
                moveSpatial(part, p);
                //part.move(-MapLoader.TILE_WIDTH / 2, 0, -MapLoader.TILE_WIDTH / 2);
                root.attachChild(part);
            }
        }*/
        return root;
    }

    @Override
    public Spatial getWallSpatial(Point p, WallSection.WallDirection direction) {
        return null;
    }
}
