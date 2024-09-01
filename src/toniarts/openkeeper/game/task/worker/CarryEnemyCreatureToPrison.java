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
package toniarts.openkeeper.game.task.worker;

import com.jme3.math.Vector2f;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractCapacityCriticalRoomTask;
import toniarts.openkeeper.game.task.TaskManager;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * A task for creatures to haul the captured enemy to prison
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CarryEnemyCreatureToPrison extends AbstractCapacityCriticalRoomTask {

    private final ICreatureController creature;
    private boolean executed = false;

    public CarryEnemyCreatureToPrison(final INavigationService navigationService, final IMapController mapController, Point p, short playerId, IRoomController room,
            TaskManager taskManager, ICreatureController creature) {
        super(navigationService, mapController, p, playerId, room, taskManager);
        this.creature = creature;
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
        return "Carrying enemy creature to prison at " + getTaskLocation();
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        this.creature.imprison(playerId);
        executed = true;

        // Set the dragged state
        this.creature.setHaulable(null);
    }

    @Override
    protected ObjectType getRoomObjectType() {
        return ObjectType.PRISONER;
    }

    @Override
    public void unassign(ICreatureController creature) {
        super.unassign(creature);

        // Set the dragged state
        this.creature.setHaulable(null);
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.CARRY_CREATURE_TO_JAIL;
    }

}
