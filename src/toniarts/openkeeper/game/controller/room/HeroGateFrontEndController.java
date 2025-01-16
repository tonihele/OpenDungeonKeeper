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
package toniarts.openkeeper.game.controller.room;

import com.jme3.math.FastMath;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.tools.convert.map.KwdFile;

/**
 * Loads up a hero gate, front end edition. Main menu. Most of the objects are
 * listed in the objects, but I don't see how they help<br>
 * TODO: Effect on the gem holder & lightning, controls for the level selection
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class HeroGateFrontEndController extends NormalRoomController {

    private static final short OBJECT_BANNER_ONE_ID = 89;
    //private static final int OBJECT_CANDLE_STICK_ID = 110;  // empty resource?
    //private static final int OBJECT_ARROW_ID = 115;  // empty resource?
    private static final short OBJECT_GEM_HOLDER_ID = 131;
    private static final short OBJECT_CHAIN_ID = 132;
    private static final short OBJECT_BANNER_TWO_ID = 134;
    private static final short OBJECT_BANNER_THREE_ID = 135;
    private static final short OBJECT_BANNER_FOUR_ID = 136;

    public HeroGateFrontEndController(KwdFile kwdFile, toniarts.openkeeper.common.RoomInstance roomInstance, IObjectsController objectsController) {
        super(kwdFile, roomInstance, objectsController);
    }

    @Override
    protected void constructObjects() {
        int i = 1;
        Point start = roomInstance.getCoordinates().get(0);
        for (Point p : roomInstance.getCoordinates()) {

            // Add some objects according to the tile number
            switch (i) {
                case 2:
                    floorFurniture.add(objectsController.loadObject(OBJECT_GEM_HOLDER_ID, roomInstance.getOwnerId(), p.x, p.y));

                    // Banners
                    floorFurniture.add(objectsController.loadObject(OBJECT_BANNER_ONE_ID, roomInstance.getOwnerId(), p.x, p.y));
                    floorFurniture.add(objectsController.loadObject(OBJECT_BANNER_TWO_ID, roomInstance.getOwnerId(), p.x, p.y));

                    // The "candles"
                    addCandles(p);
                    break;
                case 5:

                    // Banners
                    floorFurniture.add(objectsController.loadObject(OBJECT_BANNER_THREE_ID, roomInstance.getOwnerId(), p.x, p.y));
                    floorFurniture.add(objectsController.loadObject(OBJECT_BANNER_FOUR_ID, roomInstance.getOwnerId(), p.x, p.y));

                    // The "candles"
                    addCandles(p);

                    break;
                case 8:

                    // Banners
                    floorFurniture.add(objectsController.loadObject(OBJECT_BANNER_ONE_ID, roomInstance.getOwnerId(), p.x, p.y));
                    floorFurniture.add(objectsController.loadObject(OBJECT_BANNER_TWO_ID, roomInstance.getOwnerId(), p.x, p.y));

                    // The "candles"
                    addCandles(p);

                    break;
                case 11:

                    // Banners
                    floorFurniture.add(objectsController.loadObject(OBJECT_BANNER_ONE_ID, roomInstance.getOwnerId(), p.x, p.y));
                    floorFurniture.add(objectsController.loadObject(OBJECT_BANNER_TWO_ID, roomInstance.getOwnerId(), p.x, p.y));

                    // The "candles"
                    addCandles(p);

                    break;
            }

            i++;
        }
    }

    private void addCandles(Point p) {

        // The "candles"
        floorFurniture.add(objectsController.loadObject(OBJECT_CHAIN_ID, roomInstance.getOwnerId(), p.x - 1, p.y));
        floorFurniture.add(objectsController.loadObject(OBJECT_CHAIN_ID, roomInstance.getOwnerId(), p.x + 1, p.y + 1, -FastMath.PI));
    }
}
