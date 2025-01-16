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
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.world.ThingLoader;
import toniarts.openkeeper.world.creature.CreatureControl;
import toniarts.openkeeper.world.object.ObjectControl;
import toniarts.openkeeper.world.room.GenericRoom;

/**
 * Controls creature lairs in a room
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Deprecated
public abstract class RoomLairControl extends RoomObjectControl<ObjectControl, Integer> {

    private int lairs = 0;

    public RoomLairControl(GenericRoom parent) {
        super(parent);
    }

    @Override
    public int getCurrentCapacity() {
        return lairs;
    }

    @Override
    public GenericRoom.ObjectType getObjectType() {
        return GenericRoom.ObjectType.LAIR;
    }

    @Override
    public Integer addItem(Integer sum, Point p, ThingLoader thingLoader, CreatureControl creature) {
        Collection<ObjectControl> objects = objectsByCoordinate.get(p);
        if (objects != null && !objects.isEmpty()) {
            return sum; // Already a lair here
        }
        ObjectControl object = thingLoader.addObject(p, creature.getCreature().getLairObjectId(), creature.getOwnerId());
        if (objects == null) {
            objects = new ArrayList<>(1);
        }
        objects.add(object);
        objectsByCoordinate.put(p, objects);
        object.setRoomObjectControl(this);
        lairs++;
        return 0;
    }

    @Override
    public void destroy() {

        // Just release all the lairs
        removeAllObjects();
    }

    @Override
    public void removeItem(ObjectControl object) {
        super.removeItem(object);
        lairs--;
    }

    @Override
    protected int getObjectsPerTile() {
        return 1;
    }

}
