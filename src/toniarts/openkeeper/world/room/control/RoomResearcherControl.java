/*
 * Copyright (C) 2014-2016 OpenKeeper
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
package toniarts.openkeeper.world.room.control;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.world.ThingLoader;
import toniarts.openkeeper.world.creature.CreatureControl;
import toniarts.openkeeper.world.object.ObjectControl;
import toniarts.openkeeper.world.room.GenericRoom;

/**
 * Holds out the researchers populating a room
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Deprecated
public abstract class RoomResearcherControl extends RoomObjectControl<ObjectControl, Integer> {

    public RoomResearcherControl(GenericRoom parent) {
        super(parent);
    }

    @Override
    public int getCurrentCapacity() {
        return objectsByCoordinate.size();
    }

    @Override
    protected int getObjectsPerTile() {
        return 1;
    }

    @Override
    public GenericRoom.ObjectType getObjectType() {
        return GenericRoom.ObjectType.RESEARCHER;
    }

    @Override
    public Integer addItem(Integer sum, Point p, ThingLoader thingLoader, CreatureControl creature) {
        return sum;
    }

    @Override
    public void destroy() {

        // TODO: The researcher can't do his/her job
    }

    @Override
    protected Collection<Point> getCoordinates() {

        // Only furniture
        List<Point> coordinates = new ArrayList<>(parent.getFloorFurnitureCount() + parent.getWallFurnitureCount());
        for (ObjectControl oc : parent.getFloorFurniture()) {
            coordinates.add(oc.getObjectCoordinates());
        }
        for (ObjectControl oc : parent.getWallFurniture()) {
            coordinates.add(oc.getObjectCoordinates());
        }
        return coordinates;
    }

}
