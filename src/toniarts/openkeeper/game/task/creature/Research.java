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
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.player.PlayerResearchControl;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractCapacityCriticalRoomTask;
import toniarts.openkeeper.game.task.TaskManager;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Research researchable entities for the keeper
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Research extends AbstractCapacityCriticalRoomTask {

    private final PlayerResearchControl researchControl;
    private final IObjectsController objectsController;

    public Research(final INavigationService navigationService, final IMapController mapController, Point p, short playerId, IRoomController room,
            TaskManager taskManager, PlayerResearchControl researchControl, IObjectsController objectsController) {
        super(navigationService, mapController, p, playerId, room, taskManager);

        this.researchControl = researchControl;
        this.objectsController = objectsController;
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        return (researchControl.isAnythingToReaseach() && !getRoom().getObjectControl(getRoomObjectType()).isFullCapacity());
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return WorldUtils.pointToVector2f(getTaskLocation()); // FIXME 0.5f not needed?
    }

    @Override
    protected ObjectType getRoomObjectType() {
        return ObjectType.SPELL_BOOK;
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {

        // TODO: is this a general case or even smart to do this like this...?
        if (executionDuration - getExecutionDuration(creature) < 1.0f) {
            return;
        }

        setExecutionDuration(creature, executionDuration - getExecutionDuration(creature));

        // Advance players spell research
        ResearchableEntity researchableEntity = researchControl.research(creature.getResearchPerSecond());
        if (researchableEntity != null) {

            // Create a spell book
            EntityId entityId = objectsController.addRoomSpellBook((short) 0, getTaskLocation().x, getTaskLocation().y, researchableEntity);
            entityId = (EntityId) getRoomObjectControl().addItem(entityId, null);
            if (entityId != null) {

                // Failed add, wut
                objectsController.createController(entityId).remove();
            }
        }
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.RESEARCH;
    }

}
