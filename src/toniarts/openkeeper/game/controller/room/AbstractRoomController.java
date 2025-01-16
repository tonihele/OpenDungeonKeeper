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

import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Room;

/**
 * Base class for all rooms
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class AbstractRoomController implements IRoomController {

    /**
     * The type of object the room houses
     */
    public enum ObjectType {

        GOLD, LAIR, SPELL_BOOK, SPECIAL, RESEARCHER, PRISONER, TORTUREE, FOOD, TRAINEE;

    };

    /**
     * How the objects are laid out, there is always a 1 tile margin from the
     * sides. I don't know where this information really is, so I hardcoded it.
     */
    public enum RoomObjectLayout {

        /**
         * Allow side-to-side, every tile
         */
        ALLOW_NEIGHBOUR,
        /**
         * Allow a neighbouring object diagonally
         */
        ALLOW_DIAGONAL_NEIGHBOUR_ONLY,
        /**
         * No touching!
         */
        ISOLATED;
    }

    protected final KwdFile kwdFile;
    protected final RoomInstance roomInstance;
    private ObjectType defaultObjectType;
    private final Map<ObjectType, IRoomObjectControl> objectControls = new HashMap<>();
    protected boolean destroyed = false;
    protected boolean[][] map;
    protected Point start;
    protected final IObjectsController objectsController;
    protected final Set<EntityId> floorFurniture = new HashSet<>();
    protected final Set<EntityId> wallFurniture = new HashSet<>();
    private final Set<EntityId> pillars;

    public AbstractRoomController(KwdFile kwdFile, RoomInstance roomInstance, IObjectsController objectsController) {
        this.kwdFile = kwdFile;
        this.roomInstance = roomInstance;
        this.objectsController = objectsController;
        if (hasPillars()) {
            pillars = new HashSet<>();
        } else {
            pillars = Collections.emptySet();
        }
    }

    protected void setupCoordinates() {
        map = roomInstance.getCoordinatesAsMatrix();
        start = roomInstance.getMatrixStartPoint();
    }

    @Override
    public void construct() {
        setupCoordinates();

        // Construct the room objects
        removeObjects();
        constructObjects();
        if (hasPillars()) {
            pillars.addAll(constructPillars());
        }
    }

    private boolean hasPillars() {
        return getPillarObject(roomInstance.getEntity().getRoomId()) != null;
    }

    /**
     * Construct room pillars. Info in:
     * https://github.com/tonihele/OpenKeeper/issues/116
     *
     * @return the list of pillars constructed
     */
    protected List<EntityId> constructPillars() {
        // TODO: Maybe replace with something similar than the object placement ENUM, there are only few different scenarios of contructing the pillars
        return Collections.emptyList();
    }

    /**
     * Construct room objects
     */
    protected void constructObjects() {

        // Floor objects 0-2
        Room room = roomInstance.getRoom();
        int index = -1;
        if (room.getObjects().get(0) > 0 || room.getObjects().get(1) > 0 || room.getObjects().get(2) > 0) {

            // Object map
            boolean[][] objectMap = new boolean[map.length][map[0].length];

            for (int x = 0; x < map.length; x++) {
                for (int y = 0; y < map[x].length; y++) {

                    // Skip non-room tiles
                    if (!map[x][y]) {
                        continue;
                    }

                    // See neighbouring tiles
                    boolean N = hasSameTile(map, x, y - 1);
                    boolean NE = hasSameTile(map, x + 1, y - 1);
                    boolean E = hasSameTile(map, x + 1, y);
                    boolean SE = hasSameTile(map, x + 1, y + 1);
                    boolean S = hasSameTile(map, x, y + 1);
                    boolean SW = hasSameTile(map, x - 1, y + 1);
                    boolean W = hasSameTile(map, x - 1, y);
                    boolean NW = hasSameTile(map, x - 1, y - 1);

                    if (N && NE && E && SE && S && SW && W && NW) {

                        // Building options
                        N = hasSameTile(objectMap, x, y - 1);
                        NE = hasSameTile(objectMap, x + 1, y - 1);
                        E = hasSameTile(objectMap, x + 1, y);
                        SE = hasSameTile(objectMap, x + 1, y + 1);
                        S = hasSameTile(objectMap, x, y + 1);
                        SW = hasSameTile(objectMap, x - 1, y + 1);
                        W = hasSameTile(objectMap, x - 1, y);
                        NW = hasSameTile(objectMap, x - 1, y - 1);
                        if (getRoomObjectLayout() == RoomObjectLayout.ALLOW_DIAGONAL_NEIGHBOUR_ONLY
                                && (N || E || S || W)) {
                            continue;
                        }
                        if (getRoomObjectLayout() == RoomObjectLayout.ISOLATED
                                && (N || E || S || W || NE || SE || SW || NW)) {
                            continue;
                        }
                        do {
                            if (index > 1) {
                                index = -1;
                            }
                            index++;
                        } while (room.getObjects().get(index) == 0);

                        // Add object
                        objectMap[x][y] = true;
                        EntityId object = objectsController.loadObject(room.getObjects().get(index), (short) 0, start.x + x, start.y + y);
                        floorFurniture.add(object);
                    }
                }
            }
        }

        // Wall objects 3-5
        if (room.getObjects().get(3) > 0 || room.getObjects().get(4) > 0 || room.getObjects().get(5) > 0) {

        }
    }

    protected static boolean hasSameTile(boolean[][] map, int x, int y) {

        // Check for out of bounds
        if (x < 0 || x >= map.length || y < 0 || y >= map[x].length) {
            return false;
        }
        return map[x][y];
    }

    protected RoomObjectLayout getRoomObjectLayout() {
        return RoomObjectLayout.ALLOW_NEIGHBOUR;
    }

    public boolean isTileAccessible(Integer fromX, Integer fromY, int toX, int toY) {
        return true;
    }

    @Override
    public final boolean isTileAccessible(Point from, Point to) {
        return isTileAccessible(from != null ? from.x : null, (from != null ? from.y : null), to.x, to.y);
    }

    protected final void addObjectControl(IRoomObjectControl control) {
        objectControls.put(control.getObjectType(), control);
        if (defaultObjectType == null) {
            defaultObjectType = control.getObjectType();
        }
    }

    @Override
    public boolean hasObjectControl(ObjectType objectType) {
        return objectControls.containsKey(objectType);
    }

    @Override
    public <T extends IRoomObjectControl> T getObjectControl(ObjectType objectType) {
        return (T) objectControls.get(objectType);
    }

    /**
     * Destroy the room, marks the room as destroyed and releases all the
     * controls. The room <strong>should not</strong> be used after this.
     */
    @Override
    public void destroy() {
        destroyed = true;
        roomInstance.setDestroyed(destroyed);

        // Destroy the controls
        for (IRoomObjectControl control : objectControls.values()) {
            control.destroy();
        }

        // Remove objects
        removeObjects();
    }

    private void removeObjects() {

        // Clear the old ones
        // TODO: recycle?
        for (EntityId entityId : pillars) {
            objectsController.getEntityData().removeEntity(entityId);
        }
        pillars.clear();
        for (EntityId entityId : floorFurniture) {
            objectsController.getEntityData().removeEntity(entityId);
        }
        floorFurniture.clear();
        for (EntityId entityId : wallFurniture) {
            objectsController.getEntityData().removeEntity(entityId);
        }
        wallFurniture.clear();
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    /**
     * Can store gold to the room?
     *
     * @return can store gold
     */
    @Override
    public boolean canStoreGold() {
        return hasObjectControl(ObjectType.GOLD);
    }

    /**
     * Get room max capacity
     *
     * @return room max capacity
     */
    protected int getMaxCapacity() {
        IRoomObjectControl control = getDefaultRoomObjectControl();
        if (control != null) {
            return control.getMaxCapacity();
        }
        return 0;
    }

    /**
     * Get used capacity
     *
     * @return the used capacity of the room
     */
    protected int getUsedCapacity() {
        IRoomObjectControl control = getDefaultRoomObjectControl();
        if (control != null) {
            return control.getCurrentCapacity();
        }
        return 0;
    }

    private IRoomObjectControl getDefaultRoomObjectControl() {
        if (defaultObjectType != null) {
            return objectControls.get(defaultObjectType);
        }
        return null;
    }

    @Override
    public final RoomInstance getRoomInstance() {
        return roomInstance;
    }

    /**
     * Is the room at full capacity
     *
     * @return max capacity used
     */
    @Override
    public boolean isFullCapacity() {
        return getUsedCapacity() >= getMaxCapacity();
    }

    /**
     * Get the room type
     *
     * @return the room type
     */
    @Override
    public Room getRoom() {
        return roomInstance.getRoom();
    }

    /**
     * Are we the dungeon heart?
     *
     * @return are we?
     */
    @Override
    public boolean isDungeonHeart() {
        return false;
    }

    @Override
    public void captured(short playerId) {

        // Nothing, hmm, should we move some logic here from the MapController
        roomInstance.setOwnerId(playerId);

        // Notify the controls
        for (IRoomObjectControl control : objectControls.values()) {
            control.captured(playerId);
        }
    }

    /**
     * Get the total number of furniture in room
     *
     * @return furniture count
     */
    public int getFurnitureCount() {
        return wallFurniture.size() + floorFurniture.size();
    }

    /**
     * Get the number of floor furniture in a room
     *
     * @return floor furniture count
     */
    @Override
    public int getFloorFurnitureCount() {
        return floorFurniture.size();
    }

    /**
     * Get the number of wall furniture in a room
     *
     * @return wall furniture count
     */
    @Override
    public int getWallFurnitureCount() {
        return wallFurniture.size();
    }

    @Override
    public Set<EntityId> getFloorFurniture() {
        return floorFurniture;
    }

    @Override
    public Set<EntityId> getWallFurniture() {
        return wallFurniture;
    }

    /**
     * Get the object ID for the room pillars
     *
     * @param roomId the room ID
     * @return the object ID for the room pillar or {@code null} if not found
     */
    protected static Short getPillarObject(short roomId) {

        // FIXME: Is this data available somewhere??
        switch (roomId) {
            case 1:  // Treasury
                return 76;
            case 2:  // Lair
                return 77;
            case 4:  // Hatchery
                return 78;
            case 10:  // Workshop
                return 80;
            case 11:  // Prison
                //return 81; // Model exists, but not used by the game
                return null;
            case 12:  // Torture
                return 82;
            case 13:  // Temple
                //return 83; // This is the actual model, but place candle sticks instead...
                return 111;
            case 14: // Graveyard
                return 84;
            case 15: // Casino
                return 85;
            case 16: // Pit
                return 79;
            case 26: // Crypt
                return 141;
            default:
                return null; // No pillars
        }
    }
}
