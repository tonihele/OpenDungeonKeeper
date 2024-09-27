/*
 * Copyright (C) 2014-2018 OpenKeeper
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.task.ITaskManager;

/**
 * Handles creature logic updates, the creature AI updates that is. The AI is
 * implemented elsewhere for clarity. This class just attaches the AI to the
 * entity having this component and updates it periodically.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CreatureAiSystem implements IGameLogicUpdatable {

    private final EntitySet creatureEntities;

    private final SafeArrayList<ICreatureController> creatureControllers;
    private final Map<EntityId, ICreatureController> creatureControllersByEntityId;
    private final ICreaturesController creaturesController;
    private final ITaskManager taskManager;

    public CreatureAiSystem(EntityData entityData, ICreaturesController creaturesController, ITaskManager taskManager) {
        this.creaturesController = creaturesController;
        this.taskManager = taskManager;

        creatureEntities = entityData.getEntities(CreatureAi.class);
        creatureControllers = new SafeArrayList<>(ICreatureController.class);
        creatureControllersByEntityId = new HashMap<>();
        processAddedEntities(creatureEntities);
    }

    @Override
    public void processTick(float tpf, double gameTime) {

        // Add new & remove old
        if (creatureEntities.applyChanges()) {
            processDeletedEntities(creatureEntities.getRemovedEntities());

            processAddedEntities(creatureEntities.getAddedEntities());
        }

        // Process ticks
        for (ICreatureController creatureController : creatureControllers.getArray()) {
            creatureController.processTick(tpf, gameTime);
        }

        // We have a specialty here, process creature worker queue
        taskManager.processUnemployedWorkerQueue();
    }

    private void processAddedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            ICreatureController creatureController = creaturesController.createController(entity.getId());
            int index = Collections.binarySearch(creatureControllers, creatureController);
            creatureControllers.add(~index, creatureController);
            creatureControllersByEntityId.put(entity.getId(), creatureController);
        }
    }

    private void processDeletedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            ICreatureController creatureController = creatureControllersByEntityId.remove(entity.getId());
            if (creatureController != null) {
                int index = Collections.binarySearch(creatureControllers, creatureController);
                creatureControllers.remove(index);
                creatureController.getStateMachine().changeState(null);
            }
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        creatureEntities.release();
        creatureControllers.clear();
        creatureControllersByEntityId.clear();
    }

}
