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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.view.map.WallSection;

/**
 * The workshop
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class WorkshopController extends NormalRoomController {

    public WorkshopController(KwdFile kwdFile, RoomInstance roomInstance, IObjectsController objectsController) {
        super(kwdFile, roomInstance, objectsController);
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
            // Face "in" diagonally
            if (freeDirections.contains(WallSection.WallDirection.NORTH) && freeDirections.contains(WallSection.WallDirection.EAST)) {
                float yAngle = -FastMath.HALF_PI;
                pillars.add(constructPillar(p, yAngle));
            }
            if (freeDirections.contains(WallSection.WallDirection.SOUTH) && freeDirections.contains(WallSection.WallDirection.EAST)) {
                float yAngle = FastMath.PI;
                pillars.add(constructPillar(p, yAngle));
            }
            if (freeDirections.contains(WallSection.WallDirection.SOUTH) && freeDirections.contains(WallSection.WallDirection.WEST)) {
                float yAngle = FastMath.HALF_PI;
                pillars.add(constructPillar(p, yAngle));
            }
            if (freeDirections.contains(WallSection.WallDirection.NORTH) && freeDirections.contains(WallSection.WallDirection.WEST)) {
                pillars.add(constructPillar(p, 0));
            }
        }
        return pillars;
    }

    private EntityId constructPillar(Point p, float yAngle) {

        // Construct a pillar
        return objectsController.loadObject(getPillarObject(roomInstance.getRoom().getRoomId()), roomInstance.getOwnerId(), p.x, p.y, yAngle);
    }

    @Override
    protected RoomObjectLayout getRoomObjectLayout() {
        return RoomObjectLayout.ALLOW_DIAGONAL_NEIGHBOUR_ONLY;
    }

}
