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
package toniarts.openkeeper.game.logic;

import com.jme3.math.Vector2f;
import com.jme3.util.SafeArrayList;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.room.ICreatureEntrance;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.listener.RoomListener;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Creature.Attraction;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.tools.convert.map.Variable.CreaturePool;
import toniarts.openkeeper.utils.Utils;

/**
 * Handles creatures spawning, from Portals, Dungeon Hearts...
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CreatureSpawnSystem implements IGameLogicUpdatable {

    private final ICreaturesController creaturesController;
    private final int minimumImpCount;
    private final int entranceCoolDownTime;
    private final int initialPortalCapacity;
    private final int additionalPortalCapacity;
    private final int freeImpCoolDownTime;
    private final Map<Short, IPlayerController> playerControllersById;
    private final SafeArrayList<ICreatureEntrance> entrances = new SafeArrayList<>(ICreatureEntrance.class);
    private final KwdFile kwdFile;

    public CreatureSpawnSystem(ICreaturesController creaturesController, Collection<IPlayerController> playerControllers,
            Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings, ILevelInfo levelInfo,
            IMapController mapController) {
        this.creaturesController = creaturesController;

        // We need the game state just for the variables
        entranceCoolDownTime = (int) gameSettings.get(Variable.MiscVariable.MiscType.ENTRANCE_GENERATION_SPEED_SECONDS).getValue();
        minimumImpCount = (int) gameSettings.get(Variable.MiscVariable.MiscType.MINIMUM_IMP_THRESHOLD).getValue();
        initialPortalCapacity = (int) gameSettings.get(Variable.MiscVariable.MiscType.CREATURES_SUPPORTED_BY_FIRST_PORTAL).getValue();
        additionalPortalCapacity = (int) gameSettings.get(Variable.MiscVariable.MiscType.CREATURES_SUPPORTED_PER_ADDITIONAL_PORTAL).getValue();
        freeImpCoolDownTime = (int) gameSettings.get(Variable.MiscVariable.MiscType.TIME_BEFORE_FREE_IMP_GENERATED_SECONDS).getValue();
        kwdFile = levelInfo.getLevelData();

        // Populate entrance list
        playerControllersById = new HashMap<>(playerControllers.size(), 1f);
        for (IPlayerController player : playerControllers) {
            playerControllersById.put(player.getKeeper().getId(), player);

            // Add initial rooms
            for (Entry<Room, Set<IRoomController>> keeperRooms : player.getRoomControl().getTypes().entrySet()) {

                // See that should we add
                for (IRoomController genericRoom : keeperRooms.getValue()) {

                    // A bit clumsy to check like this
                    if (!(genericRoom instanceof ICreatureEntrance)) {
                        break;
                    }
                    entrances.add((ICreatureEntrance) genericRoom);
                }
            }

            // Add room listener to get notified of the changes
            // They should be quite rare vs the rate in which we iterate on each tick
            mapController.addListener(player.getKeeper().getId(), new EntranceListener(player.getKeeper().getId()));

            // TODO: also get notified about the evicted and newly introduced creatures
            // Maybe store these under the keeper etc.? For saving purposes
            // The logic classes shouldn't have anything to save
        }
    }

    @Override
    public void processTick(float tpf, double gameTime) {
        for (ICreatureEntrance entrance : entrances) {
            evaluateAndSpawnCreature(entrance, gameTime);
        }
    }

    private void evaluateAndSpawnCreature(ICreatureEntrance entrance, double gameTime) {
        double timeSinceLastSpawn = gameTime - entrance.getLastSpawnTime();
        IPlayerController player = playerControllersById.get(entrance.getRoomInstance().getOwnerId());
        boolean spawned = false;
        if (timeSinceLastSpawn >= freeImpCoolDownTime && entrance.isDungeonHeart()) {
            if (player.getCreatureControl().getImpCount() < minimumImpCount) {

                // Spawn imp
                Point entranceCoordinate = entrance.getEntranceCoordinate();
                creaturesController.spawnCreature(kwdFile.getImp().getCreatureId(), player.getKeeper().getId(), 1, new Vector2f(entranceCoordinate.x, entranceCoordinate.y), false);
                spawned = true;
            }
        } else if (timeSinceLastSpawn >= Math.max(entranceCoolDownTime, entranceCoolDownTime * player.getCreatureControl().getTypeCount() * 0.5)
                && player.getRoomControl().isPortalsOpen() && !isCreatureLimitReached(player)) {

            // Evaluate what creature can we spawn
            Map<Integer, CreaturePool> pool = kwdFile.getCreaturePool(player.getKeeper().getId());
            List<Creature> possibleCreatures = new ArrayList<>(player.getCreatureControl().getTypesAvailable());
            Iterator<Creature> iter = possibleCreatures.iterator();
            while (iter.hasNext()) {
                Creature creature = iter.next();
                if (!isCreatureAvailableFromPool(creature, player, pool)
                        || !isCreatureRequirementsSatisfied(creature, player)) {
                    iter.remove();
                }
            }

            // Spawn random?
            if (!possibleCreatures.isEmpty()) {
                short creatureId = Utils.getRandomItem(possibleCreatures).getCreatureId();
                Point entranceCoordinate = entrance.getEntranceCoordinate();
                creaturesController.spawnCreature(creatureId, player.getKeeper().getId(), 1, new Vector2f(entranceCoordinate.x, entranceCoordinate.y), true);
                spawned = true;
            }
        }

        if (spawned) {

            // Reset spawn time
            entrance.onSpawn(gameTime);
        }
    }

    private boolean isCreatureLimitReached(IPlayerController player) {
        return player.getCreatureControl().getTypeCount() >= (initialPortalCapacity + (player.getRoomControl().getTypeCount(kwdFile.getPortal()) - 1) * additionalPortalCapacity);
    }

    private boolean isCreatureRequirementsSatisfied(Creature creature, IPlayerController player) {
        for (Attraction attraction : creature.getAttractions()) {
            short roomId = (short) attraction.getRoomId();
            if (roomId > 0) {
                Room room = kwdFile.getRoomById(roomId);
                if (player.getRoomControl().getTypeCount(room) > 0) {

                    // Ok, we have these, see sizes, I recon we really need a room that size, not summed up tiles
                    if (attraction.getRoomSize() > 0) {
                        boolean roomFound = false;
                        for (IRoomController genericRoom : new ArrayList<>(player.getRoomControl().getTypes().get(room))) {
                            if (attraction.getRoomSize() <= genericRoom.getRoomInstance().getCoordinates().size()) {
                                roomFound = true;
                                break; // Ok
                            }
                        }
                        if (roomFound) {
                            continue;
                        }
                    } else {
                        continue; // We have the room
                    }

                    return false;
                }

                return false;
            }
        }
        return true;
    }

    private boolean isCreatureAvailableFromPool(Creature creature, IPlayerController player, Map<Integer, CreaturePool> pool) {
        CreaturePool creaturePool = pool.get(Short.valueOf(creature.getCreatureId()).intValue());
        if (creaturePool != null) {
            int creatures = player.getCreatureControl().getTypeCount(creature);
            return creaturePool.getValue() > creatures;
        }
        return false;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    /**
     * In DK 2 it is not possible to place spawn points in game, but but we
     * don't know that
     */
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
            if (room instanceof ICreatureEntrance) {
                entrances.add((ICreatureEntrance) room);
            }
        }

        private void removeRoom(IRoomController room) {
            if (room instanceof ICreatureEntrance) {
                entrances.remove((ICreatureEntrance) room);
            }
        }

    }

}
