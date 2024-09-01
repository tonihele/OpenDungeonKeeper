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
package toniarts.openkeeper.game.task.creature;

import com.jme3.math.Vector2f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.game.component.Food;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractTask;
import toniarts.openkeeper.game.task.TaskType;

/**
 * Go to eat!
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class GoToEat extends AbstractTask {

    private boolean executed = false;
    private final IEntityPositionLookup entityPositionLookup;
    /**
     * The food entity
     */
    private final EntityId target;
    private final EntityData entityData;
    private final ICreatureController creature;


    public GoToEat(final INavigationService navigationService, final IMapController mapController, final IEntityPositionLookup entityPositionLookup,
            EntityId target, EntityData entityData, ICreatureController creature) {
        super(navigationService, mapController);

        this.entityPositionLookup = entityPositionLookup;
        this.target = target;
        this.entityData = entityData;
        this.creature = creature;
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        return !executed && getTaskLocation() != null && entityData.getComponent(target, Food.class) != null && this.creature.isHungry();
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        Position pos = entityData.getComponent(target, Position.class);
        if (pos != null) {
            return new Vector2f(pos.position.x, pos.position.z);
        }
        return null;
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        creature.eat(entityPositionLookup.getEntityController(target));

        // This is a one timer
        executed = true;
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.GO_TO_EAT;
    }

    @Override
    public Point getTaskLocation() {
        IMapTileInformation tile = entityPositionLookup.getEntityLocation(target);
        if (tile != null) {
            return tile.getLocation();
        }

        return null;
    }

}
