/*
 * Copyright (C) 2014-2018 OpenKeeper
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
package toniarts.openkeeper.game.navigation;

import com.badlogic.gdx.ai.pfa.GraphPath;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.pathfinding.INavigable;

/**
 * Provides access to navigation inside the game world. Pathfinding and such.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface INavigationService {

    /**
     * Get a random tile, that is not a starting tile
     *
     * @param start starting coordinates
     * @param radius radius, in tiles
     * @param navigable the navigable entity
     * @return a random tile if one is found
     */
    Point findRandomAccessibleTile(Point start, int radius, INavigable navigable);

    /**
     * Get a random tile from current room, that is not a starting tile. The
     * starting tile must be a room.
     *
     * @param start starting coordinates
     * @param radius radius, in tiles
     * @param navigable the navigable entity
     * @return a random tile if one is found
     */
    Point findRandomTileInRoom(Point start, int radius, INavigable navigable);

    /**
     * Finds a path between the given points if there is one
     *
     * @param start start point
     * @param end end point
     * @param navigable the entity to find path for
     * @return output path, null if path not found
     */
    GraphPath<IMapTileInformation> findPath(Point start, Point end, INavigable navigable);

    /**
     * Check if given tile is accessible by the given creature
     *
     * @param from from where
     * @param to to where
     * @param navigable the entity to test with
     * @return is accessible
     */
    boolean isAccessible(IMapTileInformation from, IMapTileInformation to, INavigable navigable);

}
