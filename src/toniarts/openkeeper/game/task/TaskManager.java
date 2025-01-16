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
import com.jme3.math.Vector2f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.filter.FieldFilter;
import toniarts.openkeeper.utils.Point;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
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
import java.util.function.Consumer;
import java.util.stream.Collectors;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.Food;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Placeable;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.RoomStorage;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.entity.EntityController;
import toniarts.openkeeper.game.controller.object.IObjectController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.listener.MapListener;
import toniarts.openkeeper.game.listener.PlayerActionListener;
import toniarts.openkeeper.game.listener.RoomListener;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.logic.IGameLogicUpdatable;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.creature.ClaimLair;
import toniarts.openkeeper.game.task.creature.GoToEat;
import toniarts.openkeeper.game.task.creature.GoToSleep;
import toniarts.openkeeper.game.task.creature.Research;
import toniarts.openkeeper.game.task.creature.Train;
import toniarts.openkeeper.game.task.objective.AbstractObjectiveTask;
import toniarts.openkeeper.game.task.objective.KillPlayer;
import toniarts.openkeeper.game.task.objective.SendToActionPoint;
import toniarts.openkeeper.game.task.worker.CaptureEnemyCreatureTask;
import toniarts.openkeeper.game.task.worker.CarryEnemyCreatureToPrison;
import toniarts.openkeeper.game.task.worker.CarryGoldToTreasuryTask;
import toniarts.openkeeper.game.task.worker.CarryObjectToStorageTask;
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
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Task manager for several players. Can assign creatures to different tasks.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class TaskManager implements ITaskManager, IGameLogicUpdatable {
    
    private static final Logger logger = System.getLogger(TaskManager.class.getName());

    private final IMapController mapController;
    private final IGameWorldController gameWorldController;
    private final IObjectsController objectsController;
    private final ICreaturesController creaturesController;
    private final INavigationService navigationService;
    private final ILevelInfo levelInfo;
    private final IEntityPositionLookup entityPositionLookup;
    private final Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings;
    private final EntityData entityData;
    private final EntitySet taskEntities;
    private final EntitySet unconsciousEntities;
    private final EntitySet corpseEntities;
    private final EntitySet freeObjectEntities;
    private final Map<Short, Set<Task>> taskQueues;
    private final Map<Long, Task> tasksByIds = new HashMap<>();
    private final Map<EntityId, Long> tasksIdsByEntities = new HashMap<>();
    private final Map<Short, IPlayerController> playerControllers;
    private final Map<IRoomController, Map<Point, AbstractCapacityCriticalRoomTask>> roomTasks = new HashMap<>();
    private final Map<ICreatureController, Consumer<Boolean>> unemployedCreatures = new HashMap<>();

    public TaskManager(EntityData entityData, IGameWorldController gameWorldController, IMapController mapController,
            IObjectsController objectsController, ICreaturesController creaturesController, INavigationService navigationService,
            Collection<IPlayerController> players, ILevelInfo levelInfo, IEntityPositionLookup entityPositionLookup,
            Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings) {
        this.entityData = entityData;
        this.mapController = mapController;
        this.gameWorldController = gameWorldController;
        this.objectsController = objectsController;
        this.creaturesController = creaturesController;
        this.navigationService = navigationService;
        this.levelInfo = levelInfo;
        this.entityPositionLookup = entityPositionLookup;
        this.gameSettings = gameSettings;

        // Set the players
        // Create a queue for each managed player (everybody except Good & Neutral)
        taskQueues = HashMap.newHashMap(players.size());
        playerControllers = HashMap.newHashMap(players.size());
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
        unconsciousEntities = entityData.getEntities(CreatureComponent.class, Health.class, Owner.class, Unconscious.class);
        processAddedUnconsciousEntities(unconsciousEntities);

        // Listen to corpse robbing missions
        corpseEntities = entityData.getEntities(CreatureComponent.class, Death.class);
        processAddedCorpseEntities(corpseEntities);

        // Listen to object picking up missions
        freeObjectEntities = entityData.getEntities(ObjectComponent.class, Position.class, Placeable.class);
        processAddedFreeObjectEntities(freeObjectEntities);
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        taskEntities.release();
        unconsciousEntities.release();
        corpseEntities.release();
        freeObjectEntities.release();
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
        if (freeObjectEntities.applyChanges()) {
            processDeletedFreeObjectEntities(freeObjectEntities.getRemovedEntities());
            processAddedFreeObjectEntities(freeObjectEntities.getAddedEntities());
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
            if (oldTaskId == null || taskId == oldTaskId) {
                continue;
            }

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

                Task task = null;
                if (entry.getKey() == owner.ownerId) {

                    // Rescue
                    task = new RescueCreatureTask(this, navigationService, mapController, creaturesController.createController(entity.getId()), entry.getKey());
                } else if (playerControllers.get(entry.getKey()).getKeeper().isEnemy(owner.ownerId)) {

                    // Capture
                    task = new CaptureEnemyCreatureTask(navigationService, mapController, creaturesController.createController(entity.getId()), entry.getKey(), this);
                }

                if (task == null) {
                    continue;
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

    private void processAddedFreeObjectEntities(Set<Entity> entities) {

        // Add fetch object missions for changed objects
        for (Entity entity : entities) {
            Point p = WorldUtils.vectorToPoint(entity.get(Position.class).position);
            IMapTileInformation tile = mapController.getMapData().getTile(p);
            short playerId = tile.getOwnerId();
            if (!taskQueues.containsKey(playerId)) {
                continue;
            }
            createFetchObjectTask(entity, playerId);
        }
    }

    private void processDeletedFreeObjectEntities(Set<Entity> entities) {
        // TODO:
    }

    private void addListeners(Collection<IPlayerController> players) {

        // We want to be notified on tile changes, we are event based, not constantly scanning type
        this.mapController.addListener(new MapListener() {

            @Override
            public void onTilesChange(List<Point> updatedTiles) {
                for (Point tile : updatedTiles) {
                    scanTerrainTasks(tile, true, true);
                    scanFetchObjectTasks(tile);
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
                                scanTerrainTasks(adjacentPoint, false, true);
                            }
                        }
                    }
                }
            });
        }

        // Bridges! They open up new opportunities in new lands
        this.gameWorldController.addListener(new PlayerActionListener() {

            @Override
            public void onBuild(short keeperId, List<Point> tiles) {
                scanBridgeSurroundings(tiles);
            }

            @Override
            public void onSold(short keeperId, List<Point> tiles) {
                scanBridgeSurroundings(tiles);
                for (Point tile : tiles) {
                    scanFetchObjectTasks(tile);
                }
            }

            private void scanBridgeSurroundings(List<Point> tiles) {

                // Check first to see if we are on land or water
                Terrain terrain = mapController.getTerrain(mapController.getMapData().getTile(tiles.get(0)));
                if (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)) {
                    Room room = levelInfo.getLevelData().getRoomByTerrain(terrain.getTerrainId());
                    if (!(room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_LAVA) || room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_WATER))) {
                        return;
                    }
                } else if (!(levelInfo.getLevelData().getMap().getLava().equals(terrain) || levelInfo.getLevelData().getMap().getWater().equals(terrain))) {
                    return;
                }

                // Just gather the adjacent tiles to the brigde
                Set<Point> roomPoints = new HashSet<>(tiles);
                Set<Point> adjacentPoints = new HashSet<>();
                for (Point tile : roomPoints) {
                    for (Point adjacentPoint : WorldUtils.getSurroundingTiles(mapController.getMapData(), tile, false)) {
                        if (adjacentPoint != null) {
                            if (!roomPoints.contains(adjacentPoint)) {
                                adjacentPoints.add(adjacentPoint);
                            }
                        }
                    }

                    // Scan all the adjacent tiles for tasks
                    for (Point mapTile : adjacentPoints) {
                        scanTerrainTasks(mapTile, false, true);
                    }
                }
            }

        });
    }

    private void scanInitialTasks() {
        IMapDataInformation mapData = mapController.getMapData();
        for (int y = 0; y < mapData.getHeight(); y++) {
            for (int x = 0; x < mapData.getWidth(); x++) {
                scanTerrainTasks(mapController.getMapData().getTile(x, y).getLocation(), false, false);
            }
        }
    }

    private void scanTerrainTasks(final Point tile, final boolean checkNeighbours, final boolean deleteObsolete) {
        for (Entry<Short, Set<Task>> entry : taskQueues.entrySet()) {

            // Scan existing tasks that are they valid, should be only one tile task per tile?
            if (deleteObsolete) {
                Iterator<Task> iter = entry.getValue().iterator();
                while (iter.hasNext()) {
                    Task task = iter.next();
                    if (task instanceof AbstractTileTask tileTask) {
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
            if (mapController.isSelected(tile, entry.getKey())) {
                Task task = new DigTileTask(navigationService, mapController, tile, entry.getKey());
                addTask(entry.getKey(), task);
            } // Claim wall
            else if (mapController.isClaimableWall(tile, entry.getKey())) {
                Task task = new ClaimWallTileTask(navigationService, mapController, tile, entry.getKey());
                addTask(entry.getKey(), task);
            } // Claim
            else if (mapController.isClaimableTile(tile, entry.getKey())) {
                Task task = new ClaimTileTask(navigationService, mapController, tile, entry.getKey());
                addTask(entry.getKey(), task);
            } // Repair wall
            else if (mapController.isRepairableWall(tile, entry.getKey())) {
                Task task = new RepairWallTileTask(navigationService, mapController, tile, entry.getKey());
                addTask(entry.getKey(), task);
            } // Claim room
            else if (mapController.isClaimableRoom(tile, entry.getKey())) {
                Task task = new ClaimRoomTask(navigationService, mapController, tile, entry.getKey());
                addTask(entry.getKey(), task);
            }
        }

        // See the neighbours
        if (checkNeighbours) {
            for (Point p : WorldUtils.getSurroundingTiles(mapController.getMapData(), tile, false)) {
                scanTerrainTasks(p, false, false);
            }
        }
    }

    private void scanFetchObjectTasks(Point tile) {

        // Add the tasks to tile owner and only if the object is not already in storage
        short playerId = mapController.getMapData().getTile(tile).getOwnerId();
        if (!taskQueues.containsKey(playerId)) {
            return;
        }
        for (EntityId entityId : entityPositionLookup.getEntitiesInLocation(tile)) {
            Entity entity = entityData.getEntity(entityId, ObjectComponent.class, Placeable.class, Position.class);
            createFetchObjectTask(entity, playerId);
        }
    }

    private void createFetchObjectTask(Entity entity, short playerId) {
        ObjectComponent objectComponent = entity.get(ObjectComponent.class);
        if (objectComponent != null && objectComponent.objectType != null && entityData.getComponent(entity.getId(), RoomStorage.class) == null) {
            addTask(playerId, getObjectTask(objectsController.createController(entity.getId()), playerId));
        }
    }

    public void addTask(short playerId, Task task) {
        Set<Task> tasks = taskQueues.get(playerId);
        if (!tasks.contains(task)) {
            tasks.add(task);
            tasksByIds.put(task.getId(), task);
            logger.log(Level.INFO, "Added task {0} for player {1}!", new Object[]{task, playerId});
        } else {
            logger.log(Level.WARNING, "Already a task {0} for player {1}!", new Object[]{task, playerId});
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
                distancesToRooms.put(getShortestDistance(currentPosition, room.getRoomInstance().getCoordinates().toArray(new Point[0])), room
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
                GraphPath<IMapTileInformation> path = navigationService.findPath(creature.getCreatureCoordinates(), target, creature);
                if (path != null || target == creature.getCreatureCoordinates()) {

                    // Assign the task
                    Task task = getRoomTask(objectType, target, targetEntity, creature, room);

                    // See if really assign
                    if (!assign) {
                        return task.isValid(creature);
                    }

                    if (task instanceof AbstractCapacityCriticalRoomTask abstractCapacityCriticalRoomTask) {
                        if (taskPoints == null) {
                            taskPoints = new HashMap<>();
                        }
                        taskPoints.put(target, abstractCapacityCriticalRoomTask);
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
            case GOLD -> {
                return new CarryGoldToTreasuryTask(navigationService, mapController, target, creature.getOwnerId(), room, gameWorldController);
            }
            case LAIR -> {
                return new ClaimLair(navigationService, mapController, target, creature.getOwnerId(), room, this);
            }
            case RESEARCHER -> {
                return new Research(navigationService, mapController, target, creature.getOwnerId(), room, this, playerControllers.get(creature.getOwnerId()).getResearchControl(), objectsController);
            }
            case PRISONER -> {
                return new CarryEnemyCreatureToPrison(navigationService, mapController, target, creature.getOwnerId(), room, this, creaturesController.createController(targetEntity));
            }
            case SPECIAL, SPELL_BOOK -> {
                return new CarryObjectToStorageTask(navigationService, mapController, target, creature.getOwnerId(), room, this, objectsController.createController(targetEntity));
            }
            case TRAINEE -> {
                return new Train(navigationService, mapController, target, creature.getOwnerId(), room, this, gameWorldController, gameSettings, playerControllers.get(creature.getOwnerId()));
            }
            default -> {
                logger.log(Level.DEBUG, "No task defined for " + objectType);
                return null;
            }
        }
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

    private AbstractTask getObjectTask(IObjectController gameObject, short playerId) {
        return new FetchObjectTask(this, navigationService, mapController, gameObject, playerId);
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
            case TRAIN: {
                return assignClosestRoomTask(creature, ObjectType.TRAINEE, null, assign);
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
        if (task != null && task instanceof AbstractObjectiveTask nestedTask) {
            return nestedTask.getCurrentTask();
        }

        return task;
    }

    private IMapTileInformation getEntityPosition(Entity entity) {
        IMapTileInformation mapTile = entityPositionLookup.getEntityLocation(entity.getId());

        // If the entity is new, it might not be in the registry, rather unfortunatety problem...
        if (mapTile == null) {
            mapTile = mapController.getMapData().getTile(WorldUtils.vectorToPoint(EntityController.getPosition(entityData, entity.getId())));
        }

        return mapTile;
    }

    @Override
    public void addToUnemployedWorkerQueue(ICreatureController creature, Consumer<Boolean> workResult) {
        unemployedCreatures.put(creature, workResult);
    }

    @Override
    public void processUnemployedWorkerQueue() {
        if (unemployedCreatures.isEmpty()) {
            return;
        }

        unemployedCreatures
                .entrySet()
                .stream()
                .collect(Collectors.groupingBy((creature) -> creature.getKey().getOwnerId()))
                .entrySet()
                .forEach((creaturesByOwnerId) -> {
                    processUnemployedWorkers(taskQueues.getOrDefault(creaturesByOwnerId.getKey(), Collections.emptySet()), creaturesByOwnerId.getValue());
                });
        unemployedCreatures.clear();
    }

    private void processUnemployedWorkers(Set<Task> tasks, List<Entry<ICreatureController, Consumer<Boolean>>> workers) {
        Set<Task> taskQueue = new HashSet<>(tasks);
        Map<ICreatureController, Consumer<Boolean>> unemployedWorkers = workers
                .stream()
                .collect(Collectors.toMap((entry) -> entry.getKey(), (entry) -> entry.getValue()));

        // Process each available task for each available worker
        List<TaskWorkerPriority> priorizedTasks = new ArrayList<>(workers.size() + tasks.size());
        for (Entry<ICreatureController, Consumer<Boolean>> workerEntry : unemployedWorkers.entrySet()) {
            ICreatureController creature = workerEntry.getKey();
            Consumer<Boolean> workResult = workerEntry.getValue();
            final Point currentLocation = creature.getCreatureCoordinates();

            // Give points by distance and priority, no need to know can we do the task as this point as it might be a heavy check
            for (Task task : taskQueue) {
                Vector2f target = task.getTarget(creature);
                if (target == null) {

                    // Can't reach
                    continue;
                }

                int points = WorldUtils.calculateDistance(currentLocation, WorldUtils.vectorToPoint(target)) + task.getPriority();
                priorizedTasks.add(new TaskWorkerPriority(task, creature, points, workResult));
            }
        }

        // Go through the tasks and assign workers
        while (!unemployedWorkers.isEmpty() && !priorizedTasks.isEmpty()) {

            // With each iteration we sort to fill the jobs as even as possible (assignee count changes)
            Collections.sort(priorizedTasks, (o1, o2) -> sortByTaskStatusAndDistance(o1, o2));

            Iterator<TaskWorkerPriority> iter = priorizedTasks.iterator();
            while (iter.hasNext()) {
                TaskWorkerPriority taskWorkerPriority = iter.next();

                // See if the task is full or the worker is not unemployed anymore or that the task can't actually be handled by this creature
                if (!unemployedWorkers.containsKey(taskWorkerPriority.creature)
                        || taskWorkerPriority.task.isFull()
                        || !taskWorkerPriority.task.canAssign(taskWorkerPriority.creature)) {
                    iter.remove();
                    continue;
                }

                // Assign task
                taskWorkerPriority.task.assign(taskWorkerPriority.creature, true);
                taskWorkerPriority.workResult.accept(Boolean.TRUE);

                // Prune list and re-sort tasks
                unemployedWorkers.remove(taskWorkerPriority.creature);
                iter.remove();
                break;
            }
        }

        // Finally anybody without assigment will get feedback
        unemployedWorkers.forEach((t, u) -> {
            u.accept(Boolean.FALSE);
        });
    }

    private int sortByTaskStatusAndDistance(TaskWorkerPriority o1, TaskWorkerPriority o2) {

        // Fill in jobs that have no workers first
        int result = Integer.compare(o1.task.getAssigneeCount(), o2.task.getAssigneeCount());
        if (result != 0) {
            return result;
        }

        // Closest creature gets the job
        result = Integer.compare(o1.points, o2.points);
        if (result != 0) {
            return result;
        }

        // If the same, compare by date added
        return o1.task.getTaskCreated().compareTo(o2.task.getTaskCreated());
    }

    private static record TaskWorkerPriority(Task task, ICreatureController creature, int points, Consumer<Boolean> workResult) {

    }

}
