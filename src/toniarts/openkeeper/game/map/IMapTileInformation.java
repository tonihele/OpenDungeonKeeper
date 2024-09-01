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
package toniarts.openkeeper.game.map;

import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.tools.convert.map.Tile;

/**
 * A kind of a map tile container with no editing functionalities
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IMapTileInformation {

    /**
     * Get the entity ID of this map tile
     *
     * @return
     */
    EntityId getEntityId();

    Tile.BridgeTerrainType getBridgeTerrainType();

    int getGold();

    int getHealth();

    Integer getHealthPercent();

    int getIndex();

    /**
     * Get tile index in MapData 2D array as Point
     *
     * @return
     */
    Point getLocation();

    int getManaGain();

    int getMaxHealth();

    short getOwnerId();

    int getRandomTextureIndex();

    short getTerrainId();

    int getX();

    int getY();

    /**
     * Is tile at full health
     *
     * @return true if full health
     */
    boolean isAtFullHealth();

    boolean isFlashed(short playerId);

    boolean isSelected(short playerId);

}
