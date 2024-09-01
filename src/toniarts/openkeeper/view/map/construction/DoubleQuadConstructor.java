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
import com.jme3.math.Vector3f;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.map.MapViewController;

/**
 *
 * @author ArchDemon
 */
public class DoubleQuadConstructor extends RoomConstructor {

    public DoubleQuadConstructor(AssetManager assetManager, RoomInstance roomInstance) {
        super(assetManager, roomInstance);
    }

    @Override
    protected BatchNode constructFloor() {
        BatchNode root = new BatchNode();
        String modelName = roomInstance.getRoom().getCompleteResource().getName();

        // Contruct the tiles
        for (int y = 0; y < map[0].length; y++) {
            for (int x = 0; x < map.length; x++) {

                // Skip non-room tiles
                if (!roomInstance.getCoordinatesAsMatrix()[x][y]) {
                    continue;
                }

                // Figure out which peace by seeing the neighbours
                boolean N = hasSameTile(map, x, y - 1);
                boolean NE = hasSameTile(map, x + 1, y - 1);
                boolean E = hasSameTile(map, x + 1, y);
                boolean SE = hasSameTile(map, x + 1, y + 1);
                boolean S = hasSameTile(map, x, y + 1);
                boolean SW = hasSameTile(map, x - 1, y + 1);
                boolean W = hasSameTile(map, x - 1, y);
                boolean NW = hasSameTile(map, x - 1, y - 1);

                boolean northInside = isTileInside(map, x, y - 1);
                boolean northEastInside = isTileInside(map, x + 1, y - 1);
                boolean eastInside = isTileInside(map, x + 1, y);
                boolean southEastInside = isTileInside(map, x + 1, y + 1);
                boolean southInside = isTileInside(map, x, y + 1);
                boolean southWestInside = isTileInside(map, x - 1, y + 1);
                boolean westInside = isTileInside(map, x - 1, y);
                boolean northWestInside = isTileInside(map, x - 1, y - 1);

                // 2x2
                Node model = constructQuad(assetManager, modelName, N, NE, E, SE, S, SW, W, NW,
                        northWestInside, northEastInside, southWestInside, southEastInside,
                        northInside, eastInside, southInside, westInside);
                AssetUtils.translateToTile(model, new Point(x, y));
                root.attachChild(model);
            }
        }

        AssetUtils.translateToTile(root, start);

        return root;
    }

    /**
     * Checks if the tile is fully inside
     *
     * @param map the room map matrix to check
     * @param x the x coordinate to check
     * @param y the y coordinate to check
     * @return true if given point is fully surrounded by the room
     */
    protected static boolean isTileInside(boolean[][] map, int x, int y) {
        if (!hasSameTile(map, x, y)) {
            return false;
        }

        boolean N = hasSameTile(map, x, y - 1);
        boolean NE = hasSameTile(map, x + 1, y - 1);
        boolean E = hasSameTile(map, x + 1, y);
        boolean SE = hasSameTile(map, x + 1, y + 1);
        boolean S = hasSameTile(map, x, y + 1);
        boolean SW = hasSameTile(map, x - 1, y + 1);
        boolean W = hasSameTile(map, x - 1, y);
        boolean NW = hasSameTile(map, x - 1, y - 1);

        return N && NE && NW && E && SE && S && SW && W && NW;
    }

    public static Node constructQuad(AssetManager assetManager, String modelName, boolean N, boolean NE, boolean E, boolean SE, boolean S, boolean SW, boolean W, boolean NW,
            boolean northWestInside, boolean northEastInside, boolean southWestInside, boolean southEastInside,
            boolean northInside, boolean eastInside, boolean southInside, boolean westInside) {
        Node quad = new Node();
        for (int i = 0; i < 2; i++) {
            for (int k = 0; k < 2; k++) {

                // 4 - 8 - walls
                // 9 full floor?
                // 10-12, 14-19 are non-wall walls (i.e. prison bars)
                int piece = 0;
                float yAngle = 0;
                Vector3f movement;
                boolean inside = N && NE && NW && E && SE && S && SW && W && NW;

                // Determine the piece
                if (i == 0 && k == 0) { // North west corner
                    if (inside) {
                        piece = 13;
                    } else if (northInside && westInside) {
                        piece = 12;
                        yAngle = FastMath.HALF_PI;
                    } else if (northWestInside && westInside) {
                        piece = 10;
                        yAngle = -FastMath.HALF_PI;
                    } else if (northWestInside && northInside) {
                        piece = 10;
                        yAngle = FastMath.PI;
                    } else if (westInside && N && NW) {
                        piece = 10;
                        yAngle = -FastMath.HALF_PI;
                    } else if (northInside && W && NW) {
                        piece = 10;
                        yAngle = FastMath.PI;
                    } else if (northWestInside && N && W) {
                        piece = 11;
                        yAngle = -FastMath.HALF_PI;
                    } else if (N && NW && W) {
                        piece = 3;
                    } else if (N && W) {
                        piece = 2;
                        yAngle = FastMath.HALF_PI;
                    } else if (!N && !W) {
                        piece = 1;
                        yAngle = FastMath.HALF_PI;
                    } else if (N && !W) {
                        piece = 0;
                        yAngle = FastMath.HALF_PI;
                    }
                    movement = new Vector3f(-WorldUtils.TILE_WIDTH / 4, 0, -WorldUtils.TILE_WIDTH / 4);
                } else if (i == 1 && k == 0) { // North east corner
                    if (inside) {
                        piece = 13;
                    } else if (northInside && eastInside) {
                        piece = 12;
                    } else if (northEastInside && eastInside) {
                        piece = 10;
                        yAngle = FastMath.HALF_PI;
                    } else if (northEastInside && northInside) {
                        piece = 10;
                        yAngle = FastMath.PI;
                    } else if (eastInside && N && NE) {
                        piece = 10;
                        yAngle = FastMath.HALF_PI;
                    } else if (northInside && E && NE) {
                        piece = 10;
                        yAngle = FastMath.PI;
                    } else if (northEastInside && N && NE) {
                        piece = 11;
                        yAngle = FastMath.PI;
                    } else if (N && NE && E) {
                        piece = 3;
                    } else if (N && E) {
                        piece = 2;
                    } else if (!N && !E) {
                        piece = 1;
                    } else if (N && !E) {
                        piece = 0;
                        yAngle = -FastMath.HALF_PI;
                    }
                    movement = new Vector3f(WorldUtils.TILE_WIDTH / 4, 0, -WorldUtils.TILE_WIDTH / 4);
                } else if (i == 0 && k == 1) { // South west corner
                    if (inside) {
                        piece = 13;
                    } else if (southInside && westInside) {
                        piece = 12;
                        yAngle = FastMath.PI;
                    } else if (southWestInside && westInside) {
                        piece = 10;
                        yAngle = -FastMath.HALF_PI;
                    } else if (southWestInside && southInside) {
                        piece = 10;
                    } else if (westInside && S && SW) {
                        piece = 10;
                        yAngle = -FastMath.HALF_PI;
                    } else if (southInside && W && SW) {
                        piece = 10;
                    } else if (southWestInside && S && W) {
                        piece = 11;
                    } else if (S && SW && W) {
                        piece = 3;
                    } else if (S && W) {
                        piece = 2;
                        yAngle = FastMath.PI;
                    } else if (!S && !W) {
                        piece = 1;
                        yAngle = FastMath.PI;
                    } else if (!W && S) {
                        piece = 0;
                        yAngle = FastMath.HALF_PI;
                    } else if (W && !S) {
                        piece = 0;
                        yAngle = FastMath.PI;
                    }
                    movement = new Vector3f(-WorldUtils.TILE_WIDTH / 4, 0, WorldUtils.TILE_WIDTH / 4);
                } else { // South east corner  if (i == 1 && k == 1)
                    if (inside) {
                        piece = 13;
                    } else if (southInside && eastInside) {
                        piece = 12;
                        yAngle = -FastMath.HALF_PI;
                    } else if (southEastInside && eastInside) {
                        piece = 10;
                        yAngle = FastMath.HALF_PI;
                    } else if (southEastInside && southInside) {
                        piece = 10;
                    } else if (eastInside && S && SE) {
                        piece = 10;
                        yAngle = FastMath.HALF_PI;
                    } else if (southInside && E && SE) {
                        piece = 10;
                    } else if (southEastInside && S && SE) {
                        piece = 11;
                        yAngle = FastMath.HALF_PI;
                    } else if (S && SE && E) {
                        piece = 3;
                    } else if (S && E) {
                        piece = 2;
                        yAngle = -FastMath.HALF_PI;
                    } else if (!S && !E) {
                        piece = 1;
                        yAngle = -FastMath.HALF_PI;
                    } else if (!E && S) {
                        piece = 0;
                        yAngle = -FastMath.HALF_PI;
                    } else if (E && !S) {
                        piece = 0;
                        yAngle = FastMath.PI;
                    }
                    movement = new Vector3f(WorldUtils.TILE_WIDTH / 4, 0, WorldUtils.TILE_WIDTH / 4);
                }
                // Load the piece
                Spatial part = AssetUtils.loadModel(assetManager, modelName + piece, null);
                part.rotate(0, yAngle, 0);
                part.move(movement);

                quad.attachChild(part);
            }
        }

        return quad;
    }

}
