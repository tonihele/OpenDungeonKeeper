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

import com.jme3.math.Vector2f;
import com.jme3.util.SafeArrayList;
import com.simsilica.es.EntityData;
import toniarts.openkeeper.utils.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.control.Container;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.map.FlashTileControl;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.listener.MapListener;
import toniarts.openkeeper.game.listener.RoomListener;
import toniarts.openkeeper.game.map.IMapData;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.map.MapData;
import toniarts.openkeeper.game.map.MapInformation;
import toniarts.openkeeper.game.map.MapTileController;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Variable;

/**
 * This is controller for the map related functions
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class MapController extends Container implements IMapController {

    private final IMapData mapData;
    private final KwdFile kwdFile;
    private final IGameTimer gameTimer;
    private final IObjectsController objectsController;
    private final Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings;
    private final EntityData entityData;
    private final IMapInformation<IMapTileController> mapInformation;
    private final ILevelInfo levelInfo;

    private final Map<Point, RoomInstance> roomCoordinates = new HashMap<>();
    private final Map<RoomInstance, IRoomController> roomControllers = new HashMap<>();
    private final SafeArrayList<MapListener> mapListeners = new SafeArrayList<>(MapListener.class);
    private final Map<Short, SafeArrayList<RoomListener>> roomListeners = new HashMap<>();

    /**
     * Load map data from a KWD file straight (new game)
     *
     * @param kwdFile           the KWD file
     * @param objectsController objects controller
     * @param gameSettings      the game settings
     * @param gameTimer
     * @param entityData
     * @param levelInfo
     */
    public MapController(KwdFile kwdFile, IObjectsController objectsController, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings,
            IGameTimer gameTimer, EntityData entityData, ILevelInfo levelInfo) {
        this.kwdFile = kwdFile;
        this.objectsController = objectsController;
        this.mapData = new MapData(kwdFile, entityData, levelInfo.getPlayers());
        this.gameSettings = gameSettings;
        this.gameTimer = gameTimer;
        this.entityData = entityData;
        this.mapInformation = new MapInformation(mapData, kwdFile, levelInfo.getPlayers());
        this.levelInfo = levelInfo;

        // Load rooms
        loadRooms();
    }

    private void loadRooms() {

        // Go through the tiles and detect any rooms
        for (int y = 0; y < mapData.getHeight(); y++) {
            for (int x = 0; x < mapData.getWidth(); x++) {
                loadRoom(new Point(x, y));
            }
        }
    }

    private void loadRoom(Point p) {
        IMapTileController mapTile = mapData.getTile(p);
        if (!kwdFile.getTerrain(mapTile.getTerrainId()).getFlags().contains(Terrain.TerrainFlag.ROOM)) {
            return;
        }

        if (roomCoordinates.containsKey(p)) {
            return;
        }

        // Find it
        RoomInstance roomInstance = new RoomInstance(kwdFile.getRoomByTerrain(mapTile.getTerrainId()));
        roomInstance.setOwnerId(mapTile.getOwnerId());
        findRoom(p, roomInstance);

        // Create a controller for it
        IRoomController roomController = RoomControllerFactory.constructRoom(kwdFile, roomInstance, objectsController, gameSettings, gameTimer);
        roomController.construct();
        roomControllers.put(roomInstance, roomController);

        // TODO: A bit of a design problem here
        /**
         * Unclear responsibilities between the world and map controller.
         * Also a result of how we handle the building and selling by
         * destroying rooms.
         * But at least keep anyone who is listening intact
         */
        notifyOnBuild(roomController.getRoomInstance().getOwnerId(), roomController);
    }

    /**
     * Find the room starting from a certain point, rooms are never diagonally
     * attached
     *
     * @param p            starting point
     * @param roomInstance the room instance
     */
    private void findRoom(Point p, RoomInstance roomInstance) {
        IMapTileController tile = getMapData().getTile(p);

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
    public IMapData getMapData() {
        return mapData;
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
    public void selectTiles(Vector2f start, Vector2f end, boolean select, short playerId) {
        List<Point> updatableTiles = new ArrayList<>();
        for (int x = (int) Math.max(0, start.x); x < Math.min(kwdFile.getMap().getWidth(), end.x + 1); x++) {
            for (int y = (int) Math.max(0, start.y); y < Math.min(kwdFile.getMap().getHeight(), end.y + 1); y++) {
                IMapTileController tile = getMapData().getTile(x, y);
                if (tile == null) {
                    continue;
                }
                Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
                if (!terrain.getFlags().contains(Terrain.TerrainFlag.TAGGABLE)) {
                    continue;
                }
                tile.setSelected(select, playerId);
                updatableTiles.add(tile.getLocation());
            }
        }
        //Point[] tiles = updatableTiles.toArray(new Point[updatableTiles.size()]);
        //mapLoader.updateTiles(tiles);

        // Notify
        notifyTileChange(updatableTiles);
    }

    @Override
    public boolean isSelected(Point p, short playerId) {
        return mapInformation.isSelected(p, playerId);
    }

    @Override
    public boolean isTaggable(Point p) {
        return mapInformation.isTaggable(p);
    }

    @Override
    public boolean isBuildable(Point p, short playerId, short roomId) {
        return mapInformation.isBuildable(p, playerId, roomId);
    }

    @Override
    public boolean isClaimable(Point p, short playerId) {
        return mapInformation.isClaimable(p, playerId);
    }

    @Override
    public boolean isSellable(Point p, short playerId) {
        return mapInformation.isSellable(p, playerId);
    }

    @Override
    public boolean isWater(Point p) {
        return mapInformation.isWater(p);
    }

    @Override
    public boolean isLava(Point p) {
        return mapInformation.isLava(p);
    }

    @Override
    public void setTiles(List<IMapTileController> tiles) {
        mapInformation.setTiles(tiles);
    }

    private void notifyTileChange(Point updatedTile) {
        List<Point> mapTiles = new ArrayList<>(1);
        mapTiles.add(updatedTile);
        notifyTileChange(mapTiles);
    }

    private void notifyTileChange(List<Point> updatedTiles) {
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
             * destroying rooms.
             * But at least keep anyone who is listening intact
             */
            notifyOnSold(roomController.getRoomInstance().getOwnerId(), roomController);
        }
    }

    /**
     * Get rooms by function.<br> FIXME: Should the player have ready lists?
     *
     * @param objectType the function
     * @param playerId   the player id, can be null
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
            loadRoom(p);
        }
    }

    @Override
    public boolean isRepairableWall(Point p, short playerId) {
        return mapInformation.isRepairableWall(p, playerId);
    }

    @Override
    public boolean isClaimableWall(Point p, short playerId) {
        return mapInformation.isClaimableWall(p, playerId);
    }

    @Override
    public boolean isClaimableTile(Point p, short playerId) {
        return mapInformation.isClaimableTile(p, playerId);
    }

    @Override
    public boolean isClaimableRoom(Point p, short playerId) {
        return mapInformation.isClaimableRoom(p, playerId);
    }

    @Override
    public Terrain getTerrain(IMapTileInformation tile) {
        return mapInformation.getTerrain(tile);
    }

    @Override
    public void applyClaimTile(Point point, short playerId) {
        IMapTileController tile = getMapData().getTile(point);
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
        IMapTileController tile = getMapData().getTile(point);
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
//                        WorldUtils.pointToVector3f(point).addLocal(0, MapViewController.FLOOR_HEIGHT, 0),
//                        terrain.getDestroyedEffectId(), false);
//            }
            changeTerrain(tile, terrain.getDestroyedTypeTerrainId());

//            updateRoomWalls(tile);
//            mapLoader.updateTiles(mapLoader.getSurroundingTiles(tile.getLocation(), true));
            // Notify
//            notifyTileChange(point);
            // Notify
            notifyTileChange(tile.getLocation());

        } else if (terrain.getFlags().contains(Terrain.TerrainFlag.DECAY)) {
//            mapLoader.updateTiles(point);
        }

        return returnedGold;
    }

    /**
     * Heal a tile
     *
     * @param point    the point
     * @param playerId the player applying the healing
     */
    @Override
    public void healTile(Point point, short playerId) {
        IMapTileController tile = getMapData().getTile(point);
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
//                        WorldUtils.pointToVector3f(point).addLocal(0, MapViewController.FLOOR_HEIGHT, 0),
//                        terrain.getMaxHealthEffectId(), false);
//            }
            if (terrain.getMaxHealthTypeTerrainId() != 0) {
                changeTerrain(tile, terrain.getMaxHealthTypeTerrainId());
                tile.setOwnerId(playerId);
//                terrain = tile.getTerrain();
//                if (tile.isAtFullHealth()) {
//                    effectManager.load(worldNode,
//                            WorldUtils.pointToVector3f(point).addLocal(0, MapViewController.FLOOR_HEIGHT, 0),
//                            terrain.getMaxHealthEffectId(), false);
//                }
            }

//            updateRoomWalls(tile);
//            mapLoader.updateTiles(mapLoader.getSurroundingTiles(tile.getLocation(), true));
            // Notify
//            notifyTileChange(point);
            // Notify
            notifyTileChange(tile.getLocation());
        } else if (terrain.getFlags().contains(Terrain.TerrainFlag.DECAY)) {
//            mapLoader.updateTiles(point);
        }
    }

    /**
     * Damage a room
     *
     * @param point    tile coordinate
     * @param playerId for the player
     */
    private void damageRoom(Point point, short playerId) {
        IMapTileController tile = getMapData().getTile(point);

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
            IMapTileController roomTile = getMapData().getTile(p);
            if (applyDamage(roomTile, damagePerTile)) {

                // If one of the tiles runs out (everyone should run out of the same time, unless a new tile has recently being added..)
                for (Point p2 : roomTiles) {
                    roomTile = getMapData().getTile(p2);
                    roomTile.setOwnerId(playerId); // Claimed!
                    applyHealing(roomTile, tile.getMaxHealth());

//                    effectManager.load(worldNode,
//                            WorldUtils.pointToVector3f(point).addLocal(0, MapViewController.FLOOR_HEIGHT, 0),
//                            tile.getTerrain().getMaxHealthEffectId(), false);
//
//                    // FIXME ROOM_CLAIM_ID is realy claim effect?
//                    effectManager.load(worldNode,
//                            WorldUtils.pointToVector3f(p2).addLocal(0, MapViewController.FLOOR_HEIGHT, 0),
//                            room.getRoom().getEffects().get(EffectManagerState.ROOM_CLAIM_ID), false);
                    // TODO: Claimed room wall tiles lose the claiming I think?
                }

                // Notify
                notifyTileChange(roomTiles);
                IRoomController roomController = getRoomController(room);
                roomController.captured(playerId);
                notifyOnCapturedByEnemy(owner, roomController);
                notifyOnCaptured(playerId, roomController);
                break;
            }
        }
    }

    public int mineGold(IMapTileController tile, int amount) {
        int minedAmount = Math.min(amount, tile.getGold());
        tile.setGold(tile.getGold() - minedAmount);
        return minedAmount;
    }

    private boolean applyDamage(IMapTileController tile, int damage) {
        tile.setHealth(Math.max(0, tile.getHealth() - damage));
        return (tile.getHealth() == 0);
    }

    public boolean applyHealing(IMapTileController tile, int healing) {
        tile.setHealth((int) Math.min(tile.getMaxHealth(), (long) tile.getHealth() + healing));
        return tile.isAtFullHealth();
    }

    private void changeTerrain(IMapTileController tile, short terrainId) {
        tile.setTerrainId(terrainId);
        Terrain terrain = getTerrain(tile);
        MapTileController.setAttributesFromTerrain(entityData, tile, terrain);

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
        IMapTileController tile = getMapData().getTile(pos.x, pos.y);
        if (tile == null) {
            return;
        }

        // Alter
        changeTerrain(tile, terrainId);

        // Set owner
        if (playerId != 0) {
            tile.setOwnerId(playerId);
        }

        notifyTileChange(tile.getLocation());
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

        IMapTileController tile = getMapData().getTile(p);
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
    public int getPlayerSkeletonCapacity(short playerId) {
        int capacity = 0;
        for (IRoomController roomController : getRoomsByFunction(AbstractRoomController.ObjectType.PRISONER, playerId)) {
            capacity += roomController.getObjectControl(AbstractRoomController.ObjectType.PRISONER).getMaxCapacity();
        }

        return capacity;
    }

    @Override
    public boolean isSolid(Point p) {
        return mapInformation.isSolid(p);
    }
}
