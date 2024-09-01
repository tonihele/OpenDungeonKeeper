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

import toniarts.openkeeper.utils.Point;
import java.util.Collection;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.room.storage.RoomPrisonerControl;
import toniarts.openkeeper.tools.convert.map.KwdFile;

/**
 * Manages prison. Prison has a special door and special navigation for
 * creatures.
 *
 * @author ArchDemon
 */
public class PrisonController extends DoubleQuadController {

    private static final short OBJECT_DOOR_ID = 109;
    private static final short OBJECT_DOORBAR_ID = 116;

    private Point door;

    public PrisonController(KwdFile kwdFile, RoomInstance roomInstance, IObjectsController objectsController,
             IGameTimer gameTimer) {
        super(kwdFile, roomInstance, objectsController);

        addObjectControl(new RoomPrisonerControl(kwdFile, this, objectsController, gameTimer) {
            @Override
            protected Collection<Point> getCoordinates() {

                // Only the innards of prison can hold objects
                return insideCoordinates;
            }

            @Override
            protected int getNumberOfAccessibleTiles() {
                return getCoordinates().size();
            }

        });
    }

    @Override
    protected void constructObjects() {
        super.constructObjects();

        door = null;
        for (Point p : roomInstance.getCoordinates()) {
            if (door == null && insideCoordinates.contains(new Point(p.x, p.y + 1))) {
                floorFurniture.add(objectsController.loadObject(OBJECT_DOOR_ID, roomInstance.getOwnerId(), p.x, p.y));
                floorFurniture.add(objectsController.loadObject(OBJECT_DOORBAR_ID, roomInstance.getOwnerId(), p.x, p.y));

                door = p;
                break;
            }
        }
    }

    @Override
    public boolean isTileAccessible(Integer fromX, Integer fromY, int toX, int toY) {

        // Prison is all accessible, but you do have to use the door!
        if (door != null && fromX != null && fromY != null) {

            // Path finding
            Point from = new Point(fromX, fromY);
            Point to = new Point(toX, toY);
            if (insideCoordinates.contains(to) && insideCoordinates.contains(from)) {
                return true; // Inside the prison == free movement
            } else if (insideCoordinates.contains(from) && to.equals(door)) {
                return true; // From inside also through the door
            } else if (insideCoordinates.contains(to) && from.equals(door)) {
                return true; // Path finding, from outside
            } else if (!insideCoordinates.contains(to) && !insideCoordinates.contains(from)) {
                return true; // Outside the prison == free movement
            }
            return false;
        }
        if (fromX == null && fromY == null && door != null) {
            return true; // We have a door, the tile is accessible
        }
        return super.isTileAccessible(fromX, fromY, toX, toY);
    }

}
