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
package toniarts.openkeeper.world.pathfinding;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.DefaultConnection;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.utils.Array;
import toniarts.openkeeper.world.TileData;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.utils.Point;

/**
 * Map representation for the path finding
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Deprecated
public class MapIndexedGraph implements IndexedGraph<TileData> {

    private final WorldState worldState;
    private final int nodeCount;
    private PathFindable pathFindable;

    public MapIndexedGraph(WorldState worldState) {
        this.worldState = worldState;
        nodeCount = worldState.getMapData().getHeight() * worldState.getMapData().getWidth();
    }

    @Override
    public int getNodeCount() {
        return nodeCount;
    }

    @Override
    public int getIndex(TileData n) {
        return n.getIndex();
    }

    /**
     * Set this prior to finding the path to search the path for certain path
     * findable type
     *
     * @param pathFindable the path findable
     */
    public void setPathFindable(PathFindable pathFindable) {
        this.pathFindable = pathFindable;
    }

    @Override
    public Array<Connection<TileData>> getConnections(TileData tile) {

        // The connections depend on the creature type
        Array<Connection<TileData>> connections = new Array<>(8);
        boolean valids[] = new boolean[4];

        valids[0] = addIfValidCoordinate(tile, tile.getX(), tile.getY() - 1, connections); // North
        valids[1] = addIfValidCoordinate(tile, tile.getX() + 1, tile.getY(), connections); // East
        valids[2] = addIfValidCoordinate(tile, tile.getX(), tile.getY() + 1, connections); // South
        valids[3] = addIfValidCoordinate(tile, tile.getX() - 1, tile.getY(), connections); // West

        if (pathFindable.canMoveDiagonally()) {
            if (valids[0] && valids[1]) { // North-East
                addIfValidCoordinate(tile, tile.getX() + 1, tile.getY() - 1, connections);
            }
            if (valids[0] && valids[3]) { // North-West
                addIfValidCoordinate(tile, tile.getX() - 1, tile.getY() - 1, connections);
            }
            if (valids[2] && valids[1]) { // South-East
                addIfValidCoordinate(tile, tile.getX() + 1, tile.getY() + 1, connections);
            }
            if (valids[2] && valids[3]) { // South-West
                addIfValidCoordinate(tile, tile.getX() - 1, tile.getY() + 1, connections);
            }
        }

        return connections;
    }

    private boolean addIfValidCoordinate(final TileData startTile, final int x, final int y, final Array<Connection<TileData>> connections) {

        // Valid coordinate
        TileData tile = worldState.getMapData().getTile(x, y);
        if (tile != null) {
            Float cost = pathFindable.getCost(startTile, tile, worldState);
            if (cost != null) {
                connections.add(new DefaultConnection<TileData>(startTile, tile) {

                    @Override
                    public float getCost() {
                        return cost;
                    }

                });
                return true;
            }
        }
        return false;
    }

}
