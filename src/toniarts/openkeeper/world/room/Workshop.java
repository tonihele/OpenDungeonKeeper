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
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.EnumSet;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.map.WallSection;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.effect.EffectManagerState;
import toniarts.openkeeper.world.object.ObjectLoader;

/**
 * The workshop
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Deprecated
public class Workshop extends Normal {

    private boolean[][] bigTiles;

    public Workshop(AssetManager assetManager, RoomInstance roomInstance, ObjectLoader objectLoader, WorldState worldState, EffectManagerState effectManager) {
        super(assetManager, roomInstance, objectLoader, worldState, effectManager);
    }

    @Override
    protected void setupCoordinates() {
        super.setupCoordinates();

        // Big tile map
        bigTiles = new boolean[map.length][map[0].length];
    }

    @Override
    protected boolean useBigFloorTile(int x, int y) {

        // In workshop a big tile can't be in touch with other big tiles (except diagonally)
        // There is also catch, not always the big tile (a grill), sometimes normal + saw table
        // The logic is unknown to me, something maybe to do with the coordinates, not totally random it seems
        boolean N = hasSameTile(bigTiles, x, y - 1);
        boolean E = hasSameTile(bigTiles, x + 1, y);
        boolean S = hasSameTile(bigTiles, x, y + 1);
        boolean W = hasSameTile(bigTiles, x - 1, y);
        if (!N && !E && !S && !W) {
            bigTiles[x][y] = true;
            return true;
        }
        return false;
    }

    @Override
    protected void contructPillars(Node node) {

        // We have very different logic than the normal
        // Go through all the points and see if they are fit for pillar placement
        for (Point p : roomInstance.getCoordinates()) {

            // See that we have 2 "free" neigbouring tiles
            EnumSet<WallSection.WallDirection> freeDirections = EnumSet.noneOf(WallSection.WallDirection.class);
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
            // Face "in" diagonally
            if (freeDirections.contains(WallSection.WallDirection.NORTH) && freeDirections.contains(WallSection.WallDirection.EAST)) {
                float yAngle = -FastMath.HALF_PI;
                constructPillar(node, p, yAngle);//.move(0, MapLoader.TILE_HEIGHT, 0);
            }
            if (freeDirections.contains(WallSection.WallDirection.SOUTH) && freeDirections.contains(WallSection.WallDirection.EAST)) {
                float yAngle = FastMath.PI;
                constructPillar(node, p, yAngle);//.move(-0.15f, MapLoader.TILE_HEIGHT, -0.15f);
            }
            if (freeDirections.contains(WallSection.WallDirection.SOUTH) && freeDirections.contains(WallSection.WallDirection.WEST)) {
                float yAngle = FastMath.HALF_PI;
                constructPillar(node, p, yAngle);//.move(-0.85f, MapLoader.TILE_HEIGHT, -0.15f);
            }
            if (freeDirections.contains(WallSection.WallDirection.NORTH) && freeDirections.contains(WallSection.WallDirection.WEST)) {
                constructPillar(node, p, 0);//.move(-0.85f, MapLoader.TILE_HEIGHT, -0.85f);
            }
        }
    }

    private Spatial constructPillar(Node node, Point p, float yAngle) {
        Spatial part = AssetUtils.loadModel(assetManager, getPillarResource(), null);
        moveSpatial(part, p);

        if (yAngle != 0) {
            part.rotate(0, yAngle, 0);
        }

        node.attachChild(part);
        return part;
    }

    @Override
    protected RoomObjectLayout getRoomObjectLayout() {
        return RoomObjectLayout.ALLOW_DIAGONAL_NEIGHBOUR_ONLY;
    }

}
