/*
 * Copyright (C) 2014-2020 OpenKeeper
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
package toniarts.openkeeper.game.task.worker;

import com.jme3.math.Vector2f;
import toniarts.openkeeper.utils.Point;
import java.util.Objects;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.object.IObjectController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractCapacityCriticalRoomTask;
import toniarts.openkeeper.game.task.TaskManager;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Carries objects to storage
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CarryObjectToStorageTask extends AbstractCapacityCriticalRoomTask {

    private final IObjectController gameObject;
    private boolean executed = false;

    public CarryObjectToStorageTask(final INavigationService navigationService, final IMapController mapController, Point p, short playerId, IRoomController room,
            TaskManager taskManager, IObjectController gameObject) {
        super(navigationService, mapController, p, playerId, room, taskManager);

        this.gameObject = gameObject;
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return WorldUtils.pointToVector2f(getTaskLocation()); // FIXME 0.5f not needed?
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        if (!executed) {
            return super.isValid(creature);
        }
        return false;
    }

    @Override
    public String toString() {
        return "Carrying object to storage at " + getTaskLocation();
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        room.getObjectControl(getRoomObjectType()).addItem(gameObject.getEntityId(), getTaskLocation());
        executed = true;

        // Set the dragged state
        gameObject.setHaulable(null);
    }

    @Override
    protected AbstractRoomController.ObjectType getRoomObjectType() {
        return gameObject.getType();
    }

    @Override
    public void unassign(ICreatureController creature) {
        super.unassign(creature);

        // Set the dragged state
        gameObject.setHaulable(null);
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.CARRY_OBJECT_TO_STORAGE;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.gameObject);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CarryObjectToStorageTask other = (CarryObjectToStorageTask) obj;
        if (!Objects.equals(this.gameObject, other.gameObject)) {
            return false;
        }
        return true;
    }

}
