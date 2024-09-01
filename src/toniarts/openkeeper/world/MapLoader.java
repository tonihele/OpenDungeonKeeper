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
package toniarts.openkeeper.world;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import java.awt.Color;
import toniarts.openkeeper.utils.Point;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.control.TorchControl;
import toniarts.openkeeper.view.loader.ILoader;
import toniarts.openkeeper.view.map.WallSection;
import toniarts.openkeeper.view.map.WallSection.WallDirection;
import toniarts.openkeeper.world.effect.EffectManagerState;
import toniarts.openkeeper.world.object.ObjectLoader;
import toniarts.openkeeper.world.room.GenericRoom;
import toniarts.openkeeper.world.room.RoomConstructor;
import toniarts.openkeeper.world.room.RoomInstance;
import toniarts.openkeeper.world.terrain.Water;

/**
 * Loads whole maps, and handles the maps
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Deprecated
public abstract class MapLoader implements ILoader<KwdFile> {

    private static final Logger logger = System.getLogger(MapLoader.class.getName());
    
    public final static float TILE_WIDTH = 1;
    public final static float TILE_HEIGHT = 1;
    public final static float TORCH_HEIGHT = 3 * TILE_HEIGHT / 2; // FIXME use Terrain Torch Height
    public final static float TOP_HEIGHT = 2 * TILE_HEIGHT;
    public final static float FLOOR_HEIGHT = 1 * TILE_HEIGHT;
    public final static float UNDERFLOOR_HEIGHT = 0 * TILE_HEIGHT;
    public final static float WATER_LEVEL = MapLoader.FLOOR_HEIGHT - 0.07f;

    public final static ColorRGBA COLOR_FLASH = new ColorRGBA(0.8f, 0, 0, 1);
    public final static ColorRGBA COLOR_TAG = new ColorRGBA(0, 0, 0.8f, 1);
    private final static int PAGE_SQUARE_SIZE = 8; // Divide the terrain to square "pages"
    private final static int FLOOR_INDEX = 0;
    private final static int WALL_INDEX = 1;
    private final static int TOP_INDEX = 2;
    private final static String MAP_NODE = "Map";
    private final static String TERRAIN_NODE = "Terrain";
    private final static String ROOM_NODE = "Rooms";
    private List<Node> pages;
    private final KwdFile kwdFile;
    private Node map;
    private final MapData mapData;
    private final AssetManager assetManager;
    private final EffectManagerState effectManager;
    private Node roomsNode;
    private final WorldState worldState;
    private final ObjectLoader objectLoader;
    private final List<RoomInstance> rooms = new ArrayList<>(); // The list of rooms
    private final List<EntityInstance<Terrain>> waterBatches = new ArrayList<>(); // Lakes and rivers
    private final List<EntityInstance<Terrain>> lavaBatches = new ArrayList<>(); // Lakes and rivers, but hot
    private final HashMap<Point, RoomInstance> roomCoordinates = new HashMap<>(); // A quick glimpse whether room at specific coordinates is already "found"
    private final HashMap<RoomInstance, Spatial> roomNodes = new HashMap<>(); // Room instances by node
    private final Map<RoomInstance, GenericRoom> roomActuals = new LinkedHashMap<>(); // Rooms by room instance
    private final HashMap<Point, EntityInstance<Terrain>> terrainBatchCoordinates = new HashMap<>(); // A quick glimpse whether terrain batch at specific coordinates is already "found"

    public MapLoader(AssetManager assetManager, KwdFile kwdFile, EffectManagerState effectManager, WorldState worldState, ObjectLoader objectLoader) {
        this.kwdFile = kwdFile;
        this.assetManager = assetManager;
        this.effectManager = effectManager;
        this.worldState = worldState;
        this.objectLoader = objectLoader;

        // Create modifiable tiles
        mapData = new MapData(kwdFile);
    }

    @Override
    public Spatial load(AssetManager assetManager, KwdFile object) {

        //Create a root
        map = new Node(MAP_NODE);
        Node terrain = new Node(TERRAIN_NODE);
        generatePages(terrain);
        roomsNode = new Node(ROOM_NODE);
        terrain.attachChild(roomsNode);

        // Go through the fixed rooms and construct them
        for (Thing.Room room : kwdFile.getThings(Thing.Room.class)) {
            Point p = new Point(room.getPosX(), room.getPosY());
            handleRoom(p, kwdFile.getRoomByTerrain(mapData.getTile(p).getTerrain().getTerrainId()), room);
        }

        // Go through the map
        int tilesCount = mapData.getWidth() * object.getMap().getHeight();

        for (int y = 0; y < mapData.getHeight(); y++) {
            for (int x = 0; x < mapData.getWidth(); x++) {

                try {
                    handleTile(mapData.getTile(x, y), terrain);
                } catch (Exception e) {
                    logger.log(Level.ERROR, "Failed to handle tile at " + x + ", " + y + "!", e);
                }

                // Update progress
                updateProgress((float) (y * object.getMap().getWidth() + x + 1) / tilesCount);
            }
        }

        // Batch the terrain pages
        for (Node page : pages) {
            ((BatchNode) page.getChild(FLOOR_INDEX)).batch();
            ((BatchNode) page.getChild(WALL_INDEX)).batch();
            ((BatchNode) page.getChild(TOP_INDEX)).batch();
        }
        map.attachChild(terrain);

        // Create the water
        if (!waterBatches.isEmpty()) {
            map.attachChild(Water.construct(assetManager, waterBatches));
        }

        // And the lava
        if (!lavaBatches.isEmpty()) {
            map.attachChild(Water.construct(assetManager, lavaBatches));
        }

        return map;
    }

    public MapData getMapData() {
        return mapData;
    }

    /**
     * Update the selected tiles (and neighbouring tiles if needed)
     *
     * @param points tile coordinates to update
     */
    protected void updateTiles(Point... points) {

        // Reconstruct all tiles in the area
        Set<BatchNode> nodesNeedBatching = new HashSet<>();
        Node terrainNode = (Node) map.getChild(TERRAIN_NODE);
        for (Point point : points) {
            TileData tile = mapData.getTile(point);
            // Reconstruct and mark for patching
            // The tile node needs to created anew, somehow the BatchNode just doesn't get it if I remove children from subnode
            Node pageNode = getPageNode(point, terrainNode);
            Node tileNode = getTileNode(point, (Node) pageNode.getChild(FLOOR_INDEX));
            if (!tileNode.getChildren().isEmpty()) {
                tileNode.removeFromParent();
                ((BatchNode) pageNode.getChild(FLOOR_INDEX)).attachChildAt(new Node(tileNode.getName()), getTileNodeIndex(point));
                nodesNeedBatching.add((BatchNode) pageNode.getChild(FLOOR_INDEX));
            }
            tileNode = getTileNode(point, (Node) pageNode.getChild(WALL_INDEX));
            if (!tileNode.getChildren().isEmpty()) {
                tileNode.removeFromParent();
                ((BatchNode) pageNode.getChild(WALL_INDEX)).attachChildAt(new Node(tileNode.getName()), getTileNodeIndex(point));
                nodesNeedBatching.add((BatchNode) pageNode.getChild(WALL_INDEX));
            }
            tileNode = getTileNode(point, (Node) pageNode.getChild(TOP_INDEX));
            if (!tileNode.getChildren().isEmpty()) {
                tileNode.removeFromParent();
                ((BatchNode) pageNode.getChild(TOP_INDEX)).attachChildAt(new Node(tileNode.getName()), getTileNodeIndex(point));
                nodesNeedBatching.add((BatchNode) pageNode.getChild(TOP_INDEX));
            }

            // Reconstruct
            handleTile(tile, (Node) map.getChild(TERRAIN_NODE));
        }

        // Batch
        for (BatchNode batchNode : nodesNeedBatching) {
            batchNode.batch();
        }
    }

    /**
     * Sets the right material to tile (selected / decayed...)
     *
     * @param node
     */
    private void setTileMaterialToGeometries(final TileData tile, final Node node) {

        // Change the material on geometries
        if (!tile.isFlashed() && !tile.isSelected()
                && !tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.DECAY)) {
            return;
        }

        node.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                if (!(spatial instanceof Geometry)) {
                    return;
                }

                Material material = ((Geometry) spatial).getMaterial();

                // Decay
                if (tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.DECAY) && tile.getTerrain().getTextureFrames() > 1) {

                    Integer texCount = null;//spatial.getUserData(KmfModelLoader.MATERIAL_ALTERNATIVE_TEXTURES_COUNT);
                    if (texCount != null) {

                        // FIXME: This doesn't sit well with the material thinking (meaning we produce the actual material files)
                        // Now we have a random starting texture...
                        int textureIndex = tile.getTerrain().getTextureFrames() - (int) Math.ceil(tile.getHealthPercent() / (100f / tile.getTerrain().getTextureFrames()));
                        String diffuseTexture = ((Texture) material.getParam("DiffuseMap").getValue()).getKey().getName().replaceFirst("_DECAY\\d", ""); // Unharmed texture
                        if (textureIndex > 0) {

                            // The first one doesn't have a number
                            if (textureIndex == 1) {
                                diffuseTexture = diffuseTexture.replaceFirst(".png", "_DECAY.png");
                            } else {
                                diffuseTexture = diffuseTexture.replaceFirst(".png", "_DECAY" + textureIndex + ".png");
                            }
                        }
                        try {
                            Texture texture = assetManager.loadTexture(new TextureKey(AssetUtils.getCanonicalAssetKey(diffuseTexture), false));
                            material.setTexture("DiffuseMap", texture);

                            AssetUtils.assignMapsToMaterial(assetManager, material);
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Error applying decay texture: {0} to {1} terrain! ({2})", new Object[]{diffuseTexture, tile.getTerrain().getName(), e.getMessage()});
                        }
                    }
                }

                if (tile.isFlashed()) {
                    material.setColor("Ambient", COLOR_FLASH);
                    material.setBoolean("UseMaterialColors", true);
                }
                if (tile.isSelected()) {
                    material.setColor("Ambient", COLOR_TAG);
                    material.setBoolean("UseMaterialColors", true);
                }

            }

        });

    }

    /**
     * Generate the page nodes
     *
     * @param root where to generate pages on
     */
    private void generatePages(Node root) {
        pages = new ArrayList<>(((int) Math.ceil(mapData.getHeight() / (float) PAGE_SQUARE_SIZE))
                * ((int) Math.ceil(mapData.getWidth() / (float) PAGE_SQUARE_SIZE)));
        for (int y = 0; y < (int) Math.ceil(mapData.getHeight() / (float) PAGE_SQUARE_SIZE); y++) {
            for (int x = 0; x < (int) Math.ceil(mapData.getWidth() / (float) PAGE_SQUARE_SIZE); x++) {
                Node page = new Node(x + "_" + y);

                // Create batch nodes for ceiling, floor and walls
                BatchNode floor = new BatchNode("floor");
                floor.setShadowMode(RenderQueue.ShadowMode.Receive); // Floors don't cast
                generateTileNodes(floor, x, y);
                page.attachChild(floor);
                BatchNode wall = new BatchNode("wall");
                wall.setShadowMode(RenderQueue.ShadowMode.CastAndReceive); // Walls cast and receive shadows
                generateTileNodes(wall, x, y);
                page.attachChild(wall);
                BatchNode ceiling = new BatchNode("ceiling");
                ceiling.setShadowMode(RenderQueue.ShadowMode.Off); // No lights above ceilings
                generateTileNodes(ceiling, x, y);
                page.attachChild(ceiling);

                pages.add(page);
                root.attachChild(page);
            }
        }
    }

    /**
     * Create tile nodes inside a page
     *
     * @param pageBatch the page
     * @param pageX page x
     * @param pageY page y
     */
    private void generateTileNodes(BatchNode pageBatch, int pageX, int pageY) {
        for (int y = 0; y < PAGE_SQUARE_SIZE; y++) {
            for (int x = 0; x < PAGE_SQUARE_SIZE; x++) {
                pageBatch.attachChild(new Node((x + pageX * PAGE_SQUARE_SIZE) + "_" + (y + pageY * PAGE_SQUARE_SIZE)));
            }
        }
    }

    private Spatial getWallSpatial(TileData tile, WallDirection direction) {
        String modelName = tile.getTerrain().getSideResource().getName();
        Point p = tile.getLocation();
        TileData neigbourTile;
        switch (direction) {
            case NORTH:
                neigbourTile = mapData.getTile(p.x, p.y - 1);
                break;
            case SOUTH:
                neigbourTile = mapData.getTile(p.x, p.y + 1);
                break;
            case EAST:
                neigbourTile = mapData.getTile(p.x + 1, p.y);
                break;
            default: // WEST
                neigbourTile = mapData.getTile(p.x - 1, p.y);
                break;
        }
        // Check for out of bounds
        if (neigbourTile == null) {
            return loadModel(modelName);
        }

        if (neigbourTile.getTerrain().getFlags().contains(Terrain.TerrainFlag.SOLID)) {
            return null;
        }

        if (!(tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.ALLOW_ROOM_WALLS))) {
            return loadModel(modelName);
        } else if (hasRoomWalls(neigbourTile)) {
            return getRoomWall(neigbourTile, direction);
        }

        return loadModel(modelName);
    }

    private Spatial getRoomWall(TileData tile, WallDirection direction) {
        Point p = tile.getLocation();
        Room room = kwdFile.getRoomByTerrain(tile.getTerrainId());
        RoomInstance roomInstance = handleRoom(p, room, null);
        GenericRoom gr = roomActuals.get(roomInstance);
        return gr.getWallSpatial(p, direction);
    }

    /**
     * Sets random material (from the list) to all the geometries that have been
     * tagged for this in this spatial
     *
     * @param spatial the spatial
     * @param tile the tile
     */
    private void setRandomTexture(final Spatial spatial, final TileData tile) {

        // Check the data on geometry
        spatial.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                Integer texCount = null;//spatial.getUserData(KmfModelLoader.MATERIAL_ALTERNATIVE_TEXTURES_COUNT);
                if (texCount != null) {

                    // On redrawing, see if we already randomized this
                    // The principle is bit wrong, the random texture is tied to the tile, and not material etc.
                    // But it is probably just the tops of few tiles, so...
                    int tex;
                    if (tile.getRandomTextureIndex() != null) {
                        tex = tile.getRandomTextureIndex();
                    } else {
                        tex = FastMath.rand.nextInt(texCount);
                        tile.setRandomTextureIndex(tex);
                    }
                    if (tex != 0) { // 0 is the default anyway
                        Geometry g = (Geometry) spatial;
                        Material m = g.getMaterial();
                        String asset = m.getAssetName();

                        // Load new material
                        Material newMaterial = assetManager.loadMaterial(asset.substring(0,
                                asset.lastIndexOf(KmfModelLoader.MATERIAL_ALTERNATIVE_TEXTURE_SUFFIX_SEPARATOR) + 1).concat(tex + ".j3m"));
                        AssetUtils.assignMapsToMaterial(assetManager, newMaterial);
                        g.setMaterial(newMaterial);
                    }
                }
            }
        });
    }

    private Spatial loadModel(final String model) {
        Spatial spatial = AssetUtils.loadModel(assetManager, model, null);

        return spatial;
    }

    /**
     * Handle single tile from the map, represented by the X & Y coordinates
     *
     * @param tile tile to handle
     * @param root the root node
     */
    private void handleTile(TileData tile, Node root) {

        // Get the terrain
        Terrain terrain = tile.getTerrain();
        Point p = tile.getLocation();
        Node pageNode = getPageNode(p, root);

        // Torch (see https://github.com/tonihele/OpenKeeper/issues/128)
        if (!terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)
                && (tile.getX() % 2 == 0 || tile.getY() % 2 == 0)) {
            handleTorch(tile, pageNode);
        }

        // Room
        if (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)) {

            // Construct the actual room
            Room room = kwdFile.getRoomByTerrain(terrain.getTerrainId());
            handleRoom(p, room, null);

            // Swap the terrain if this is a bridge
            terrain = kwdFile.getTerrainBridge(tile.getFlag(), room);
            if (terrain == null) {
                return;
            }
        }

        handleTop(tile, terrain, pageNode);
        if (terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)) {
            handleSide(tile, pageNode);
        }
    }

    private void handleTorch(TileData tile, Node pageNode) {

        // The rooms actually contain the torch model resource, but it is always the same,
        // and sometimes even null and there is still a torch. So I don't think they are used
        // Take the first direction where we can put a torch
        String name = null;
        float angleY = 0;
        Vector3f position = Vector3f.ZERO;

        if (tile.getY() % 2 == 0 && tile.getX() % 2 != 0 && canPlaceTorch(tile.getX(), tile.getY() - 1)) { // North
            name = "Torch1";
            angleY = -FastMath.HALF_PI;
            position = new Vector3f(0, TORCH_HEIGHT, -TILE_WIDTH / 2);

        } else if (tile.getX() % 2 == 0 && tile.getY() % 2 == 0 && canPlaceTorch(tile.getX() - 1, tile.getY())) { // West
            name = "Torch1";
            position = new Vector3f(-TILE_WIDTH / 2, TORCH_HEIGHT, 0);

        } else if (tile.getY() % 2 == 0 && tile.getX() % 2 != 0 && canPlaceTorch(tile.getX(), tile.getY() + 1)) { // South
            name = "Torch1";
            angleY = FastMath.HALF_PI;
            position = new Vector3f(0, TORCH_HEIGHT, TILE_WIDTH / 2);

        } else if (tile.getX() % 2 == 0 && tile.getY() % 2 == 0 && canPlaceTorch(tile.getX() + 1, tile.getY())) { // East
            name = "Torch1";
            angleY = FastMath.PI;
            position = new Vector3f(TILE_WIDTH / 2, TORCH_HEIGHT, 0);
        }

        // Move to tile and right height
        if (name != null) {
            // if room get room torch
            if (tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.ROOM)) {
                RoomInstance roomInstance = roomCoordinates.get(tile.getLocation());
                if (roomInstance != null) {
                    ArtResource torch = roomInstance.getRoom().getTorch();
                    if (torch == null) {
                        return;
                    }
                    name = torch.getName();
                }
            }
            Spatial spatial = AssetUtils.loadModel(assetManager, name, null);
            spatial.addControl(new TorchControl(kwdFile, assetManager, angleY));
            spatial.rotate(0, angleY, 0);
            spatial.setLocalTranslation(WorldUtils.pointToVector3f(tile.getLocation()).addLocal(position));

            ((Node) getTileNode(tile.getLocation(), (Node) pageNode.getChild(WALL_INDEX))).attachChild(spatial);
        }
    }

    private boolean canPlaceTorch(int x, int y) {
        TileData tile = mapData.getTile(x, y);
        return (tile != null && tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.TORCH));

    }

    private RoomInstance handleRoom(Point p, Room room, Thing.Room thing) {
        if (roomCoordinates.containsKey(p)) {
            RoomInstance roomInstance = roomCoordinates.get(p);
            return roomInstance;
        }

        RoomInstance roomInstance = new RoomInstance(room, mapData, thing);
        findRoom(p, roomInstance);
        findRoomWallSections(roomInstance);
        rooms.add(roomInstance);

        // Put the thing attributes in
        if (thing != null) {
            for (Point roomPoint : roomInstance.getCoordinates()) {
                TileData tile = mapData.getTile(roomPoint);
                tile.setPlayerId(thing.getPlayerId());
                tile.setHealth((int) (tile.getTerrain().getMaxHealth() * (thing.getInitialHealth() / 100f)));
            }
        }

        Spatial roomNode = handleRoom(roomInstance);
        roomsNode.attachChild(roomNode);

        // Add to registry
        roomNodes.put(roomInstance, roomNode);
        return roomInstance;
    }

    /**
     * Handle top construction on the tile
     *
     * @param tile this tile
     * @param terrain DO NOT REMOVE. Need for construct water bed
     * @param pageNode page node
     */
    private void handleTop(TileData tile, Terrain terrain, Node pageNode) {

        ArtResource model = terrain.getCompleteResource();
        Point p = tile.getLocation();
        Spatial spatial;
        // For water construction type (lava & water), there are 8 pieces (0-7 suffix) in complete resource
        // And in the top resource there is the actual lava/water
        if (terrain.getFlags().contains(Terrain.TerrainFlag.CONSTRUCTION_TYPE_WATER)) {

            // Store the batch instance
            if (!terrainBatchCoordinates.containsKey(p)) {
                EntityInstance<Terrain> entityInstance = new EntityInstance<>(terrain);
                findTerrainBatch(p, entityInstance);
                if (terrain.getFlags().contains(Terrain.TerrainFlag.LAVA)) {
                    lavaBatches.add(entityInstance);
                } else {
                    waterBatches.add(entityInstance);
                }
            }

            spatial = new WaterConstructor(kwdFile).construct(mapData, p.x, p.y, terrain, assetManager, model.getName());

        } else if (terrain.getFlags().contains(Terrain.TerrainFlag.CONSTRUCTION_TYPE_QUAD)) {
            // If this resource is type quad, parse it together. With fixed Hero Lair
            String modelName = (model == null && terrain.getTerrainId() == 35) ? "hero_outpost_floor" : model.getName();
            spatial = new QuadConstructor(kwdFile).construct(mapData, p.x, p.y, terrain, assetManager, modelName);

        } else {

            if (terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)) {
                model = terrain.getTopResource();
            }
            spatial = loadModel(model.getName());
        }

        if (terrain.getFlags().contains(Terrain.TerrainFlag.RANDOM_TEXTURE)) {
            setRandomTexture(spatial, tile);
        }

        Node topTileNode;
        if (terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)) {
            topTileNode = getTileNode(p, (Node) pageNode.getChild(TOP_INDEX));
        } else {
            topTileNode = getTileNode(p, (Node) pageNode.getChild(FLOOR_INDEX));
        }

        topTileNode.attachChild(spatial);
        setTileMaterialToGeometries(tile, topTileNode);
        AssetUtils.translateToTile(topTileNode, p);

        tile.setTopNode(topTileNode);
    }

    private void handleSide(TileData tile, Node pageNode) {
        Point p = tile.getLocation();
        Node sideTileNode = getTileNode(p, (Node) pageNode.getChild(WALL_INDEX));

        for (WallDirection direction : WallDirection.values()) {
            Spatial wall = getWallSpatial(tile, direction);
            if (wall != null) {
                wall.rotate(0, direction.getAngle(), 0);
                sideTileNode.attachChild(wall);
            }
        }

        setTileMaterialToGeometries(tile, sideTileNode);
        AssetUtils.translateToTile(sideTileNode, p);

        tile.setSideNode(sideTileNode);
    }

    public void flashTile(boolean enabled, List<Point> points) {

        for (Point p : points) {
            mapData.getTile(p.x, p.y).setFlashed(enabled);
        }

        updateTiles(points.toArray(new Point[points.size()]));
    }

    /**
     * Get the terrain tile node
     *
     * @param p the tile coordinates
     * @param page the page node
     * @return tile node
     */
    private Node getTileNode(Point p, Node page) {
        return (Node) page.getChild(getTileNodeIndex(p));
    }

    /**
     * Get index for the tile node, where it should be
     *
     * @param p the tile coordinates
     * @return the index inside the page
     */
    private int getTileNodeIndex(Point p) {
        int tileX = p.x - ((int) Math.floor(p.x / (float) PAGE_SQUARE_SIZE)) * PAGE_SQUARE_SIZE;
        int tileY = p.y - ((int) Math.floor(p.y / (float) PAGE_SQUARE_SIZE)) * PAGE_SQUARE_SIZE;
        return tileY * PAGE_SQUARE_SIZE + tileX;
    }

    /**
     * Get the terrain "page" we are on
     *
     * @param p the tile coordinates
     * @param root the root node
     * @return page node
     */
    protected Node getPageNode(Point p, Node root) {
        int pageX = (int) Math.floor(p.x / (float) PAGE_SQUARE_SIZE);
        int pageY = (int) Math.floor(p.y / (float) PAGE_SQUARE_SIZE);

        // Get the page index
        int index = pageX;
        if (pageY > 0) {
            int pagesPerRow = (int) Math.ceil(mapData.getWidth() / (float) PAGE_SQUARE_SIZE);
            index += pagesPerRow * pageY;
        }
        return (Node) root.getChild(index);
    }

    /**
     * Checks if this terrain piece is actually a room and the room type has
     * walls
     *
     * @param tile the terrain tile
     * @return true if this is a room and it has its own walls
     */
    private boolean hasRoomWalls(TileData tile) {
        Terrain terrain = tile.getTerrain();
        if (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)) {

            Room room = kwdFile.getRoomByTerrain(terrain.getTerrainId());
            return hasRoomWalls(room);
        }
        return false;
    }

    public static boolean hasRoomWalls(Room room) {
        return room.getFlags().contains(Room.RoomFlag.HAS_WALLS)
                || room.getTileConstruction() == Room.TileConstruction.HERO_GATE_FRONT_END
                || room.getTileConstruction() == Room.TileConstruction.HERO_GATE_3_BY_1;
    }

    /**
     * Find the room starting from a certain point, rooms are never diagonally
     * attached
     *
     * @param p starting point
     * @param roomInstance the room instance
     */
    private void findRoom(Point p, RoomInstance roomInstance) {
        TileData tile = mapData.getTile(p);
        // Get the terrain
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        if (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)) {

            if (!roomCoordinates.containsKey(p)) {
                if (roomInstance.getRoom().equals(kwdFile.getRoomByTerrain(terrain.getTerrainId()))) {

                    // Add the coordinate
                    roomCoordinates.put(p, roomInstance);
                    roomInstance.addCoordinate(p);

                    // Find north
                    findRoom(new Point(p.x, p.y - 1), roomInstance);

                    // Find east
                    findRoom(new Point(p.x + 1, p.y), roomInstance);

                    // Find south
                    findRoom(new Point(p.x, p.y + 1), roomInstance);

                    // Find west
                    findRoom(new Point(p.x - 1, p.y), roomInstance);
                }
            }
        }
    }

    /**
     * Find a terrain batch starting from a certain point, they are never
     * diagonally attached
     *
     * @param p starting point
     * @param entityInstance the batch instance
     */
    private void findTerrainBatch(Point p, EntityInstance<Terrain> entityInstance) {
        TileData tile = mapData.getTile(p);

        if (!terrainBatchCoordinates.containsKey(p)) {

            // Get the terrain
            Terrain terrain = tile.getTerrain();
            Terrain bridgeTerrain = kwdFile.getTerrainBridge(tile.getFlag(), terrain);
            if (bridgeTerrain != null) {
                terrain = bridgeTerrain;
            }

            if (entityInstance.getEntity().equals(terrain)) {

                // Add the coordinate
                terrainBatchCoordinates.put(p, entityInstance);
                entityInstance.addCoordinate(p);

                // Find north
                findTerrainBatch(new Point(p.x, p.y - 1), entityInstance);

                // Find east
                findTerrainBatch(new Point(p.x + 1, p.y), entityInstance);

                // Find south
                findTerrainBatch(new Point(p.x, p.y + 1), entityInstance);

                // Find west
                findTerrainBatch(new Point(p.x - 1, p.y), entityInstance);
            }
        }
    }

    /**
     * Constructs the given room
     *
     * @param roomInstance the room instance
     */
    private Spatial handleRoom(RoomInstance roomInstance) {
        GenericRoom room = RoomConstructor.constructRoom(roomInstance, assetManager, effectManager, worldState, objectLoader);
        roomActuals.put(roomInstance, room);
        updateRoomWalls(roomInstance);
        return room.construct();
    }

    /**
     * Sets material lighting accorting to the terrain setting
     *
     * @param material the material to adjust
     * @param terrain the terrain data
     */
    public static void setTerrainMaterialLighting(Material material, Terrain terrain) {

        // Ambient light
        if (terrain.getFlags().contains(Terrain.TerrainFlag.AMBIENT_LIGHT)) {
            Color c = terrain.getAmbientLight();
            int r = c.getRed();
            if (terrain.getFlags().contains(Terrain.TerrainFlag.AMBIENT_COLOR_RED)) {
                r += 256;
            }
            int g = c.getGreen();
            if (terrain.getFlags().contains(Terrain.TerrainFlag.AMBIENT_COLOR_GREEN)) {
                g += 256;
            }
            int b = c.getBlue();
            if (terrain.getFlags().contains(Terrain.TerrainFlag.AMBIENT_COLOR_BLUE)) {
                b += 256;
            }
            material.setColor("Ambient", new ColorRGBA(r / 255f, g / 255f, b / 255f, 0));
        }

        // Not sure what the terrain light is supposed to be
        if (terrain.getFlags().contains(Terrain.TerrainFlag.TERRAIN_LIGHT)) {
            Color c = terrain.getTerrainLight();
            int r = c.getRed();
            if (terrain.getFlags().contains(Terrain.TerrainFlag.TERRAIN_COLOR_RED)) {
                r += 256;
            }
            int g = c.getGreen();
            if (terrain.getFlags().contains(Terrain.TerrainFlag.TERRAIN_COLOR_GREEN)) {
                g += 256;
            }
            int b = c.getBlue();
            if (terrain.getFlags().contains(Terrain.TerrainFlag.TERRAIN_COLOR_BLUE)) {
                b += 256;
            }
            material.setColor("Specular", new ColorRGBA(r / 255f, g / 255f, b / 255f, 0));
        }
        material.setColor("Diffuse", ColorRGBA.White);
        material.setBoolean("UseMaterialColors", false); // Hmm...
    }

    /**
     * Get coordinate / room instance mapping
     *
     * @return mapping
     */
    public HashMap<Point, RoomInstance> getRoomCoordinates() {
        return roomCoordinates;
    }

    /**
     * Get list of room instances
     *
     * @return room instances
     */
    protected List<RoomInstance> getRooms() {
        return rooms;
    }

    /**
     * Remove all the given room instances (and actually removes them from the
     * scene)
     *
     * @param instances room instances to remove
     */
    protected void removeRoomInstances(RoomInstance... instances) {
        for (RoomInstance instance : instances) {
            roomsNode.detachChild(roomNodes.get(instance));
            roomNodes.remove(instance);
            rooms.remove(instance);

            // Signal the room
            GenericRoom room = roomActuals.get(instance);
            room.destroy();

            roomActuals.remove(instance);
            for (Point p : instance.getCoordinates()) {
                roomCoordinates.remove(p);
            }
        }
    }

    /**
     * Find room wall sections, continuous sections facing the same way
     *
     * @param roomInstance room instance
     */
    private void findRoomWallSections(RoomInstance roomInstance) {
        if (hasRoomWalls(roomInstance.getRoom())) {
            List<WallSection> sections = new ArrayList<>();
            Map<Point, Set<WallDirection>> alreadyWalledPoints = new HashMap<>();
            for (Point p : roomInstance.getCoordinates()) {

                // Traverse in all four directions to find wallable sections facing the same direction
                traverseRoomWalls(p, roomInstance, WallDirection.NORTH, sections, alreadyWalledPoints);
                traverseRoomWalls(p, roomInstance, WallDirection.EAST, sections, alreadyWalledPoints);
                traverseRoomWalls(p, roomInstance, WallDirection.SOUTH, sections, alreadyWalledPoints);
                traverseRoomWalls(p, roomInstance, WallDirection.WEST, sections, alreadyWalledPoints);
            }
            roomInstance.setWallSections(sections);
        }
    }

    /**
     * Traverses room walls facing in certain direction from a room point. Finds
     * a section.
     *
     * @param p starting room point
     * @param roomInstance the room instance of which walls we need to find
     * @param direction the direction of which the want the walls to face
     * @param sections list of already find sections
     * @param alreadyWalledPoints already found wall points
     */
    private void traverseRoomWalls(Point p, RoomInstance roomInstance, WallDirection direction,
            List<WallSection> sections, Map<Point, Set<WallDirection>> alreadyWalledPoints) {

        Set<WallDirection> alreadyTraversedDirections = alreadyWalledPoints.get(p);
        if (alreadyTraversedDirections == null || !alreadyTraversedDirections.contains(direction)) {

            List<Point> section = getRoomWalls(p, roomInstance, direction);
            if (section != null) {

                // Add section
                sections.add(new WallSection(direction, section));

                // Add to known points
                for (Point sectionPoint : section) {
                    Set<WallDirection> directions = alreadyWalledPoints.get(sectionPoint);
                    if (directions == null) {
                        directions = EnumSet.noneOf(WallDirection.class);
                    }
                    directions.add(direction);
                    alreadyWalledPoints.put(sectionPoint, directions);
                }
            }
        }
    }

    private List<Point> getRoomWalls(Point p, RoomInstance roomInstance, WallDirection wallDirection) {

        // See if the starting point has a wall to the given direction
        TileData tile;
        if (wallDirection == WallDirection.NORTH) {
            tile = mapData.getTile(p.x, p.y + 1);
        } else if (wallDirection == WallDirection.EAST) {
            tile = mapData.getTile(p.x - 1, p.y);
        } else if (wallDirection == WallDirection.SOUTH) {
            tile = mapData.getTile(p.x, p.y - 1);
        } else {
            tile = mapData.getTile(p.x + 1, p.y); // West
        }
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        if (terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)
                && terrain.getFlags().contains(Terrain.TerrainFlag.ALLOW_ROOM_WALLS)) {

            // Found wallable
            List<Point> wallPoints = new ArrayList<>();
            wallPoints.add(p);

            // Traverse possible directions, well one direction, "to the right"
            List<Point> adjacentWallPoints = null;
            if (wallDirection == WallDirection.NORTH) {
                Point nextPoint = new Point(p.x + 1, p.y); // East
                RoomInstance instance = roomCoordinates.get(nextPoint);
                if (instance != null && instance.equals(roomInstance)) {
                    adjacentWallPoints = getRoomWalls(nextPoint, roomInstance, wallDirection);
                }
            } else if (wallDirection == WallDirection.EAST) {
                Point nextPoint = new Point(p.x, p.y + 1); // South
                RoomInstance instance = roomCoordinates.get(nextPoint);
                if (instance != null && instance.equals(roomInstance)) {
                    adjacentWallPoints = getRoomWalls(nextPoint, roomInstance, wallDirection);
                }
            } else if (wallDirection == WallDirection.SOUTH) {
                Point nextPoint = new Point(p.x + 1, p.y); // East, sorting, so right is left now
                RoomInstance instance = roomCoordinates.get(nextPoint);
                if (instance != null && instance.equals(roomInstance)) {
                    adjacentWallPoints = getRoomWalls(nextPoint, roomInstance, wallDirection);
                }
            } else {
                Point nextPoint = new Point(p.x, p.y + 1); // South, sorting, so right is left now
                RoomInstance instance = roomCoordinates.get(nextPoint);
                if (instance != null && instance.equals(roomInstance)) {
                    adjacentWallPoints = getRoomWalls(nextPoint, roomInstance, wallDirection);
                }
            }

            // Add the point(s)
            if (adjacentWallPoints != null) {
                wallPoints.addAll(adjacentWallPoints);
            }
            return wallPoints;
        }

        return null;
    }

    public Point[] getSurroundingTiles(Point point, boolean diagonal) {

        // Get all surrounding tiles
        List<Point> tileCoords = new ArrayList<>(diagonal ? 9 : 5);
        tileCoords.add(point);

        addIfValidCoordinate(point.x, point.y - 1, tileCoords); // North
        addIfValidCoordinate(point.x + 1, point.y, tileCoords); // East
        addIfValidCoordinate(point.x, point.y + 1, tileCoords); // South
        addIfValidCoordinate(point.x - 1, point.y, tileCoords); // West
        if (diagonal) {
            addIfValidCoordinate(point.x - 1, point.y - 1, tileCoords); // NW
            addIfValidCoordinate(point.x + 1, point.y - 1, tileCoords); // NE
            addIfValidCoordinate(point.x - 1, point.y + 1, tileCoords); // SW
            addIfValidCoordinate(point.x + 1, point.y + 1, tileCoords); // SE
        }

        return tileCoords.toArray(new Point[tileCoords.size()]);
    }

    private void addIfValidCoordinate(final int x, final int y, List<Point> tileCoords) {
        TileData tile = mapData.getTile(x, y);
        if (tile != null) {
            tileCoords.add(tile.getLocation());
        }
    }

    /**
     * Update the selected rooms' walls
     *
     * @param rooms the rooms to update
     */
    protected void updateRoomWalls(RoomInstance... rooms) {
        for (RoomInstance room : rooms) {
            findRoomWallSections(room);
        }
    }

    protected void updateRoomWalls(List<RoomInstance> rooms) {
        updateRoomWalls(rooms.toArray(new RoomInstance[rooms.size()]));
    }

    /**
     * Redraws a room instance
     *
     * @param roomInstance the instance to redraw
     */
    protected void updateRoom(RoomInstance roomInstance) {

        // Redraw
        updateRoomWalls(roomInstance);
        roomActuals.get(roomInstance).construct();
//        roomsNode.attachChild(roomActuals.get(roomInstance).construct());
    }

    /**
     * Get a rooms by knowing its instance
     *
     * @return room
     */
    public Map<RoomInstance, GenericRoom> getRoomActuals() {
        return roomActuals;
    }

    /**
     * Get rooms by function.<br> FIXME: Should the player have ready lists?
     *
     * @param objectType the function
     * @param playerId the player id, can be null
     * @return list of rooms that match the criteria
     */
    public List<GenericRoom> getRoomsByFunction(GenericRoom.ObjectType objectType, Short playerId) {
        List<GenericRoom> roomsList = new ArrayList<>();
        for (Entry<RoomInstance, GenericRoom> entry : roomActuals.entrySet()) {
            if (playerId != null && entry.getKey().getOwnerId() != playerId) {
                continue;
            }
            if (entry.getValue().hasObjectControl(objectType)) {
                roomsList.add(entry.getValue());
            }
        }
        return roomsList;
    }

    /**
     * If you want to monitor the map loading progress, use this method
     *
     * @param progress current progress from 0.0 to 1.0
     */
    protected abstract void updateProgress(final float progress);

}
