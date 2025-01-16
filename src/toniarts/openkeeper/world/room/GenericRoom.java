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
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.modelviewer.SoundsLoader;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.view.map.WallSection;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.effect.EffectManagerState;
import toniarts.openkeeper.world.object.ObjectControl;
import toniarts.openkeeper.world.object.ObjectLoader;
import toniarts.openkeeper.world.room.control.RoomObjectControl;

/**
 * Base class for all rooms
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Deprecated
public abstract class GenericRoom {

    public enum ObjectType {

        GOLD, LAIR, SPELL_BOOK, RESEARCHER, PRISONER, TORTUREE;

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

    protected final AssetManager assetManager;
    protected final RoomInstance roomInstance;
    protected final WorldState worldState;
    private final static int[] wallIndexes = new int[]{7, 8};
    private Node root;
    protected String tooltip;
    private static String notOwnedTooltip = null;
    protected final EffectManagerState effectManager;
    private ObjectType defaultObjectType;
    private final Map<ObjectType, RoomObjectControl> objectControls = new HashMap<>();
    protected boolean destroyed = false;
    protected boolean[][] map;
    protected Point start;
    protected final ObjectLoader objectLoader;
    private final Set<ObjectControl> floorFurniture = new HashSet<>();
    private final Set<ObjectControl> wallFurniture = new HashSet<>();

    // FIXME we need todo something
    private static final Map<Integer, String> temp = new HashMap<>();

    static {
        temp.put(3, "%44"); // Portal
        temp.put(2, "%45"); // Lairs
        temp.put(4, "%46"); // Hatchery
        temp.put(1, "%47"); // Treasure
        temp.put(6, "%48"); // Library
        temp.put(7, "%49"); // Training Room
        temp.put(10, "%50"); // Workshop
        temp.put(9, "%51"); // Guard Rooms
        temp.put(16, "%53"); // Combat Pit
        temp.put(12, "%54"); // Torture
        temp.put(11, "%58"); // Prison
        temp.put(14, "%61"); // Graveyard
        temp.put(13, "%62"); // Temple
        temp.put(15, "%63"); // Casino
    }

    public GenericRoom(AssetManager assetManager,
            RoomInstance roomInstance, ObjectLoader objectLoader, WorldState worldState, EffectManagerState effectManager) {
        this.assetManager = assetManager;
        this.roomInstance = roomInstance;
        this.effectManager = effectManager;
        this.objectLoader = objectLoader;
        this.worldState = worldState;

        // sounds
        SoundsLoader.load(roomInstance.getRoom().getSoundCategory());

        // Strings
        tooltip = Utils.getMainTextResourceBundle().getString(Integer.toString(roomInstance.getRoom().getTooltipStringId()));
        if (notOwnedTooltip == null) {
            notOwnedTooltip = Utils.getMainTextResourceBundle().getString("2471");
        }
    }

    protected void setupCoordinates() {
        map = roomInstance.getCoordinatesAsMatrix();
        start = roomInstance.getMatrixStartPoint();
    }

    public Spatial construct() {
        setupCoordinates();

        // Add the floor
        getRootNode().detachAllChildren();
        BatchNode floorNode = constructFloor();
        if (floorNode != null) {
            floorNode.setName("Floor");
            floorNode.setShadowMode(getFloorShadowMode());
            floorNode.batch();
            getRootNode().attachChild(floorNode);
        }

        // Custom wall
        BatchNode wallNode = constructWall();
        if (wallNode != null) {
            wallNode.setName("Wall");
            wallNode.setShadowMode(getWallShadowMode());
            wallNode.batch();
            getRootNode().attachChild(wallNode);
        }

        // The objects on the floor
        Node objectsNode = constructObjects();
        if (objectsNode != null) {
            objectsNode.setName("Objects");
            objectsNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
            getRootNode().attachChild(objectsNode);
        }
        return getRootNode();
    }

    public Node getRootNode() {
        if (root == null) {
            root = new Node(roomInstance.getRoom().getName());
        }
        return root;
    }

    protected abstract BatchNode constructFloor();

    /**
     * Rooms typically don't contruct walls themselves, instead they are asked
     * for the wall spatials by the map loader in normal map drawing situation
     *
     * @see #getWallSpatial(java.awt.Point,
     * toniarts.openkeeper.view.map.WallSection.WallDirection)
     * @return contructed wall
     */
    protected BatchNode constructWall() {
        return null;
    }

    /**
     * Construct room objects
     *
     * @return node with all the room objects
     */
    protected Node constructObjects() {
        floorFurniture.clear();
        wallFurniture.clear();

        // Floor objects 0-2
        Room room = roomInstance.getRoom();
        int index = -1;
        Node objects = new Node();
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
                        Spatial object = objectLoader.load(assetManager, start.x + x, start.y + y,
                                room.getObjects().get(index), roomInstance.getOwnerId());
                        objects.attachChild(object);
                        floorFurniture.add(object.getControl(ObjectControl.class));
                    }
                }
            }
        }

        // Wall objects 3-5
        if (room.getObjects().get(3) > 0 || room.getObjects().get(4) > 0 || room.getObjects().get(5) > 0) {

        }
        return objects;
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

    protected RenderQueue.ShadowMode getFloorShadowMode() {
        return RenderQueue.ShadowMode.Receive;
    }

    protected RenderQueue.ShadowMode getWallShadowMode() {
        return RenderQueue.ShadowMode.CastAndReceive;
    }

    public Spatial getWallSpatial(Point p, WallSection.WallDirection direction) {
        float yAngle = FastMath.PI;
        String resource = roomInstance.getRoom().getCompleteResource().getName();

        /*for (WallSection section : roomInstance.getWallSections()) {

            if (section.getDirection() != direction) {
                continue;
            }

            int sectionSize = section.getCoordinates().size();
            for (int i = 0; i < sectionSize; i++) {
                // skip others
                if (!p.equals(section.getCoordinates().get(i))) {
                    continue;
                }

                Spatial spatial;
                if (i == 0 || i == (sectionSize - 1)) {
                    Vector3f moveFirst;
                    Vector3f moveSecond;
                    if (section.getDirection() == WallSection.WallDirection.WEST
                            || section.getDirection() == WallSection.WallDirection.SOUTH) {
                        moveFirst = new Vector3f(MapLoader.TILE_WIDTH / 4, 0, -3 * MapLoader.TILE_WIDTH / 4);
                        moveSecond = new Vector3f(-MapLoader.TILE_WIDTH / 4, 0, -3 * MapLoader.TILE_WIDTH / 4);
                    } else { // NORTH, EAST
                        moveFirst = new Vector3f(-MapLoader.TILE_WIDTH / 4, 0, -3 * MapLoader.TILE_WIDTH / 4);
                        moveSecond = new Vector3f(MapLoader.TILE_WIDTH / 4, 0, -3 * MapLoader.TILE_WIDTH / 4);
                    }

                    spatial = new BatchNode();
                    int firstPiece = (i == 0 ? 4 : 6);
                    if (firstPiece == 4 && (section.getDirection() == WallSection.WallDirection.EAST
                            || section.getDirection() == WallSection.WallDirection.NORTH)) {
                        firstPiece = 5; // The sorting direction forces us to do this
                    }

                    // Load the piece
                    Spatial part = AssetUtils.loadModel(assetManager, resource + firstPiece, null);
                    part.move(moveFirst);
                    part.rotate(0, yAngle, 0);
                    ((BatchNode) spatial).attachChild(part);

                    // Second
                    int secondPiece = (i == (sectionSize - 1) ? 5 : 6);
                    if (secondPiece == 5 && (section.getDirection() == WallSection.WallDirection.EAST
                            || section.getDirection() == WallSection.WallDirection.NORTH)) {
                        secondPiece = 4; // The sorting direction forces us to do this
                    }

                    part = AssetUtils.loadModel(assetManager, resource + secondPiece, null);
                    part.move(moveSecond);
                    part.rotate(0, yAngle, 0);
                    ((BatchNode) spatial).attachChild(part);

                    ((BatchNode) spatial).batch();
                } else {
                    // Complete walls, 8, 7, 8, 7 and so forth
                    spatial = AssetUtils.loadModel(assetManager, resource + getWallIndex(i), null);
                    spatial.rotate(0, yAngle, 0);

                    switch (section.getDirection()) {
                        case WEST:
                            spatial.move(-MapLoader.TILE_WIDTH, 0, 0);
                            break;
                        case SOUTH:
                            spatial.move(0, 0, MapLoader.TILE_WIDTH);
                            break;
                        case EAST:
                            spatial.move(MapLoader.TILE_WIDTH, 0, 0);
                            break;
                        default:
                            // NORTH
                            spatial.move(0, 0, -MapLoader.TILE_WIDTH);
                            break;
                    }
                }

                return spatial;
            }
        }*/

        return null;
    }

    /**
     * Resets (scale & translation) and moves the spatial to the point. The
     * point is relative to the start point
     *
     * @param tile the tile, spatial
     * @param start start point
     * @param p the tile point
     */
    protected void moveSpatial(Spatial tile, Point start, Point p) {
        // Reset, really, the size is 1 after this...
        //resetSpatial(tile);
        tile.move(p.x - start.x, 0, p.y - start.y);
    }

    /**
     * Resets (scale & translation) and moves the spatial to the point. The
     * point is relative to the start point
     *
     * @param tile the tile, Spatial
     * @param start start point
     */
    protected void moveSpatial(Spatial tile, Point start) {
        // Reset, really, the size is 1 after this...
        //resetSpatial(tile);
        tile.move(start.x, 0, start.y);
    }

    public int getWallIndex(int index) {
        int pointer = index % wallIndexes.length;
        return wallIndexes[pointer];
    }

    public boolean isTileAccessible(Integer fromX, Integer fromY, int toX, int toY) {
        return true;
    }

    public final boolean isTileAccessible(Point from, Point to) {
        return isTileAccessible(from != null ? from.x : null, (from != null ? from.y : null), to.x, to.y);
    }

    public String getTooltip(short playerId) {
        if (roomInstance.getOwnerId() != playerId) {
            return notOwnedTooltip;
        }

        String temp = tooltip;
        RoomCount total = getRoomCount();
        if (total != null) {
            temp = tooltip.replaceAll(total.placeholder, String.valueOf(total.amount));
        }

        return temp.replaceAll("%37%", Integer.toString(roomInstance.getHealthPercentage()))
                .replaceAll("%38", Integer.toString(getUsedCapacity()))
                .replaceAll("%39", Integer.toString(getMaxCapacity()));
    }

    protected final Spatial loadModel(String model) {
        Spatial spatial = AssetUtils.loadModel(assetManager, model, null);
        //resetSpatial(spatial);
        return spatial;
    }

    protected final void addObjectControl(RoomObjectControl control) {
        objectControls.put(control.getObjectType(), control);
        if (defaultObjectType == null) {
            defaultObjectType = control.getObjectType();
        }
    }

    public boolean hasObjectControl(ObjectType objectType) {
        return objectControls.containsKey(objectType);
    }

    public <T extends RoomObjectControl> T getObjectControl(ObjectType objectType) {
        return (T) objectControls.get(objectType);
    }

    /**
     * Destroy the room, marks the room as destroyed and releases all the
     * controls. The room <strong>should not</strong> be used after this.
     */
    public void destroy() {
        destroyed = true;

        // Destroy the controls
        for (RoomObjectControl control : objectControls.values()) {
            control.destroy();
        }
    }

    /**
     * Is this room instance destroyed? Not in the world anymore.
     *
     * @see #destroy()
     * @return is the room destroyed
     */
    public boolean isDestroyed() {
        return destroyed;
    }

    /**
     * Can store gold to the room?
     *
     * @return can store gold
     */
    public boolean canStoreGold() {
        return hasObjectControl(ObjectType.GOLD);
    }

    /**
     * Get room max capacity
     *
     * @return room max capacity
     */
    protected int getMaxCapacity() {
        RoomObjectControl control = getDefaultRoomObjectControl();
        if (control != null) {
            return control.getMaxCapacity();
        }
        return 0;
    }

    private String getOwner() {
        short ownerId = roomInstance.getOwnerId();
        String result = worldState.getLevelData().getPlayer(ownerId).getName();

        return result;
    }

    /**
     * Get used capacity
     *
     * @return the used capacity of the room
     */
    protected int getUsedCapacity() {
        RoomObjectControl control = getDefaultRoomObjectControl();
        if (control != null) {
            return control.getCurrentCapacity();
        }
        return 0;
    }

    private RoomObjectControl getDefaultRoomObjectControl() {
        if (defaultObjectType != null) {
            return objectControls.get(defaultObjectType);
        }
        return null;
    }

    public RoomInstance getRoomInstance() {
        return roomInstance;
    }

    /**
     * Is the room at full capacity
     *
     * @return max capacity used
     */
    public boolean isFullCapacity() {
        return getUsedCapacity() >= getMaxCapacity();
    }

    /**
     * Get the room type
     *
     * @return the room type
     */
    public Room getRoom() {
        return roomInstance.getRoom();
    }

    /**
     * Are we the dungeon heart?
     *
     * @return are we?
     */
    public boolean isDungeonHeart() {
        return false;
    }

    public WorldState getWorldState() {
        return worldState;
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
    public int getFloorFurnitureCount() {
        return floorFurniture.size();
    }

    /**
     * Get the number of wall furniture in a room
     *
     * @return wall furniture count
     */
    public int getWallFurnitureCount() {
        return wallFurniture.size();
    }

    public Set<ObjectControl> getFloorFurniture() {
        return floorFurniture;
    }

    public Set<ObjectControl> getWallFurniture() {
        return wallFurniture;
    }

    @Nullable
    public GameObject getPillarObject() {
        // TODO hardcode maybe something else?
        switch (roomInstance.getRoom().getRoomId()) {
            case 1:  // Treasury
                return worldState.getLevelData().getObject(76);
            case 2:  // Lair
                return worldState.getLevelData().getObject(77);
            case 4:  // Hatchery
                return worldState.getLevelData().getObject(78);
            case 10:  // Workshop
                return worldState.getLevelData().getObject(80);
            case 11:  // Prison
                return worldState.getLevelData().getObject(81);
            case 12:  // Torture
                return worldState.getLevelData().getObject(82);
            case 13:  // Temple
                return worldState.getLevelData().getObject(83);
            case 14: // Graveyard
                return worldState.getLevelData().getObject(84);
            case 15: // Casino
                return worldState.getLevelData().getObject(85);
            case 16: // Pit
                return worldState.getLevelData().getObject(79);
            case 26: // Crypt
                return worldState.getLevelData().getObject(141);
        }

        return null;
    }

    @Nullable
    private RoomCount getRoomCount() {

        int roomId = roomInstance.getRoom().getRoomId();
        if (!temp.containsKey(roomId)) {
            return null;
        }

        Keeper player = worldState.getGameState().getPlayer(roomInstance.getOwnerId());
//        int total = player.getRoomControl().getTypeCount(roomInstance.getRoom());

//        return new RoomCount(temp.get(roomId), total);
        return null;
    }

    private static class RoomCount {

        public String placeholder;
        public int amount;

        public RoomCount(String placeholder, int amount) {
            this.placeholder = placeholder;
            this.amount = amount;
        }
    }
}
