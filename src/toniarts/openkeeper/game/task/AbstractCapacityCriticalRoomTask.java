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

import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.navigation.INavigationService;

/**
 * A base class for tasks that we should keep track off
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class AbstractCapacityCriticalRoomTask extends AbstractRoomTask {

    protected final TaskManager taskManager;

    public AbstractCapacityCriticalRoomTask(final INavigationService navigationService, final IMapController mapController, Point p, short playerId, IRoomController room, TaskManager taskManager) {
        super(navigationService, mapController, p, playerId, room);

        this.taskManager = taskManager;
    }

    @Override
    public void unassign(ICreatureController creature) {
        super.unassign(creature);

        // Remove us
        taskManager.removeRoomTask(this);
    }

}
