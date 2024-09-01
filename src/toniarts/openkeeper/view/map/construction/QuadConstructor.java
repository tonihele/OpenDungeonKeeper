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
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.map.MapViewController;

/**
 * FIXME: QuadConstructor
 *
 * @author ArchDemon
 */
public class QuadConstructor extends RoomConstructor {

    public QuadConstructor(AssetManager assetManager, RoomInstance roomInstance) {
        super(assetManager, roomInstance);
    }

    public static Node constructQuad(AssetManager assetManager, String modelName, ArtResource artResource,
            boolean N, boolean NE, boolean E, boolean SE, boolean S, boolean SW, boolean W, boolean NW) {

        return constructQuad(assetManager, modelName, artResource, 0, 0, N, NE, E, SE, S, SW, W, NW);
    }

    /**
     *
     * @param assetManager
     * @param modelName
     * @param base base index of piece (need to construct water bed)
     * @param angle base rotation yAngle of piece (need to construct water bed)
     * @param N
     * @param NE
     * @param E
     * @param SE
     * @param S
     * @param SW
     * @param W
     * @param NW
     * @return
     */
    public static Node constructQuad(AssetManager assetManager, String modelName, ArtResource artResource, int base, float angle,
            boolean N, boolean NE, boolean E, boolean SE, boolean S, boolean SW, boolean W, boolean NW) {

        Node quad = new Node();

        for (int i = 0; i < 2; i++) {
            for (int k = 0; k < 2; k++) {

                int piece = 0;
                float yAngle = 0;
                Vector3f movement;

                // Determine the piece
                if (i == 0 && k == 0) { // North west corner
                    if (N && W && NW) {
                        piece = 3;
                        yAngle = -FastMath.HALF_PI;
                    } else if (N && W && !NW) {
                        piece = 2;
                        yAngle = -FastMath.HALF_PI;
                    } else if (!N && W) { // && NW no matter
                        piece = 0;
                    } else if (N && !W) { // && NW no matter
                        piece = 0;
                        yAngle = FastMath.HALF_PI;
                    } else { // if (!N && !W)
                        piece = 1;
                        yAngle = FastMath.HALF_PI;
                    }
                    movement = new Vector3f(-WorldUtils.TILE_WIDTH / 4, 0, -WorldUtils.TILE_WIDTH / 4);
                } else if (i == 1 && k == 0) { // North east corner
                    if (N && E && NE) {
                        piece = 3;
                        yAngle = FastMath.PI;
                    } else if (N && E && !NE) {
                        piece = 2;
                        yAngle = FastMath.PI;
                    } else if (!N && E) {
                        piece = 0;
                    } else if (N && !E) {
                        piece = 0;
                        yAngle = -FastMath.HALF_PI;
                    } else {
                        piece = 1;
                    }
                    movement = new Vector3f(WorldUtils.TILE_WIDTH / 4, 0, -WorldUtils.TILE_WIDTH / 4);
                } else if (i == 0 && k == 1) { // South west corner
                    if (S && W && SW) {
                        piece = 3;
                    } else if (S && W && !SW) {
                        piece = 2;
                    } else if (!S && W) {
                        piece = 0;
                        yAngle = FastMath.PI;
                    } else if (S && !W) {
                        piece = 0;
                        yAngle = FastMath.HALF_PI;
                    } else {
                        piece = 1;
                        yAngle = FastMath.PI;
                    }
                    movement = new Vector3f(-WorldUtils.TILE_WIDTH / 4, 0, WorldUtils.TILE_WIDTH / 4);
                } else { // South east corner if (i == 1 && k == 1)
                    if (S && E && SE) {
                        piece = 3;
                        yAngle = FastMath.HALF_PI;
                    } else if (S && E && !SE) {
                        piece = 2;
                        yAngle = FastMath.HALF_PI;
                    } else if (!S && E) {
                        piece = 0;
                        yAngle = FastMath.PI;
                    } else if (S && !E) {
                        piece = 0;
                        yAngle = -FastMath.HALF_PI;
                    } else {
                        piece = 1;
                        yAngle = -FastMath.HALF_PI;
                    }
                    movement = new Vector3f(WorldUtils.TILE_WIDTH / 4, 0, WorldUtils.TILE_WIDTH / 4);
                }
                // Load the piece
                Spatial part = AssetUtils.loadModel(assetManager, modelName + (base + piece), artResource);
                part.rotate(0, angle + yAngle, 0);
                part.move(movement);

                quad.attachChild(part);
            }
        }

        return quad;
    }

    @Override
    protected BatchNode constructFloor() {
        BatchNode root = new BatchNode();
        ArtResource artResource = roomInstance.getRoom().getCompleteResource();
        String modelName = artResource.getName();
        //Point start = roomInstance.getCoordinates().get(0);
        // Contruct the tiles
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
            // 2x2
            Node model = constructQuad(assetManager, modelName, artResource, N, NE, E, SE, S, SW, W, NW);
            //AssetUtils.scale(model);
            AssetUtils.translateToTile(model, p);
            root.attachChild(model);
        }

        // Set the transform and scale to our scale and 0 the transform
        //AssetUtils.scale(root);
        //AssetUtils.moveToTile(root, start);
        return root;
    }

}
