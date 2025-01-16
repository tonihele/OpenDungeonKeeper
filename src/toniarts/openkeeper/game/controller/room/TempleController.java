/*
 * Copyright (C) 2014-2019 OpenKeeper
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
package toniarts.openkeeper.game.controller.room;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.map.WallSection;

/**
 * The temple... there is the hand and special pillar (rather torch) placement
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class TempleController extends DoubleQuadController {

    public static final short OBJECT_TEMPLE_HAND_ID = 66;
    private static final int MIN_HAND_SIZE = 5;

    public TempleController(KwdFile kwdFile, toniarts.openkeeper.common.RoomInstance roomInstance, IObjectsController objectsController) {
        super(kwdFile, roomInstance, objectsController);
    }

    @Override
    protected void constructObjects() {
        super.constructObjects();

        // The temple hand is placed basically in center of the first 5x5 square, given our normal traversing direction
        // It is by no means in the center of the temple in all shapes
        Point p = getFirstSubSquare(map, MIN_HAND_SIZE);
        if (p != null) {
            Point worldPoint = roomInstance.localCoordinateToWorldCoordinate(p.x - (MIN_HAND_SIZE / 2), p.y - (MIN_HAND_SIZE / 2));
            floorFurniture.add(objectsController.loadObject(OBJECT_TEMPLE_HAND_ID, roomInstance.getOwnerId(), worldPoint.x, worldPoint.y));
        }
    }

    @Override
    protected List<EntityId> constructPillars() {

        // We have very different logic than the normal
        // Go through all the points and see if they are fit for pillar placement
        List<EntityId> pillars = new ArrayList<>();
        for (Point p : roomInstance.getCoordinates()) {

            // See that we have 2 "free" neigbouring tiles
            Set<WallSection.WallDirection> freeDirections = EnumSet.noneOf(WallSection.WallDirection.class);
            if (!hasSameTile(map, p.x - start.x, p.y - start.y - 1)) { // North
                freeDirections.add(WallSection.WallDirection.NORTH);
            }
            if (!hasSameTile(map, p.x - start.x, p.y - start.y + 1)) { // South
                freeDirections.add(WallSection.WallDirection.SOUTH);
            }
            if (!hasSameTile(map, p.x - start.x + 1, p.y - start.y)) { // East
                freeDirections.add(WallSection.WallDirection.EAST);
            }
            if (!hasSameTile(map, p.x - start.x - 1, p.y - start.y)) { // West
                freeDirections.add(WallSection.WallDirection.WEST);
            }

            // We may have up to 4 pillars in the same tile even, every corner gets one, no need to check anything else
            // Add a pillar
            // The model is in the center, rotation will not do any good, nudge them over the small sub quads, or corners of the tile
            if (freeDirections.contains(WallSection.WallDirection.NORTH) && freeDirections.contains(WallSection.WallDirection.EAST)) {
                Vector3f pos = WorldUtils.pointToVector3f(p.x, p.y);
                pos.x = pos.x + WorldUtils.TILE_WIDTH / 4;
                pos.z = pos.z - WorldUtils.TILE_WIDTH / 4;
                pillars.add(constructPillar(pos));
            }
            if (freeDirections.contains(WallSection.WallDirection.SOUTH) && freeDirections.contains(WallSection.WallDirection.EAST)) {
                Vector3f pos = WorldUtils.pointToVector3f(p.x, p.y);
                pos.x = pos.x + WorldUtils.TILE_WIDTH / 4;
                pos.z = pos.z + WorldUtils.TILE_WIDTH / 4;
                pillars.add(constructPillar(pos));
            }
            if (freeDirections.contains(WallSection.WallDirection.SOUTH) && freeDirections.contains(WallSection.WallDirection.WEST)) {
                Vector3f pos = WorldUtils.pointToVector3f(p.x, p.y);
                pos.x = pos.x - WorldUtils.TILE_WIDTH / 4;
                pos.z = pos.z + WorldUtils.TILE_WIDTH / 4;
                pillars.add(constructPillar(pos));
            }
            if (freeDirections.contains(WallSection.WallDirection.NORTH) && freeDirections.contains(WallSection.WallDirection.WEST)) {
                Vector3f pos = WorldUtils.pointToVector3f(p.x, p.y);
                pos.x = pos.x - WorldUtils.TILE_WIDTH / 4;
                pos.z = pos.z - WorldUtils.TILE_WIDTH / 4;
                pillars.add(constructPillar(pos));
            }
        }
        return pillars;
    }

    private EntityId constructPillar(Vector3f pos) {

        // Construct a pillar
        return objectsController.loadObject(getPillarObject(roomInstance.getRoom().getRoomId()), roomInstance.getOwnerId(), pos, 0);
    }

    /**
     * Finds sub matrix from bigger matrix. Determined by the given size.
     *
     * @param matrix the matrix to search from
     * @param minSize the minimum size of the square to be searched
     * @return null if no such matrix is found, otherwise the lower right
     * coordinate of the matrix
     */
    private static Point getFirstSubSquare(boolean matrix[][], int minSize) {

        // Check the size, is it even possible to find such
        if (matrix.length < minSize || matrix[0].length < minSize) {
            return null;
        }

        int i, j;
        int R = matrix.length;         // no rows in M[][]
        int C = matrix[0].length;     // no columns in M[][]
        int S[][] = new int[R][C];

        int size;

        // Set first column of S[][]
        for (i = 0; i < R; i++) {
            S[i][0] = matrix[i][0] ? 1 : 0;
        }

        // Set first row of S[][]
        for (j = 0; j < C; j++) {
            S[0][j] = matrix[0][j] ? 1 : 0;
        }

        // Construct other entries of S[][]
        for (i = 1; i < R; i++) {
            for (j = 1; j < C; j++) {
                if (matrix[i][j]) {
                    S[i][j] = Math.min(S[i][j - 1],
                            Math.min(S[i - 1][j], S[i - 1][j - 1])) + 1;
                } else {
                    S[i][j] = 0;
                }
            }
        }

        // Find the maximum entry, and indexes of maximum entry in S[][]
        // Follow our traversing direction!
        size = S[0][0];
        for (j = 0; j < C; j++) {
            for (i = 0; i < R; i++) {
                if (size < S[i][j]) {
                    size = S[i][j];
                    if (size >= minSize) {

                        // Found it
                        return new Point(i, j);
                    }
                }
            }
        }

        return null;
    }

}
