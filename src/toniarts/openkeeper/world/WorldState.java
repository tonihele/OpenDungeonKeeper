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
package toniarts.openkeeper.world;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.ai.steer.utils.paths.LinePath;
import com.badlogic.gdx.ai.steer.utils.paths.LinePath.Segment;
import com.badlogic.gdx.math.Vector2;
import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Line;
import java.lang.System.Logger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.ai.creature.CreatureState;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.state.GameState;
import toniarts.openkeeper.game.task.TaskManager;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Tile;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.tools.modelviewer.Debug;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.animation.AnimationLoader;
import toniarts.openkeeper.view.selection.SelectionArea;
import toniarts.openkeeper.world.control.FlashTileControl;
import toniarts.openkeeper.world.control.IInteractiveControl;
import toniarts.openkeeper.world.creature.CreatureControl;
import toniarts.openkeeper.world.effect.EffectManagerState;
import toniarts.openkeeper.world.listener.CreatureListener;
import toniarts.openkeeper.world.listener.RoomListener;
import toniarts.openkeeper.world.listener.TileChangeListener;
import toniarts.openkeeper.world.object.GoldObjectControl;
import toniarts.openkeeper.world.object.ObjectControl;
import toniarts.openkeeper.world.pathfinding.MapDistance;
import toniarts.openkeeper.world.pathfinding.MapIndexedGraph;
import toniarts.openkeeper.world.pathfinding.MapPathFinder;
import toniarts.openkeeper.world.pathfinding.PathFindable;
import toniarts.openkeeper.world.room.GenericRoom;
import toniarts.openkeeper.world.room.RoomInstance;
import toniarts.openkeeper.world.room.control.RoomGoldControl;

/**
 * Handles the handling of game world, physics & visual wise
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Deprecated
public abstract class WorldState extends AbstractAppState {
    
    private static final Logger logger = System.getLogger(WorldState.class.getName());

    private Main app;
    private AppStateManager stateManager;
    private final MapLoader mapLoader;
    private final ThingLoader thingLoader;
    private final KwdFile kwdFile;
    private AssetManager assetManager;
    private Node worldNode;
    private final MapIndexedGraph pathFindingMap;
    private final MapPathFinder pathFinder;
    private final MapDistance heuristic;
    private final Node thingsNode;
    //private final BulletAppState bulletAppState;
    private final EffectManagerState effectManager;
    private List<TileChangeListener> tileChangeListener;
    private Map<Short, List<RoomListener>> roomListeners;
    private final GameState gameState;
    private final FlashTileControl flashTileControl;
    public final Object goldLock = new Object();

    public WorldState(final KwdFile kwdFile, final AssetManager assetManager, GameState gameState) {
        this.kwdFile = kwdFile;
        this.gameState = gameState;

        // Effect manager
        effectManager = new EffectManagerState(kwdFile, assetManager);

        // World node
        worldNode = new Node("World");
        if (Main.isDebug()) {
            Debug.showNodeAxes(assetManager, worldNode, 10);
        }

        // Create physics state
        //bulletAppState = new BulletAppState();
        //bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);

        // Create the actual map
        thingLoader = new ThingLoader(this, kwdFile, assetManager);
        this.mapLoader = new MapLoader(assetManager, kwdFile, effectManager, this, thingLoader.getObjectLoader()) {
            @Override
            protected void updateProgress(float progress) {
                WorldState.this.updateProgress(progress);
            }
        };
        worldNode.attachChild(mapLoader.load(assetManager, kwdFile));

        // For path finding
        pathFindingMap = new MapIndexedGraph(this);
        pathFinder = new MapPathFinder(pathFindingMap, false);
        heuristic = new MapDistance();

        // Things
        thingsNode = thingLoader.loadAll(gameState.getCreatureTriggerState(), gameState.getObjectTriggerState(), gameState.getDoorTriggerState(), gameState.getPartyTriggerState());
        worldNode.attachChild(thingsNode);

        flashTileControl = new FlashTileControl(this, (Main) gameState.getApplication());

        // Player money
        initPlayerMoney();

        // Player creatures
        initPlayerCreatures();

        // Player rooms
        initPlayerRooms();
    }

    private void initPlayerMoney() {

        // The max money$$$
        for (Entry<RoomInstance, GenericRoom> roomEntry : mapLoader.getRoomActuals().entrySet()) {
            if (roomEntry.getValue().canStoreGold()) {
                Keeper keeper = gameState.getPlayer(roomEntry.getKey().getOwnerId());
                if (keeper != null) {
//                    keeper.getGoldControl().setGoldMax(keeper.getGoldControl().getGoldMax() + roomEntry.getValue().getObjectControl(GenericRoom.ObjectType.GOLD).getMaxCapacity());
                }
            }
        }

        // Set up the money$$$
        for (Keeper keeper : gameState.getPlayers()) {
            Player player = kwdFile.getPlayer(keeper.getId());
            if (player.getStartingGold() > 0) {
                addGold(keeper.getId(), player.getStartingGold());
            }
        }
    }

    private void initPlayerCreatures() {

        // Add the initial creatures and add the listeners
        Map<Short, List<CreatureControl>> playerCreatures = thingLoader.getCreatures().stream().collect(Collectors.groupingBy(c -> c.getOwnerId()));
        for (Keeper player : gameState.getPlayers()) {
            List<CreatureControl> creatures = playerCreatures.get(player.getId());
//            player.getCreatureControl().init(creatures, kwdFile.getImp());
            thingLoader.addListener(player.getId(), new CreatureListener() {

                @Override
                public void onSpawn(CreatureControl creature) {
//                    if (player.getId() == stateManager.getState(PlayerState.class).getPlayerId() && player.getCreatureControl().getTypeCount(creature.getCreature()) == 0) {
//
//                        // First appearance
//                        String message;
//                        Integer overrideTextId = kwdFile.getGameLevel().getIntroductionOverrideTextIds().get(creature.getCreature().getCreatureId());
//                        if (overrideTextId != null) {
//                            message = String.format("${level.%d}", overrideTextId - 1);
//                            stateManager.getState(SoundState.class).attachLevelSpeech(overrideTextId);
//
//                            stateManager.getState(PlayerState.class).setText(overrideTextId, true, 0);
//                        } else {
//                            // default entrance message
//                            stateManager.getState(SoundState.class).attachMentorSpeech(MentorType.SPECIES_CREATURE_ENTERED);
//                            message = String.format("${speech.%d}", MentorType.SPECIES_CREATURE_ENTERED.getId() - 1);
//                        }
//
//                        stateManager.getState(SystemMessageState.class).addMessage(SystemMessageState.MessageType.CREATURE, message);
//                    }
//                    player.getCreatureControl().onSpawn(creature);
                }

                @Override
                public void onStateChange(CreatureControl creature, CreatureState newState, CreatureState oldState) {
//                    player.getCreatureControl().onStateChange(creature, newState, oldState);
                }

                @Override
                public void onDie(CreatureControl creature) {
//                    player.getCreatureControl().onDie(creature);
                }
            });
        }
    }

    private void initPlayerRooms() {

        // Add the initial creatures and add the listeners
        Map<Short, List<Entry<RoomInstance, GenericRoom>>> playerRooms = mapLoader.getRoomActuals().entrySet().stream().collect(Collectors.groupingBy(entry -> entry.getKey().getOwnerId()));
        for (Keeper player : gameState.getPlayers()) {
            List<Entry<RoomInstance, GenericRoom>> rooms = playerRooms.get(player.getId());
            if (rooms != null) {
//                player.getRoomControl().init(rooms);
            }

            // Add the listener
//            addListener(player.getId(), player.getRoomControl());
        }
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);
        this.app = (Main) app;
        this.stateManager = stateManager;
        this.assetManager = app.getAssetManager();

        // Attach physics
        //this.stateManager.attach(bulletAppState);

        // Effects
        this.stateManager.attach(effectManager);

        // Attach the world
        this.app.getRootNode().attachChild(worldNode);
    }

    @Override
    public void cleanup() {

        // Detach our map
        if (worldNode != null) {
            app.getRootNode().detachChild(worldNode);
            worldNode = null;
        }

        // Physics away
        //stateManager.detach(bulletAppState);

        // Effects
        this.stateManager.detach(effectManager);

        super.cleanup();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        // Stop all the animations
        for (CreatureControl creature : getThingLoader().getCreatures()) {
            creature.setEnabled(enabled);
            if (enabled) {
                AnimationLoader.resumeAnimations(creature.getSpatial());
            } else {
                AnimationLoader.pauseAnimations(creature.getSpatial());
            }
        }
    }

    @Override
    public void update(float tpf) {
        if (!isInitialized() || !isEnabled()) {
            return;
        }

        flashTileControl.update(tpf);
    }

    public AssetManager getAssetManager() {
        return assetManager;
    }

    /**
     * If you want to monitor the map loading progress, use this method
     *
     * @param progress current progress from 0.0 to 1.0
     */
    protected abstract void updateProgress(final float progress);

    /**
     * If you want to get notified about tile changes
     *
     * @param listener the listener
     */
    public void addListener(TileChangeListener listener) {
        if (tileChangeListener == null) {
            tileChangeListener = new ArrayList<>();
        }
        tileChangeListener.add(listener);
    }

    public MapData getMapData() {
        return mapLoader.getMapData();
    }

    /**
     * Get the map loader
     *
     * @return MapLoader
     */
    public MapLoader getMapLoader() {
        return mapLoader;
    }

    public ThingLoader getThingLoader() {
        return thingLoader;
    }

    public Node getWorld() {
        return worldNode;
    }

    /**
     * @deprecated
     *
     * @return KwdFile
     */
    @Deprecated
    public KwdFile getLevelData() {
        return kwdFile;
    }

    /**
     * Set some tiles selected/undelected
     *
     * @param selectionArea the selection area
     * @param select select or unselect
     * @param playerId the player who selected the tile
     */
    public void selectTiles(SelectionArea selectionArea, boolean select, short playerId) {
        List<Point> updatableTiles = new ArrayList<>();
        for (int x = (int) Math.max(0, selectionArea.getStart().x); x < Math.min(kwdFile.getMap().getWidth(), selectionArea.getEnd().x + 1); x++) {
            for (int y = (int) Math.max(0, selectionArea.getStart().y); y < Math.min(kwdFile.getMap().getHeight(), selectionArea.getEnd().y + 1); y++) {
                TileData tile = getMapData().getTile(x, y);
                if (tile == null) {
                    continue;
                }
                Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
                if (!terrain.getFlags().contains(Terrain.TerrainFlag.TAGGABLE)) {
                    continue;
                }
                tile.setSelected(select, playerId);
                updatableTiles.add(new Point(x, y));
            }
        }
        Point[] tiles = updatableTiles.toArray(new Point[updatableTiles.size()]);
        mapLoader.updateTiles(tiles);

        // Notify
        notifyTileChange(tiles);
    }

    /**
     * Determine if a tile at x & y is selected or not
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return is the tile selected
     */
    public boolean isSelected(int x, int y) {
        TileData tile = getMapData().getTile(x, y);
        if (tile == null) {
            return false;
        }
        return tile.isSelected();
    }

    /**
     * Determine if a tile at x & y is selectable or not
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return is the tile selectable
     */
    public boolean isTaggable(int x, int y) {
        TileData tile = getMapData().getTile(x, y);
        if (tile == null) {
            return false;
        }
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        return terrain.getFlags().contains(Terrain.TerrainFlag.TAGGABLE);
    }

    /**
     * Determine if a tile at x & y is buildable by the player
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param player the player
     * @param room the room to be build
     * @return is the tile buildable
     */
    public boolean isBuildable(int x, int y, Player player, Room room) {
        TileData tile = getMapData().getTile(x, y);
        if (tile == null) {
            return false;
        }

        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());

        // Check that this is not already a room
        if (mapLoader.getRoomCoordinates().containsKey(new Point(x, y))) {
            return false;
        }

        // Ownable tile is needed for land building (and needs to be owned by us)
        if (room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_LAND)
                && !terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)
                && terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE)
                && tile.getPlayerId() == player.getPlayerId()) {
            return true;
        }

        // See if we are dealing with bridges
        if (room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_WATER) && terrain.getFlags().contains(Terrain.TerrainFlag.WATER)) {
            return true;
        }
        if (room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_LAVA) && terrain.getFlags().contains(Terrain.TerrainFlag.LAVA)) {
            return true;
        }

        return false;
    }

    /**
     * Determine if a tile (maybe a room) at x & y is claimable by the player
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param playerId the player
     * @return is the tile claimable by you
     */
    public boolean isClaimable(int x, int y, short playerId) {
        TileData tile = getMapData().getTile(x, y);
        if (tile == null) {
            return false;
        }

        // See if the terrain is claimable at all
        Terrain terrain = tile.getTerrain();
        boolean claimable = false;
        if (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)) {
            if (tile.getPlayerId() != playerId) {
                claimable = true;
            }
        } else if (terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE)) {
            if (tile.getPlayerId() != playerId) {
                claimable = true;
            }
        } else {
            claimable = (terrain.getMaxHealthTypeTerrainId() != terrain.getTerrainId());
        }

        // In order to claim, it needs to be adjacent to your own tiles
        if (claimable) {
            for (Point p : getMapLoader().getSurroundingTiles(new Point(x, y), false)) {
                TileData neighbourTile = getMapData().getTile(p);
                if (neighbourTile != null && neighbourTile.getPlayerId() == playerId && neighbourTile.getTerrain().getFlags().contains(Terrain.TerrainFlag.OWNABLE) && !neighbourTile.getTerrain().getFlags().contains(Terrain.TerrainFlag.SOLID)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void addPlayerGold(short playerId, int value) {
        if (value == 0) {
            return;
        }
        Keeper keeper = gameState.getPlayer(playerId);
//        keeper.getGoldControl().addGold(value);
    }

    public void alterTerrain(Point pos, short terrainId, short playerId, boolean enqueue) {
        TileData tile = getMapData().getTile(pos.x, pos.y);
        if (tile == null) {
            return;
        }
        // set type
        tile.setTerrainId(terrainId);
        // set owner
        if (playerId != 0) {
            tile.setPlayerId(playerId);
        }

        // See if room walls are allowed and does this touch any rooms
        updateRoomWalls(tile);

        // update one
        updateTiles(enqueue, mapLoader.getSurroundingTiles(pos, true));
    }

    /**
     * Update map tiles, on the scene graph
     *
     * @param enqueue if {@code false} this is executed in the current thread,
     * otherwise it is enqueued to the update loop
     * @param points the map points to update
     */
    protected void updateTiles(boolean enqueue, Point... points) {

        // Enqueue if app is set
        if (enqueue) {

            app.enqueue(() -> {

                mapLoader.updateTiles(points);

                return null;
            });
        } else {
            mapLoader.updateTiles(points);
        }
    }

    /**
     * Dig a tile at x & y
     *
     * @param x x coordinate
     * @param y y coordinate
     */
    public void digTile(int x, int y) {

        TileData tile = getMapData().getTile(x, y);
        if (tile == null) {
            return;
        }

        Terrain terrain = tile.getTerrain();
        if (terrain.getFlags().contains(Terrain.TerrainFlag.IMPENETRABLE)) {
            return;
        }

        // FIXME: this is just a debug stuff, remove when the imps can carry the gold
        addPlayerGold(Player.KEEPER1_ID, terrain.getGoldValue());

        tile.setTerrainId(terrain.getDestroyedTypeTerrainId());
        tile.setSelected(false, Player.KEEPER1_ID);
        tile.setFlashed(false);

        // See if room walls are allowed and does this touch any rooms
        updateRoomWalls(tile);
        mapLoader.updateTiles(mapLoader.getSurroundingTiles(tile.getLocation(), true));
    }

    private void updateRoomWalls(TileData tile) {
        Terrain terrain = tile.getTerrain();
        // See if room walls are allowed and does this touch any rooms
        if (!terrain.getFlags().contains(Terrain.TerrainFlag.ALLOW_ROOM_WALLS)) {
            return;
        }
        List<RoomInstance> wallUpdatesNeeded = new ArrayList<>();
        for (Point p : mapLoader.getSurroundingTiles(tile.getLocation(), false)) {
            RoomInstance room = mapLoader.getRoomCoordinates().get(p);
            if (room != null && room.getRoom().getFlags().contains(Room.RoomFlag.HAS_WALLS)) {
                wallUpdatesNeeded.add(room);
            }
        }
        if (!wallUpdatesNeeded.isEmpty()) {
            mapLoader.updateRoomWalls(wallUpdatesNeeded);
        }
    }

    public void flashTile(boolean enabled, List<Point> points) {
        flashTileControl.attach(points, enabled);
    }

    /**
     * Claim a tile at x & y to the player's name
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param playerId the player, the new owner
     */
    public void claimTile(int x, int y, short playerId) {
        if (!isClaimable(x, y, playerId)) {
            return;
        }

        TileData tile = getMapData().getTile(x, y);
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());

        if (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)) {
            // TODO: Claim all current room
        } else if (terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE)) {
            // tile is claimed by another player and needs to be destroyed
            if (tile.getPlayerId() != playerId) {
                tile.setTerrainId(terrain.getDestroyedTypeTerrainId());
            }
        } else {
            tile.setTerrainId(terrain.getMaxHealthTypeTerrainId());
        }

        terrain = tile.getTerrain();
        if (terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE)) {
            tile.setPlayerId(playerId);
        }
        // See if room walls are allowed and does this touch any rooms
        updateRoomWalls(tile);

        mapLoader.updateTiles(mapLoader.getSurroundingTiles(tile.getLocation(), true));
    }

    /**
     * Build a building to the wanted area
     *
     * @param selectionArea the selection area
     * @param player the player, the new owner
     * @param room room to build
     */
    public void build(SelectionArea selectionArea, Player player, Room room) {
        Set<Point> updatableTiles = new HashSet<>();
        Set<Point> buildPlots = new HashSet<>();
        List<Point> instancePlots = new ArrayList<>();
        for (int x = (int) Math.max(0, selectionArea.getStart().x); x < Math.min(kwdFile.getMap().getWidth(), selectionArea.getEnd().x + 1); x++) {
            for (int y = (int) Math.max(0, selectionArea.getStart().y); y < Math.min(kwdFile.getMap().getHeight(), selectionArea.getEnd().y + 1); y++) {

                // See that is this valid
                if (!isBuildable(x, y, player, room)) {
                    continue;
                }

                Point p = new Point(x, y);
                instancePlots.add(p);
                buildPlots.addAll(Arrays.asList(mapLoader.getSurroundingTiles(p, false)));
                updatableTiles.addAll(Arrays.asList(mapLoader.getSurroundingTiles(p, true)));
            }
        }

        // See that can we afford the building
        int cost = instancePlots.size() * room.getCost();
        if (instancePlots.size() * room.getCost() > gameState.getPlayer(player.getPlayerId()).getGold()) {
            return;
        }
        substractGoldFromPlayer(cost, player.getPlayerId());

        // Build
        for (Point p : instancePlots) {
            TileData tile = getMapData().getTile(p);
            tile.setPlayerId(player.getPlayerId());
            tile.setTerrainId(room.getTerrainId());
        }

        // See if we hit any of the adjacent rooms
        Set<RoomInstance> adjacentInstances = new LinkedHashSet<>();
        for (Point p : buildPlots) {
            if (mapLoader.getRoomCoordinates().containsKey(p)) {
                RoomInstance adjacentInstance = mapLoader.getRoomCoordinates().get(p);
                if (adjacentInstance.getRoom().equals(room) && !adjacentInstances.contains(adjacentInstance)) {

                    // Same room, see that we own it
                    TileData tile = getMapData().getTile(p.x, p.y);
                    if (tile.getPlayerId() == player.getPlayerId()) {

                        // Bingo!
                        adjacentInstances.add(adjacentInstance);
                    }
                }
            }
        }

        // If any hits, merge to the first one, and update whole room
        if (!adjacentInstances.isEmpty()) {

            // Add the mergeable rooms to updatable tiles as well
            RoomInstance firstInstance = null;
            for (RoomInstance instance : adjacentInstances) {

                // Merge to the first found room instance
                if (firstInstance == null) {
                    firstInstance = instance;
                    substractGoldCapacityFromPlayer(firstInstance); // Important to update the gold here
                    firstInstance.addCoordinates(instancePlots);
                    for (Point p : instancePlots) {
                        mapLoader.getRoomCoordinates().put(p, firstInstance);
                    }
                } else {
                    removeRoomInstances(instance);
                }

                for (Point p : instance.getCoordinates()) {
                    updatableTiles.addAll(Arrays.asList(mapLoader.getSurroundingTiles(p, true)));
                    if (!firstInstance.equals(instance)) {
                        firstInstance.addCoordinate(p);
                        mapLoader.getRoomCoordinates().put(p, firstInstance);
                    }
                }
            }
            // TODO: The room health! We need to make sure that the health is distributed evenly
            updateRoom(firstInstance);
        }

        mapLoader.updateTiles(updatableTiles.toArray(new Point[updatableTiles.size()]));

        // New room, calculate gold capacity
        RoomInstance instance = mapLoader.getRoomCoordinates().get(instancePlots.get(0));
        if (adjacentInstances.isEmpty()) {
            addGoldCapacityToPlayer(instance);
            notifyOnBuild(instance.getOwnerId(), mapLoader.getRoomActuals().get(instance));
        }

        // Add any loose gold to the building
        attachLooseGoldToRoom(mapLoader.getRoomActuals().get(instance), instance);
    }

    private void attachLooseGoldToRoom(GenericRoom genericRoom, RoomInstance instance) {
        if (genericRoom.canStoreGold()) {
            synchronized (goldLock) {
                for (ObjectControl objectControl : thingLoader.getObjects()) {
                    if (objectControl instanceof GoldObjectControl && instance.hasCoordinate(objectControl.getTile().getLocation())) {
                        GoldObjectControl gold = (GoldObjectControl) objectControl;
                        int goldLeft = (int) genericRoom.getObjectControl(GenericRoom.ObjectType.GOLD).addItem(gold.getGold(), gold.getTile().getLocation(), thingLoader, null);
                        if (goldLeft == 0) {
                            gold.removeObject();
                        } else {
                            gold.setGold(goldLeft);
                        }
                    }
                }
            }
        }
    }

    /**
     * Is the tile (building) sellable by us
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param player the player, the seller
     */
    public boolean isSellable(int x, int y, Player player) {
        TileData tile = getMapData().getTile(x, y);
        Point p = new Point(x, y);
        if (tile.getPlayerId() == player.getPlayerId() && mapLoader.getRoomCoordinates().containsKey(p)) {

            // We own it, see if sellable
            RoomInstance instance = mapLoader.getRoomCoordinates().get(p);
            return instance.getRoom().getFlags().contains(Room.RoomFlag.BUILDABLE);
        }
        return false;
    }

    /**
     * Sell building(s) from the wanted area
     *
     * @param selectionArea the selection area
     * @param player the player, the new owner
     */
    public void sell(SelectionArea selectionArea, Player player) {
        Set<Point> updatableTiles = new HashSet<>();
        Set<RoomInstance> soldInstances = new HashSet<>();
        List<Point> roomCoordinates = new ArrayList<>();
        for (int x = (int) Math.max(0, selectionArea.getStart().x); x < Math.min(kwdFile.getMap().getWidth(), selectionArea.getEnd().x + 1); x++) {
            for (int y = (int) Math.max(0, selectionArea.getStart().y); y < Math.min(kwdFile.getMap().getHeight(), selectionArea.getEnd().y + 1); y++) {

                // See that is this valid
                if (!isSellable(x, y, player)) {
                    continue;
                }

                // Sell
                Point p = new Point(x, y);
                TileData tile = getMapData().getTile(p);
                if (tile == null) {
                    continue;
                }
                Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
                if (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)) {
                    Room room = kwdFile.getRoomByTerrain(tile.getTerrainId());
                    if (room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_LAND)) {
                        tile.setTerrainId(terrain.getDestroyedTypeTerrainId());
                    } else // Water or lava
                     if (tile.getFlag() == Tile.BridgeTerrainType.LAVA) {
                            tile.setTerrainId(kwdFile.getMap().getLava().getTerrainId());
                        } else {
                            tile.setTerrainId(kwdFile.getMap().getWater().getTerrainId());
                        }

                    // Give money back
                    int goldLeft = addGold(player.getPlayerId(), room.getCost() / 2);
                    if (goldLeft > 0) {

                        // Add loose gold to this tile
                        getThingLoader().addLooseGold(p, player.getPlayerId(), goldLeft);
                    }
                }

                // Get the instance
                soldInstances.add(mapLoader.getRoomCoordinates().get(p));
                updatableTiles.addAll(Arrays.asList(mapLoader.getSurroundingTiles(p, true)));
            }
        }

        // Remove the sold instances (will be regenerated) and add them to updatable
        for (RoomInstance roomInstance : soldInstances) {
            for (Point p : roomInstance.getCoordinates()) {
                updatableTiles.addAll(Arrays.asList(mapLoader.getSurroundingTiles(p, true)));
            }
            roomCoordinates.addAll(roomInstance.getCoordinates());
        }
        removeRoomInstances(soldInstances.toArray(new RoomInstance[soldInstances.size()]));

        mapLoader.updateTiles(updatableTiles.toArray(new Point[updatableTiles.size()]));

        // See if any of the rooms survived
        Set<RoomInstance> newInstances = new HashSet<>();
        for (Point p : roomCoordinates) {
            RoomInstance instance = mapLoader.getRoomCoordinates().get(p);
            if (instance != null && !newInstances.contains(instance)) {
                newInstances.add(instance);
                addGoldCapacityToPlayer(instance);
                attachLooseGoldToRoom(mapLoader.getRoomActuals().get(instance), instance);
            }
        }
    }

    /**
     * Plays a positional sound in an given tile
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param soundFile the sound file
     */
    public void playSoundAtTile(int x, int y, String soundFile) {

        // We might cache these per file? Then they would be persistent, just move them, does it matter?
        // Since creation of new objects and all, I don't know if they stay in the scene graph..
        AudioNode audio = new AudioNode(assetManager,
                AssetUtils.getCanonicalAssetKey(AssetsConverter.SOUNDS_FOLDER + soundFile),
                AudioData.DataType.Buffer);
        audio.setPositional(true);
        audio.setReverbEnabled(false);
        audio.setLocalTranslation(x, 0, y);
        worldNode.attachChild(audio);
        audio.play();
    }

    /**
     * Get a random tile, that is not a starting tile
     *
     * @param start starting coordinates
     * @param radius radius, in tiles
     * @param creature
     * @return a random tile if one is found
     */
    public Point findRandomAccessibleTile(Point start, int radius, CreatureControl creature) {
        List<Point> tiles = new ArrayList<>(radius * radius - 1);
        for (int y = start.y - radius; y <= start.y + radius; y++) {
            for (int x = start.x - radius; x <= start.x + radius; x++) {

                // Skip start tile
                if (x == start.x && y == start.y) {
                    continue;
                }

                TileData tile = getMapData().getTile(x, y);
                if (tile != null && isAccessible(tile, creature)) {
                    tiles.add(new Point(x, y));
                }
            }
        }

        // Take a random point
        if (!tiles.isEmpty()) {
            return Utils.getRandomItem(tiles);
        }
        return null;
    }

    /**
     * FIXME: This can NOT be. Just for quick easy testing.
     *
     * @param start start point
     * @param end end point
     * @param pathFindable the entity to find path for
     * @return output path, null if path not found
     */
    public GraphPath<TileData> findPath(Point start, Point end, PathFindable pathFindable) {
        pathFindingMap.setPathFindable(pathFindable);
        GraphPath<TileData> outPath = new DefaultGraphPath<>();
        TileData startTile = getMapData().getTile(start.x, start.y);
        TileData endTile = getMapData().getTile(end.x, end.y);
        if (startTile != null && endTile != null && pathFinder.searchNodePath(startTile, endTile, heuristic, outPath)) {
            return outPath;
        }
        return null;
    }

    /**
     * Check if given tile is accessible by the given creature
     *
     * @param tile the tile
     * @param pathFindable the entity to test with
     * @return is accessible
     */
    public boolean isAccessible(TileData tile, PathFindable pathFindable) {
        Float cost = pathFindable.getCost(null, tile, this);
        return cost != null;
    }

    /**
     * Debug drawing of path
     *
     * @param linePath
     */
    public void drawPath(LinePath<Vector2> linePath) {
        for (Segment<Vector2> segment : linePath.getSegments()) {

            Line line = new Line(new Vector3f(segment.getBegin().x, 0.25f, segment.getBegin().y), new Vector3f(segment.getEnd().x, 0.25f, segment.getEnd().y));

            Material orange = new Material(getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            orange.setColor("Color", ColorRGBA.Red);
            orange.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
            orange.getAdditionalRenderState().setLineWidth(2);

            Geometry geometry = new Geometry("Bullet", line);
            geometry.setCullHint(Spatial.CullHint.Never);
            geometry.setMaterial(orange);
            getWorld().attachChild(geometry);
        }
    }

    public Node getThingsNode() {
        return thingsNode;
    }

    /**
     * Notify the tile change listeners
     *
     * @param tiles changed tiles
     */
    private void notifyTileChange(Point... tiles) {
        if (tileChangeListener != null) {
            for (Point p : tiles) {
                for (TileChangeListener listener : tileChangeListener) {
                    listener.onTileChange(p.x, p.y);
                }
            }
        }
    }

    /**
     * Get the task manager
     *
     * @return task manager
     */
    public TaskManager getTaskManager() {
        return stateManager.getState(GameState.class).getTaskManager();
    }

    /**
     * Damage a tile
     *
     * @param point the point
     * @param playerId the player applying the damage
     * @return you might get gold out of this
     */
    public int damageTile(Point point, short playerId) {
        TileData tile = getMapData().getTile(point);
        Terrain terrain = tile.getTerrain();

        // Calculate the damage
        int damage = 0;
        int returnedGold = 0;
        if (terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)) {
            if (terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE)) {
                if (tile.getPlayerId() == playerId) {
                    damage = (int) getLevelVariable(Variable.MiscVariable.MiscType.DIG_OWN_WALL_HEALTH);
                } else {
                    damage = (int) getLevelVariable(Variable.MiscVariable.MiscType.DIG_ENEMY_WALL_HEALTH);
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
                damage = (int) getLevelVariable(Variable.MiscVariable.MiscType.DIG_ROCK_HEALTH);
            }
        } else if (terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE) && tile.getPlayerId() != playerId) {

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
                returnedGold = tile.mineGold(damage);
                tileDestroyed = (tile.getGold() < 1);
            }
        } else { // Apply damage
            tileDestroyed = tile.applyDamage(damage);
        }

        // See the results
        if (tileDestroyed) {

            // TODO: effect, drop loot & checks, claimed walls should also get destroyed if all adjacent tiles are not in cotrol anymore
            // The tile is dead
            if (terrain.getDestroyedEffectId() != 0) {
                effectManager.load(worldNode,
                        WorldUtils.pointToVector3f(point).addLocal(0, MapLoader.FLOOR_HEIGHT, 0),
                        terrain.getDestroyedEffectId(), false);
            }
            tile.setTerrainId(terrain.getDestroyedTypeTerrainId());

            updateRoomWalls(tile);
            mapLoader.updateTiles(mapLoader.getSurroundingTiles(tile.getLocation(), true));

            // Notify
            notifyTileChange(point);
        } else if (terrain.getFlags().contains(Terrain.TerrainFlag.DECAY)) {
            mapLoader.updateTiles(point);
        }

        return returnedGold;
    }

    /**
     * Heal a tile
     *
     * @param point the point
     * @param playerId the player applying the healing
     */
    public void healTile(Point point, short playerId) {
        TileData tile = getMapData().getTile(point);
        Terrain terrain = tile.getTerrain();

        // See the amount of healing
        // TODO: now just claiming of a tile (claim health variable is too big it seems)
        int healing;
        if (terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)) {

            if (terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE)) {
                if (tile.getPlayerId() == playerId) {
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
        if (tile.applyHealing(healing)) {

            // TODO: effect & checks
            // The tile is upgraded
            if (terrain.getMaxHealthEffectId() != 0) {
                effectManager.load(worldNode,
                        WorldUtils.pointToVector3f(point).addLocal(0, MapLoader.FLOOR_HEIGHT, 0),
                        terrain.getMaxHealthEffectId(), false);
            }
            if (terrain.getMaxHealthTypeTerrainId() != 0) {
                tile.setTerrainId(terrain.getMaxHealthTypeTerrainId());
                tile.setPlayerId(playerId);
                terrain = tile.getTerrain();
                if (tile.isAtFullHealth()) {
                    effectManager.load(worldNode,
                            WorldUtils.pointToVector3f(point).addLocal(0, MapLoader.FLOOR_HEIGHT, 0),
                            terrain.getMaxHealthEffectId(), false);
                }
            }

            updateRoomWalls(tile);
            mapLoader.updateTiles(mapLoader.getSurroundingTiles(tile.getLocation(), true));

            // Notify
            notifyTileChange(point);
        } else if (terrain.getFlags().contains(Terrain.TerrainFlag.DECAY)) {
            mapLoader.updateTiles(point);
        }
    }

    /**
     * Get level variable value
     *
     * @param variable the variable type
     * @return variable value
     */
    public float getLevelVariable(Variable.MiscVariable.MiscType variable) {
        return gameState.getLevelVariable(variable);
    }

    /**
     * Is repairable wall at tile point
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param playerId for the player
     * @return is the wall repairable
     */
    public boolean isRepairableWall(int x, int y, short playerId) {
        TileData tile = getMapData().getTile(x, y);
        Terrain terrain = tile.getTerrain();
        return (!tile.isSelectedByPlayerId(playerId) && tile.getPlayerId() == playerId && terrain.getFlags().contains(Terrain.TerrainFlag.SOLID) && terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE) && !tile.isAtFullHealth());
    }

    /**
     * Is claimable wall at tile point
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param playerId for the player
     * @return is the wall claimable
     */
    public boolean isClaimableWall(int x, int y, short playerId) {
        TileData tile = getMapData().getTile(x, y);
        Terrain terrain = tile.getTerrain();
        return (terrain.getFlags().contains(Terrain.TerrainFlag.SOLID) && isClaimable(x, y, playerId));
    }

    /**
     * Is claimable floor at tile point (not a room)
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param playerId for the player
     * @return is the floor claimable
     */
    public boolean isClaimableTile(int x, int y, short playerId) {
        TileData tile = getMapData().getTile(x, y);
        Terrain terrain = tile.getTerrain();
        return (!terrain.getFlags().contains(Terrain.TerrainFlag.ROOM) && isClaimable(x, y, playerId));
    }

    /**
     * Is claimable room tile at tile point
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param playerId for the player
     * @return is the room claimable
     */
    public boolean isClaimableRoom(int x, int y, short playerId) {
        TileData tile = getMapData().getTile(x, y);
        Terrain terrain = tile.getTerrain();
        return (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM) && isClaimable(x, y, playerId));
    }

    /**
     * Attempt to claim the tile or room, applies either damage of heal,
     * depending whose tile is it
     *
     * @param point tile coordinate
     * @param playerId for the player
     */
    public void applyClaimTile(Point point, short playerId) {
        TileData tile = getMapData().getTile(point);
        Terrain terrain = tile.getTerrain();
        if (terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE) && tile.getPlayerId() != playerId) {
            if (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)) {
                damageRoom(point, playerId);
            } else {
                damageTile(point, playerId);
            }
        } else {
            // TODO: Room healing
            healTile(point, playerId);
        }
    }

    /**
     * Damage a room
     *
     * @param point tile coordinate
     * @param playerId for the player
     */
    private void damageRoom(Point point, short playerId) {
        TileData tile = getMapData().getTile(point);

        // Calculate the damage
        int damage;
        short owner = tile.getPlayerId();
        if (owner == Player.NEUTRAL_PLAYER_ID) {
            damage = (int) getLevelVariable(Variable.MiscVariable.MiscType.CONVERT_ROOM_HEALTH);
        } else {
            damage = (int) getLevelVariable(Variable.MiscVariable.MiscType.ATTACK_ROOM_HEALTH);
        }

        // Get the room
        RoomInstance room = getMapLoader().getRoomCoordinates().get(point);
        List<Point> roomTiles = room.getCoordinates();

        // Apply the damage equally to all tiles so that the overall condition can be checked easily
        // I don't know if this model is correct or not, but like this the bigger the room the more effort it requires to claim
        int damagePerTile = Math.abs(damage / roomTiles.size());
        for (Point p : roomTiles) {
            TileData roomTile = getMapData().getTile(p);
            if (roomTile.applyDamage(damagePerTile)) {

                // If one of the tiles runs out (everyone should run out of the same time, unless a new tile has recently being added..)
                for (Point p2 : roomTiles) {
                    roomTile = getMapData().getTile(p2);
                    roomTile.setPlayerId(playerId); // Claimed!
                    roomTile.applyHealing(tile.getTerrain().getMaxHealth());

                    effectManager.load(worldNode,
                            WorldUtils.pointToVector3f(point).addLocal(0, MapLoader.FLOOR_HEIGHT, 0),
                            tile.getTerrain().getMaxHealthEffectId(), false);

                    // FIXME ROOM_CLAIM_ID is realy claim effect?
                    effectManager.load(worldNode,
                            WorldUtils.pointToVector3f(p2).addLocal(0, MapLoader.FLOOR_HEIGHT, 0),
                            room.getRoom().getEffects().get(EffectManagerState.ROOM_CLAIM_ID), false);

                    // TODO: Claimed room wall tiles lose the claiming I think?
                    notifyTileChange(p2);
                }

                // Notify
                GenericRoom genericRoom = mapLoader.getRoomActuals().get(room);
                notifyOnCapturedByEnemy(owner, genericRoom);
                notifyOnCaptured(playerId, genericRoom);

                break;
            }
        }
    }

    /**
     * Add a lump sum of gold to a player, distributes the gold to the available
     * rooms
     *
     * @param playerId for the player
     * @param sum the gold sum
     * @return returns a sum of gold that could not be added to player's gold
     */
    public int addGold(short playerId, int sum) {
        return addGold(playerId, null, sum);
    }

    /**
     * Add a lump sum of gold to a player, distributes the gold to the available
     * rooms
     *
     * @param playerId for the player
     * @param p a point where to drop the gold, can be {@code  null}
     * @param sum the gold sum
     * @return returns a sum of gold that could not be added to player's gold
     */
    public int addGold(short playerId, Point p, int sum) {

        // Gold to specified point/room
        if (p != null) {

            // Get a room in point
            RoomInstance roomInstance = getMapLoader().getRoomCoordinates().get(p);
            if (roomInstance != null) {
                GenericRoom room = getMapLoader().getRoomActuals().get(roomInstance);
                if (room.canStoreGold()) {
                    RoomGoldControl control = room.getObjectControl(GenericRoom.ObjectType.GOLD);
                    sum = control.addItem(sum, p, thingLoader, null);
                }
            }
        } else {

            // Distribute the gold
            for (Entry<RoomInstance, GenericRoom> roomEntry : getMapLoader().getRoomActuals().entrySet()) {
                if (roomEntry.getKey().getOwnerId() == playerId && roomEntry.getValue().canStoreGold()) {
                    RoomGoldControl control = roomEntry.getValue().getObjectControl(GenericRoom.ObjectType.GOLD);
                    sum = control.addItem(sum, p, thingLoader, null);
                    if (sum == 0) {
                        break;
                    }
                }
            }
        }

        return sum;
    }

    private void removeRoomInstances(RoomInstance... instances) {
        for (RoomInstance instance : instances) {
            notifyOnSold(instance.getOwnerId(), mapLoader.getRoomActuals().get(instance));
        }
        mapLoader.removeRoomInstances(instances);
    }

    private void updateRoom(RoomInstance instance) {
        addGoldCapacityToPlayer(instance);

        // TODO: the loose gold should be stored
        // Not only gold but all items that the rooms can hold
        mapLoader.updateRoom(instance);
    }

    private void substractGoldCapacityFromPlayer(RoomInstance instance) {
        GenericRoom room = mapLoader.getRoomActuals().get(instance);
        if (room.canStoreGold()) {
            Keeper keeper = gameState.getPlayer(instance.getOwnerId());
            RoomGoldControl control = room.getObjectControl(GenericRoom.ObjectType.GOLD);
//            keeper.getGoldControl().setGoldMax(keeper.getGoldControl().getGoldMax() - control.getMaxCapacity());
        }
    }

    private void addGoldCapacityToPlayer(RoomInstance instance) {
        GenericRoom room = mapLoader.getRoomActuals().get(instance);
        if (room.canStoreGold()) {
            Keeper keeper = gameState.getPlayer(instance.getOwnerId());
            RoomGoldControl control = room.getObjectControl(GenericRoom.ObjectType.GOLD);
//            keeper.getGoldControl().setGoldMax(keeper.getGoldControl().getGoldMax() + control.getMaxCapacity());
        }
    }

    public GameState getGameState() {
        return gameState;
    }

    /**
     * If you want to get notified about the room changes
     *
     * @param playerId the player id of which room you want to assign the
     * listener to
     * @param listener the listener
     */
    public void addListener(short playerId, RoomListener listener) {
        if (roomListeners == null) {
            roomListeners = new HashMap<>();
        }
        List<RoomListener> listeners = roomListeners.get(playerId);
        if (listeners == null) {
            listeners = new ArrayList<>();
        }
        listeners.add(listener);
        roomListeners.put(playerId, listeners);
    }

    private void notifyOnBuild(short playerId, GenericRoom room) {
        if (roomListeners != null && roomListeners.containsKey(playerId)) {
            for (RoomListener listener : roomListeners.get(playerId)) {
                listener.onBuild(room);
            }
        }
    }

    private void notifyOnCaptured(short playerId, GenericRoom room) {
        if (roomListeners != null && roomListeners.containsKey(playerId)) {
            for (RoomListener listener : roomListeners.get(playerId)) {
                listener.onCaptured(room);
            }
        }
    }

    private void notifyOnCapturedByEnemy(short playerId, GenericRoom room) {
        if (roomListeners != null && roomListeners.containsKey(playerId)) {
            for (RoomListener listener : roomListeners.get(playerId)) {
                listener.onCapturedByEnemy(room);
            }
        }
    }

    private void notifyOnSold(short playerId, GenericRoom room) {
        if (roomListeners != null && roomListeners.containsKey(playerId)) {
            for (RoomListener listener : roomListeners.get(playerId)) {
                listener.onSold(room);
            }
        }
    }

    public EffectManagerState getEffectManager() {
        return effectManager;
    }

    /**
     * Drop object to the world
     *
     * @param object the object to drop
     * @param tile the tile to drop on
     * @param coordinates coordinates inside the tile
     * @param control control that this object was dropped on
     */
    public void dropObject(ObjectControl object, TileData tile, Vector2f coordinates, IInteractiveControl control) {
        if (object instanceof GoldObjectControl) {
            dropGold((GoldObjectControl) object, tile, coordinates, control);
        } else {
            throw new UnsupportedOperationException("Dropping " + object.getClass() + " not supported yet.");
        }
    }

    /**
     * Drop a gold object to tile
     *
     * @param gold the gold to drop
     * @param tile the tile to drop on
     * @param coordinates coordinates inside the tile
     * @param control control that this gold was dropped on
     */
    private void dropGold(GoldObjectControl gold, TileData tile, Vector2f coordinates, IInteractiveControl control) {

        // In the original game:
        // Floor: If the drop point is quite accurately on top of another pile of gold -> fuse the gold together. Otherwise create another pile, even to the same tile.
        // Room: Add to the room, but any excess gold IS added to the floor tile of the room as loose gold. This loose gold is then automatically transfered to the room when there is room with a small delay.
        // Merge to another loose gold
        if (control != null && control instanceof GoldObjectControl && ((GoldObjectControl) control).getState() == ObjectControl.ObjectState.NORMAL) {

            // Merge
            GoldObjectControl goc = (GoldObjectControl) control;
            goc.setGold(goc.getGold() + gold.getGold());
            return;
        }

        // Add to room
        int goldLeft = addGold(gold.getOwnerId(), tile.getLocation(), gold.getGold());
        if (goldLeft > 0) {

            // Create a gold pile
            thingLoader.addLooseGold(tile.getLocation(), gold.getOwnerId(), goldLeft);
        }
    }

    /**
     * Substract gold from player
     *
     * @param amount the amount to try to substract
     * @param playerId the player id
     * @return amount of money that could not be substracted from the player
     */
    public int substractGoldFromPlayer(int amount, short playerId) {

        // See if the player has any gold even
        Keeper keeper = gameState.getPlayer(playerId);
        if (keeper.getGold() == 0) {
            return amount;
        }

        // The gold is subtracted evenly from all treasuries
        List<GenericRoom> playersTreasuries = getMapLoader().getRoomsByFunction(GenericRoom.ObjectType.GOLD, playerId);
        while (amount > 0 && !playersTreasuries.isEmpty()) {
            Iterator<GenericRoom> iter = playersTreasuries.iterator();
            int goldToRemove = (int) Math.ceil((float) amount / playersTreasuries.size());
            while (iter.hasNext()) {
                GenericRoom room = iter.next();
                RoomGoldControl control = room.getObjectControl(GenericRoom.ObjectType.GOLD);
                goldToRemove = Math.min(amount, goldToRemove); // Rounding...
                amount -= goldToRemove - control.removeGold(goldToRemove);
                if (control.getCurrentCapacity() == 0) {
                    iter.remove();
                }
                if (amount == 0) {
                    break;
                }
            }
        }

        return amount;
    }

    /**
     * Tests if there is a room in the given location
     *
     * @param p the point to test
     * @return returns the room, or {@code null} if no room is found
     */
    public GenericRoom getRoomAtPoint(Point p) {
        RoomInstance roomInstance = mapLoader.getRoomCoordinates().get(p);
        if (roomInstance != null) {
            return mapLoader.getRoomActuals().get(roomInstance);
        }
        return null;
    }

}
