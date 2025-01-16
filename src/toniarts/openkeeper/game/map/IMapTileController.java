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

import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.tools.convert.map.Tile;

/**
 * A kind of a map tile controller
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IMapTileController extends IMapTileInformation {

    void setBridgeTerrainType(Tile.BridgeTerrainType bridgeTerrainType);

    void setFlashed(boolean flashed, short playerId);

    void setGold(int gold);

    /**
     * Set tile health, only internal usage
     *
     * @param health the health points to set
     */
    void setHealth(int health);

    void setIndex(int index);

    void setMaxHealth(int maxHealth);

    void setOwnerId(short ownerId);

    void setPoint(Point p);

    void setRandomTextureIndex(int randomTextureIndex);

    void setSelected(boolean selected, short playerId);

    void setTerrainId(short terrainId);

}
