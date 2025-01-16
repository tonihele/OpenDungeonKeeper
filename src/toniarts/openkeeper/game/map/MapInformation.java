/*
 * Copyright (C) 2014-2015 OpenKeeper
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * This is controller for the map related functions
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 * @param <T> the map data container
 * @param <S> the tile type of the map data container
 */
public class MapInformation<T extends IMapDataInformation<S>, S extends IMapTileInformation> implements IMapInformation<S> {

    private final T mapData;
    private final KwdFile kwdFile;
    private final Map<Short, Keeper> playersById;

    public MapInformation(T mapData, KwdFile kwdFile, Collection<Keeper> players) {
        this.mapData = mapData;
        this.kwdFile = kwdFile;
        playersById = players.stream().collect(Collectors.toMap(Keeper::getId, keeper -> keeper));
    }

    @Override
    public T getMapData() {
        return mapData;
    }

    @Override
    public boolean isSelected(Point p, short playerId) {
        S tile = getMapData().getTile(p);
        if (tile == null) {
            return false;
        }
        return tile.isSelected(playerId);
    }

    @Override
    public boolean isTaggable(Point p) {
        S tile = getMapData().getTile(p);
        if (tile == null) {
            return false;
        }
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        return terrain.getFlags().contains(Terrain.TerrainFlag.TAGGABLE);
    }

    @Override
    public boolean isBuildable(Point p, short playerId, short roomId) {
        S tile = getMapData().getTile(p);
        if (tile == null) {
            return false;
        }

        Room room = kwdFile.getRoomById(roomId);
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());

        // Ownable tile is needed for land building (and needs to be owned by us)
        if (room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_LAND)
                && !terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)
                && terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE)
                && !terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)
                && tile.getOwnerId() == playerId) {
            return true;
        }

        // See if we are dealing with bridges
        if ((room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_WATER) && terrain.getFlags().contains(Terrain.TerrainFlag.WATER))
                || room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_LAVA) && terrain.getFlags().contains(Terrain.TerrainFlag.LAVA)) {

            // We need to have an adjacent owned tile
            return hasAdjacentOwnedPath(tile.getLocation(), playerId);
        }

        return false;
    }

    /**
     * Checks if you have an adjacent (not diagonally) owned tile. Flat tile
     * that is. Or a piece of bridge.
     *
     * @param point the origin point
     * @param playerId the player ID, the owner
     * @return has adjacent owned path tile
     */
    private boolean hasAdjacentOwnedPath(Point point, short playerId) {
        for (Point p : WorldUtils.getSurroundingTiles(mapData, point, false)) {
            S neighbourTile = getMapData().getTile(p);
            if (neighbourTile != null) {
                Terrain neighbourTerrain = kwdFile.getTerrain(neighbourTile.getTerrainId());
                if (neighbourTile.getOwnerId() == playerId && neighbourTerrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE) && !neighbourTerrain.getFlags().contains(Terrain.TerrainFlag.SOLID)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isClaimable(Point p, short playerId) {
        S tile = getMapData().getTile(p);
        if (tile == null) {
            return false;
        }

        // See if the terrain is claimable at all
        Terrain terrain = getTerrain(tile);
        boolean claimable = false;
        if (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)) {
            if (tile.getOwnerId() == Player.NEUTRAL_PLAYER_ID || !playersById.get(playerId).isAlly(tile.getOwnerId())) {
                claimable = true;
            }
        } else if (terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE)) {
            if (tile.getOwnerId() == Player.NEUTRAL_PLAYER_ID || !playersById.get(playerId).isAlly(tile.getOwnerId())) {
                claimable = true;
            }
        } else {
            claimable = (terrain.getMaxHealthTypeTerrainId() != terrain.getTerrainId());
        }

        // In order to claim, it needs to be adjacent to your own tiles
        if (claimable) {
            return hasAdjacentOwnedPath(tile.getLocation(), playerId);
        }

        return false;
    }

    @Override
    public boolean isSellable(Point p, short playerId) {
        S tile = getMapData().getTile(p);
        if (tile.getOwnerId() == playerId) {

            // We own it, see if sellable
            Terrain terrain = getTerrain(tile);
            if (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)) {
                Room room = kwdFile.getRoomByTerrain(terrain.getTerrainId());

                return room != null && room.getFlags().contains(Room.RoomFlag.BUILDABLE);
            }
        }
        return false;
    }

    @Override
    public boolean isWater(Point p) {
        S tile = getMapData().getTile(p);
        if (tile == null) {
            return false;
        }
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        return terrain.getFlags().contains(Terrain.TerrainFlag.WATER);
    }

    @Override
    public boolean isLava(Point p) {
        S tile = getMapData().getTile(p);
        if (tile == null) {
            return false;
        }
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
            return terrain.getFlags().contains(Terrain.TerrainFlag.LAVA);
    }

    @Override
    public void setTiles(List<S> tiles) {
        mapData.setTiles(tiles);
    }

    @Override
    public boolean isRepairableWall(Point p, short playerId) {
        S tile = getMapData().getTile(p);
        if (tile == null) {
            return false;
        }
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        return (!tile.isSelected(playerId) && tile.getOwnerId() == playerId && terrain.getFlags().contains(Terrain.TerrainFlag.SOLID) && terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE) && !tile.isAtFullHealth());
    }

    @Override
    public boolean isClaimableWall(Point p, short playerId) {
        S tile = getMapData().getTile(p);
        if (tile == null) {
            return false;
        }
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        return (terrain.getFlags().contains(Terrain.TerrainFlag.SOLID) && isClaimable(p, playerId));
    }

    @Override
    public boolean isClaimableTile(Point p, short playerId) {
        S tile = getMapData().getTile(p);
        if (tile == null) {
            return false;
        }
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        return (!terrain.getFlags().contains(Terrain.TerrainFlag.ROOM) && isClaimable(p, playerId));
    }

    @Override
    public boolean isClaimableRoom(Point p, short playerId) {
        S tile = getMapData().getTile(p);
        if (tile == null) {
            return false;
        }
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        return (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM) && isClaimable(p, playerId));
    }

    @Override
    public Terrain getTerrain(IMapTileInformation tile) {
        return kwdFile.getTerrain(tile.getTerrainId());
    }

    @Override
    public boolean isSolid(Point p) {
        S tile = getMapData().getTile(p);
        if (tile == null) {
            return false;
        }
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        return terrain.getFlags().contains(Terrain.TerrainFlag.SOLID);
    }

}
