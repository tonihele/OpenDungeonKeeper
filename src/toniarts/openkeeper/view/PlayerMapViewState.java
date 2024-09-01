/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.view;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityContainer;
import com.simsilica.es.EntityData;
import toniarts.openkeeper.utils.Point;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Mana;
import toniarts.openkeeper.game.component.MapTile;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.listener.MapListener;
import toniarts.openkeeper.game.listener.PlayerActionListener;
import toniarts.openkeeper.game.map.AbstractMapTileInformation;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.map.MapInformation;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.modelviewer.Debug;
import toniarts.openkeeper.view.map.FlashTileViewState;
import toniarts.openkeeper.view.map.MapViewController;
import toniarts.openkeeper.world.effect.EffectManagerState;
import toniarts.openkeeper.world.listener.RoomListener;
import toniarts.openkeeper.world.listener.TileChangeListener;

/**
 * Handles the handling of game world for a player, visually
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class PlayerMapViewState extends AbstractAppState implements MapListener, PlayerActionListener {
    
    private static final Logger logger = System.getLogger(PlayerMapViewState.class.getName());

    private Main app;
    private AppStateManager stateManager;
    private final IMapInformation mapInformation;
    private final MapViewController mapLoader;
    private final KwdFile kwdFile;
    private final AssetManager assetManager;
    private final MapTileContainer mapTileContainer;
    private Node worldNode;
    private final EffectManagerState effectManager;
    private List<TileChangeListener> tileChangeListener;
    private Map<Short, List<RoomListener>> roomListeners;
    private final FlashTileViewState flashTileControl;

    public PlayerMapViewState(Main app, final KwdFile kwdFile, final AssetManager assetManager, Collection<Keeper> players, EntityData entityData, short playerId, ILoadCompleteNotifier loadCompleteNotifier) {
        this.app = app;
        this.kwdFile = kwdFile;
        this.assetManager = assetManager;

        // World node
        worldNode = new Node("World");
        if (Main.isDebug()) {
            Debug.showNodeAxes(assetManager, worldNode, 10);
        }

        // Make sure we load the whole map before we continue
        this.mapTileContainer = new MapTileContainer(entityData, kwdFile) {

            @Override
            protected void onLoadComplete() {

                // Don't block the caller, might be called from the render thread...
                Thread mapLoaderThread = new Thread(() -> {

                    Spatial map = mapLoader.load(assetManager, kwdFile);
                    app.enqueue(() -> {
                        worldNode.attachChild(map);

                        loadCompleteNotifier.onLoadComplete();
                    });
                }, "GameClientMapLoader");
                mapLoaderThread.start();
            }

        };

        this.mapInformation = new MapInformation(mapTileContainer, kwdFile, players);

        // Effect manager
        effectManager = new EffectManagerState(kwdFile, assetManager);

        // Create the actual map
        this.mapLoader = new MapViewController(assetManager, kwdFile, mapInformation, playerId) {

            @Override
            protected void updateProgress(float progress) {
                PlayerMapViewState.this.updateProgress(progress);
            }

        };

        this.flashTileControl = new FlashTileViewState(mapLoader);

        // Start collecting the map entities
        this.mapTileContainer.start();
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);
        this.app = (Main) app;
        this.stateManager = stateManager;

        // Effects
        this.stateManager.attach(effectManager);

        // Tile flash state
        this.stateManager.attach(flashTileControl);

        // Attach the world
        this.app.getRootNode().attachChild(worldNode);
    }

    @Override
    public void cleanup() {

        // Detach our map
        if (worldNode != null) {
            app.getRootNode().detachChild(worldNode);
            worldNode = null;
        }

        // Tile flash state
        this.stateManager.detach(flashTileControl);

        // Effects
        this.stateManager.detach(effectManager);

        // The actual map data
        this.mapTileContainer.stop();

        super.cleanup();
    }

    @Override
    public void update(float tpf) {
        this.mapTileContainer.update();
    }

    public AssetManager getAssetManager() {
        return assetManager;
    }

    /**
     * If you want to monitor the map loading progress, use this method
     *
     * @param progress current progress from 0.0 to 1.0
     */
    protected abstract void updateProgress(final float progress);

    @Override
    public void onTilesChange(List<Point> updatedTiles) {
//        Point[] points = new Point[updatedTiles.size()];
//        for (int i = 0; i < updatedTiles.size(); i++) {
//            IMapTileInformation mapTile = updatedTiles.get(i);
//            points[i] = new Point(mapTile.getX(), mapTile.getY());
//        }
//
//        // FIXME: See in what thread we are, perhaps even do everything ready, just the attaching in render thread
//        app.enqueue(() -> {
//            mapLoader.updateTiles(points);
//        });
    }

    @Override
    public void onBuild(short keeperId, List<Point> tiles) {
//        mapClientService.setTiles(tiles);
//        Point[] updatableTiles = new Point[tiles.size()];
//        for (int i = 0; i < tiles.size(); i++) {
//            updatableTiles[i] = tiles.get(i).getLocation();
//        }
//
//        // FIXME: See in what thread we are, perhaps even do everything ready, just the attaching in render thread
//        app.enqueue(() -> {
//            mapLoader.updateTiles(updatableTiles);
//        });
    }

    @Override
    public void onSold(short keeperId, List<Point> tiles) {

        // For now there is no difference between buying and selling
//        onBuild(keeperId, tiles);
    }

    @Override
    public void onTileFlash(List<Point> points, boolean enabled, short keeperId) {
        flashTileControl.attach(points, enabled);
    }

    public IMapInformation getMapInformation() {
        return mapInformation;
    }

    public interface ILoadCompleteNotifier {

        void onLoadComplete();

    }

    /**
     * Contains the map tiles
     */
    private abstract class MapTileContainer extends EntityContainer<IMapTileInformation> implements IMapDataInformation<IMapTileInformation> {

        private final int width;
        private final int height;
        private final IMapTileInformation[][] tiles;

        private int tilesAdded = 0;

        public MapTileContainer(EntityData entityData, KwdFile kwdFile) {
            super(entityData, MapTile.class, Owner.class, Health.class, Gold.class, Mana.class);

            width = kwdFile.getMap().getWidth();
            height = kwdFile.getMap().getHeight();

            // Duplicate the map
            this.tiles = new IMapTileInformation[width][height];
        }

        @Override
        protected IMapTileInformation addObject(Entity e) {
            logger.log(Level.TRACE, "MapTileContainer.addObject({0})", e);

            IMapTileInformation result = new MapTileInformation(e);
            Point p = result.getLocation();
            this.tiles[p.x][p.y] = result;

            // Naive completion checker
            tilesAdded++;
            if (tilesAdded == getSize()) {
                onLoadComplete();
            }

            return result;
        }

        @Override
        protected void updateObjects(Set<Entity> set) {
            if (set.isEmpty()) {
                return;
            }

            logger.log(Level.TRACE, "MapTileContainer.updateObjects({0})", set.size());

            // Collect the tiles
            Point[] updatableTiles = new Point[set.size()];
            int i = 0;
            for (Entity e : set) {
                IMapTileInformation object = getObject(e.getId());
                if (object == null) {
                    logger.log(Level.WARNING, "Update: No matching object for entity:{0}", e);
                    continue;
                }
                updatableTiles[i] = object.getLocation();
                i++;
            }

            // Update the batch
            mapLoader.updateTiles(updatableTiles);
        }

        @Override
        protected void updateObject(IMapTileInformation object, Entity e) {
            throw new UnsupportedOperationException("Should use the batch method.");

        }

        @Override
        protected void removeObject(IMapTileInformation object, Entity e) {
            logger.log(Level.TRACE, "MapTileContainer.removeObject({0})", e);
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public IMapTileInformation getTile(int x, int y) {
            if (x < 0 || y < 0 || x >= width || y >= height) {
                return null;
            }
            return this.tiles[x][y];
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public void setTiles(List<IMapTileInformation> mapTiles) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        protected abstract void onLoadComplete();

    }

    /**
     * Single map tile that taps into the entity information
     */
    private static class MapTileInformation extends AbstractMapTileInformation {

        private final Entity entity;

        public MapTileInformation(Entity entity) {
            super(entity.getId());

            this.entity = entity;
        }

        @Override
        protected <T extends EntityComponent> T getEntityComponent(Class<T> type) {
            return entity.get(type);
        }

    }

}
