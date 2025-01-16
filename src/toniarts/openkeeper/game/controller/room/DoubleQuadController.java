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
import java.util.HashSet;
import java.util.Set;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.IObjectsController;
import static toniarts.openkeeper.game.controller.room.AbstractRoomController.hasSameTile;
import toniarts.openkeeper.tools.convert.map.KwdFile;

/**
 *
 * @author ArchDemon
 */
public class DoubleQuadController extends NormalRoomController {

    protected Set<Point> insideCoordinates;

    public DoubleQuadController(KwdFile kwdFile, RoomInstance roomInstance, IObjectsController objectsController) {
        super(kwdFile, roomInstance, objectsController);
    }

    @Override
    public boolean isTileAccessible(Integer fromX, Integer fromY, int toX, int toY) {

        // You can't access insides from outsides and vice versa, by default
        Point toPoint = new Point(toX, toY);
        if (fromX != null && fromY != null) {
            Point fromPoint = new Point(fromX, fromY);
            return (insideCoordinates.contains(fromPoint) && insideCoordinates.contains(toPoint)) || ((!insideCoordinates.contains(fromPoint) && !insideCoordinates.contains(toPoint)));
        }
        return !insideCoordinates.contains(toPoint);
    }

    @Override
    protected void setupCoordinates() {
        super.setupCoordinates();

        insideCoordinates = getInsideCoordinates();
    }

    private Set<Point> getInsideCoordinates() {
        Set<Point> coordinates = new HashSet<>();
        for (int x = 1; x < map.length - 1; x++) {
            for (int y = 1; y < map[x].length - 1; y++) {
                if (!map[x][y]) {
                    continue;
                }

                boolean N = hasSameTile(map, x, y + 1);
                boolean NE = hasSameTile(map, x - 1, y + 1);
                boolean E = hasSameTile(map, x - 1, y);
                boolean SE = hasSameTile(map, x - 1, y - 1);
                boolean S = hasSameTile(map, x, y - 1);
                boolean SW = hasSameTile(map, x + 1, y - 1);
                boolean W = hasSameTile(map, x + 1, y);
                boolean NW = hasSameTile(map, x + 1, y + 1);
                if (N && NE && E && SE && S && SW && W && NW) {
                    coordinates.add(new Point(roomInstance.getMatrixStartPoint().x + x, roomInstance.getMatrixStartPoint().y + y));
                }
            }
        }
        return coordinates;
    }
}
