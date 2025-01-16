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
package toniarts.openkeeper.game.task.creature;

import com.jme3.math.Vector2f;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractCapacityCriticalRoomTask;
import toniarts.openkeeper.game.task.TaskManager;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Claim a lair for a creature
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ClaimLair extends AbstractCapacityCriticalRoomTask {

    private boolean executed = false;

    public ClaimLair(final INavigationService navigationService, final IMapController mapController, Point p, short playerId, IRoomController room, TaskManager taskManager) {
        super(navigationService, mapController, p, playerId, room, taskManager);
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        if (!executed) {
            return super.isValid(creature);
        }
        return false;
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return WorldUtils.pointToVector2f(getTaskLocation()); // FIXME 0.5f not needed?
    }

    @Override
    protected ObjectType getRoomObjectType() {
        return ObjectType.LAIR;
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {

        // Create a lair
        IRoomObjectControl control = getRoomObjectControl();
        EntityId lairId = (EntityId) control.addItem(creature.getEntityId(), getTaskLocation());
        if (lairId != null) {
            creature.setCreatureLair(lairId);
        }

        // This is a one timer
        executed = true;
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.CLAIM_LAIR;
    }
}
