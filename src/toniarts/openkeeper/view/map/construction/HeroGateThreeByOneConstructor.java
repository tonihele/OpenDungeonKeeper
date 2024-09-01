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
import toniarts.openkeeper.tools.convert.map.Thing.Room.Direction;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.view.map.WallSection;
import toniarts.openkeeper.view.map.WallSection.WallDirection;

/**
 * FIXME some problem with wall when reinforce near tile
 *
 * @author ArchDemon
 */
public class HeroGateThreeByOneConstructor extends RoomConstructor {

    public HeroGateThreeByOneConstructor(AssetManager assetManager, RoomInstance roomInstance) {
        super(assetManager, roomInstance);
    }

    @Override
    protected BatchNode constructFloor() {
        BatchNode root = new BatchNode();
        ArtResource artResource = roomInstance.getRoom().getCompleteResource();
        String modelName = artResource.getName();
        //Point center = roomInstance.getCenter();
        // Contruct the tiles
        int j = 0;
        for (Point p : roomInstance.getCoordinates()) {
            int piece = (roomInstance.getDirection() == Direction.WEST || roomInstance.getDirection() == Direction.SOUTH) ? j + 3 : 5 - j;
            Spatial tile = AssetUtils.loadModel(assetManager, modelName + piece, artResource, false, true);
            j++;
            moveSpatial(tile, p);
            root.attachChild(tile);

            // Set the transform and scale to our scale and 0 the transform
            switch (roomInstance.getDirection()) {
                case NORTH:
                    tile.rotate(0, -FastMath.HALF_PI, 0);
                    break;
                case EAST:
                    tile.rotate(0, FastMath.PI, 0);
                    break;
                case SOUTH:
                    tile.rotate(0, FastMath.HALF_PI, 0);
                    break;
            }
        }
        //root.move(-MapViewController.TILE_WIDTH / 2, 0, -MapViewController.TILE_WIDTH / 2);

        return root;
    }

    @Override
    protected BatchNode constructWall() {
        BatchNode root = new BatchNode();
        // Get the wall points
        ArtResource artResource = roomInstance.getRoom().getCompleteResource();
        String modelName = artResource.getName();
        for (WallSection section : roomInstance.getWallSections()) {

            int i = 0;
            int sectionSize = section.coordinates().size();

            for (Point p : section.coordinates()) {

                int piece;
                if (sectionSize == 3) {
                    piece = (section.direction() == WallDirection.EAST
                            || section.direction() == WallDirection.NORTH) ? 2 - i : i;
                } else {
                    piece = 6;
                }
                i++;
                float yAngle = 0;
                if (section.direction() == WallDirection.NORTH) {
                    yAngle = -FastMath.HALF_PI;
                } else if (section.direction() == WallDirection.SOUTH) {
                    yAngle = FastMath.HALF_PI;
                } else if (section.direction() == WallDirection.EAST) {
                    yAngle = FastMath.PI;
                }
                //yAngle = -section.getDirection().ordinal() * FastMath.HALF_PI;

                Spatial tile = AssetUtils.loadModel(assetManager, modelName + piece + (roomInstance.isDestroyed() ? "DESTROYED" : ""), artResource, false, true);
                if (yAngle != 0) {
                    tile.rotate(0, yAngle, 0);
                }
                moveSpatial(tile, p);
                //tile.move(-MapViewController.TILE_WIDTH / 2, 0, -MapViewController.TILE_WIDTH / 2);
                root.attachChild(tile);
            }
        }
        return root;
    }

    @Override
    public Spatial getWallSpatial(Point p, WallSection.WallDirection direction) {
        return null;
    }

}
