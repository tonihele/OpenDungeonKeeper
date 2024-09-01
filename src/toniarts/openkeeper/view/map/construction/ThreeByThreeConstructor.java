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
package toniarts.openkeeper.view.map.construction;

import com.jme3.asset.AssetManager;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.tools.convert.map.ArtResource;

/**
 * Portal is the only one I think
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ThreeByThreeConstructor extends RoomConstructor {

    public ThreeByThreeConstructor(AssetManager assetManager, RoomInstance roomInstance) {
        super(assetManager, roomInstance);
    }

    @Override
    protected BatchNode constructFloor() {
        BatchNode root = new BatchNode();
        // 3 by 3, a simple case
        int i = 0;
        Point start = roomInstance.getCoordinates().get(0);
        ArtResource artResource = roomInstance.getRoom().getCompleteResource();
        for (Point p : roomInstance.getCoordinates()) {
            Spatial tile = (Spatial) AssetUtils.loadModel(assetManager, artResource.getName() + i, artResource, false, true);

            moveSpatial(tile, start, p);

            root.attachChild(tile);
            i++;
        }

        // Set the transform and scale to our scale and 0 the transform
        //AssetUtils.scale(root);
        AssetUtils.translateToTile(root, start);

        return root;
    }

}
