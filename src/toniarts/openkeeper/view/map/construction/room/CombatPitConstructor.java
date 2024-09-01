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
package toniarts.openkeeper.view.map.construction.room;

import com.jme3.asset.AssetManager;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.map.construction.DoubleQuadConstructor;

/**
 * Manages combat pit door placement, currently it is decoupled from the actual
 * door. But the rules are pretty static, so... And now one so visibly uses this
 * door.
 *
 * @author ArchDemon
 */
public class CombatPitConstructor extends DoubleQuadConstructor {

    public CombatPitConstructor(AssetManager assetManager, toniarts.openkeeper.common.RoomInstance roomInstance) {
        super(assetManager, roomInstance);
    }

    @Override
    protected BatchNode constructFloor() {
        BatchNode root = new BatchNode();
        ArtResource artResource = roomInstance.getRoom().getCompleteResource();
        String modelName = artResource.getName();

        // Contruct the tiles
        boolean door = false;
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

                if (!door && southInside) {

                    // This is true, the door is always like this, it might not look correct visually (the opposite quads of the door...) but it is
                    Spatial part = AssetUtils.loadModel(assetManager, modelName + "14", artResource);
                    AssetUtils.translateToTile(part, new Point(x, y));
                    part.move(-WorldUtils.TILE_WIDTH / 4, 0, -WorldUtils.TILE_WIDTH / 4);

                    root.attachChild(part);

                    door = true;
                    continue;
                }

                Node model = constructQuad(assetManager, modelName, N, NE, E, SE, S, SW, W, NW,
                        northWestInside, northEastInside, southWestInside, southEastInside,
                        northInside, eastInside, southInside, westInside);
                AssetUtils.translateToTile(model, new Point(x, y));
                root.attachChild(model);
            }
        }

        AssetUtils.translateToTile(root, start);

        return root;
    }
}
