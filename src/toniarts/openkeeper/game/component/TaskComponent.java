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
package toniarts.openkeeper.game.component;

import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.game.task.TaskType;

/**
 * Simple task component. States that the entity is on a mission
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class TaskComponent implements EntityComponent {

    public long taskId;
    public EntityId targetEntity;
    public Point targetLocation;
    public TaskType taskType;

    /**
     * How long we have contributed to the task
     */
    public float taskDuration;
    public boolean taskStarted;

    public TaskComponent() {
        // For serialization
    }

    public TaskComponent(long taskId, EntityId targetEntity, Point targetLocation, TaskType taskType, float taskDuration, boolean taskStarted) {
        this.taskId = taskId;
        this.targetEntity = targetEntity;
        this.targetLocation = targetLocation;
        this.taskType = taskType;
        this.taskDuration = taskDuration;
        this.taskStarted = taskStarted;
    }

}
