/*
 * Copyright (C) 2014-2020 OpenKeeper
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
package toniarts.openkeeper.game.component;

import com.simsilica.es.EntityComponent;
import toniarts.openkeeper.utils.Point;
import java.util.HashMap;
import java.util.Map;
import toniarts.openkeeper.tools.convert.map.Tile;

/**
 * Represents single map tile
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class MapTile implements EntityComponent {

    // TODO: Get rid of these, not a correct place to store this information
    public Map<Short, Boolean> selection;
    public Map<Short, Boolean> flashing;

    public int randomTextureIndex;

    /* Refers to the room this tile holds */
    public boolean destroyed = false;

    public short terrainId;
    public Tile.BridgeTerrainType bridgeTerrainType;

    public Point p;
    public int index;

    public MapTile() {
        // For serialization
    }

    public MapTile(int randomTextureIndex, short terrainId, Tile.BridgeTerrainType bridgeTerrainType, Point p, int index) {
        this.randomTextureIndex = randomTextureIndex;
        this.terrainId = terrainId;
        this.bridgeTerrainType = bridgeTerrainType;
        this.p = p;
        this.index = index;
    }

    public MapTile(MapTile mapTile) {
        this.randomTextureIndex = mapTile.randomTextureIndex;
        this.terrainId = mapTile.terrainId;
        this.bridgeTerrainType = mapTile.bridgeTerrainType;
        this.p = mapTile.p;
        this.index = mapTile.index;

        if (mapTile.selection != null && !mapTile.selection.isEmpty()) {
            this.selection = HashMap.newHashMap(4);
            this.selection.putAll(mapTile.selection);
        }
        if (mapTile.flashing != null && !mapTile.flashing.isEmpty()) {
            this.flashing = HashMap.newHashMap(4);
            this.flashing.putAll(mapTile.flashing);
        }
    }

}
