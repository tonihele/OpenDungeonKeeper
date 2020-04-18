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
package toniarts.openkeeper.game.task;

import com.badlogic.gdx.ai.pfa.GraphPath;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.filter.FieldFilter;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.Food;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.listener.MapListener;
import toniarts.openkeeper.game.listener.PlayerActionListener;
import toniarts.openkeeper.game.listener.RoomListener;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.logic.IGameLogicUpdatable;
import toniarts.openkeeper.game.map.MapData;
import toniarts.openkeeper.game.map.MapTile;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.creature.ClaimLair;
import toniarts.openkeeper.game.task.creature.GoToEat;
import toniarts.openkeeper.game.task.creature.GoToSleep;
import toniarts.openkeeper.game.task.creature.Research;
import toniarts.openkeeper.game.task.objective.AbstractObjectiveTask;
import toniarts.openkeeper.game.task.objective.KillPlayer;
import toniarts.openkeeper.game.task.objective.SendToActionPoint;
import toniarts.openkeeper.game.task.worker.CaptureEnemyCreatureTask;
import toniarts.openkeeper.game.task.worker.CarryEnemyCreatureToPrison;
import toniarts.openkeeper.game.task.worker.CarryGoldToTreasuryTask;
import toniarts.openkeeper.game.task.worker.ClaimRoomTask;
import toniarts.openkeeper.game.task.worker.ClaimTileTask;
import toniarts.openkeeper.game.task.worker.ClaimWallTileTask;
import toniarts.openkeeper.game.task.worker.DigTileTask;
import toniarts.openkeeper.game.task.worker.FetchObjectTask;
import toniarts.openkeeper.game.task.worker.RepairWallTileTask;
import toniarts.openkeeper.game.task.worker.RescueCreatureTask;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.world.object.ObjectControl;

/**
 * Task manager for several players. Can assign creatures to different tasks.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class TaskManager implements ITaskManager, IGameLogicUpdatable {

    private final IMapController mapController;
    private final IGameWorldController gameWorldController;
    private final ICreaturesController creaturesController;
    private final INavigationService navigationService;
    private final ILevelInfo levelInfo;
    private final IEntityPositionLookup entityPositionLookup;
    private final EntityData entityData;
    private final EntitySet taskEntities;
    private final EntitySet unconsciousEntities;
    private final EntitySet corpseEntities;
    private final Map<Short, Set<Task>> taskQueues;
    private final Map<Long, Task> tasksByIds = new HashMap<>();
    private final Map<EntityId, Long> tasksIdsByEntities = new HashMap<>();
    private final Map<Short, IPlayerController> playerControllers;
    private final Map<IRoomController, Map<Point, AbstractCapacityCriticalRoomTask>> roomTasks = new HashMap<>();

    private static final Logger LOGGER = Logger.getLogger(TaskManager.class.getName());

    public TaskManager(EntityData entityData, IGameWorldController gameWorldController, IMapController mapController, ICreaturesController creaturesController, INavigationService navigationService,
            Collection<IPlayerController> players, ILevelInfo levelInfo, IEntityPositionLookup entityPositionLookup) {
        this.entityData = entityData;
        this.mapController = mapController;
        this.gameWorldController = gameWorldController;
        this.creaturesController = creaturesController;
        this.navigationService = navigationService;
        this.levelInfo = levelInfo;
        this.entityPositionLookup = entityPositionLookup;

        // Set the players
        // Create a queue for each managed player (everybody except Good & Neutral)
        taskQueues = new HashMap<>(players.size());
        playerControllers = new HashMap<>();
        for (IPlayerController playerController : players) {
            Keeper keeper = playerController.getKeeper();

            playerControllers.put(keeper.getId(), playerController);

            if (keeper.getId() != Player.GOOD_PLAYER_ID && keeper.getId() != Player.NEUTRAL_PLAYER_ID) {
                taskQueues.put(keeper.getId(), new HashSet<>());
            }
        }

        // Scan the initial tasks
        scanInitialTasks();

        // Add task listeners
        addListeners(players);

        // Listen to entities having a task, for cleanup purposes
        taskEntities = entityData.getEntities(TaskComponent.class);
        processAddedTasks(taskEntities);

        // Listen to rescue/capture missions
        unconsciousEntities = entityData.getEntities(new FieldFilter(Health.class, "unconscious", true), CreatureComponent.class, Health.class, Owner.class);
        processAddedUnconsciousEntities(unconsciousEntities);

        // Listen to corpse robbing missions
        corpseEntities = entityData.getEntities(CreatureComponent.class, Death.class);
        processAddedCorpseEntities(corpseEntities);
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        taskEntities.release();
        unconsciousEntities.release();
        corpseEntities.release();
    }

    @Override
    public void processTick(float tpf, double gameTime) {
        if (taskEntities.applyChanges()) {
            processDeletedTasks(taskEntities.getRemovedEntities());
            processAddedTasks(taskEntities.getAddedEntities());
            processChangedTasks(taskEntities.getChangedEntities());
        }
        if (unconsciousEntities.applyChanges()) {
            processDeletedUnconsciousEntities(unconsciousEntities.getRemovedEntities());
            processAddedUnconsciousEntities(unconsciousEntities.getAddedEntities());
        }
        if (corpseEntities.applyChanges()) {
            processDeletedCorpseEntities(corpseEntities.getRemovedEntities());
            processAddedCorpseEntities(corpseEntities.getAddedEntities());
        }
    }

    private void processAddedTasks(Set<Entity> entities) {
        for (Entity entity : entities) {
            long taskId = entity.get(TaskComponent.class).taskId;
            tasksIdsByEntities.put(entity.getId(), taskId);
        }
    }

    private void processDeletedTasks(Set<Entity> entities) {
        for (Entity entity : entities) {
            Long taskId = tasksIdsByEntities.remove(entity.getId());
            Task task = tasksByIds.get(taskId);
            if (task != null) {
                task.unassign(creaturesController.createController(entity.getId()));
                if (task.getAssigneeCount() == 0 && task.isRemovable()) {
                    //tasksByIds.remove(taskId);
                }
            }
        }
    }

    private void processChangedTasks(Set<Entity> entities) {
        for (Entity entity : entities) {
            long taskId = entity.get(TaskComponent.class).taskId;
            Long oldTaskId = tasksIdsByEntities.put(entity.getId(), taskId);
            Task task = tasksByIds.get(oldTaskId);
            if (task != null) {
                task.unassign(creaturesController.createController(entity.getId()));
                if (task.getAssigneeCount() == 0 && task.isRemovable()) {
                    //tasksByIds.remove(taskId);
                }
            }
        }
    }

    private void processAddedUnconsciousEntities(Set<Entity> entities) {

        // Add rescue mission for the own troops and capture for the enemy
        for (Entity entity : entities) {
            Owner owner = entity.get(Owner.class);
            for (Entry<Short, Set<Task>> entry : taskQueues.entrySet()) {

                Task task;
                if (entry.getKey() == owner.ownerId) {

                    // Rescue
                    task = new RescueCreatureTask(this, navigationService, mapController, creaturesController.createController(entity.getId()), entry.getKey());
                } else {

                    // Capture
                    task = new CaptureEnemyCreatureTask(navigationService, mapController, creaturesController.createController(entity.getId()), entry.getKey(), this);
                }
                entry.getValue().add(task);
                tasksByIds.put(task.getId(), task);
            }
        }
    }

    private void processDeletedUnconsciousEntities(Set<Entity> entities) {
        // TODO:
    }

    private void processAddedCorpseEntities(Set<Entity> entities) {
        // TODO:
    }

    private void processDeletedCorpseEntities(Set<Entity> entities) {
        // TODO:
    }

    private void addListeners(Collection<IPlayerController> players) {

        // We want to be notified on tile changes, we are event based, not constantly scanning type
        this.mapController.addListener(new MapListener() {

            @Override
            public void onTilesChange(List<MapTile> updatedTiles) {
                for (MapTile tile : updatedTiles) {
                    scanTerrainTasks(tile, true, true);
                }
            }

            @Override
            public void onTileFlash(List<Point> points, boolean enabled, short keeperId) {
                // Not interested
            }
        });

        // Bridges! They open up new opportunities in new lands
        // Here we got capturing
        for (IPlayerController player : players) {
            this.mapController.addListener(player.getKeeper().getId(), new RoomListener() {

                @Override
                public void onBuild(IRoomController room) {

                }

                @Override
                public void onCaptured(IRoomController room) {
                    scanBridgeSurroundings(room);
                }

                @Override
                public void onCapturedByEnemy(IRoomController room) {

                }

                @Override
                public void onSold(IRoomController room) {

                }

                private void scanBridgeSurroundings(IRoomController room) {
                    if (room.getRoom().getFlags().contains(Room.RoomFlag.PLACEABLE_ON_LAVA) || room.getRoom().getFlags().contains(Room.RoomFlag.PLACEABLE_ON_WATER)) {

                        // Just gather the adjacent tiles to the brigde
                        Set<Point> roomPoints = new HashSet<>(room.getRoomInstance().getCoordinates());
                        Set<Point> adjacentPoints = new HashSet<>();
                        for (Point p : roomPoints) {
                            for (Point adjacentPoint : WorldUtils.getSurroundingTiles(mapController.getMapData(), p, false)) {
                                if (!roomPoints.contains(adjacentPoint)) {
                                    adjacentPoints.add(adjacentPoint);
                                }
                            }

                            // Scan all the adjacent tiles for tasks
                            for (Point adjacentPoint : adjacentPoints) {
                                MapTile mapTile = mapController.getMapData().getTile(adjacentPoint);
                                if (mapTile != null) {
                                    scanTerrainTasks(mapTile, false, true);
                                }
                            }
                        }
                    }
                }
            });
        }

        // Bridges! They open up new opportunities in new lands
        this.gameWorldController.addListener(new PlayerActionListener() {

            @Override
            public void onBuild(short keeperId, List<MapTile> tiles) {
                scanBridgeSurroundings(tiles);
            }

            @Override
            public void onSold(short keeperId, List<MapTile> tiles) {
                scanBridgeSurroundings(tiles);
            }

            private void scanBridgeSurroundings(List<MapTile> tiles) {

                // Check first to see if we are on land or water
                Terrain terrain = mapController.getTerrain(tiles.get(0));
                if (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)) {
                    Room room = levelInfo.getLevelData().getRoomByTerrain(terrain.getTerrainId());
                    if (!(room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_LAVA) || room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_WATER))) {
                        return;
                    }
                } else if (!(levelInfo.getLevelData().getMap().getLava().equals(terrain) || levelInfo.getLevelData().getMap().getWater().equals(terrain))) {
                    return;
                }

                // Just gather the adjacent tiles to the brigde
                Set<MapTile> roomPoints = new HashSet<>(tiles);
                Set<MapTile> adjacentPoints = new HashSet<>();
                for (MapTile tile : roomPoints) {
                    for (Point adjacentPoint : WorldUtils.getSurroundingTiles(mapController.getMapData(), tile.getLocation(), false)) {
                        MapTile mapTile = mapController.getMapData().getTile(adjacentPoint);
                        if (mapTile != null) {
                            if (!roomPoints.contains(mapTile)) {
                                adjacentPoints.add(mapTile);
                            }
                        }
                    }

                    // Scan all the adjacent tiles for tasks
                    for (MapTile mapTile : adjacentPoints) {
                        scanTerrainTasks(mapTile, false, true);
                    }
                }
            }

        });

        // Get notified by object tasks
        // TODO: pick uppable objects entityset listener
//        this.worldState.getThingLoader().addListener(new ObjectListener() {
//
//            @Override
//            public void onAdded(ObjectControl objectControl) {
//                for (Entry<Short, Set<AbstractTask>> entry : taskQueues.entrySet()) {
//                    entry.getValue().add(getObjectTask(objectControl, entry.getKey()));
//                }
//            }
//
//            @Override
//            public void onRemoved(ObjectControl objectControl) {
//                for (Entry<Short, Set<AbstractTask>> entry : taskQueues.entrySet()) {
//                    entry.getValue().remove(getObjectTask(objectControl, entry.getKey()));
//                }
//            }
//        });
    }

    private void scanInitialTasks() {
        for (MapTile tile : mapController.getMapData()) {
            scanTerrainTasks(tile, false, false);
        }
        // Object tasks
//        for (ObjectControl objectControl : worldState.getThingLoader().getObjects()) {
//            for (Entry<Short, Set<AbstractTask>> entry : taskQueues.entrySet()) {
//                entry.getValue().add(getObjectTask(objectControl, entry.getKey()));
//            }
//        }
    }

    private void scanTerrainTasks(final MapTile tile, final boolean checkNeighbours, final boolean deleteObsolete) {
        for (Entry<Short, Set<Task>> entry : taskQueues.entrySet()) {

            // Scan existing tasks that are they valid, should be only one tile task per tile?
            if (deleteObsolete) {
                Iterator<Task> iter = entry.getValue().iterator();
                while (iter.hasNext()) {
                    Task task = iter.next();
                    if (task instanceof AbstractTileTask) {
                        AbstractTileTask tileTask = (AbstractTileTask) task;
                        if (tileTask.isRemovable()) {
                            iter.remove();
                            if (tileTask.getAssigneeCount() == 0) {
                                //tasksByIds.remove(task.getId());
                            }
                        }
                    }
                }
            }

            // Perhaps we should have a store for these, since only one of such per player can exist, would save IDs
            // Dig
            if (mapController.isSelected(tile.getLocation(), entry.getKey())) {
                Task task = new DigTileTask(navigationService, mapController, tile.getLocation(), entry.getKey());
                addTask(entry.getKey(), task);
            } // Claim wall
            else if (mapController.isClaimableWall(tile.getLocation(), entry.getKey())) {
                Task task = new ClaimWallTileTask(navigationService, mapController, tile.getLocation(), entry.getKey());
                addTask(entry.getKey(), task);
            } // Claim
            else if (mapController.isClaimableTile(tile.getLocation(), entry.getKey())) {
                Task task = new ClaimTileTask(navigationService, mapController, tile.getLocation(), entry.getKey());
                addTask(entry.getKey(), task);
            } // Repair wall
            else if (mapController.isRepairableWall(tile.getLocation(), entry.getKey())) {
                Task task = new RepairWallTileTask(navigationService, mapController, tile.getLocation(), entry.getKey());
                addTask(entry.getKey(), task);
            } // Claim room
            else if (mapController.isClaimableRoom(tile.getLocation(), entry.getKey())) {
                Task task = new ClaimRoomTask(navigationService, mapController, tile.getLocation(), entry.getKey());
                addTask(entry.getKey(), task);
            }
        }

        // See the neighbours
        if (checkNeighbours) {
            for (Point p : WorldUtils.getSurroundingTiles(mapController.getMapData(), tile.getLocation(), false)) {
                scanTerrainTasks(mapController.getMapData().getTile(p), false, false);
            }
        }
    }

    @Override
    public boolean assignTask(ICreatureController creature, boolean byDistance) {

        Set<Task> taskQueue = taskQueues.get(creature.getOwnerId());
        if (taskQueue == null) {
            return false;
//            throw new IllegalArgumentException("This task manager instance is not for the given player!");
        }

        // Sort by distance & priority
        final Point currentLocation = creature.getCreatureCoordinates();
        List<Task> prioritisedTaskQueue = new ArrayList<>(taskQueue);
        Collections.sort(prioritisedTaskQueue, (Task t, Task t1) -> {
            int result = Integer.compare(t.getAssigneeCount(), t1.getAssigneeCount());
            if (result == 0) {
                result = Integer.compare(
                        WorldUtils.calculateDistance(currentLocation, t.getTaskLocation()) + t.getPriority(),
                        WorldUtils.calculateDistance(currentLocation, t1.getTaskLocation()) + t1.getPriority()
                );

                if (result == 0) {
                    // If the same, compare by date added
                    return t.getTaskCreated().compareTo(t1.getTaskCreated());
                }
            }
            return result;
        });

        // Take the first available task from the sorted queue
        for (Task task : prioritisedTaskQueue) {
            if (task.canAssign(creature)) {

                // Assign to first task
                task.assign(creature, true);
                return true;
            }
        }

        return false;
    }

    public void addTask(short playerId, Task task) {
        Set<Task> tasks = taskQueues.get(playerId);
        if (!tasks.contains(task)) {
            tasks.add(task);
            tasksByIds.put(task.getId(), task);
            LOGGER.log(Level.INFO, "Added task {0} for player {1}!", new Object[]{task, playerId});
        } else {
            LOGGER.log(Level.WARNING, "Already a task {0} for player {1}!", new Object[]{task, playerId});
        }
    }

    @Override
    public boolean assignGoldToTreasuryTask(ICreatureController creature) {

        // See if the creature's player lacks of gold
        IPlayerController player = playerControllers.get(creature.getOwnerId());
        if (!player.getGoldControl().isFullCapacity()) {
            return assignClosestRoomTask(creature, ObjectType.GOLD, null);
        }
        return false;
    }

    @Override
    public boolean assignClosestRoomTask(ICreatureController creature, ObjectType objectType, EntityId target) {
        return assignClosestRoomTask(creature, objectType, target, true);
    }

    /**
     * Assigns closest room task to a given creature of requested type or check
     * for validity
     *
     * @param creature the creature to assign to
     * @param objectType the type of room service
     * @param assign whether to actually assign the creature to the task or just
     * test
     * @return true if the task was assigned
     */
    private boolean assignClosestRoomTask(ICreatureController creature, ObjectType objectType, EntityId targetEntity, boolean assign) {
        Point currentPosition = creature.getCreatureCoordinates();

        // Get all the rooms of the given type
        List<IRoomController> rooms = mapController.getRoomsByFunction(objectType, creature.getOwnerId());
        Map<Integer, IRoomController> distancesToRooms = new TreeMap<>();
        for (IRoomController room : rooms) {
            if (!room.isFullCapacity()) {
                distancesToRooms.put(getShortestDistance(currentPosition, room.getRoomInstance().getCoordinates().toArray(new Point[room.getRoomInstance().getCoordinates().size()])), room
                );
            }
        }

        // See that are they really accessible starting from the least distance one
        for (IRoomController room : distancesToRooms.values()) {

            // FIXME: if we are to have more capacity than one per tile, we need to refactor
            // The whole rooms are always accessible, take a random point from the room like DK II seems to do
            List<Point> coordinates = new ArrayList<>(room.getObjectControl(objectType).getAvailableCoordinates());
            Iterator<Point> iter = coordinates.iterator();
            Map<Point, AbstractCapacityCriticalRoomTask> taskPoints = roomTasks.get(room);
            while (iter.hasNext()) {
                Point p = iter.next();
                if (!room.isTileAccessible(null, p) || (taskPoints != null && taskPoints.containsKey(p))) {
                    iter.remove();
                }
            }

            // Assign
            if (!coordinates.isEmpty()) {
                Point target = Utils.getRandomItem(coordinates);
                GraphPath<MapTile> path = navigationService.findPath(creature.getCreatureCoordinates(), target, creature);
                if (path != null || target == creature.getCreatureCoordinates()) {

                    // Assign the task
                    Task task = getRoomTask(objectType, target, targetEntity, creature, room);

                    // See if really assign
                    if (!assign) {
                        return task.isValid(creature);
                    }

                    if (task instanceof AbstractCapacityCriticalRoomTask) {
                        if (taskPoints == null) {
                            taskPoints = new HashMap<>();
                        }
                        taskPoints.put(target, (AbstractCapacityCriticalRoomTask) task);
                        roomTasks.put(room, taskPoints);
                    }
                    task.assign(creature, true);
                    tasksByIds.put(task.getId(), task);
                    return true;
                }
            }
        }

        return false;
    }

    private static Integer getShortestDistance(Point currentPosition, Point... coordinates) {
        int distance = Integer.MAX_VALUE;
        for (Point p : coordinates) {
            // TODO: do we need to do this diagonally?
            distance = Math.min(distance, WorldUtils.calculateDistance(currentPosition, p));
            if (distance == 0) {
                break;
            }
        }
        return distance;
    }

    private AbstractTask getRoomTask(ObjectType objectType, Point target, EntityId targetEntity, ICreatureController creature, IRoomController room) {
        switch (objectType) {
            case GOLD: {
                return new CarryGoldToTreasuryTask(navigationService, mapController, target, creature.getOwnerId(), room, gameWorldController);
            }
            case LAIR: {
                return new ClaimLair(navigationService, mapController, target, creature.getOwnerId(), room, this);
            }
            case RESEARCHER: {
                return new Research(navigationService, mapController, target, creature.getOwnerId(), room, this, playerControllers.get(creature.getOwnerId()).getResearchControl());
            }
            case PRISONER: {
                return new CarryEnemyCreatureToPrison(navigationService, mapController, target, creature.getOwnerId(), room, this, creaturesController.createController(targetEntity));
            }
        }
        return null;
    }

    protected void removeRoomTask(AbstractCapacityCriticalRoomTask task) {
        Map<Point, AbstractCapacityCriticalRoomTask> taskPoints = roomTasks.get(task.getRoom());
        if (taskPoints != null) {
            taskPoints.remove(task.getTaskLocation());
            if (taskPoints.isEmpty()) {
                roomTasks.remove(task.getRoom());
                if (task.getAssigneeCount() == 0) {
                    //tasksByIds.remove(task.getId(), task);
                }
            }
        }
    }

    @Override
    public boolean assignObjectiveTask(ICreatureController creature, Thing.HeroParty.Objective objective) {
        AbstractObjectiveTask task = null;
        switch (objective) {
            case SEND_TO_ACTION_POINT: {
                task = new SendToActionPoint(navigationService, mapController, levelInfo.getActionPoint(creature.getObjectiveTargetActionPointId()), creature.getOwnerId());
                break;
            }
            case KILL_PLAYER: {
                task = new KillPlayer(navigationService, mapController, levelInfo, creature.getObjectiveTargetPlayerId(), creature);
                break;
            }
        }

        // Assign
        if (task != null && task.getCurrentTask() != null) {
            task.getCurrentTask().assign(creature, true);
            tasksByIds.put(task.getId(), task);
            return true;
        }
        return false;
    }

    private AbstractTask getObjectTask(ObjectControl objectControl, short playerId) {
        return new FetchObjectTask(navigationService, mapController, objectControl, playerId);
    }

    @Override
    public boolean isTaskAvailable(ICreatureController creature, Creature.JobType jobType) {
        return assignTask(creature, jobType, false);
    }

    @Override
    public boolean assignTask(ICreatureController creature, Creature.JobType jobType) {
        return assignTask(creature, jobType, true);
    }

    private boolean assignTask(ICreatureController creature, Creature.JobType jobType, boolean assign) {
        switch (jobType) {
            case RESEARCH: {
                return assignClosestRoomTask(creature, ObjectType.RESEARCHER, null, assign);
            }
            default:
                return false;
        }
    }

    @Override
    public boolean assignSleepTask(ICreatureController creature) {
        GoToSleep task = new GoToSleep(navigationService, mapController, creature);
        if (task.isReachable(creature)) {
            task.assign(creature, true);
            tasksByIds.put(task.getId(), task);
            return true;
        }
        return false;
    }

    @Override
    public boolean assignEatTask(ICreatureController creature) {

        // The creatures in the original don't seem to make anykind of reservations on the food items, I've seen them "fight over food", first come first serve
        // Hmm, is this a good way to find the food, performance-wise...?
        List<EntityId> foods = new ArrayList<>(entityData.findEntities(new FieldFilter(Owner.class, "ownerId", creature.getOwnerId()), Food.class, Owner.class, Position.class));
        if (foods.isEmpty()) {
            return false; // No food available
        }

        // Sort by manhattan distance in relation to the creature
        Collections.sort(foods, new Comparator<EntityId>() {

            private final Point currentLocation = creature.getCreatureCoordinates();

            @Override
            public int compare(EntityId entityId, EntityId entityId1) {
                int result = Integer.compare(WorldUtils.calculateDistance(currentLocation, entityPositionLookup.getEntityLocation(entityId).getLocation()), WorldUtils.calculateDistance(currentLocation, entityPositionLookup.getEntityLocation(entityId1).getLocation()));
                return result;
            }

        });

        // Pick closest we can actually access
        for (EntityId food : foods) {
            Point target = entityPositionLookup.getEntityLocation(food).getLocation();
            if (target == creature.getCreatureCoordinates() || navigationService.findPath(creature.getCreatureCoordinates(), target, creature) != null) {
                GoToEat task = new GoToEat(navigationService, mapController, entityPositionLookup, food, entityData, creature);
                task.assign(creature, true);
                tasksByIds.put(task.getId(), task);
                return true;
            }
        }

        return false;
    }

    @Override
    public Task getTaskById(long taskId) {
        Task task = tasksByIds.get(taskId);

        // For nested task, return the actual task
        if (task != null && task instanceof AbstractObjectiveTask) {
            return ((AbstractObjectiveTask) task).getCurrentTask();
        }

        return task;
    }

}
