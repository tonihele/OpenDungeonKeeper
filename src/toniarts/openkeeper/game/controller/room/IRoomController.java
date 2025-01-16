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
package toniarts.openkeeper.game.controller.room;

import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;
import java.util.Set;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.tools.convert.map.Room;

/**
 * Controls rooms and provides services related to rooms
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IRoomController {

    /**
     * Constructs a room
     */
    public void construct();

    /**
     * Checks if the given tile is accessible, from an adjacent tile. If no from
     * tile is given, checks general accessibility
     *
     * @param from from tile, can be {@code null}
     * @param to the target tile
     * @return true is the tile is accessible
     */
    public boolean isTileAccessible(Point from, Point to);

    /**
     * Get the actual room instance representation of the room
     *
     * @return the room instance
     */
    public RoomInstance getRoomInstance();

    /**
     * Get the number of floor furniture in a room
     *
     * @return floor furniture count
     */
    public int getFloorFurnitureCount();

    /**
     * Get the number of wall furniture in a room
     *
     * @return wall furniture count
     */
    public int getWallFurnitureCount();

    /**
     * Get the floot furniture IDs
     *
     * @return floor furniture
     */
    public Set<EntityId> getFloorFurniture();

    /**
     * Get the wall furniture IDs
     *
     * @return wall furniture
     */
    public Set<EntityId> getWallFurniture();

    public boolean canStoreGold();

    public boolean hasObjectControl(AbstractRoomController.ObjectType objectType);

    public <T extends IRoomObjectControl> T getObjectControl(AbstractRoomController.ObjectType objectType);

    public Room getRoom();

    /**
     * Are we the dungeon heart?
     *
     * @return are we?
     */
    public boolean isDungeonHeart();

    /**
     * Notify and mark the room as destroyed
     */
    public void destroy();

    /**
     * Is this room instance destroyed? Not in the world anymore.
     *
     * @see #destroy()
     * @return is the room destroyed
     */
    public boolean isDestroyed();

    /**
     * Signal that the room has been captured
     * @param playerId the new owner ID
     */
    public void captured(short playerId);

    public boolean isFullCapacity();

}
