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
package toniarts.openkeeper.world.creature.pathfinding;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.DefaultConnection;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.utils.Array;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.world.TileData;
import toniarts.openkeeper.world.WorldState;

/**
 * Map representation for the path finding
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class MapIndexedGraph implements IndexedGraph<TileData> {

    private final WorldState worldState;
    private final int nodeCount;
    private Creature creature;

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
     * Set this prior to finding the path to search the path for certain
     * creature type
     *
     * @param creature the creature
     */
    public void setCreature(Creature creature) {
        this.creature = creature;
    }

    @Override
    public Array<Connection<TileData>> getConnections(TileData tile) {

        // The connections depend on the creature type
        // No diagonal movement
        Array<Connection<TileData>> connections = new Array<>(8);
        boolean valids[] = new boolean[4];

        valids[0] = addIfValidCoordinate(tile, tile.getX(), tile.getY() - 1, connections); // North
        valids[1] = addIfValidCoordinate(tile, tile.getX() + 1, tile.getY(), connections); // East
        valids[2] = addIfValidCoordinate(tile, tile.getX(), tile.getY() + 1, connections); // South
        valids[3] = addIfValidCoordinate(tile, tile.getX() - 1, tile.getY(), connections); // West

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

        return connections;
    }

    private boolean addIfValidCoordinate(final TileData startTile, final int x, final int y, final Array<Connection<TileData>> connections) {

        // Valid coordinate
        TileData tile = worldState.getMapData().getTile(x, y);
        if (tile == null) {
            return false;
        }
        Terrain terrain = tile.getTerrain();
        if (terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)) {
            return false;
        }

        // TODO: Rooms, obstacles and what not, should create an universal isAccessible(Creature) to map loader / world handler maybe
        if (creature != null) {

            if (!worldState.isAccessible(tile, creature)) {
                return false;
            }

            if (creature.getFlags().contains(Creature.CreatureFlag.CAN_FLY)) {
                connections.add(new DefaultConnection<>(startTile, tile)); // No cost
                return true;
            }
        }
        connections.add(new MapConnection(startTile, tile));
        return true;

    }

}
