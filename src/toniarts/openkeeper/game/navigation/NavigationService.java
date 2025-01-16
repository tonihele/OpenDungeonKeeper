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

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.GraphPath;
import toniarts.openkeeper.utils.Point;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.pathfinding.INavigable;
import toniarts.openkeeper.game.navigation.pathfinding.MapDistance;
import toniarts.openkeeper.game.navigation.pathfinding.MapIndexedGraph;
import toniarts.openkeeper.game.navigation.pathfinding.MapPathFinder;
import toniarts.openkeeper.utils.Utils;

/**
 * Offers navigation related services
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class NavigationService implements INavigationService {
    
    private static final Logger logger = System.getLogger(NavigationService.class.getName());

    private final IMapController mapController;
    private final IEntityPositionLookup entityPositionLookup;
    private final MapIndexedGraph pathFindingMap;
    private final MapPathFinder pathFinder;
    private final MapDistance heuristic;

    public NavigationService(IMapController mapController, IEntityPositionLookup entityPositionLookup) {
        this.mapController = mapController;
        this.entityPositionLookup = entityPositionLookup;

        pathFindingMap = new MapIndexedGraph(mapController, entityPositionLookup);
        pathFinder = new MapPathFinder(pathFindingMap, false);
        heuristic = new MapDistance();
    }


    @Override
    public Point findRandomAccessibleTile(Point start, int radius, INavigable navigable) {
        return findRandomAccessibleTile(start, radius, navigable, null);
    }

    @Override
    public Point findRandomTileInRoom(Point start, int radius, INavigable navigable) {
        RoomInstance roomInstance = mapController.getRoomInstanceByCoordinates(start);
        if (roomInstance == null) {
            logger.log(Level.WARNING, () -> "Starting point " + start + " is not in a room!");
            return null;
        }
        Set<Point> allowedTiles = new HashSet<>(roomInstance.getCoordinates());

        return findRandomAccessibleTile(start, radius, navigable, allowedTiles);
    }

    private Point findRandomAccessibleTile(Point start, int radius, INavigable navigable, Set<Point> allowedTiles) {
        Set<Point> tiles = HashSet.newHashSet(radius * radius - 1);

        // Start growing the circle, always testing the tile
        getAccessibleNeighbours(mapController.getMapData().getTile(start.x, start.y), radius, navigable, tiles, allowedTiles);
        tiles.remove(start);

        // Take a random point
        if (!tiles.isEmpty()) {
            return Utils.getRandomItem(new ArrayList<>(tiles));
        }
        return null;
    }

    private void getAccessibleNeighbours(IMapTileInformation startTile, int radius, INavigable navigable, Set<Point> tiles, Set<Point> allowedTiles) {
        if (radius > 0) {
            for (int y = startTile.getY() - 1; y <= startTile.getY() + 1; y++) {
                for (int x = startTile.getX() - 1; x <= startTile.getX() + 1; x++) {

                    // If this is good, add and get neighbours
                    IMapTileInformation tile = mapController.getMapData().getTile(x, y);
                    if (tile != null
                            && !tiles.contains(tile.getLocation())
                            && isAccessible(startTile, tile, navigable)
                            && (allowedTiles == null || allowedTiles.contains(tile.getLocation()))) {
                        tiles.add(tile.getLocation());
                        getAccessibleNeighbours(tile, radius - 1, navigable, tiles, allowedTiles);
                    }
                }
            }
        }
    }

    /**
     * Note that this is not thread safe!!
     *
     * @param start
     * @param end
     * @param navigable
     * @return
     */
    @Override
    public GraphPath<IMapTileInformation> findPath(Point start, Point end, INavigable navigable) {
        pathFindingMap.setPathFindable(navigable);
        GraphPath<IMapTileInformation> outPath = new DefaultGraphPath<>();
        IMapTileInformation startTile = mapController.getMapData().getTile(start.x, start.y);
        IMapTileInformation endTile = mapController.getMapData().getTile(end.x, end.y);
        if (startTile != null && endTile != null && pathFinder.searchNodePath(startTile, endTile, heuristic, outPath)) {
            return outPath;
        }
        return null;
    }

    @Override
    public boolean isAccessible(IMapTileInformation from, IMapTileInformation to, INavigable navigable) {
        Float cost = navigable.getCost(from, to, mapController, entityPositionLookup);
        return cost != null;
    }

}
