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
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractRoomTask;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Carry gold to treasury
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CarryGoldToTreasuryTask extends AbstractRoomTask {

    private final IGameWorldController gameWorldController;
    private boolean executed = false;

    public CarryGoldToTreasuryTask(final INavigationService navigationService, final IMapController mapController,
            Point p, short playerId, final IRoomController room, final IGameWorldController gameWorldController) {
        super(navigationService, mapController, p, playerId, room);
        this.gameWorldController = gameWorldController;
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
        return ObjectType.GOLD;
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        int gold = creature.getGold();
        creature.substractGold(gold - gameWorldController.addGold(playerId, getTaskLocation(), gold));

        // This is a one timer
        executed = true;
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.CARRY_GOLD_TO_TREASURY;
    }

}
