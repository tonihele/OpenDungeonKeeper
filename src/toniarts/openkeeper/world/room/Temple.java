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
import java.awt.Point;

import toniarts.openkeeper.utils.RoomUtils;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.effect.EffectManagerState;
import toniarts.openkeeper.world.object.ObjectLoader;

/**
 *
 * @author ArchDemon
 */
public class Temple extends DoubleQuad {

    private static final short OBJECT_TEMPLE_HAND_ID = 66;

    public Temple(AssetManager assetManager, RoomInstance roomInstance, ObjectLoader objectLoader, WorldState worldState, EffectManagerState effectManager) {
        super(assetManager, roomInstance, objectLoader, worldState, effectManager);
    }

    @Override
    protected BatchNode constructFloor() {
        BatchNode root = new BatchNode();
        String modelName = roomInstance.getRoom().getCompleteResource().getName();
        //Point start = roomInstance.getCoordinates().get(0);
        // Water
        boolean[][] waterArea = RoomUtils.calculateWaterArea(roomInstance.getCoordinatesAsMatrix());


        // Hand
        boolean drawHand = RoomUtils.matrixContainsSquare(roomInstance.getCoordinatesAsMatrix(), 5);
        if(drawHand) {

            final Point topLeft = new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
            final Point bottomRight = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);

            for(int i = 0; i < waterArea.length; ++i) {
                for(int j = 0; j < waterArea[0].length; ++j) {
                    if(waterArea[i][j]) {
                        topLeft.x = Math.min(i, topLeft.x);
                        topLeft.y = Math.min(j, topLeft.y);
                        bottomRight.x = Math.max(i, bottomRight.x);
                        bottomRight.y = Math.max(j, bottomRight.y);
                    }
                }
            }

            Point centre = new Point((topLeft.x + bottomRight.x) / 2, (topLeft.y + bottomRight.y) / 2);
            Spatial part = objectLoader.load(assetManager, centre.x, centre.y, OBJECT_TEMPLE_HAND_ID, roomInstance.getOwnerId());
            part.move(0, -3 * MapLoader.FLOOR_HEIGHT / 2, MapLoader.TILE_WIDTH / 4);
            root.attachChild(part);
        }

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

            Node model = DoubleQuad.constructQuad(assetManager, modelName, N, NE, E, SE, S, SW, W, NW);
            moveSpatial(model, p);
            root.attachChild(model);
        }

        return root;
    }
}
