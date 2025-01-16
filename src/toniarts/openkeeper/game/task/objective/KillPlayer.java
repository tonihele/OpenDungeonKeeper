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

import com.badlogic.gdx.ai.pfa.GraphPath;
import com.jme3.math.Vector2f;
import java.util.Iterator;
import java.util.List;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Kill player objective for those goodly heroes. Can create a complex set of
 * tasks
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class KillPlayer extends AbstractObjectiveTask {

    protected final short targetPlayerId;
    protected final ICreatureController creature;
    protected final ILevelInfo levelInfo;

    public KillPlayer(final INavigationService navigationService, final IMapController mapController, final ILevelInfo levelInfo, short targetPlayerId, ICreatureController creature) {
        super(navigationService, mapController, levelInfo.getPlayer(targetPlayerId).getDungeonHeartLocation(), creature.getOwnerId());

        this.targetPlayerId = targetPlayerId;
        this.creature = creature;
        this.levelInfo = levelInfo;
        createSubTasks();
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        if (!isPlayerDestroyed() && creature != null && !getTaskQueue().isEmpty()) {

            // Check that the objectives are still the same
            return Thing.HeroParty.Objective.KILL_PLAYER.equals(creature.getObjective()) && Short.valueOf(targetPlayerId).equals(creature.getObjectiveTargetPlayerId());
        }
        return !isPlayerDestroyed() && !getTaskQueue().isEmpty();
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        Point location = creature.getCreatureCoordinates();
        List<Point> points = mapController.getRoomControllerByCoordinates(getTaskLocation()).getRoomInstance().getCoordinates();
        Point result = points.get(0);
        double temp = result.distance(location);

        for (Point point : points) {
            // TODO check is point accessible by creature?
            if (point.distance(location) < temp) {
                result = point;
            }
        }

        return new Vector2f(result.x, result.y);
//        return WorldUtils.pointToVector2f(getTaskLocation()); // FIXME 0.5f not needed?
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {

    }

    private boolean isPlayerDestroyed() {
        return levelInfo.getPlayer(targetPlayerId).isDestroyed();
    }

    private void createSubTasks() {

        // See if we can navigate there
        GraphPath<IMapTileInformation> outPath = navigationService.findPath(creature.getCreatureCoordinates(), WorldUtils.vectorToPoint(getTarget(creature)), creature.getParty() != null ? creature.getParty() : creature);
        if (outPath != null) {
            Iterator<IMapTileInformation> iter = outPath.iterator();
            IMapTileInformation lastPoint = null;
            boolean first = true;
            int i = 0;
            while (iter.hasNext()) {
                IMapTileInformation tile = iter.next();
                if (!navigationService.isAccessible(lastPoint, tile, creature)) {

                    // Add task to last accessible point
                    if (i != 1 && first && lastPoint != null) {
                        addSubTask(new ObjectiveTaskDecorator(getId(), new GoToTask(navigationService, mapController, lastPoint.getLocation(), playerId)));
                        first = false;
                    }

                    // See if we should dig
                    Terrain terrain = levelInfo.getLevelData().getTerrain(tile.getTerrainId());
                    if (terrain.getFlags().contains(Terrain.TerrainFlag.SOLID) && (creature.isWorker() || (creature.getParty() != null && creature.getParty().isWorkersAvailable())) && (terrain.getFlags().contains(Terrain.TerrainFlag.DWARF_CAN_DIG_THROUGH) || terrain.getFlags().contains(Terrain.TerrainFlag.ATTACKABLE))) {
                        addSubTask(new ObjectiveTaskDecorator(getId(), new ObjectiveDigTileTask(navigationService, mapController, tile.getLocation(), playerId)) {

                            @Override
                            public boolean isWorkerPartyTask() {
                                return true;
                            }

                        });
                        addSubTask(new ObjectiveTaskDecorator(getId(), new GoToTask(navigationService, mapController, tile.getLocation(), playerId)));
                    } else {

                        // Hmm, this is it, should we have like attack target type tasks? Or let the AI just figure out itself
                        return;
                    }
                } else {
                    first = true;
                }
                lastPoint = tile;
                i++;
            }
        }
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.KILL_PLAYER;
    }

}
