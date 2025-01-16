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
package toniarts.openkeeper.view.map.construction.room;

import com.jme3.asset.AssetManager;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Node;
import toniarts.openkeeper.utils.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import toniarts.openkeeper.common.EntityInstance;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.view.map.Water;
import toniarts.openkeeper.view.map.construction.DoubleQuadConstructor;
import static toniarts.openkeeper.view.map.construction.DoubleQuadConstructor.constructQuad;

/**
 * Manages temple construction. Mainly adds the water body
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class TempleConstructor extends DoubleQuadConstructor {

    private final KwdFile kwdFile;

    public TempleConstructor(AssetManager assetManager, toniarts.openkeeper.common.RoomInstance roomInstance, KwdFile kwdFile) {
        super(assetManager, roomInstance);

        this.kwdFile = kwdFile;
    }

    @Override
    protected BatchNode constructFloor() {
        BatchNode root = new BatchNode();
        String modelName = roomInstance.getRoom().getCompleteResource().getName();

        Set<Point> waterTiles = new HashSet<>();
        List<EntityInstance<Terrain>> waterBodies = new ArrayList<>();
        for (int y = 0; y < map[0].length; y++) {
            for (int x = 0; x < map.length; x++) {

                // Skip non-room tiles
                if (!roomInstance.getCoordinatesAsMatrix()[x][y]) {
                    continue;
                }

                // Figure out which peace by seeing the neighbours
                boolean N = hasSameTile(map, x, y - 1);
                boolean NE = hasSameTile(map, x + 1, y - 1);
                boolean E = hasSameTile(map, x + 1, y);
                boolean SE = hasSameTile(map, x + 1, y + 1);
                boolean S = hasSameTile(map, x, y + 1);
                boolean SW = hasSameTile(map, x - 1, y + 1);
                boolean W = hasSameTile(map, x - 1, y);
                boolean NW = hasSameTile(map, x - 1, y - 1);

                boolean northInside = isTileInside(map, x, y - 1);
                boolean northEastInside = isTileInside(map, x + 1, y - 1);
                boolean eastInside = isTileInside(map, x + 1, y);
                boolean southEastInside = isTileInside(map, x + 1, y + 1);
                boolean southInside = isTileInside(map, x, y + 1);
                boolean southWestInside = isTileInside(map, x - 1, y + 1);
                boolean westInside = isTileInside(map, x - 1, y);
                boolean northWestInside = isTileInside(map, x - 1, y - 1);

                // Flood fill the water pools, there can be several in a temple
                boolean inside = N && NE && NW && E && SE && S && SW && W && NW;
                if (inside && !waterTiles.contains(new Point(x, y))) {
                    EntityInstance<Terrain> entityInstance = new EntityInstance<>(kwdFile.getMap().getWater());
                    findInsideBatch(new Point(x, y), entityInstance, waterTiles);
                    waterBodies.add(entityInstance);
                }

                Node model = constructQuad(assetManager, modelName, N, NE, E, SE, S, SW, W, NW,
                        northWestInside, northEastInside, southWestInside, southEastInside,
                        northInside, eastInside, southInside, westInside);
                AssetUtils.translateToTile(model, new Point(x, y));
                root.attachChild(model);
            }
        }

        // Add the actual water pools
        if (!waterBodies.isEmpty()) {
            root.attachChild(Water.construct(assetManager, waterBodies));
        }

        AssetUtils.translateToTile(root, start);

        return root;
    }

    /**
     * Find a terrain batch starting from a certain point, they are never
     * diagonally attached
     *
     * @param p starting point
     * @param entityInstance the batch instance
     */
    private void findInsideBatch(Point p, EntityInstance<Terrain> entityInstance, Set<Point> terrainBatchCoordinates) {
        if (!terrainBatchCoordinates.contains(p)) {
            boolean N = hasSameTile(map, p.x, p.y - 1);
            boolean NE = hasSameTile(map, p.x + 1, p.y - 1);
            boolean E = hasSameTile(map, p.x + 1, p.y);
            boolean SE = hasSameTile(map, p.x + 1, p.y + 1);
            boolean S = hasSameTile(map, p.x, p.y + 1);
            boolean SW = hasSameTile(map, p.x - 1, p.y + 1);
            boolean W = hasSameTile(map, p.x - 1, p.y);
            boolean NW = hasSameTile(map, p.x - 1, p.y - 1);
            boolean inside = N && NE && NW && E && SE && S && SW && W && NW;
            if (inside) {

                // Add the coordinate
                terrainBatchCoordinates.add(p);
                entityInstance.addCoordinate(p);

                // Find north
                findInsideBatch(new Point(p.x, p.y - 1), entityInstance, terrainBatchCoordinates);

                // Find east
                findInsideBatch(new Point(p.x + 1, p.y), entityInstance, terrainBatchCoordinates);

                // Find south
                findInsideBatch(new Point(p.x, p.y + 1), entityInstance, terrainBatchCoordinates);

                // Find west
                findInsideBatch(new Point(p.x - 1, p.y), entityInstance, terrainBatchCoordinates);
            }
        }
    }

}
