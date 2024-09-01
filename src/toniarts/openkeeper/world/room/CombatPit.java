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

import com.jme3.asset.AssetManager;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.AssetUtils;
import static toniarts.openkeeper.world.MapLoader.TILE_WIDTH;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.effect.EffectManagerState;
import toniarts.openkeeper.world.object.ObjectLoader;

/**
 * TODO: not completed
 *
 * @author ArchDemon
 */
@Deprecated
public class CombatPit extends DoubleQuad {

    private Point door;

    public CombatPit(AssetManager assetManager, RoomInstance roomInstance, ObjectLoader objectLoader, WorldState worldState, EffectManagerState effectManager) {
        super(assetManager, roomInstance, objectLoader, worldState, effectManager);
    }

    @Override
    protected BatchNode constructFloor() {
        BatchNode root = new BatchNode();
        String modelName = roomInstance.getRoom().getCompleteResource().getName();
        //Point start = roomInstance.getCoordinates().get(0);
        // Contruct the tiles
        door = null;
        for (Point p : roomInstance.getCoordinates()) {
            // Figure out which peace by seeing the neighbours
            boolean N = roomInstance.hasCoordinate(new Point(p.x, p.y - 1));
            boolean NE = roomInstance.hasCoordinate(new Point(p.x + 1, p.y - 1));
            boolean E = roomInstance.hasCoordinate(new Point(p.x + 1, p.y));
            boolean SE = roomInstance.hasCoordinate(new Point(p.x + 1, p.y + 1));
            boolean S = roomInstance.hasCoordinate(new Point(p.x, p.y + 1));
            boolean SW = roomInstance.hasCoordinate(new Point(p.x - 1, p.y + 1));
            boolean W = roomInstance.hasCoordinate(new Point(p.x - 1, p.y));
            boolean NW = roomInstance.hasCoordinate(new Point(p.x - 1, p.y - 1));

            if (door == null && !N && !NE && E && SE && S && SW && W && !NW) {
                Spatial part = AssetUtils.loadModel(assetManager, modelName + "14", null);
                part.move(-TILE_WIDTH / 4, 0, -TILE_WIDTH / 4);
                moveSpatial(part, p);

                door = p;
                root.attachChild(part);
                continue;
            }

            Node model = DoubleQuad.constructQuad(assetManager, modelName, N, NE, E, SE, S, SW, W, NW);
            moveSpatial(model, p);
            root.attachChild(model);
        }

        // Set the transform and scale to our scale and 0 the transform
        //AssetUtils.moveToTile(root, start);

        return root;
    }
}
