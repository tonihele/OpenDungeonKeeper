/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.world.room;

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Spatial;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.world.ThingLoader;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.creature.CreatureControl;
import toniarts.openkeeper.world.effect.EffectManagerState;
import toniarts.openkeeper.world.object.ObjectLoader;

/**
 * Portal is the only one I think
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Deprecated
public class ThreeByThree extends GenericRoom implements ICreatureEntrance {

    private final List<CreatureControl> attractedCreatures = new ArrayList<>();

    public ThreeByThree(AssetManager assetManager, RoomInstance roomInstance, ObjectLoader objectLoader, WorldState worldState, EffectManagerState effectManager) {
        super(assetManager, roomInstance, objectLoader, worldState, effectManager);
    }

    @Override
    protected BatchNode constructFloor() {
        BatchNode root = new BatchNode();
        // 3 by 3, a simple case
        int i = 0;
        Point start = roomInstance.getCoordinates().get(0);
        for (Point p : roomInstance.getCoordinates()) {
            Spatial tile = (Spatial) AssetUtils.loadModel(assetManager, roomInstance.getRoom().getCompleteResource().getName() + i, null, false, true);

            moveSpatial(tile, start, p);

            root.attachChild(tile);
            i++;
        }

        // Set the transform and scale to our scale and 0 the transform
        //AssetUtils.scale(root);
        AssetUtils.translateToTile(root, start);

        return root;
    }

    @Override
    public boolean isTileAccessible(Integer fromX, Integer fromY, int toX, int toY) {

        // The center tile is not accessible
        Point roomPoint = roomInstance.worldCoordinateToLocalCoordinate(toX, toY);
        return !(roomPoint.x == 1 && roomPoint.y == 1);
    }

    @Override
    public String getTooltip(short playerId) {
        String result = super.getTooltip(playerId);

        return result.replaceAll("%42", Integer.toString(attractedCreatures.size())) // Creatures attracted
                .replaceAll("%43", Integer.toString(getMaxCapacity()));  // FIXME Creatures attracted Max;
    }

    @Override
    public Point getEntranceCoordinate() {
        return roomInstance.getCenter();
    }

    @Override
    public List<CreatureControl> getAttractedCreatures() {
        // FIXME if creature die ?
        return attractedCreatures;
    }

    @Override
    public CreatureControl spawnCreature(short creatureId, short level, Application app, ThingLoader thingLoader) {

//        CreatureControl creature = CreatureSpawnLogicState.spawnCreature(creatureId,
//                roomInstance.getOwnerId(), (short) 1, app, thingLoader, getEntranceCoordinate(), true);
//        attractedCreatures.add(creature);
//
//        Vector3f effectPos = WorldUtils.pointToVector3f(roomInstance.getCenter());
//        effectManager.load(getRootNode(), effectPos,
//                creature.getCreature().getEntranceEffectId(), false);
//
//        return creature;
        return null;
    }

    @Override
    public CreatureControl spawnCreature(short creatureId, Application app, ThingLoader thingLoader) {
        return spawnCreature(creatureId, (short) 1, app, thingLoader);
    }
}
