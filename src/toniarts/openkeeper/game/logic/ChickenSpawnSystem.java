/*
 * Copyright (C) 2014-2019 OpenKeeper
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
package toniarts.openkeeper.game.logic;

import com.jme3.util.SafeArrayList;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import toniarts.openkeeper.utils.Point;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import toniarts.openkeeper.game.component.ChickenGenerator;
import toniarts.openkeeper.game.component.Decay;
import toniarts.openkeeper.game.component.Food;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.room.IChickenGenerator;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.listener.RoomListener;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Handles spawing chickens in hatcheries and coops
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ChickenSpawnSystem implements IGameLogicUpdatable {

    private final IObjectsController objectsController;
    private final IMapController mapController;
    private final int maximumFreerangeChickenCount;
    private final int entranceCooldownTime;
    private final Map<Short, IPlayerController> playerControllersById;
    private final SafeArrayList<IChickenGenerator> entrances = new SafeArrayList<>(IChickenGenerator.class);
    private final EntitySet freerangeChickenGenerators;
    private final Map<IChickenGenerator, Set<EntityId>> freerangeChickenGeneratorsByRoom = new HashMap<>();
    private final Map<EntityId, IChickenGenerator> roomsByFreerangeChickenGenerators = new HashMap<>();
    private final EntitySet freerangeChickens;
    private final Map<Short, Set<EntityId>> freeRangeChickensByPlayer;
    private final Map<EntityId, Short> freeRangeChickenOwners;

    public ChickenSpawnSystem(IObjectsController objectsController, Collection<IPlayerController> playerControllers,
            Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings, ILevelInfo levelInfo,
            IMapController mapController) {
        this.objectsController = objectsController;
        this.mapController = mapController;

        // We need the game state just for the variables
        entranceCooldownTime = (int) gameSettings.get(Variable.MiscVariable.MiscType.CHICKEN_GENERATION_TIME_PER_HATCHERY).getValue() / levelInfo.getLevelData().getGameLevel().getTicksPerSec();
        maximumFreerangeChickenCount = (int) gameSettings.get(Variable.MiscVariable.MiscType.MAX_FREE_RANGE_CHICKENS_PER_PLAYER).getValue();

        // Populate entrance list
        playerControllersById = HashMap.newHashMap(playerControllers.size());
        for (IPlayerController player : playerControllers) {
            playerControllersById.put(player.getKeeper().getId(), player);

            // Add initial rooms
            for (Entry<Room, Set<IRoomController>> keeperRooms : player.getRoomControl().getRoomControllers().entrySet()) {

                // See that should we add
                for (IRoomController genericRoom : keeperRooms.getValue()) {

                    // A bit clumsy to check like this
                    if (!(genericRoom instanceof IChickenGenerator)) {
                        break;
                    }
                    entrances.add((IChickenGenerator) genericRoom);
                }
            }

            // Add room listener to get notified of the changes
            // They should be quite rare vs the rate in which we iterate on each tick
            mapController.addListener(player.getKeeper().getId(), new EntranceListener(player.getKeeper().getId()));
        }

        // Listen for the coops, also room specific generation...
        freerangeChickenGenerators = objectsController.getEntityData().getEntities(ChickenGenerator.class, Position.class);
        processAddedEntities(freerangeChickenGenerators);

        // Listen for freerange chickens
        // For know we know this for the food and decay tag, not very elegant perhaps
        freerangeChickens = objectsController.getEntityData().getEntities(Food.class, Decay.class, Owner.class);
        freeRangeChickenOwners = HashMap.newHashMap(playerControllersById.size() * maximumFreerangeChickenCount);
        freeRangeChickensByPlayer = HashMap.newHashMap(playerControllers.size());
        for (IPlayerController player : playerControllers) {
            freeRangeChickensByPlayer.put(player.getKeeper().getId(), HashSet.newHashSet(maximumFreerangeChickenCount));
        }
        processAddedChickenEntities(freerangeChickens);
    }

    @Override
    public void processTick(float tpf, double gameTime) {

        // Add new & remove old
        // Basically they don't move, so lets not worry about that now, also the owner goes with the room
        if (freerangeChickenGenerators.applyChanges()) {
            processDeletedEntities(freerangeChickenGenerators.getRemovedEntities());

            processAddedEntities(freerangeChickenGenerators.getAddedEntities());
        }

        if (freerangeChickens.applyChanges()) {
            processDeletedChickenEntities(freerangeChickens.getRemovedEntities());

            processAddedChickenEntities(freerangeChickens.getAddedEntities());

            processChangedChickenEntities(freerangeChickens.getChangedEntities());
        }

        for (IChickenGenerator entrance : entrances.getArray()) {
            evaluateAndSpawnCreature(entrance, gameTime);
        }
    }

    private void evaluateAndSpawnCreature(IChickenGenerator entrance, double gameTime) {
        double timeSinceLastSpawn = gameTime - entrance.getLastSpawnTime();
        boolean spawned = false;
        EntityId entityId = null;
        if (timeSinceLastSpawn >= entranceCooldownTime) {

            if (!entrance.isFullCapacity()) {

                // Spawn chicken
                Point entranceCoordinate = entrance.getEntranceCoordinate();
                entityId = objectsController.spawnChicken(entrance.getRoomInstance().getOwnerId(), WorldUtils.pointToVector3f(entranceCoordinate));
                spawned = true;
            } else if (freeRangeChickensByPlayer.get(entrance.getRoomInstance().getOwnerId()).size() < maximumFreerangeChickenCount && freerangeChickenGeneratorsByRoom.get(entrance) != null) {
                Set<EntityId> generators = freerangeChickenGeneratorsByRoom.get(entrance);
                Optional<EntityId> generator = Utils.getRandomItem(generators);
                if (generator.isPresent()) {

                    // Spawn a free range chicken
                    // Don't give the entity ID, it is not added to room inventory
                    // TODO: Need to have the generator IN USE component etc. This goes for all the objects, how we use them
                    Position position = objectsController.getEntityData().getComponent(generator.get(), Position.class);
                    objectsController.spawnFreerangeChicken(entrance.getRoomInstance().getOwnerId(), position.position.clone(), gameTime);
                    spawned = true;
                }
            }
        }

        if (spawned) {

            // Reset spawn time
            entrance.onSpawn(gameTime, entityId);
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        freerangeChickenGenerators.release();
        freerangeChickenGeneratorsByRoom.clear();
        roomsByFreerangeChickenGenerators.clear();

        freerangeChickens.release();
        freeRangeChickensByPlayer.clear();
        freeRangeChickenOwners.clear();
    }

    private void processAddedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            Position position = entity.get(Position.class);
            Point p = WorldUtils.vectorToPoint(position.position);
            IRoomController roomController = mapController.getRoomControllerByCoordinates(p);
            IChickenGenerator chickenGenerator = null;
            if (roomController instanceof IChickenGenerator) {
                chickenGenerator = (IChickenGenerator) roomController;
                Set<EntityId> generators = freerangeChickenGeneratorsByRoom.get(chickenGenerator);
                if (generators == null) {
                    generators = new HashSet<>();
                }
                generators.add(entity.getId());
                freerangeChickenGeneratorsByRoom.put(chickenGenerator, generators);
            }
            roomsByFreerangeChickenGenerators.put(entity.getId(), chickenGenerator);
        }
    }

    private void processDeletedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            IChickenGenerator chickenGenerator = roomsByFreerangeChickenGenerators.remove(entity.getId());
            if (chickenGenerator != null) {
                freerangeChickenGeneratorsByRoom.get(chickenGenerator).remove(entity.getId());
            }
        }
    }

    private void processAddedChickenEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            Owner owner = entity.get(Owner.class);
            Set<EntityId> chickens = freeRangeChickensByPlayer.get(owner.ownerId);
            if (chickens == null) {
                chickens = new HashSet<>();
            }
            chickens.add(entity.getId());
            freeRangeChickensByPlayer.put(owner.ownerId, chickens);
            freeRangeChickenOwners.put(entity.getId(), owner.ownerId);
        }
    }

    private void processDeletedChickenEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            short ownerId = freeRangeChickenOwners.remove(entity.getId());
            freeRangeChickensByPlayer.get(ownerId).remove(entity.getId());
        }
    }

    private void processChangedChickenEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            Owner owner = entity.get(Owner.class);
            short previousOwner = freeRangeChickenOwners.get(entity.getId());
            if (previousOwner != owner.ownerId) {
                freeRangeChickenOwners.put(entity.getId(), owner.ownerId);
                freeRangeChickensByPlayer.get(previousOwner).remove(entity.getId());
                freeRangeChickensByPlayer.get(owner.ownerId).add(entity.getId());
            }
        }
    }

    private class EntranceListener implements RoomListener {

        private final short playerId;

        public EntranceListener(short playerId) {
            this.playerId = playerId;
        }

        @Override
        public void onBuild(IRoomController room) {
            addRoom(room);
        }

        @Override
        public void onCaptured(IRoomController room) {
            //addRoom(room);
        }

        @Override
        public void onCapturedByEnemy(IRoomController room) {
            //removeRoom(room);
        }

        @Override
        public void onSold(IRoomController room) {
            removeRoom(room);
        }

        private void addRoom(IRoomController room) {
            if (room instanceof IChickenGenerator) {
                entrances.add((IChickenGenerator) room);
            }
        }

        private void removeRoom(IRoomController room) {
            if (room instanceof IChickenGenerator) {
                entrances.remove((IChickenGenerator) room);
            }
        }

    }

}
