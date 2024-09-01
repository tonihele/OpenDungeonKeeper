/*
 * Copyright (C) 2014-2016 OpenKeeper
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
package toniarts.openkeeper.utils;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.MaterialKey;
import com.jme3.asset.ModelKey;
import com.jme3.asset.TextureKey;
import com.jme3.asset.cache.AssetCache;
import com.jme3.asset.cache.SimpleAssetCache;
import com.jme3.asset.cache.WeakRefAssetCache;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.AWTLoader;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.imageio.ImageIO;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.cinematics.CameraSweepData;
import toniarts.openkeeper.cinematics.CameraSweepDataLoader;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;

/**
 * Collection of asset related common functions
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class AssetUtils {

    private static final Logger logger = System.getLogger(AssetUtils.class.getName());
    
    private final static Object ASSET_LOCK = new Object();
    private final static AssetCache ASSET_CACHE = new SimpleAssetCache();
    private final static AssetCache WEAK_ASSET_CACHE = new WeakRefAssetCache();
    private final static Map<String, Boolean> TEXTURE_MAP_CACHE = new HashMap<>();

    // Custom model data keys
    public final static String USER_DATA_KEY_REMOVABLE = "Removable";
    
    private static volatile boolean preWarmedAssets = false;

    private AssetUtils() {
        // Nope
    }

    /**
     * Loads a model. The model is cached on the first call and loaded from
     * cache.
     *
     * @param assetManager the asset manager to use
     * @param modelName the model name, the model name is checked and fixed
     * @param useCache use cache or not
     * @param useWeakCache use weak cache, if not then permanently cache the
     * models. Use weak cache to load some models that are not often needed
     * (water bed etc.)
     * @return a cloned instance from the cache
     */
    public static Spatial loadModel(final AssetManager assetManager, String modelName,
            ArtResource artResource, final boolean useCache, final boolean useWeakCache) {

        String filename = AssetsConverter.MODELS_FOLDER + File.separator + modelName + ".j3o";
        ModelKey assetKey = new ModelKey(getCanonicalAssetKey(filename));

        Spatial result;
        if (useCache) {

            // Set the correct asset cache
            final AssetCache cache = (useWeakCache) ? WEAK_ASSET_CACHE : ASSET_CACHE;

            // Get the model from cache
            Spatial model = cache.getFromCache(assetKey);
            if (model == null) {
                model = loadModel(assetManager, assetKey, artResource);

                cache.addToCache(assetKey, model);
            }
            result = model.clone();
        } else {
            result = loadModel(assetManager, assetKey, artResource);
        }

        return result;
    }

    private static Spatial loadModel(final AssetManager assetManager, ModelKey assetKey, ArtResource artResource) {
        Spatial model = assetManager.loadModel(assetKey);
        resetSpatial(model);
        
        // Assign maps
        assignMapsToMaterial(model, assetManager);
        
        // Create possible animating textures
        // This information is not found directly in KMF... at least to my knowledge
        if(artResource != null && artResource.getFlags().contains(ArtResource.ArtResourceFlag.ANIMATING_TEXTURE) && 
                !artResource.getFlags().contains(ArtResource.ArtResourceFlag.USE_ANIMATING_TEXTURE_FOR_SELECTION)) {
            assignAnimatingTextures(model, assetManager, artResource.getData("fps"));
        }
        
        return model;
    }
    
    private static void assignAnimatingTextures(Spatial model, AssetManager assetManager, Integer fps) {
        model.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                List<String> textures = spatial.getUserData(KmfModelLoader.MATERIAL_ALTERNATIVE_TEXTURES);
                if (spatial instanceof Geometry && textures != null && textures.size() > 1) {
                    Material material = ((Geometry) spatial).getMaterial();
                    material = createLightningSpriteMaterial(material.getName(), material.isTransparent(), fps, () -> {
                        return textures;
                    }, assetManager);
                    spatial.setMaterial(material);
                }
            }

        });
    }

    /**
     * Only for ModelViewer
     *
     * @param assetManager
     * @param modelName
     * @return
     */
    public static Spatial loadAsset(final AssetManager assetManager, String modelName, ArtResource artResource) {

        String filename = AssetsConverter.MODELS_FOLDER + File.separator + modelName + ".j3o";
        ModelKey assetKey = new ModelKey(getCanonicalAssetKey(filename));

        Spatial result = loadModel(assetManager, assetKey, artResource);

        return result;
    }

    public static Spatial loadModel(final AssetManager assetManager, String resourceName,
            final boolean useWeakCache) {

        return loadModel(assetManager, resourceName, null, true, useWeakCache);
    }

    public static Spatial loadModel(final AssetManager assetManager, String resourceName, ArtResource artResource) {
        return loadModel(assetManager, resourceName, artResource, true, false);
    }

    public static CameraSweepData loadCameraSweep(final AssetManager assetManager, String resourceName) {
        String filename = AssetsConverter.PATHS_FOLDER + File.separator + resourceName + "."
                + CameraSweepDataLoader.FILE_EXTENSION;
        String assetKey = getCanonicalAssetKey(filename);

        Object asset = assetManager.loadAsset(assetKey);

        if (asset == null || !(asset instanceof CameraSweepData)) {
            String msg = "Failed to load the camera sweep file " + resourceName + "!";
            logger.log(Level.ERROR, msg);
            throw new RuntimeException(msg);
        }

        return (CameraSweepData) asset;
    }

    private static void assignMapsToMaterial(Spatial model, AssetManager assetManager) {
        model.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                if (spatial instanceof Geometry) {
                    Material material = ((Geometry) spatial).getMaterial();
                    assignMapsToMaterial(assetManager, material);
                }
            }

        });
    }

    /**
     * Assign different kind of maps (Specular, Norma, etc.) to material, if
     * found
     *
     * @param assetManager the asset manager
     * @param material the material to apply to
     */
    public static void assignMapsToMaterial(AssetManager assetManager, Material material) {

        // Unharmed texture
        String diffuseTexture = ((Texture) material.getParam("DiffuseMap").getValue()).getKey().getName();

        assignMapToMaterial(assetManager, material, "NormalMap", getNormalMapName(diffuseTexture));
        assignMapToMaterial(assetManager, material, "SpecularMap", getSpecularMapName(diffuseTexture));
        assignMapToMaterial(assetManager, material, "ParallaxMap", getDisplacementMapName(diffuseTexture));
    }

    private static void assignMapToMaterial(AssetManager assetManager, Material material, String paramName, String textureName) {

        // Try to locate the texture
        Boolean found = TEXTURE_MAP_CACHE.get(textureName);
        if (found == null) {
            TextureKey textureKey = new TextureKey(textureName, false);

            // See if it exists
            AssetInfo assetInfo = assetManager.locateAsset(textureKey);
            found = (assetInfo != null);
            TEXTURE_MAP_CACHE.put(textureName, found);
        }

        // Set it
        if (found) {
            TextureKey textureKey = new TextureKey(textureName, false);
            material.setTexture(paramName, assetManager.loadTexture(textureKey));
        } else {
            material.clearParam(paramName);
        }
    }

    private static String getNormalMapName(String texture) {
        return getCustomTextureMapName(texture, "n");
    }

    private static String getSpecularMapName(String texture) {
        return getCustomTextureMapName(texture, "s");
    }

    private static String getDisplacementMapName(String texture) {
        return getCustomTextureMapName(texture, "p");
    }

    private static String getCustomTextureMapName(String texture, String suffix) {
        int extensionIndex = texture.lastIndexOf(".");
        return texture.substring(0, extensionIndex).concat("_").concat(suffix).concat(texture.substring(extensionIndex));
    }

    /**
     * Creates a material from an ArtResource
     *
     * @param resource the ArtResource
     * @param assetManager the asset manager
     * @return JME material
     */
    public static Material createLightningSpriteMaterial(ArtResource resource, AssetManager assetManager) {
        if (resource.getFlags().contains(ArtResource.ArtResourceFlag.ANIMATING_TEXTURE) &&
                !resource.getFlags().contains(ArtResource.ArtResourceFlag.USE_ANIMATING_TEXTURE_FOR_SELECTION)) {
            return createLightningSpriteMaterial(resource.getName(), 
                    resource.getType() == ArtResource.ArtResourceType.ALPHA, resource.getData("fps"), () -> {
                        return getTextureFrames(resource);
                    }, assetManager);
        }
        return null;
    }

    private static Material createLightningSpriteMaterial(String name, boolean hasAlpha, Integer fps, Supplier<List<String>> texturesSupplier, AssetManager assetManager) {
        
        // Cache
        MaterialKey assetKey = new MaterialKey(name);
        Material mat = ASSET_CACHE.getFromCache(assetKey);
        
        if (mat == null) {
            List<String> textures = texturesSupplier.get();
            mat = new Material(assetManager, "MatDefs/LightingSprite.j3md");
            mat.setInt("NumberOfTiles", textures.size());
            mat.setInt("Speed", fps != null ? fps : 30);
            
            // Create the texture
            try {
                
                if (hasAlpha) {
                    mat.setTransparent(true);
                    mat.setFloat("AlphaDiscardThreshold", 0.1f);
                    mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
                }
                
                Texture tex = createAnimatingTexture(name, hasAlpha, textures, assetManager);
                
                // Load the texture up
                mat.setTexture("DiffuseMap", tex);
            } catch (Exception e) {
                logger.log(Level.ERROR, "Can't create a texture out of " + name + "!", e);
            }

            // Add to cache
            ASSET_CACHE.addToCache(assetKey, mat);
        }
        return mat.clone();
    }

    /**
     * Create particle type material from ArtResource
     *
     * @param resource the ArtResource
     * @param assetManager the asset manager
     * @return JME material
     */
    public static Material createParticleMaterial(ArtResource resource, AssetManager assetManager) {

        // Cache
        MaterialKey assetKey = new MaterialKey(resource.getName());
        Material mat = ASSET_CACHE.getFromCache(assetKey);

        if (mat == null) {
            mat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
            try {
                mat.setTexture("Texture", createArtResourceTexture(resource, assetManager));
            } catch (Exception e) {
                logger.log(Level.ERROR, "Can't create a texture out of " + resource + "!", e);
            }

            // Add to cache
            ASSET_CACHE.addToCache(assetKey, mat);
        }
        return mat.clone();
    }

    private static Texture createArtResourceTexture(ArtResource resource, AssetManager assetManager) throws IOException {
        String assetFolder = AssetsConverter.TEXTURES_FOLDER + File.separator;

        if (resource.getFlags().contains(ArtResource.ArtResourceFlag.ANIMATING_TEXTURE)) {
            return createAnimatingTexture(resource.getName(),  
                    resource.getType() == ArtResource.ArtResourceType.ALPHA,
                    getTextureFrames(resource), assetManager);
        } else {
            if (resource.getType().equals(ArtResource.ArtResourceType.SPRITE)
                    && resource.getData(ArtResource.KEY_WIDTH).intValue() > 1) {
                // only the unused sprites have a size of bigger than one
                assetFolder = AssetsConverter.SPRITES_FOLDER + File.separator;
            }

            // A regular texture
            TextureKey key = new TextureKey(getCanonicalAssetKey(assetFolder + resource.getName() + ".png"), false);
            return assetManager.loadTexture(key);
        }
    }
    
    private static List<String> getTextureFrames(ArtResource resource) {
        int frames = resource.getData(ArtResource.KEY_FRAMES);
        String assetFolder = AssetsConverter.TEXTURES_FOLDER + File.separator;
        List<String> framesList = new ArrayList<>(frames);
        for (int x = 0; x < frames; x++) {
            framesList.add(assetFolder + resource.getName() + x + ".png");
        }
        
        return framesList;
    }

    private static Texture createAnimatingTexture(String name, boolean hasAlpha, List<String> textures, AssetManager assetManager) throws IOException {
        
        // Get the first frame, the frames need to be same size
        BufferedImage img = readImageFromAsset(assetManager.locateAsset(new AssetKey(getCanonicalAssetKey(textures.get(0)))));
        
        // Create image big enough to fit all the frames
        BufferedImage text = new BufferedImage(img.getWidth() * textures.size(), img.getHeight(),
                hasAlpha ? BufferedImage.TYPE_INT_ARGB : img.getType());
        Graphics2D g = text.createGraphics();
        
        // If the source image doesn't have an alpha channel but the art resource wants one... Apply 75% opacity. See water
        if (hasAlpha && !img.getColorModel().hasAlpha()) {
            AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC, 0.75f);
            g.setComposite(ac);
        }
        
        g.drawImage(img, null, 0, 0);
        for (int x = 1; x < textures.size(); x++) {
            AssetInfo asset = assetManager.locateAsset(new AssetKey(getCanonicalAssetKey(textures.get(x))));
            if (asset != null) {
                img = readImageFromAsset(asset);
            } else {
                // use previous img
                logger.log(Level.WARNING, "Animated Texture {0}{1} not found", new Object[]{name, x});
            }
            g.drawImage(img, null, img.getWidth() * x, 0);
        }
        g.dispose();
        
        // Convert the new image to a texture
        AWTLoader loader = new AWTLoader();
        Texture tex = new Texture2D(loader.load(text, false));
        return tex;
    }

    public static BufferedImage readImageFromAsset(AssetInfo asset) throws IOException {
        ImageIO.setUseCache(false);
        try (InputStream is = asset.openStream();
                BufferedInputStream bis = new BufferedInputStream(is)) {
            return ImageIO.read(bis);
        }
    }

    /**
     * Preloads all assets, to memory and to the GPU. May take some time, but
     * everything works smoothly after. Enqueues the actual GPU loading to the
     * main render loop.
     *
     * @param kwdFile the KWD file to scan for the loadable assets
     * @param assetManager the asset manager
     * @param app the app
     */
    public static void prewarmAssets(KwdFile kwdFile, AssetManager assetManager, Main app) {
        if (!preWarmedAssets) {
            synchronized (ASSET_LOCK) {
                if (!preWarmedAssets) {
                    try {

                        // Objects
                        prewarmArtResources(new ArrayList<>(kwdFile.getObjectList()), assetManager, app);

                        // Creatures
                        prewarmArtResources(new ArrayList<>(kwdFile.getCreatureList()), assetManager, app);

                        // Doors
                        prewarmArtResources(kwdFile.getDoors(), assetManager, app);

                        // Traps
                        prewarmArtResources(kwdFile.getTraps(), assetManager, app);

                        // Terrain
                        prewarmArtResources(new ArrayList<>(kwdFile.getTerrainList()), assetManager, app);

                        // Rooms
                        prewarmArtResources(kwdFile.getRooms(), assetManager, app);
                    } catch (Exception e) {
                        logger.log(Level.ERROR, "Failed to prewarm assets!", e);
                    } finally {
                        preWarmedAssets = true;
                    }
                }
            }
        }
    }

    private static void prewarmArtResources(List<?> objects, AssetManager assetManager, Main app) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, Exception {

        // Get the fields that house a possible ArtResource
        Class clazz = objects.get(0).getClass();
        Method[] methods = clazz.getMethods();
        List<Method> methodsToScan = new ArrayList<>();
        for (Method method : methods) {
            if (method.getReturnType().equals(ArtResource.class) && method.getParameterCount() == 0) {
                methodsToScan.add(method);
            }
        }

        // Scan every object
        List<Spatial> models = new ArrayList<>(methodsToScan.size() * objects.size());
        if (!methodsToScan.isEmpty()) {
            for (Object obj : objects) {
                for (Method method : methodsToScan) {
                    ArtResource artResource = (ArtResource) method.invoke(obj);
                    if (artResource != null && artResource.getFlags().contains(ArtResource.ArtResourceFlag.PRELOAD)) {

                        try {

                            // TODO: if possible, we should have here a general loadAsset(ArtResource) stuff
                            if (artResource.getType() == ArtResource.ArtResourceType.MESH
                                    || artResource.getType() == ArtResource.ArtResourceType.ANIMATING_MESH
                                    || artResource.getType() == ArtResource.ArtResourceType.MESH_COLLECTION
                                    || artResource.getType() == ArtResource.ArtResourceType.PROCEDURAL_MESH) {
                                models.add(loadModel(assetManager, artResource.getName(), artResource));
                            } else if (artResource.getType() == ArtResource.ArtResourceType.TERRAIN_MESH && obj instanceof Terrain terrain) {

                                // With terrains, we need to see the contruction type
                                if (method.getName().startsWith("getTaggedTopResource") || method.getName().startsWith("getSideResource")) {
                                    models.add(loadModel(assetManager, artResource.getName(), artResource));
                                } else if (terrain.getFlags().contains(Terrain.TerrainFlag.CONSTRUCTION_TYPE_QUAD)) {
                                    for (int i = 0; i < 5; i++) {
                                        if (terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE)) {
                                            for (int y = 0; y < 7; y++) {
                                                models.add(loadModel(assetManager, artResource.getName() + y + "_" + i, artResource, true, false));
                                            }
                                        } else {
                                            models.add(loadModel(assetManager, artResource.getName() + i, artResource));
                                        }
                                    }
                                } // TODO: No water... it is done in Water.java, need to tweak somehow
                                else if (!terrain.getFlags().contains(Terrain.TerrainFlag.CONSTRUCTION_TYPE_WATER)) {
                                    models.add(loadModel(assetManager, artResource.getName(), artResource));
                                }
                            } else if (artResource.getType() == ArtResource.ArtResourceType.TERRAIN_MESH && obj instanceof Room room) {

                                // With terrains, we need to see the contruction type
                                int count = 0;
                                int start = 0;
                                switch (room.getTileConstruction()) {
                                    case NORMAL: {
                                        count = 10;
                                        break;
                                    }
                                    case QUAD: {
                                        count = 4;
                                        break;
                                    }
                                    case DOUBLE_QUAD: {
                                        count = 15; // Hmm not perfect, see Prison
                                        break;
                                    }
                                    case _3_BY_3_ROTATED:
                                    case _3_BY_3: {
                                        count = 9;
                                        break;
                                    }
                                    case HERO_GATE_2_BY_2:
                                    case _5_BY_5_ROTATED: {
                                        count = 4;
                                        break;
                                    }
                                    case HERO_GATE_FRONT_END: {
                                        count = 17;
                                        start = 1;
                                        break;
                                    }
                                }
                                for (int i = start; i < count; i++) {
                                    models.add(loadModel(assetManager, artResource.getName() + i, false));
                                }
                            }

                        } catch (Exception e) {
                            throw new Exception("Failed to load " + artResource + " on object " + obj + "!", e);
                        }
                    }
                }
            }
        }

        // Enque the warming up, we need GL context
        if (!models.isEmpty()) {
            logger.log(Level.INFO, "Prewarming {0} objects!", models.size());
            app.enqueue(() -> {

                for (Spatial spatial : models) {
                    app.getRenderManager().preloadScene(spatial);
                }

                return null;
            });
        }
    }

    /**
     * Sets model highlight with selected color. Technically sets the material's
     * ambient color to one of your choosings
     *
     * @param spatial the spatial which to highlight
     * @param highlightColor the highlight color
     * @param enabled turn the effect on/off
     */
    public static void setModelHighlight(Spatial spatial, ColorRGBA highlightColor, boolean enabled) {
        spatial.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {

                if (!(spatial instanceof Geometry)) {
                    return;
                }

                // Don't highlight non-removables
                if (Boolean.FALSE.equals(spatial.getUserData(AssetUtils.USER_DATA_KEY_REMOVABLE))
                        || Boolean.FALSE.equals(spatial.getParent().getParent().getUserData(AssetUtils.USER_DATA_KEY_REMOVABLE))) {
                    return;
                }

                try {
                    Material material = ((Geometry) spatial).getMaterial();
                    if (material.getMaterialDef().getMaterialParam("Ambient") != null) {
                        material.setColor("Ambient", highlightColor);
                    } else {
                        material.setColor("Color", highlightColor);
                    }
                    if (material.getMaterialDef().getMaterialParam("UseMaterialColors") != null) {
                        material.setBoolean("UseMaterialColors", enabled);
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to set material color!", e);
                }
            }
        });
    }

    /**
     * Reset translation of spatial and all children
     *
     * @param spatial the spatial to reset
     */
    public static void resetSpatial(Spatial spatial) {
        /*
        if (spatial instanceof Node) {
            for (Spatial subSpat : ((Node) spatial).getChildren()) {
                subSpat.setLocalTranslation(0, 0, 0);
            }
        } else {
            spatial.setLocalTranslation(0, 0, 0);
        }
         */
        spatial.breadthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                spatial.setLocalTranslation(0, 0, 0);
            }
        });
    }

    public static void translateToTile(final Spatial spatial, final Point tile) {
        spatial.setLocalTranslation(WorldUtils.pointToVector3f(tile));
    }

    public static void scale(final Spatial spatial) {
        spatial.scale(WorldUtils.TILE_WIDTH, WorldUtils.TILE_HEIGHT, WorldUtils.TILE_WIDTH);
    }

    /**
     * Creates a blueprint material for the wanted spatial
     *
     * @param assetManager the asset manager
     * @param spatial the spatial which to change to blueprint
     */
    public static void setBlueprint(AssetManager assetManager, Spatial spatial) {
        spatial.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                if (!(spatial instanceof Geometry)) {
                    return;
                }

                // Don't highlight non-removables
                if (Boolean.FALSE.equals(spatial.getUserData(AssetUtils.USER_DATA_KEY_REMOVABLE))) {
                    return;
                }

                try {
                    Material mat = new Material(assetManager,
                            "Common/MatDefs/Misc/Unshaded.j3md");
                    mat.setColor("Color", new ColorRGBA(0, 0, 0.8f, 0.4f));
                    mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
                    spatial.setMaterial(mat);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to set material color!", e);
                }
            }
        });
    }

    /**
     * Generate procedural mesh TODO: procedural mesh
     *
     * @param resource
     * @return generated mesh
     */
    public static Spatial createProceduralMesh(ArtResource resource) {
        int id = resource.getData(ArtResource.KEY_ID);

        return new Node();
    }

    /**
     * Returns case sensitive and valid asset key for loading the given asset
     *
     * @param asset the asset key, i.e. Textures\GUI/wrongCase.png
     * @return fully qualified and working asset key
     */
    public static String getCanonicalAssetKey(String asset) {
        return PathUtils.getCanonicalRelativePath(AssetsConverter.getAssetsFolder(), asset).replaceAll(PathUtils.QUOTED_FILE_SEPARATOR, "/");
    }

}
