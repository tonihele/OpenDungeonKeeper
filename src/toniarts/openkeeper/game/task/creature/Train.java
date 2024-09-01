/*
 * Copyright (C) 2014-2023 OpenKeeper
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
import toniarts.openkeeper.utils.Point;
import java.util.Map;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractCapacityCriticalRoomTask;
import toniarts.openkeeper.game.task.TaskManager;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

/**
 * Trains creature
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Train extends AbstractCapacityCriticalRoomTask {

    private final IGameWorldController gameWorldController;
    private final IPlayerController playerController;
    private final int trainingRoomMaxExperienceLevel;
    private final int trainingCost;

    public Train(final INavigationService navigationService, final IMapController mapController,
            Point p, short playerId, IRoomController room, TaskManager taskManager,
            IGameWorldController gameWorldController, Map<MiscType, MiscVariable> gameSettings,
            IPlayerController playerController) {
        super(navigationService, mapController, p, playerId, room, taskManager);

        this.gameWorldController = gameWorldController;
        this.playerController = playerController;

        trainingRoomMaxExperienceLevel = (int) gameSettings.get(Variable.MiscVariable.MiscType.TRAINING_ROOM_MAX_EXPERIENCE_LEVEL).getValue();
        trainingCost = Math.abs((int) gameSettings.get(Variable.MiscVariable.MiscType.MODIFY_PLAYER_GOLD_WHILE_TRAINING_PER_SECOND).getValue());
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        return playerController.getKeeper().getGold() >= trainingCost && creature.getLevel() < trainingRoomMaxExperienceLevel && super.isValid(creature);
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return getAccessibleTargetNextToLocation(creature);
    }

    @Override
    public boolean isFaceTarget() {
        return true;
    }

    @Override
    protected ObjectType getRoomObjectType() {
        return ObjectType.TRAINEE;
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {

        // TODO: is this a general case or even smart to do this like this...?
        if (executionDuration - getExecutionDuration(creature) < 1.0f) {
            return;
        }

        setExecutionDuration(creature, executionDuration - getExecutionDuration(creature));
        gameWorldController.substractGold(trainingCost, playerId);
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.TRAIN;
    }

    @Override
    public boolean isRemovable() {
        return true;
    }
}
