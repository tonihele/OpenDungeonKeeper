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

import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.tools.convert.map.KwdFile;

/**
 * Portal is the only one I think
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ThreeByThreeController extends AbstractRoomController implements ICreatureEntrance {

    private double lastSpawnTime = Double.MIN_VALUE;

    public ThreeByThreeController(KwdFile kwdFile, RoomInstance roomInstance, IObjectsController objectsController) {
        super(kwdFile, roomInstance, objectsController);
    }

    @Override
    public boolean isTileAccessible(Integer fromX, Integer fromY, int toX, int toY) {

        // The center tile is not accessible
        Point roomPoint = roomInstance.worldCoordinateToLocalCoordinate(toX, toY);
        return !(roomPoint.x == 1 && roomPoint.y == 1);
    }

    @Override
    public Point getEntranceCoordinate() {
        return roomInstance.getCenter();
    }

    @Override
    public double getLastSpawnTime() {
        return lastSpawnTime;
    }

    @Override
    public void onSpawn(double time, EntityId entityId) {
        this.lastSpawnTime = time;
    }

    @Override
    public void captured(short playerId) {
        super.captured(playerId);
        lastSpawnTime = Double.MIN_VALUE;
    }
}
