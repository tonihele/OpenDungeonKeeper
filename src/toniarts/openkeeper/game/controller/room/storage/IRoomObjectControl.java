/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.game.controller.room.storage;

import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;
import java.util.Collection;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;

/**
 * General room object control
 *
 * @param <V> the value type to add
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IRoomObjectControl<V> {

    /**
     * Add item to room
     *
     * @param value the stuff to add
     * @param p preferred dropping point for the item
     * @return the item count that doesn't fit
     */
    V addItem(V value, Point p);

    /**
     * When the room gets destroyed, controls need to be destroyed
     */
    void destroy();

    /**
     * Get the room currently used capacity
     *
     * @return the currently used capacity in number of objects
     */
    int getCurrentCapacity();

    /**
     * Get a room objects
     *
     * @param p the object from point
     * @return the objects in given point
     */
    Collection<EntityId> getItems(Point p);

    /**
     * Get the max capacity of the room
     *
     * @return max capacity in number of objects
     */
    int getMaxCapacity();

    /**
     * Get the type of object that can be stored in here
     *
     * @return the object type
     */
    AbstractRoomController.ObjectType getObjectType();

    /**
     * Is the room at full capacity
     *
     * @return max capacity used
     */
    boolean isFullCapacity();

    /**
     * Remove an item
     *
     * @param object the object
     */
    void removeItem(EntityId object);

    /**
     * Gets available coordinates
     *
     * @return list of available coordinates
     */
    Collection<Point> getAvailableCoordinates();

    /**
     * Notifies that the room has been captured
     *
     * @param playerId the new owner
     */
    void captured(short playerId);

}
