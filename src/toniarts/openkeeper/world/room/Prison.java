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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.effect.EffectManagerState;
import toniarts.openkeeper.world.object.ObjectLoader;
import toniarts.openkeeper.world.room.control.RoomPrisonerControl;

/**
 * TODO: not completed
 *
 * @author ArchDemon
 */
@Deprecated
public class Prison extends DoubleQuad {

    private static final short OBJECT_DOOR_ID = 109;
    private static final short OBJECT_DOORBAR_ID = 116;

    private Point door;

    public Prison(AssetManager assetManager, RoomInstance roomInstance, ObjectLoader objectLoader, WorldState worldState, EffectManagerState effectManager) {
        super(assetManager, roomInstance, objectLoader, worldState, effectManager);

        addObjectControl(new RoomPrisonerControl(this) {
            @Override
            protected Collection<Point> getCoordinates() {

                // Only the innards of prison can hold objects
                return Prison.this.getInsideCoordinates();
            }

            @Override
            protected int getNumberOfAccessibleTiles() {
                return getCoordinates().size();
            }

        });
    }

    @Override
    protected BatchNode constructFloor() {
        BatchNode root = new BatchNode();
        String modelName = roomInstance.getRoom().getCompleteResource().getName();
        //Point start = roomInstance.getCoordinates().get(0);

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
                Spatial part = objectLoader.load(assetManager, p.x, p.y, OBJECT_DOOR_ID, roomInstance.getOwnerId());
                part.setBatchHint(Spatial.BatchHint.Never);
//                part.addControl(new DoorControl(worldState.getMapData().getTile(p),
//                        null, worldState.getLevelData().getObject(OBJECT_DOORBAR_ID),
//                        null, worldState, assetManager));
                root.attachChild(part);

                part = objectLoader.load(assetManager, p.x, p.y, OBJECT_DOORBAR_ID, roomInstance.getOwnerId());
                part.setBatchHint(Spatial.BatchHint.Never);
                root.attachChild(part);

                part = AssetUtils.loadModel(assetManager, modelName + "14", null);
                part.move(-MapLoader.TILE_WIDTH / 4, 0, -MapLoader.TILE_WIDTH / 4);
                moveSpatial(part, p);

                root.attachChild(part);
                door = p;
                continue;
            }

            Node model = DoubleQuad.constructQuad(assetManager, modelName, N, NE, E, SE, S, SW, W, NW);
            moveSpatial(model, p);
            root.attachChild(model);
        }

        return root;
    }

    @Override
    public boolean isTileAccessible(Integer fromX, Integer fromY, int toX, int toY) {

        // Prison is all accessible, but you do have to use the door!
        if (door != null && fromX != null && fromY != null) {

            // Path finding
            Set<Point> insides = new HashSet<>(getInsideCoordinates());
            Point from = new Point(fromX, fromY);
            Point to = new Point(toX, toY);
            if (insides.contains(to) && insides.contains(from)) {
                return true; // Inside the prison == free movement
            } else if (insides.contains(from) && to.equals(door)) {
                return true; // From inside also through the door
            } else if (insides.contains(to) && from.equals(door)) {
                return true; // Path finding, from outside
            } else if (!insides.contains(to) && !insides.contains(from)) {
                return true; // Outside the prison == free movement
            }
            return false;
        }
        if (fromX == null && fromY == null && door != null) {
            return true; // We have a door, the tile is accessible
        }
        return super.isTileAccessible(fromX, fromY, toX, toY);
    }

    private Collection<Point> getInsideCoordinates() {
        boolean[][] matrix = roomInstance.getCoordinatesAsMatrix();
        List<Point> coordinates = new ArrayList<>();
        for (int x = 1; x < matrix.length - 1; x++) {
            for (int y = 1; y < matrix[x].length - 1; y++) {
                boolean N = hasSameTile(map, x, y + 1);
                boolean NE = hasSameTile(map, x - 1, y + 1);
                boolean E = hasSameTile(map, x - 1, y);
                boolean SE = hasSameTile(map, x - 1, y - 1);
                boolean S = hasSameTile(map, x, y - 1);
                boolean SW = hasSameTile(map, x + 1, y - 1);
                boolean W = hasSameTile(map, x + 1, y);
                boolean NW = hasSameTile(map, x + 1, y + 1);
                if (N && NE && E && SE && S && SW && W && NW) {
                    coordinates.add(new Point(roomInstance.getMatrixStartPoint().x + x, roomInstance.getMatrixStartPoint().y + y));
                }
            }
        }
        return coordinates;
    }

    @Override
    public String getTooltip(short playerId) {
        String result = super.getTooltip(playerId);

        return result;//.replaceAll("%55", ) // TODO Skeleton Animated
                //.replaceAll("%56", ) // TODO Skeleton Animated Max
                //.replaceAll("%56", ) // TODO Prison status
    }
}
