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
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractTileTask;
import toniarts.openkeeper.game.task.TaskType;

/**
 * Dig a tile task, for workers
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class DigTileTask extends AbstractTileTask {

    public DigTileTask(final INavigationService navigationService, final IMapController mapController, Point p, short playerId) {
        super(navigationService, mapController, p, playerId);
    }

    @Override
    public int getMaxAllowedNumberOfAsignees() {
        // TODO: I think it is 3 per accessible side, but we would need the map data for this
        return 3;
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return getAccessibleTargetNextToLocation(creature);
    }

    @Override
    public boolean isReachable(ICreatureController creature) {
        return (getTarget(creature) != null); // To avoid multiple path finds
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        IMapTileInformation tile = mapController.getMapData().getTile(getTaskLocation());
        return tile.isSelected(playerId);
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public String toString() {
        return "Dig tile at " + getTaskLocation();
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
//        if (creature.getMeleeAttack().execute()) {
        // TODO: is this a general case or even smart to do this like this...?
        if (executionDuration - getExecutionDuration(creature) >= 1.0f) {
            setExecutionDuration(creature, executionDuration - getExecutionDuration(creature));

            creature.addGold(mapController.damageTile(getTaskLocation(), playerId, creature));
        }
//        }
    }

    @Override
    public boolean isFaceTarget() {
        return true;
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.DIG_TILE;
    }

}
