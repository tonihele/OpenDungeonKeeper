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
package toniarts.openkeeper.game.controller.entity;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Objects;
import toniarts.openkeeper.game.component.CreatureSleep;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.HauledBy;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.RoomStorage;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * A base class for all entity controllers
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class EntityController implements IEntityController {
    
    private static final Logger logger = System.getLogger(EntityController.class.getName());

    protected final EntityId entityId;
    protected final EntityData entityData;
    protected final IObjectsController objectsController;
    protected final IMapController mapController;

    public EntityController(EntityId entityId, EntityData entityData, IObjectsController objectsController, IMapController mapController) {
        this.entityId = entityId;
        this.entityData = entityData;
        this.objectsController = objectsController;
        this.mapController = mapController;
    }

    @Override
    public EntityId getEntityId() {
        return entityId;
    }

    @Override
    public short getOwnerId() {
        Owner owner = entityData.getComponent(entityId, Owner.class);
        return owner.ownerId;
    }

    @Override
    public int getHealth() {
        Health health = entityData.getComponent(entityId, Health.class);
        return health.health;
    }

    @Override
    public int getMaxHealth() {
        Health health = entityData.getComponent(entityId, Health.class);
        return health.maxHealth;
    }

    @Override
    public boolean isFullHealth() {
        Health health = entityData.getComponent(entityId, Health.class);
        return health.health == health.maxHealth;
    }

    @Override
    public boolean isPickedUp() {
        InHand inHand = entityData.getComponent(entityId, InHand.class);
        return inHand != null;
    }

    @Override
    public Vector3f getPosition() {
        return getPosition(entityData, entityId);
    }

    @Override
    public IMapTileInformation getTile() {
        return mapController.getMapData().getTile(WorldUtils.vectorToPoint(getPosition()));
    }

    public static Vector3f getPosition(EntityData entityData, EntityId entity) {
        Position position = entityData.getComponent(entity, Position.class);
        if (position != null) {
            return position.position;
        }
        return null;
    }

    @Override
    public void remove() {
        removePossession();
        entityData.removeEntity(entityId);
    }

    @Override
    public void removePossession() {
        handleLootDrop(entityId);
        handleAssociatedEntities(entityId);
    }

    private void handleLootDrop(EntityId entityId) {

        // Drop gold
        Gold gold = entityData.getComponent(entityId, Gold.class);
        if (gold != null && gold.gold > 0) {
            Position position = entityData.getComponent(entityId, Position.class);
            Owner owner = entityData.getComponent(entityId, Owner.class);
            Point point = WorldUtils.vectorToPoint(position.position);
            // TODO: some central place, we need to add more than one pile if it exceeds the max
            objectsController.addLooseGold(owner.ownerId, point.x, point.y, gold.gold, gold.maxGold);
            entityData.removeComponent(entityId, Gold.class);
        }

        // Drop Portal Gem
        // TODO:
    }

    private void handleAssociatedEntities(EntityId entityId) {
        RoomStorage roomStorage = null;
        Position position = null;

        // Get rid of lairs
        CreatureSleep creatureSleep = entityData.getComponent(entityId, CreatureSleep.class);
        if (creatureSleep != null && creatureSleep.lairObjectId != null) {
            roomStorage = entityData.getComponent(creatureSleep.lairObjectId, RoomStorage.class);
            position = entityData.getComponent(creatureSleep.lairObjectId, Position.class);
            removeRoomStorage(roomStorage, position, creatureSleep.lairObjectId);
        }

        // We are a property of a room
        roomStorage = entityData.getComponent(entityId, RoomStorage.class);
        position = entityData.getComponent(entityId, Position.class);
        removeRoomStorage(roomStorage, position, entityId);
    }

    private void removeRoomStorage(RoomStorage roomStorage, Position position, EntityId entityId) {
        if (roomStorage == null) {
            return;
        }
        if (position == null) {
            logger.log(Level.WARNING, () -> "Entity died and is part of room storage (" + roomStorage + ") but hasn't got location!");
            return;
        }

        IRoomController roomController = mapController.getRoomControllerByCoordinates(WorldUtils.vectorToPoint(position.position));
        IRoomObjectControl roomObjectControl = roomController.getObjectControl(roomStorage.objectType);
        roomObjectControl.removeItem(entityId);
    }

    @Override
    public boolean isRemoved() {
        return entityData.getComponent(entityId, Position.class) == null && entityData.getComponent(entityId, InHand.class) == null;
    }

    @Override
    public void setHaulable(ICreatureController creature) {
        if (creature != null) {
            entityData.setComponent(entityId, new HauledBy(creature.getEntityId()));
        } else {
            entityData.removeComponent(entityId, HauledBy.class);
        }
    }

    @Override
    public boolean isDragged() {
        return entityData.getComponent(entityId, HauledBy.class) != null;
    }

    @Override
    public void setDamage(int damageAmount) {
        setDamage(entityId, damageAmount);
    }

    @Override
    public void setDamage(EntityId entityId, int damageAmount) {
        setDamage(entityData, entityId, damageAmount);
    }

    public static void setDamage(EntityData entityData, EntityId entityId, int damageAmount) {
        Damage damage = entityData.getComponent(entityId, Damage.class);
        int damageTotal = damage != null ? damage.damage : 0;
        entityData.setComponent(entityId, new Damage(damageTotal + damageAmount));
    }

    @Override
    public int compareTo(IEntityController t) {
        return entityId.compareTo(t.getEntityId());
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + Objects.hashCode(this.entityId);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final IEntityController other = (IEntityController) obj;
        if (!Objects.equals(this.entityId, other.getEntityId())) {
            return false;
        }
        return true;
    }

}
