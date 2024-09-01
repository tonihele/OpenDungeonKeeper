/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.openkeeper.view.map.construction;

import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.map.WallSection;

/**
 * A base class for constructing different kind of rooms. FIXME: Basically
 * static style constructor would be kinda better since these are one use
 * wonders
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class RoomConstructor {

    protected final AssetManager assetManager;
    protected final RoomInstance roomInstance;
    protected final boolean[][] map;
    protected final Point start;

    private final static int[] WALL_INDEXES = new int[]{7, 8};

    public RoomConstructor(AssetManager assetManager, RoomInstance roomInstance) {
        this.assetManager = assetManager;
        this.roomInstance = roomInstance;
        this.map = roomInstance.getCoordinatesAsMatrix();
        this.start = roomInstance.getMatrixStartPoint();
    }

    /**
     * Constructs the room
     *
     * @return the spatial representing the room
     */
    public final Spatial construct() {
        Node root = new Node(roomInstance.getRoom().getName());

        // Add the floor
        BatchNode floorNode = constructFloor();
        if (floorNode != null) {
            floorNode.setName("Floor");
            floorNode.setShadowMode(getFloorShadowMode());
            floorNode.batch();
            root.attachChild(floorNode);
        }

        // Custom wall
        BatchNode wallNode = constructWall();
        if (wallNode != null) {
            wallNode.setName("Wall");
            wallNode.setShadowMode(getWallShadowMode());
            wallNode.batch();
            root.attachChild(wallNode);
        }

        return root;
    }

    protected abstract BatchNode constructFloor();

    /**
     * Rooms typically don't construct walls themselves, instead they are asked
     * for the wall spatials by the map loader in normal map drawing situation
     *
     * @see #getWallSpatial(java.awt.Point,
     * toniarts.openkeeper.view.map.WallSection.WallDirection)
     * @return constructed wall
     */
    protected BatchNode constructWall() {
        return null;
    }

    protected RenderQueue.ShadowMode getFloorShadowMode() {
        return RenderQueue.ShadowMode.Receive;
    }

    protected RenderQueue.ShadowMode getWallShadowMode() {
        return RenderQueue.ShadowMode.CastAndReceive;
    }

    /**
     * Use the big floor tile at the specified point
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @return use big tile
     */
    public boolean useBigFloorTile(int x, int y) {
        return true;
    }

    /**
     * Does the given tile have the same room instance
     *
     * @param map the map
     * @param x x coordinate
     * @param y y coordinate
     * @return has the same tile
     */
    protected static boolean hasSameTile(boolean[][] map, int x, int y) {

        // Check for out of bounds
        if (x < 0 || x >= map.length || y < 0 || y >= map[x].length) {
            return false;
        }
        return map[x][y];
    }

    protected final Spatial loadModel(String model, ArtResource artResource) {
        Spatial spatial = AssetUtils.loadModel(assetManager, model, artResource);
        //resetSpatial(spatial);
        return spatial;
    }

    public Spatial getWallSpatial(Point p, WallSection.WallDirection direction) {
        float yAngle = FastMath.PI;
        ArtResource artResource = roomInstance.getRoom().getCompleteResource();
        String resource = artResource.getName();

        for (WallSection section : roomInstance.getWallSections()) {

            if (section.direction() != direction) {
                continue;
            }

            int sectionSize = section.coordinates().size();
            for (int i = 0; i < sectionSize; i++) {
                // skip others
                if (!p.equals(section.coordinates().get(i))) {
                    continue;
                }

                Spatial spatial;
                if (i == 0 || i == (sectionSize - 1)) {
                    Vector3f moveFirst;
                    Vector3f moveSecond;
                    if (section.direction() == WallSection.WallDirection.WEST
                            || section.direction() == WallSection.WallDirection.SOUTH) {
                        moveFirst = new Vector3f(WorldUtils.TILE_WIDTH / 4, 0, -3 * WorldUtils.TILE_WIDTH / 4);
                        moveSecond = new Vector3f(-WorldUtils.TILE_WIDTH / 4, 0, -3 * WorldUtils.TILE_WIDTH / 4);
                    } else { // NORTH, EAST
                        moveFirst = new Vector3f(-WorldUtils.TILE_WIDTH / 4, 0, -3 * WorldUtils.TILE_WIDTH / 4);
                        moveSecond = new Vector3f(WorldUtils.TILE_WIDTH / 4, 0, -3 * WorldUtils.TILE_WIDTH / 4);
                    }

                    spatial = new BatchNode();
                    int firstPiece = (i == 0 ? 4 : 6);
                    if (firstPiece == 4 && (section.direction() == WallSection.WallDirection.EAST
                            || section.direction() == WallSection.WallDirection.NORTH)) {
                        firstPiece = 5; // The sorting direction forces us to do this
                    }

                    // Load the piece
                    Spatial part = AssetUtils.loadModel(assetManager, resource + firstPiece, artResource);
                    part.move(moveFirst);
                    part.rotate(0, yAngle, 0);
                    ((BatchNode) spatial).attachChild(part);

                    // Second
                    int secondPiece = (i == (sectionSize - 1) ? 5 : 6);
                    if (secondPiece == 5 && (section.direction() == WallSection.WallDirection.EAST
                            || section.direction() == WallSection.WallDirection.NORTH)) {
                        secondPiece = 4; // The sorting direction forces us to do this
                    }

                    part = AssetUtils.loadModel(assetManager, resource + secondPiece, artResource);
                    part.move(moveSecond);
                    part.rotate(0, yAngle, 0);
                    ((BatchNode) spatial).attachChild(part);

                    ((BatchNode) spatial).batch();
                } else {
                    // Complete walls, 8, 7, 8, 7 and so forth
                    spatial = AssetUtils.loadModel(assetManager, resource + getWallIndex(i), artResource);
                    spatial.rotate(0, yAngle, 0);

                    switch (section.direction()) {
                        case WEST:
                            spatial.move(-WorldUtils.TILE_WIDTH, 0, 0);
                            break;
                        case SOUTH:
                            spatial.move(0, 0, WorldUtils.TILE_WIDTH);
                            break;
                        case EAST:
                            spatial.move(WorldUtils.TILE_WIDTH, 0, 0);
                            break;
                        default:
                            // NORTH
                            spatial.move(0, 0, -WorldUtils.TILE_WIDTH);
                            break;
                    }
                }

                return spatial;
            }
        }

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
        int pointer = index % WALL_INDEXES.length;
        return WALL_INDEXES[pointer];
    }

}
