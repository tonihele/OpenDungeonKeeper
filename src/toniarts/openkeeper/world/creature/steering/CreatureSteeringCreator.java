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
package toniarts.openkeeper.world.creature.steering;

import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.ai.steer.SteeringBehavior;
import com.badlogic.gdx.ai.steer.behaviors.FollowPath;
import com.badlogic.gdx.ai.steer.behaviors.PrioritySteering;
import com.badlogic.gdx.ai.steer.behaviors.ReachOrientation;
import com.badlogic.gdx.ai.steer.utils.paths.LinePath;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.world.TileData;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.creature.CreatureControl;
import toniarts.openkeeper.world.pathfinding.PathFindable;

/**
 * A steering factory, static methods for constructing steerings for creatures
 *
 * @see toniarts.openkeeper.game.navigation.steering.EntitySteeringFactory
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Deprecated
public class CreatureSteeringCreator {

    private CreatureSteeringCreator() {
    }

    public static SteeringBehavior<Vector2> navigateToPoint(final WorldState worldState, final PathFindable pathFindable, final CreatureControl creature, final Point p) {
        return navigateToPoint(worldState, pathFindable, creature, p, null);
    }

    public static SteeringBehavior<Vector2> navigateToPoint(final WorldState worldState, final PathFindable pathFindable, final CreatureControl creature, final Point p, final Vector2 faceTarget) {
        GraphPath<TileData> outPath = worldState.findPath(WorldUtils.vectorToPoint(creature.getSpatial().getWorldTranslation()), p, pathFindable);
        return navigateToPoint(outPath, faceTarget, creature, p);
    }

    public static SteeringBehavior<Vector2> navigateToPoint(GraphPath<TileData> outPath, final Vector2 faceTarget, final CreatureControl creature, final Point p) {
        if ((outPath != null && outPath.getCount() > 1) || faceTarget != null) {

            PrioritySteering<Vector2> prioritySteering = new PrioritySteering(creature);

            if (outPath != null && outPath.getCount() > 1) {
                // Add regular avoidance
//                CollisionAvoidance<Vector2> ca = new CollisionAvoidance<>(creature, new ProximityBase<Vector2>(creature, null) {
//
//                    @Override
//                    public int findNeighbors(Proximity.ProximityCallback<Vector2> callback) {
//                        List<CreatureControl> creatures = new ArrayList<>(creature.getVisibleCreatures());
//                        int neighborCount = 0;
//                        int agentCount = creatures.size();
//                        for (int i = 0; i < agentCount; i++) {
//                            Steerable<Vector2> currentAgent = creatures.get(i);
//
//                            // Skip if this is us, when sharing collission avoidance i.e. this can contain us
//                            if (!currentAgent.equals(owner)) {
//                                if (callback.reportNeighbor(currentAgent)) {
//                                    neighborCount++;
//                                }
//                            }
//                        }
//
//                        return neighborCount;
//                    }
//                });
//                prioritySteering.add(ca);

                // Navigate
                FollowPath<Vector2, LinePath.LinePathParam> followPath = new FollowPath(creature,
                        new LinePath<>(pathToArray(outPath), true), 0.2f);
                followPath.setDecelerationRadius(0.2f);
                followPath.setArrivalTolerance(0.1f);
                prioritySteering.add(followPath);
            }

            if (faceTarget != null) {

                // Add reach orientation
                ReachOrientation orient = new ReachOrientation(creature,
                        new TargetLocation(faceTarget,
                                WorldUtils.pointToVector2(p)));
                orient.setDecelerationRadius(0.2f);
                orient.setTimeToTarget(0.001f);
                orient.setAlignTolerance(0.1f);
                prioritySteering.add(orient);
            }

            return prioritySteering;
        }

        return null;
    }

    private static Array<Vector2> pathToArray(GraphPath<TileData> outPath) {
        Array<Vector2> path = new Array<>(outPath.getCount());
        for (TileData tile : outPath) {
            path.add(WorldUtils.pointToVector2(tile.getLocation()));
        }
        return path;
    }
}
