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
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.utils.Point;
import java.util.Arrays;
import java.util.EnumSet;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.view.map.WallSection.WallDirection;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.effect.EffectManagerState;
import toniarts.openkeeper.world.object.ObjectLoader;

/**
 * Constructs "normal" rooms
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Deprecated
public class Normal extends GenericRoom {

    public Normal(AssetManager assetManager, RoomInstance roomInstance, ObjectLoader objectLoader, WorldState worldState, EffectManagerState effectManager) {
        super(assetManager, roomInstance, objectLoader, worldState, effectManager);
    }

    @Override
    public Spatial construct() {
        super.construct();

        // Pillars
        if (hasPillars()) {
            BatchNode pillarsNode = new BatchNode("Pillars");
            contructPillars(pillarsNode);
            if (!pillarsNode.getChildren().isEmpty()) {
                pillarsNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
                pillarsNode.batch();
                getRootNode().attachChild(pillarsNode);
            }
        }

        return getRootNode();
    }

    @Override
    protected BatchNode constructFloor() {
        BatchNode root = new BatchNode();
        String modelName = roomInstance.getRoom().getCompleteResource().getName();
        // Normal rooms
        for (int x = 0; x < map.length; x++) {
            for (int y = 0; y < map[x].length; y++) {

                // Skip non-room tiles
                if (!map[x][y]) {
                    continue;
                }

                // There are 4 different floor pieces
                Spatial part;

                // Figure out which piece by seeing the neighbours
                boolean N = hasSameTile(map, x, y - 1);
                boolean NE = hasSameTile(map, x + 1, y - 1);
                boolean E = hasSameTile(map, x + 1, y);
                boolean SE = hasSameTile(map, x + 1, y + 1);
                boolean S = hasSameTile(map, x, y + 1);
                boolean SW = hasSameTile(map, x - 1, y + 1);
                boolean W = hasSameTile(map, x - 1, y);
                boolean NW = hasSameTile(map, x - 1, y - 1);

                // If we are completely covered, use a big tile
                if (N && NE && E && SE && S && SW && W && NW && useBigFloorTile(x, y)) {
                    part = AssetUtils.loadModel(assetManager, modelName + "9", null);
                } else {
                    part = Quad.constructQuad(assetManager, modelName, N, NE, E, SE, S, SW, W, NW);
                }
                AssetUtils.translateToTile(part, new Point(x, y));
                root.attachChild(part);
            }
        }

        // Set the transform and scale to our scale and 0 the transform
        AssetUtils.translateToTile(root, start);

        return root;
    }

    /**
     * Does the room have pillars
     *
     * @return has pillars
     */
    protected boolean hasPillars() {
        return true; // I have no idea where to get this data
    }

    /**
     * Construct room pillars. Info in:
     * https://github.com/tonihele/OpenKeeper/issues/116
     *
     * @param node the pillar node
     */
    protected void contructPillars(Node node) {

        // NOTE: I don't understand how the pillars are referenced, they are neither in Terrain nor Room, but they are listed in the Objects. Even Lair has pillars, but I've never seem the in-game
        // Pillars go into all at least 3x3 corners, there can be more than 4 pillars per room
        // Go through all the points and see if they are fit for pillar placement
        for (Point p : roomInstance.getCoordinates()) {

            // See that we have 2 "free" neigbouring tiles
            EnumSet<WallDirection> freeDirections = EnumSet.noneOf(WallDirection.class);
            if (!hasSameTile(map, p.x - start.x, p.y - start.y - 1)) { // North
                freeDirections.add(WallDirection.NORTH);
            }
            if (!hasSameTile(map, p.x - start.x, p.y - start.y + 1)) { // South
                freeDirections.add(WallDirection.SOUTH);
            }
            if (!hasSameTile(map, p.x - start.x + 1, p.y - start.y)) { // East
                freeDirections.add(WallDirection.EAST);
            }
            if (!hasSameTile(map, p.x - start.x - 1, p.y - start.y)) { // West
                freeDirections.add(WallDirection.WEST);
            }

            // If we have 2, see the other directions that they have 3x3 the same tile
            if (freeDirections.size() == 2 && !freeDirections.containsAll(Arrays.asList(WallDirection.NORTH, WallDirection.SOUTH)) && !freeDirections.containsAll(Arrays.asList(WallDirection.WEST, WallDirection.EAST))) {
                boolean found = true;
                loop:
                for (int y = 0; y < 3; y++) {
                    for (int x = 0; x < 3; x++) {

                        // Get the tile from wanted direction
                        int xPoint = x;
                        int yPoint = y;
                        if (freeDirections.contains(WallDirection.EAST)) { // Go west
                            xPoint = -x;
                        }
                        if (freeDirections.contains(WallDirection.SOUTH)) { // Go north
                            yPoint = -y;
                        }
                        if (!hasSameTile(map, p.x - start.x + xPoint, p.y - start.y + yPoint)) {
                            found = false;
                            break loop;
                        }
                    }
                }

                // Add
                if (found) {

                    // Contruct a pillar
                    Spatial part = AssetUtils.loadModel(assetManager, getPillarResource(), null);
                    // Face "in" diagonally
                    if (freeDirections.contains(WallDirection.NORTH)
                            && freeDirections.contains(WallDirection.EAST)) {
                        float yAngle = -FastMath.HALF_PI;
                        part.rotate(0, yAngle, 0);
                    } else if (freeDirections.contains(WallDirection.SOUTH)
                            && freeDirections.contains(WallDirection.EAST)) {
                        float yAngle = FastMath.PI;
                        part.rotate(0, yAngle, 0);
                    } else if (freeDirections.contains(WallDirection.SOUTH)
                            && freeDirections.contains(WallDirection.WEST)) {
                        float yAngle = FastMath.HALF_PI;
                        part.rotate(0, yAngle, 0);
                    }

                    part.move(0, MapLoader.FLOOR_HEIGHT, 0);
                    moveSpatial(part, p);
                    node.attachChild(part);
                }
            }
        }
    }

    /**
     * Get the pillar resource. These are actually in object list. But there are
     * no mapping to my knowledge. So hard code :(
     * @deprecated use toniarts.openkeeper.world.room.GenericRoom.getPillarObject
     *
     * @return room pillar resource
     */
    @Deprecated
    protected String getPillarResource() {
        return roomInstance.getRoom().getCompleteResource().getName() + "_Pillar";
    }

    /**
     * Use the big floor tile at the specified point
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @return use big tile
     */
    protected boolean useBigFloorTile(int x, int y) {
        return true;
    }
}
