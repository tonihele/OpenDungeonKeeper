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
package toniarts.openkeeper.game.controller;

import java.awt.Point;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.listener.MapListener;
import toniarts.openkeeper.game.listener.RoomListener;
import toniarts.openkeeper.game.logic.IGameLogicUpdatable;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.view.selection.SelectionArea;

/**
 * Map related actions available to all players
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IMapController extends IMapInformation, IGameLogicUpdatable {

    /**
     * Set some tiles selected/undelected
     *
     * @param area
     * @param playerId the player who selected the tile
     */
    void selectTiles(SelectionArea area, short playerId);

    /**
     * If you want to get notified about tile changes
     *
     * @param listener the listener
     */
    public void addListener(MapListener listener);

    /**
     * Stop listening to map updates
     *
     * @param listener the listener
     */
    public void removeListener(MapListener listener);

    /**
     * If you want to get notified about room changes
     *
     * @param playerId whose room changes you want to listen
     * @param listener the listener
     */
    public void addListener(short playerId, RoomListener listener);

    /**
     * Stop listening to room updates
     *
     * @param playerId whose room changes you want to listen
     * @param listener the listener
     */
    public void removeListener(short playerId, RoomListener listener);

    /**
     * Get all the room controllers. FIXME: At least with the current design,
     * the clients do not have this data, so either separate interface or design
     * controllers so that they share data
     *
     * @return all the room controllers
     */
    public Collection<IRoomController> getRoomControllers();

    /**
     * Get room instance by coordinates
     *
     * @param p the coordinates
     * @return the room instance in the coordinates
     */
    public RoomInstance getRoomInstanceByCoordinates(Point p);

    /**
     * Get room controller by coordinates
     *
     * @param p the coordinates
     * @return the room controller in the coordinates, if any
     */
    public IRoomController getRoomControllerByCoordinates(Point p);

    /**
     * Get a room controller by a room instance. FIXME: At least with the
     * current design, the clients do not have this data, so either separate
     * interface or design controllers so that they share data
     *
     * @param roomInstance the room instance
     * @return room controller associaced to the given instance
     */
    public IRoomController getRoomController(RoomInstance roomInstance);

    /**
     * Get all the rooms that implement a certain function i.e. serves as a
     * storage for the given type etc. FIXME: At least with the current design,
     * the clients do not have this data, so either separate interface or design
     * controllers so that they share data
     *
     * @param objectType the object type (or function if you may)
     * @param playerId the owner of the room
     * @return list of room controllers that match your search criteria
     */
    public List<IRoomController> getRoomsByFunction(AbstractRoomController.ObjectType objectType, Short playerId);

    /**
     * Get all the coordinates that contain rooms and their instances
     *
     * @return room coordinates
     */
    public Map<Point, RoomInstance> getRoomCoordinates();

    /**
     * Get room controllers by the instances
     *
     * @return the room controllers by instances
     */
    public Map<RoomInstance, IRoomController> getRoomControllersByInstances();

    /**
     * Removes room instances
     *
     * @param instances the instances to remove
     */
    public void removeRoomInstances(RoomInstance... instances);

    /**
     * Update rooms in specified coordinates
     *
     * @param coordinates the coordinates
     */
    public void updateRooms(Point[] coordinates);

    /**
     * Damage a tile
     *
     * @param point the point
     * @param playerId the player applying the damage
     * @param creature optional, a creature that is damaging the tile
     * @return you might get gold out of this
     */
    int damageTile(Point point, short playerId, ICreatureController creature);

    /**
     * Attempt to claim the tile or room, applies either damage of heal,
     * depending whose tile is it
     *
     * @param point tile coordinate
     * @param playerId for the player
     */
    void applyClaimTile(Point point, short playerId);

    /**
     * Heal a tile
     *
     * @param point the point
     * @param playerId the player applying the healing
     */
    void healTile(Point point, short playerId);

    /**
     * Alter terrain type
     *
     * @param pos the coordinates
     * @param terrainId the new terrain id
     * @param playerId the new tile owner
     */
    void alterTerrain(Point pos, short terrainId, short playerId);

    /**
     * Set specified tiles flashing for certain period of time
     *
     * @param points the points to flash
     * @param playerId the player whose flashing will be affected
     * @param time the time to flash
     */
    public void flashTiles(List<Point> points, short playerId, int time);

    /**
     * Set tile flashing off for specified tiles
     *
     * @param points the points to unflash
     * @param playerId the player whose flashing will be affected
     */
    public void unFlashTiles(List<Point> points, short playerId);

    /**
     * Get the same terrain adjacent (not diagonally) to the stating point(s).
     * Kinda flood fill. The starting points maybe different terrain types.
     *
     * @param startingPoints starting coordinates for the search
     * @param x1 min x coordinate, inclusive
     * @param x2 max x coordinate, exclusive
     * @param y1 min y coordinate, inclusive
     * @param y2 max y coordinate, exclusive
     * @return all the terrain points within the given area that match the
     * terrain in the starting coordinates
     */
    public Set<Point> getTerrainBatches(List<Point> startingPoints, int x1, int x2, int y1, int y2);

    public void buildOrSellRoom(SelectionArea area, short playerId, short roomId);

    public void buildOrSellRoom(SelectionArea area, short playerId);

}
