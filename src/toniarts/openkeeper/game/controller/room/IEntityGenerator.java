/*
 * Copyright (C) 2014-2019 OpenKeeper
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

/**
 * Signifies that a room generates entities
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IEntityGenerator extends IRoomController {

    /**
     * Get the coordinate for the entity to spawn on this room
     *
     * @return the coordinate
     */
    public Point getEntranceCoordinate();

    /**
     * Get time when the last entity was spawn from this room
     *
     * @return last entity spawn time, in game time
     */
    public double getLastSpawnTime();

    /**
     * Notifies that a entity was spawn from this room
     *
     * @param time the time the entity was spawn
     * @param entityId spawned entity ID
     */
    public void onSpawn(double time, EntityId entityId);

}
