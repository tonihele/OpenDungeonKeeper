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
package toniarts.openkeeper.game.controller.room;

import com.jme3.math.FastMath;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.view.map.WallSection.WallDirection;

/**
 * Constructs "normal" rooms
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class NormalRoomController extends AbstractRoomController {

    public NormalRoomController(KwdFile kwdFile, RoomInstance roomInstance, IObjectsController objectsController) {
        super(kwdFile, roomInstance, objectsController);
    }

    @Override
    protected List<EntityId> constructPillars() {

        // NOTE: I don't understand how the pillars are referenced, they are neither in Terrain nor Room, but they are listed in the Objects. Even Lair has pillars, but I've never seem the in-game
        // Pillars go into all at least 3x3 corners, there can be more than 4 pillars per room
        // Go through all the points and see if they are fit for pillar placement
        List<EntityId> pillars = null;
        for (Point p : roomInstance.getCoordinates()) {

            // See that we have 2 "free" neigbouring tiles
            Set<WallDirection> freeDirections = EnumSet.noneOf(WallDirection.class);
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

                    // Face "in" diagonally
                    float yAngle = 0;
                    if (freeDirections.contains(WallDirection.NORTH)
                            && freeDirections.contains(WallDirection.EAST)) {
                        yAngle = -FastMath.HALF_PI;
                    } else if (freeDirections.contains(WallDirection.SOUTH)
                            && freeDirections.contains(WallDirection.EAST)) {
                        yAngle = FastMath.PI;
                    } else if (freeDirections.contains(WallDirection.SOUTH)
                            && freeDirections.contains(WallDirection.WEST)) {
                        yAngle = FastMath.HALF_PI;
                    }

                    if (pillars == null) {
                        pillars = new ArrayList<>();
                    }

                    // Construct a pillar
                    pillars.add(objectsController.loadObject(getPillarObject(roomInstance.getRoom().getRoomId()), (short) 0, p.x, p.y, yAngle));
                }
            }
        }
        return pillars == null ? Collections.emptyList() : pillars;
    }
}
