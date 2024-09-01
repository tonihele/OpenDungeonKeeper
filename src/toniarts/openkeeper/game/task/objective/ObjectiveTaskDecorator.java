/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.game.task.objective;

import com.jme3.math.Vector2f;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.task.Task;
import toniarts.openkeeper.game.task.TaskType;

/**
 * A decorator for some simple task to create complex task chains
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ObjectiveTaskDecorator implements ObjectiveTask {

    private final long taskId;
    private final Task task;
    private final Deque<ObjectiveTask> taskQueue = new ArrayDeque<>();

    public ObjectiveTaskDecorator(long originalTaskId, Task task) {
        this.taskId = originalTaskId;
        this.task = task;
    }

    @Override
    public Deque<ObjectiveTask> getTaskQueue() {
        return taskQueue;
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        task.executeTask(creature, executionDuration);
        ObjectiveTask.super.executeTask(creature, executionDuration);
    }

    @Override
    public boolean canExecute(ICreatureController creature) {
        return true;
    }

    @Override
    public void assign(ICreatureController creature, boolean setToCreature) {
        task.assign(creature, false);

        // Override the assign
        creature.setAssignedTask(this);
        if (isWorkerPartyTask() && creature.getParty() != null) {

            // Assign to all workers
            for (ICreatureController c : creature.getParty().getActualMembers()) {
                if (!c.equals(creature) && c.isWorker() && task.canAssign(c)) {
                    task.assign(c, true);
                }
            }
        }
    }

    @Override
    public boolean canAssign(ICreatureController creature) {
        return task.canAssign(creature);
    }

    @Override
    public int getAssigneeCount() {
        return task.getAssigneeCount();
    }

    @Override
    public int getMaxAllowedNumberOfAsignees() {
        return task.getMaxAllowedNumberOfAsignees();
    }

    @Override
    public int getPriority() {
        return task.getPriority();
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return task.getTarget(creature);
    }

    @Override
    public Instant getTaskCreated() {
        return task.getTaskCreated();
    }

    @Override
    public Point getTaskLocation() {
        return task.getTaskLocation();
    }

    @Override
    public boolean isFaceTarget() {
        return task.isFaceTarget();
    }

    @Override
    public boolean isReachable(ICreatureController creature) {
        return task.isReachable(creature);
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        return task.isValid(creature);
    }

    @Override
    public void unassign(ICreatureController creature) {
        task.unassign(creature);
    }

    @Override
    public boolean isRemovable() {
        return task.isRemovable();
    }

    @Override
    public TaskType getTaskType() {
        return task.getTaskType();
    }

    @Override
    public EntityId getTaskTarget() {
        return task.getTaskTarget();
    }

    @Override
    public long getId() {
        return taskId;
    }

    @Override
    public boolean isFull() {
        return task.isFull();
    }

}
