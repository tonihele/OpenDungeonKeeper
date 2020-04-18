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
package toniarts.openkeeper.game.controller;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.util.SafeArrayList;
import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.control.Container;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.map.BuildTileControl;
import toniarts.openkeeper.game.controller.map.FlashTileControl;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.listener.MapListener;
import toniarts.openkeeper.game.listener.RoomListener;
import toniarts.openkeeper.game.map.MapData;
import toniarts.openkeeper.game.map.MapTile;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.selection.SelectionArea;

/**
 * This is controller for the map related functions
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class MapController extends Container implements Savable, IMapController {

    private MapData mapData;
    private KwdFile kwdFile;
    private IGameTimer gameTimer;
    private IObjectsController objectsController;
    private Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings;
    private final Map<Point, RoomInstance> roomCoordinates = new HashMap<>();
    private final Map<RoomInstance, IRoomController> roomControllers = new HashMap<>();
    private final SafeArrayList<MapListener> mapListeners = new SafeArrayList<>(MapListener.class);
    private final Map<Short, SafeArrayList<RoomListener>> roomListeners = new HashMap<>();

    public MapController() {
        // For serialization
    }

    /**
     * Load map data from a KWD file straight (new game)
     *
     * @param kwdFile the KWD file
     * @param objectsController objects controller
     * @param gameSettings the game settings
     * @param gameTimer
     * @param gameWorldController
     */
    public MapController(KwdFile kwdFile, IObjectsController objectsController, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings,
            IGameTimer gameTimer, IBuildOrSellRoom gameWorldController) {
        this.kwdFile = kwdFile;
        this.objectsController = objectsController;
        this.mapData = new MapData(kwdFile);
        this.gameSettings = gameSettings;
        this.gameTimer = gameTimer;
        // Load rooms
        loadRooms();
        addControl(new BuildTileControl(gameWorldController));
    }

    /**
     * Instantiate a map controller from map data (loaded game)
     *
     * @param mapData the map data
     * @param kwdFile the KWD file
     * @param gameSettings the game settings
     */
    public MapController(MapData mapData, KwdFile kwdFile, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings, IGameTimer gameTimer) {
        this.mapData = mapData;
        this.kwdFile = kwdFile;
        this.gameSettings = gameSettings;
        this.gameTimer = gameTimer;
    }

    public MapController(MapData mapData, KwdFile kwdFile) {
        this.mapData = mapData;
        this.kwdFile = kwdFile;
    }

    private void loadRooms() {
        // Go through the tiles and detect any rooms
        for (MapTile tile : mapData) {
            loadRoom(tile);
        }
    }

    private void loadRoom(MapTile mapTile) {
        // check flag
        if (!kwdFile.getTerrain(mapTile.getTerrainId()).getFlags().contains(Terrain.TerrainFlag.ROOM)) {
            return;
        }

        if (roomCoordinates.containsKey(mapTile.getLocation())) {
            return;
        }

        // Find it
        RoomInstance roomInstance = new RoomInstance(kwdFile.getRoomByTerrain(mapTile.getTerrainId()));
        roomInstance.setOwnerId(mapTile.getOwnerId());
        findRoom(mapTile.getLocation(), roomInstance);

        // Create a controller for it
        IRoomController roomController = RoomControllerFactory.constructRoom(kwdFile, roomInstance, objectsController, gameSettings, gameTimer);
        roomController.construct();
        roomControllers.put(roomInstance, roomController);

        // TODO: A bit of a design problem here
        /**
         * Unclear responsibilities between the world and map controller. Also a
         * result of how we handle the building and selling by destroying rooms.
         * But at least keep anyone who is listening intact
         */
        notifyOnBuild(roomController.getRoomInstance().getOwnerId(), roomController);
    }

    /**
     * Find the room starting from a certain point, rooms are never diagonally
     * attached
     *
     * @param p starting point
     * @param roomInstance the room instance
     */
    private void findRoom(Point p, RoomInstance roomInstance) {
        MapTile tile = getMapData().getTile(p);

        // Get the terrain
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        if (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)) {

            if (!roomCoordinates.containsKey(p)) {
                if (roomInstance.getRoom().equals(kwdFile.getRoomByTerrain(terrain.getTerrainId()))) {

                    // Add the coordinate
                    roomCoordinates.put(p, roomInstance);
                    roomInstance.addCoordinate(p);

                    // Find north
                    findRoom(new Point(p.x, p.y - 1), roomInstance);

                    // Find east
                    findRoom(new Point(p.x + 1, p.y), roomInstance);

                    // Find south
                    findRoom(new Point(p.x, p.y + 1), roomInstance);

                    // Find west
                    findRoom(new Point(p.x - 1, p.y), roomInstance);
                }
            }
        }
    }

    @Override
    public MapData getMapData() {
        return mapData;
    }

    public void setMapData(MapData mapData) {
        this.mapData = mapData;
    }

    public void setKwdFile(KwdFile kwdFile) {
        this.kwdFile = kwdFile;
    }

    @Override
    public void addListener(MapListener listener) {
        mapListeners.add(listener);
    }

    @Override
    public void removeListener(MapListener listener) {
        mapListeners.remove(listener);
    }

    @Override
    public void removeListener(short playerId, RoomListener listener) {
        SafeArrayList<RoomListener> listeners = roomListeners.get(playerId);
        if (listeners == null) {
            return;
        }
        listeners.remove(listener);
    }

    @Override
    public void addListener(short playerId, RoomListener listener) {
        SafeArrayList<RoomListener> listeners = roomListeners.get(playerId);
        if (listeners == null) {
            listeners = new SafeArrayList<>(RoomListener.class);
        }
        listeners.add(listener);
        roomListeners.put(playerId, listeners);
    }

    private void notifyOnBuild(short playerId, IRoomController room) {
        if (roomListeners != null && roomListeners.containsKey(playerId)) {
            for (RoomListener listener : roomListeners.get(playerId)) {
                listener.onBuild(room);
            }
        }
    }

    private void notifyOnCaptured(short playerId, IRoomController room) {
        if (roomListeners != null && roomListeners.containsKey(playerId)) {
            for (RoomListener listener : roomListeners.get(playerId)) {
                listener.onCaptured(room);
            }
        }
    }

    private void notifyOnCapturedByEnemy(short playerId, IRoomController room) {
        if (roomListeners != null && roomListeners.containsKey(playerId)) {
            for (RoomListener listener : roomListeners.get(playerId)) {
                listener.onCapturedByEnemy(room);
            }
        }
    }

    private void notifyOnSold(short playerId, IRoomController room) {
        if (roomListeners != null && roomListeners.containsKey(playerId)) {
            for (RoomListener listener : roomListeners.get(playerId)) {
                listener.onSold(room);
            }
        }
    }

    @Override
    public void selectTiles(SelectionArea area, short playerId) {
        List<MapTile> updatableTiles = new ArrayList<>();
        MapTile startTile = getMapData().getTile(WorldUtils.vectorToPoint(area.getRealStart()));
        boolean select = (startTile == null) ? true : !startTile.isSelected(playerId);

        for (Iterator<Point> it = area.simpleIterator(); it.hasNext();) {
            MapTile tile = getMapData().getTile(it.next());
            if (tile == null) {
                continue;
            }
            Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
            if (!terrain.getFlags().contains(Terrain.TerrainFlag.TAGGABLE)) {
                continue;
            }
            tile.setSelected(select, playerId);
            updatableTiles.add(tile);
        }
        //Point[] tiles = updatableTiles.toArray(new Point[updatableTiles.size()]);
        //mapLoader.updateTiles(tiles);

        // Notify
        notifyTileChange(updatableTiles);
    }

    @Override
    public boolean isSelected(Point p, short playerId) {
        MapTile tile = getMapData().getTile(p);
        if (tile == null) {
            return false;
        }
        return tile.isSelected(playerId);
    }

    @Override
    public boolean isTaggable(Point p) {
        MapTile tile = getMapData().getTile(p);
        if (tile == null) {
            return false;
        }
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        return terrain.getFlags().contains(Terrain.TerrainFlag.TAGGABLE);
    }

    /**
     * Does not check adjacent to own tile
     *
     * @param p location
     * @param playerId
     * @param roomId
     * @return
     */
    @Override
    public boolean isBuildable(Point p, short playerId, short roomId) {
        MapTile tile = getMapData().getTile(p);
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

            return true;
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
            MapTile neighbourTile = getMapData().getTile(p);
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
        MapTile tile = getMapData().getTile(p);
        if (tile == null) {
            return false;
        }

        // See if the terrain is claimable at all
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        boolean claimable = false;
        if (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)) {
            if (tile.getOwnerId() != playerId) {
                claimable = true;
            }
        } else if (terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE)) {
            if (tile.getOwnerId() != playerId) {
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
        MapTile tile = getMapData().getTile(p);
        if (tile.getOwnerId() == playerId && getRoomCoordinates().containsKey(p)) {

            // We own it, see if sellable
            RoomInstance instance = roomCoordinates.get(p);
            return instance.getRoom().getFlags().contains(Room.RoomFlag.BUILDABLE);
        }
        return false;
    }

    @Override
    public boolean isWater(Point p) {
        MapTile tile = getMapData().getTile(p);
        if (tile == null) {
            return false;
        }
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        return terrain.getFlags().contains(Terrain.TerrainFlag.WATER);
    }

    @Override
    public boolean isLava(Point p) {
        MapTile tile = getMapData().getTile(p);
        if (tile == null) {
            return false;
        }
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        return terrain.getFlags().contains(Terrain.TerrainFlag.LAVA);
    }

    @Override
    public void setTiles(List<MapTile> tiles) {
        mapData.setTiles(tiles);
    }

    private void notifyTileChange(MapTile updatedTile) {
        List<MapTile> mapTiles = new ArrayList<>(1);
        mapTiles.add(updatedTile);
        notifyTileChange(mapTiles);
    }

    private void notifyTileChange(List<MapTile> updatedTiles) {
        for (MapListener mapListener : mapListeners.getArray()) {
            mapListener.onTilesChange(updatedTiles);
        }
    }

    @Override
    public Collection<IRoomController> getRoomControllers() {
        return roomControllers.values();
    }

    @Override
    public RoomInstance getRoomInstanceByCoordinates(Point p) {
        return roomCoordinates.get(p);
    }

    @Override
    public IRoomController getRoomControllerByCoordinates(Point p) {
        RoomInstance roomInstance = roomCoordinates.get(p);
        if (roomInstance != null) {
            return getRoomController(roomInstance);
        }

        return null;
    }

    @Override
    public IRoomController getRoomController(RoomInstance roomInstance) {
        return roomControllers.get(roomInstance);
    }

    @Override
    public Map<Point, RoomInstance> getRoomCoordinates() {
        return roomCoordinates;
    }

    @Override
    public Map<RoomInstance, IRoomController> getRoomControllersByInstances() {
        return roomControllers;
    }

    @Override
    public void removeRoomInstances(RoomInstance... instances) {
        for (RoomInstance instance : instances) {

            // Signal the room
            IRoomController roomController = getRoomController(instance);
            roomController.destroy();

            roomControllers.remove(instance);
            for (Point p : instance.getCoordinates()) {
                roomCoordinates.remove(p);
            }

            // TODO: A bit of a design problem here
            /**
             * Unclear responsibilities between the world and map controller.
             * Also a result of how we handle the building and selling by
             * destroying rooms. But at least keep anyone who is listening
             * intact
             */
            notifyOnSold(roomController.getRoomInstance().getOwnerId(), roomController);
        }
    }

    /**
     * Get rooms by function.<br> FIXME: Should the player have ready lists?
     *
     * @param objectType the function
     * @param playerId the player id, can be null
     * @return list of rooms that match the criteria
     */
    @Override
    public List<IRoomController> getRoomsByFunction(ObjectType objectType, Short playerId) {
        List<IRoomController> roomsList = new ArrayList<>();
        for (Map.Entry<RoomInstance, IRoomController> entry : roomControllers.entrySet()) {
            if (playerId != null && entry.getKey().getOwnerId() != playerId) {
                continue;
            }
            if (entry.getValue().hasObjectControl(objectType)) {
                roomsList.add(entry.getValue());
            }
        }
        return roomsList;
    }

    @Override
    public void updateRooms(Point[] coordinates) {
        for (Point p : coordinates) {
            loadRoom(mapData.getTile(p));
        }
    }

    @Override
    public boolean isRepairableWall(Point p, short playerId) {
        MapTile tile = getMapData().getTile(p);
        if (tile == null) {
            return false;
        }
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        return (!tile.isSelected(playerId) && tile.getOwnerId() == playerId && terrain.getFlags().contains(Terrain.TerrainFlag.SOLID) && terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE) && !tile.isAtFullHealth());
    }

    @Override
    public boolean isClaimableWall(Point p, short playerId) {
        MapTile tile = getMapData().getTile(p);
        if (tile == null) {
            return false;
        }
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        return (terrain.getFlags().contains(Terrain.TerrainFlag.SOLID) && isClaimable(p, playerId));
    }

    @Override
    public boolean isClaimableTile(Point p, short playerId) {
        MapTile tile = getMapData().getTile(p);
        if (tile == null) {
            return false;
        }
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        return (!terrain.getFlags().contains(Terrain.TerrainFlag.ROOM) && isClaimable(p, playerId));
    }

    @Override
    public boolean isClaimableRoom(Point p, short playerId) {
        MapTile tile = getMapData().getTile(p);
        if (tile == null) {
            return false;
        }
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        return (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM) && isClaimable(p, playerId));
    }

    @Override
    public Terrain getTerrain(MapTile tile) {
        return kwdFile.getTerrain(tile.getTerrainId());
    }

    @Override
    public void applyClaimTile(Point point, short playerId) {
        MapTile tile = getMapData().getTile(point);
        Terrain terrain = getTerrain(tile);
        if (terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE) && tile.getOwnerId() != playerId) {
            if (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)) {
                damageRoom(point, playerId);
            } else {
                damageTile(point, playerId, null);
            }
        } else {
            // TODO: Room healing
            healTile(point, playerId);
        }
    }

    @Override
    public int damageTile(Point point, short playerId, ICreatureController creature) {
        MapTile tile = getMapData().getTile(point);
        Terrain terrain = getTerrain(tile);

        // Calculate the damage
        int damage = 0;
        int returnedGold = 0;
        int multiplier = (creature != null && kwdFile.getDwarf() == creature.getCreature() ? (int) getLevelVariable(Variable.MiscVariable.MiscType.DWARF_DIGGING_MULTIPLIER) : 1);
        if (terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)) {
            if (terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE)) {
                if (tile.getOwnerId() == playerId) {
                    damage = (int) getLevelVariable(Variable.MiscVariable.MiscType.DIG_OWN_WALL_HEALTH) * multiplier;
                } else {
                    damage = (int) getLevelVariable(Variable.MiscVariable.MiscType.DIG_ENEMY_WALL_HEALTH) * multiplier;
                }
            } else if (tile.getGold() > 0) {

                // This is how I believe the gold mining works, it is not health damage we do, it is substracting gold
                // The mined tiles leave no loot, the loot is left by the imps if there is no place to store the gold
                if (terrain.getFlags().contains(Terrain.TerrainFlag.IMPENETRABLE)) {
                    damage = (int) getLevelVariable(Variable.MiscVariable.MiscType.GOLD_MINED_FROM_GEMS);
                } else {
                    damage = (int) getLevelVariable(Variable.MiscVariable.MiscType.MINE_GOLD_HEALTH);
                }
            } else {
                damage = (int) getLevelVariable(Variable.MiscVariable.MiscType.DIG_ROCK_HEALTH) * multiplier;
            }
        } else if (terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE) && tile.getOwnerId() != playerId) {

            // Attack enemy tile
            damage = (int) getLevelVariable(Variable.MiscVariable.MiscType.ATTACK_TILE_HEALTH);
        } else {
            throw new UnsupportedOperationException("Wat?! Tried to damage tile " + terrain.getName() + " at " + point + "!");
        }

        // Do the damage
        boolean tileDestroyed;
        damage = Math.abs(damage);
        if (tile.getGold() > 0) { // Mine
            if (terrain.getFlags().contains(Terrain.TerrainFlag.IMPENETRABLE)) {
                returnedGold = damage;
                tileDestroyed = false;
            } else {
                returnedGold = mineGold(tile, damage);
                tileDestroyed = (tile.getGold() < 1);
            }
        } else { // Apply damage
            tileDestroyed = applyDamage(tile, damage);
        }

        // See the results
        if (tileDestroyed) {

            // TODO: effect, drop loot & checks, claimed walls should also get destroyed if all adjacent tiles are not in cotrol anymore
            // The tile is dead
//            if (terrain.getDestroyedEffectId() != 0) {
//                effectManager.load(worldNode,
//                        WorldUtils.pointToVector3f(point).addLocal(0, MapLoader.FLOOR_HEIGHT, 0),
//                        terrain.getDestroyedEffectId(), false);
//            }
            changeTerrain(tile, terrain.getDestroyedTypeTerrainId());

//            updateRoomWalls(tile);
//            mapLoader.updateTiles(mapLoader.getSurroundingTiles(tile.getLocation(), true));
            // Notify
//            notifyTileChange(point);
        } else if (terrain.getFlags().contains(Terrain.TerrainFlag.DECAY)) {
//            mapLoader.updateTiles(point);
        }

        // Notify
        notifyTileChange(tile);

        return returnedGold;
    }

    /**
     * Heal a tile
     *
     * @param point the point
     * @param playerId the player applying the healing
     */
    @Override
    public void healTile(Point point, short playerId) {
        MapTile tile = getMapData().getTile(point);
        Terrain terrain = getTerrain(tile);

        // See the amount of healing
        // TODO: now just claiming of a tile (claim health variable is too big it seems)
        int healing;
        if (terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)) {

            if (terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE)) {
                if (tile.getOwnerId() == playerId) {
                    healing = (int) getLevelVariable(Variable.MiscVariable.MiscType.REPAIR_WALL_HEALTH);
                } else {
                    healing = (int) getLevelVariable(Variable.MiscVariable.MiscType.CLAIM_TILE_HEALTH);
                }
            } else {
                healing = (int) getLevelVariable(Variable.MiscVariable.MiscType.REINFORCE_WALL_HEALTH);
            }
        } else {
            healing = (int) getLevelVariable(Variable.MiscVariable.MiscType.REPAIR_TILE_HEALTH);
        }

        // Apply
        if (applyHealing(tile, healing)) {

            // TODO: effect & checks
            // The tile is upgraded
//            if (terrain.getMaxHealthEffectId() != 0) {
//                effectManager.load(worldNode,
//                        WorldUtils.pointToVector3f(point).addLocal(0, MapLoader.FLOOR_HEIGHT, 0),
//                        terrain.getMaxHealthEffectId(), false);
//            }
            if (terrain.getMaxHealthTypeTerrainId() != 0) {
                changeTerrain(tile, terrain.getMaxHealthTypeTerrainId());
                tile.setOwnerId(playerId);
//                terrain = tile.getTerrain();
//                if (tile.isAtFullHealth()) {
//                    effectManager.load(worldNode,
//                            WorldUtils.pointToVector3f(point).addLocal(0, MapLoader.FLOOR_HEIGHT, 0),
//                            terrain.getMaxHealthEffectId(), false);
//                }
            }

//            updateRoomWalls(tile);
//            mapLoader.updateTiles(mapLoader.getSurroundingTiles(tile.getLocation(), true));
            // Notify
//            notifyTileChange(point);
        } else if (terrain.getFlags().contains(Terrain.TerrainFlag.DECAY)) {
//            mapLoader.updateTiles(point);
        }

        // Notify
        notifyTileChange(tile);
    }

    /**
     * Damage a room
     *
     * @param point tile coordinate
     * @param playerId for the player
     */
    private void damageRoom(Point point, short playerId) {
        MapTile tile = getMapData().getTile(point);

        // Calculate the damage
        int damage;
        short owner = tile.getOwnerId();
        if (owner == Player.NEUTRAL_PLAYER_ID) {
            damage = (int) getLevelVariable(Variable.MiscVariable.MiscType.CONVERT_ROOM_HEALTH);
        } else {
            damage = (int) getLevelVariable(Variable.MiscVariable.MiscType.ATTACK_ROOM_HEALTH);
        }

        // Get the room
        RoomInstance room = getRoomInstanceByCoordinates(point);
        List<Point> roomTiles = room.getCoordinates();

        // Apply the damage equally to all tiles so that the overall condition can be checked easily
        // I don't know if this model is correct or not, but like this the bigger the room the more effort it requires to claim
        int damagePerTile = Math.abs(damage / roomTiles.size());
        for (Point p : roomTiles) {
            MapTile roomTile = getMapData().getTile(p);
            if (applyDamage(roomTile, damagePerTile)) {

                // If one of the tiles runs out (everyone should run out of the same time, unless a new tile has recently being added..)
                for (Point p2 : roomTiles) {
                    roomTile = getMapData().getTile(p2);
                    roomTile.setOwnerId(playerId); // Claimed!
                    applyHealing(roomTile, tile.getMaxHealth());

//                    effectManager.load(worldNode,
//                            WorldUtils.pointToVector3f(point).addLocal(0, MapLoader.FLOOR_HEIGHT, 0),
//                            tile.getTerrain().getMaxHealthEffectId(), false);
//
//                    // FIXME ROOM_CLAIM_ID is realy claim effect?
//                    effectManager.load(worldNode,
//                            WorldUtils.pointToVector3f(p2).addLocal(0, MapLoader.FLOOR_HEIGHT, 0),
//                            room.getRoom().getEffects().get(EffectManagerState.ROOM_CLAIM_ID), false);
                    // TODO: Claimed room wall tiles lose the claiming I think?
                    notifyTileChange(roomTile);
                }

                // Notify
                IRoomController roomController = getRoomController(room);
                roomController.captured(playerId);
                notifyOnCapturedByEnemy(owner, roomController);
                notifyOnCaptured(playerId, roomController);
                break;
            }
        }
    }

    public int mineGold(MapTile tile, int amount) {
        int minedAmount = Math.min(amount, tile.getGold());
        tile.setGold(tile.getGold() - minedAmount);
        return minedAmount;
    }

    private boolean applyDamage(MapTile tile, int damage) {
        tile.setHealth(Math.max(0, tile.getHealth() - damage));
        return (tile.getHealth() == 0);
    }

    public boolean applyHealing(MapTile tile, int healing) {
        tile.setHealth((int) Math.min(tile.getMaxHealth(), (long) tile.getHealth() + healing));
        return tile.isAtFullHealth();
    }

    private void changeTerrain(MapTile tile, short terrainId) {
        tile.setTerrainId(terrainId);
        Terrain terrain = getTerrain(tile);
        MapTile.setAttributesFromTerrain(tile, terrain);

        // If the terrain is not taggable anymore, reset the tagging data
        if (!terrain.getFlags().contains(Terrain.TerrainFlag.TAGGABLE)) {
            tile.setSelected(false, Player.KEEPER1_ID);
            tile.setSelected(false, Player.KEEPER2_ID);
            tile.setSelected(false, Player.KEEPER3_ID);
            tile.setSelected(false, Player.KEEPER4_ID);
        }
    }

    @Override
    public void alterTerrain(Point pos, short terrainId, short playerId) {
        MapTile tile = getMapData().getTile(pos.x, pos.y);
        if (tile == null) {
            return;
        }

        // Alter
        changeTerrain(tile, terrainId);
        tile.setTerrainId(terrainId);

        // Set owner
        if (playerId != 0) {
            tile.setOwnerId(playerId);
        }

        notifyTileChange(tile);
    }

    private float getLevelVariable(Variable.MiscVariable.MiscType variable) {
        return gameSettings.get(variable).getValue();
    }

    @Override
    public void flashTiles(List<Point> points, short playerId, int time) {
        List<Point> tilesToUpdate = new ArrayList<>(points.size());

        // Mark the tiles as being flashed
        for (Point point : points) {
            if (!getMapData().getTile(point).isFlashed(playerId)) {
                getMapData().getTile(point).setFlashed(true, playerId);
                tilesToUpdate.add(point);
            }
        }

        // Set a control that will turn them off at some point if they are timed
        if (time > 0) {
            addControl(new FlashTileControl(points, playerId, time));
        }

        // Notify listeners
        if (!tilesToUpdate.isEmpty()) {
            for (MapListener mapListener : mapListeners.getArray()) {
                mapListener.onTileFlash(tilesToUpdate, true, playerId);
            }
        }
    }

    @Override
    public void buildOrSellRoom(SelectionArea area, short playerId, short roomId) {
        getControl(BuildTileControl.class).add(area, playerId, roomId);
    }

    @Override
    public void buildOrSellRoom(SelectionArea area, short playerId) {
        buildOrSellRoom(area, playerId, (short) 0);
    }

    @Override
    public void unFlashTiles(List<Point> points, short playerId) {
        List<Point> tilesToUpdate = new ArrayList<>(points.size());

        // Mark the tiles as being unflashed
        // Hmm, we don't really keep track, so it is entirely possible that we still have a flash control for given tile
        // But technically it shouldn't matter, as it will just eventually set the flashing false and die out
        for (Point point : points) {
            if (getMapData().getTile(point).isFlashed(playerId)) {
                getMapData().getTile(point).setFlashed(false, playerId);
                tilesToUpdate.add(point);
            }
        }

        // Notify listeners
        if (!tilesToUpdate.isEmpty()) {
            for (MapListener mapListener : mapListeners.getArray()) {
                mapListener.onTileFlash(tilesToUpdate, false, playerId);
            }
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
    }

    @Override
    public void processTick(float tpf, double gameTime) {
        this.update(tpf);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);

        OutputCapsule out = ex.getCapsule(this);
        out.write(mapData, "mapData", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);

        InputCapsule in = im.getCapsule(this);
        mapData = (MapData) in.readSavable("mapData", null);
    }

    @Override
    public Set<Point> getTerrainBatches(List<Point> startingPoints, int x1, int x2, int y1, int y2) {
        Set<Point> batches = new HashSet<>();
        for (Point start : startingPoints) {
            findTerrainBatch(start, null, batches, x1, x2, y1, y2);
        }
        return batches;
    }

    private void findTerrainBatch(Point p, Short terrainId, Set<Point> batches, int x1, int x2, int y1, int y2) {

        // See the constraints
        if (p.x < x1 || p.x >= x2 || p.y < y1 || p.y >= y2) {
            return;
        }

        MapTile tile = getMapData().getTile(p);
        if (terrainId == null) {
            terrainId = tile.getTerrainId();
        }

        if (!batches.contains(p)) {
            if (tile.getTerrainId() == terrainId) {

                // Add the coordinate
                batches.add(p);

                // Find north
                findTerrainBatch(new Point(p.x, p.y - 1), terrainId, batches, x1, x2, y1, y2);

                // Find east
                findTerrainBatch(new Point(p.x + 1, p.y), terrainId, batches, x1, x2, y1, y2);

                // Find south
                findTerrainBatch(new Point(p.x, p.y + 1), terrainId, batches, x1, x2, y1, y2);

                // Find west
                findTerrainBatch(new Point(p.x - 1, p.y), terrainId, batches, x1, x2, y1, y2);
            }
        }
    }

    @Override
    public boolean isSellable(SelectionArea selectionArea, short playerId) {
        for (Iterator<Point> it = selectionArea.simpleIterator(); it.hasNext();) {
            Point p = it.next();

            MapTile tile = getMapData().getTile(p);
            if (tile != null && isSellable(p, playerId)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isBuildable(SelectionArea selectionArea, short playerId, short roomId) {

        for (Iterator<Point> it = selectionArea.simpleIterator(); it.hasNext();) {
            Point p = it.next();

            MapTile tile = getMapData().getTile(p);
            if (tile == null) {
                continue;
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
            if ((room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_WATER)
                    && terrain.getFlags().contains(Terrain.TerrainFlag.WATER))
                    || room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_LAVA)
                    && terrain.getFlags().contains(Terrain.TerrainFlag.LAVA)) {

                // We need to have an adjacent owned tile
                boolean adjacent = hasAdjacentOwnedPath(tile.getLocation(), playerId);
                if (adjacent) {
                    return true;
                }
            }
        }

        return false;
    }
}
