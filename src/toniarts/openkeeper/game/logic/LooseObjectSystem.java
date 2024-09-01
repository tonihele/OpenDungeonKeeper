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

import com.jme3.util.SafeArrayList;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import toniarts.openkeeper.utils.Point;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Placeable;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.RoomStorage;
import toniarts.openkeeper.game.controller.GameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Player;

/**
 * A simple state to scan the loose objects inside rooms. The loose objects are
 * added to the rooms automatically if there is some storage capacity left
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class LooseObjectSystem implements IGameLogicUpdatable {

    private final EntitySet looseObjectEntities;

    private final EntityData entityData;
    private final SafeArrayList<EntityId> looseObjectEntityIds;
    private final IMapController mapController;
    private final Map<Short, IPlayerController> playerControllers;
    private final IEntityPositionLookup entityPositionLookup;

    public LooseObjectSystem(EntityData entityData, IMapController mapController, Map<Short, IPlayerController> playerControllers,
            IEntityPositionLookup entityPositionLookup) {
        this.entityData = entityData;
        this.mapController = mapController;
        this.playerControllers = playerControllers;
        this.entityPositionLookup = entityPositionLookup;

        looseObjectEntities = entityData.getEntities(ObjectComponent.class, Position.class, Placeable.class);
        looseObjectEntityIds = new SafeArrayList<>(EntityId.class);
        processAddedEntities(looseObjectEntities);
    }

    @Override
    public void processTick(float tpf, double gameTime) {

        // Add new & remove old
        if (looseObjectEntities.applyChanges()) {

            processAddedEntities(looseObjectEntities.getAddedEntities());

            processDeletedEntities(looseObjectEntities.getRemovedEntities());

        }

        // Attach loose objects to rooms
        for (EntityId entityId : looseObjectEntityIds.getArray()) {
            RoomStorage roomStorage = entityData.getComponent(entityId, RoomStorage.class);
            if (roomStorage != null) {

                // TODO: Dunno if this the best way, we always iterate through a lot of unnecessary objects
                // Maybe storage should remove the placeable temporarily...
                continue;
            }

            IMapTileInformation mapTile = entityPositionLookup.getEntityLocation(entityId);
            if (mapTile == null) {

                // No position yet, we get it next time
                continue;
            }
            if (mapTile.getOwnerId() != Player.GOOD_PLAYER_ID && mapTile.getOwnerId() != Player.NEUTRAL_PLAYER_ID) {
                continue;
            }

            Entity entity = looseObjectEntities.getEntity(entityId);
            Point point = mapTile.getLocation();
            IRoomController roomController = mapController.getRoomControllerByCoordinates(point);
            ObjectComponent objectComponent = entity.get(ObjectComponent.class);
            if (roomController != null && objectComponent.objectType != null && roomController.hasObjectControl(objectComponent.objectType) && !roomController.getObjectControl(objectComponent.objectType).isFullCapacity()) {
                short ownerId = roomController.getRoomInstance().getOwnerId();
                if (objectComponent.objectType == AbstractRoomController.ObjectType.GOLD) {
                    synchronized (GameWorldController.GOLD_LOCK) {
                        Gold gold = entityData.getComponent(entityId, Gold.class);
                        int goldLeft = (int) roomController.getObjectControl(AbstractRoomController.ObjectType.GOLD).addItem(gold.gold, point);
                        playerControllers.get(ownerId).getGoldControl().addGold(gold.gold - goldLeft);
                        if (goldLeft == 0) {
                            entityData.removeEntity(entityId);
                        } else {
                            entityData.setComponent(entityId, new Gold(goldLeft, gold.maxGold));
                        }
                    }
                } else {
                    roomController.getObjectControl(objectComponent.objectType).addItem(entityId, point);
                }
            }
        }
    }

    private void processAddedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            int index = Collections.binarySearch(looseObjectEntityIds, entity.getId());
            looseObjectEntityIds.add(~index, entity.getId());
        }
    }

    private void processDeletedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            int index = Collections.binarySearch(looseObjectEntityIds, entity.getId());
            looseObjectEntityIds.remove(index);
        }
    }


    @Override
    public void start() {

    }

    @Override
    public void stop() {
        looseObjectEntities.release();
        looseObjectEntities.clear();
    }

}
