/*
 * Copyright (C) 2014-2018 OpenKeeper
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
package toniarts.openkeeper.game.logic;

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import toniarts.openkeeper.utils.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Senses;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IDoorsController;
import toniarts.openkeeper.game.controller.IEntityWrapper;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.door.IDoorController;
import toniarts.openkeeper.game.controller.entity.EntityController;
import toniarts.openkeeper.game.controller.entity.IEntityController;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Maintains a tile based position map of all the entities for a quick lookup
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class PositionSystem implements IGameLogicUpdatable, IEntityPositionLookup {

    private final EntityData entityData;
    private final IMapController mapController;
    private final IObjectsController objectsController;
    private final EntitySet positionedEntities;
    private final Set<EntityId>[][] entitiesByMapTile;
    private final Set<EntityId>[][] obstaclesByMapTile;
    private final Map<EntityId, IMapTileInformation> mapTilesByEntities = new HashMap<>();
    private final Map<Class, IEntityWrapper<?>> entityWrappers = new HashMap<>();

    private final Map<EntityId, Set<EntityId>> sensedEntitiesByEntity = new HashMap<>();

    public PositionSystem(IMapController mapController, EntityData entityData, ICreaturesController creaturesController, IDoorsController doorsController, IObjectsController objectsController) {
        this.entityData = entityData;
        this.mapController = mapController;
        this.objectsController = objectsController;
        entityWrappers.put(ICreatureController.class, creaturesController);
        entityWrappers.put(IDoorController.class, doorsController);

        // Initialize data structures
        int width = mapController.getMapData().getWidth();
        int height = mapController.getMapData().getHeight();
        entitiesByMapTile = initializeMatrix(width, height);
        obstaclesByMapTile = initializeMatrix(width, height);

        positionedEntities = entityData.getEntities(Position.class);
        processAddedEntities(positionedEntities);
    }

    private static Set<EntityId>[][] initializeMatrix(int width, int height) {
        Set<EntityId>[][] matrix = new Set[width][height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                matrix[x][y] = new HashSet<>();
            }
        }

        return matrix;
    }

    @Override
    public void processTick(float tpf, double gameTime) {

        // This is just a cache for a tick
        sensedEntitiesByEntity.clear();

        if (positionedEntities.applyChanges()) {

            processAddedEntities(positionedEntities.getAddedEntities());

            processDeletedEntities(positionedEntities.getRemovedEntities());

            processChangedEntities(positionedEntities.getChangedEntities());
        }
    }

    private void processChangedEntities(Set<Entity> entities) {

        // Update
        for (Entity entity : entities) {
            Point p = WorldUtils.vectorToPoint(entity.get(Position.class).position);
            IMapTileInformation currentMapTile = mapController.getMapData().getTile(p);

            IMapTileInformation previousMapTile = mapTilesByEntities.get(entity.getId());
            if (currentMapTile.equals(previousMapTile)) {
                continue;
            }

            int x = previousMapTile.getX();
            int y = previousMapTile.getY();

            // Moved
            mapTilesByEntities.put(entity.getId(), currentMapTile);
            entitiesByMapTile[x][y].remove(entity.getId());

            // Obstacles
            obstaclesByMapTile[x][y].remove(entity.getId());

            addEntityToTile(currentMapTile, entity);
        }
    }

    private void addEntityToTile(IMapTileInformation mapTile, Entity entity) {
        int x = mapTile.getX();
        int y = mapTile.getY();

        entitiesByMapTile[x][y].add(entity.getId());

        // Obstacles
        if (isObstacle(entityData, entity.getId())) {
            obstaclesByMapTile[x][y].add(entity.getId());
        }
    }

    private void processDeletedEntities(Set<Entity> entities) {

        // Remove
        for (Entity entity : entities) {
            IMapTileInformation mapTile = mapTilesByEntities.remove(entity.getId());

            int x = mapTile.getX();
            int y = mapTile.getY();
            entitiesByMapTile[x][y].remove(entity.getId());
            obstaclesByMapTile[x][y].remove(entity.getId());
        }
    }

    private void processAddedEntities(Set<Entity> entities) {

        // Add
        for (Entity entity : entities) {
            Point p = WorldUtils.vectorToPoint(entity.get(Position.class).position);
            IMapTileInformation mapTile = mapController.getMapData().getTile(p);
            mapTilesByEntities.put(entity.getId(), mapTile);

            addEntityToTile(mapTile, entity);
        }
    }

    @Override
    public List<EntityId> getEntitiesInLocation(Point p) {
        return getEntitiesInLocation(p.x, p.y);
    }

    @Override
    public List<EntityId> getEntitiesInLocation(IMapTileInformation mapTile) {
        return getEntitiesInLocation(mapTile.getX(), mapTile.getY());
    }

    @Override
    public List<EntityId> getEntitiesInLocation(int x, int y) {
        Set<EntityId> entityIds = entitiesByMapTile[x][y];
        if (entityIds.isEmpty()) {
            return Collections.emptyList();
        }

        return new ArrayList<>(entityIds);
    }

    @Override
    public IMapTileInformation getEntityLocation(EntityId entityId) {
        return mapTilesByEntities.get(entityId);
    }

    @Override
    public <T extends IEntityController> List<T> getEntityTypesInLocation(Point p, Class<T> clazz) {
        return getEntityTypesInLocation(p.x, p.y, clazz);
    }

    @Override
    public <T extends IEntityController> List<T> getEntityTypesInLocation(IMapTileInformation mapTile, Class<T> clazz) {
        return getEntityTypesInLocation(mapTile.getX(), mapTile.getY(), clazz);
    }

    @Override
    public <T extends IEntityController> List<T> getEntityTypesInLocation(int x, int y, Class<T> clazz) {
        Set<EntityId> entityIds = entitiesByMapTile[x][y];
        if (entityIds.isEmpty()) {
            return Collections.emptyList();
        }

        IEntityWrapper<T> entityWrapper = getEntityWrapper(clazz);

        List<T> entities = new ArrayList<>(entityIds.size());
        for (EntityId entityId : new ArrayList<>(entityIds)) {
            if (!entityWrapper.isValidEntity(entityId)) {
                continue;
            }

            entities.add(entityWrapper.createController(entityId));
        }

        return entities;
    }

    @Override
    public IEntityController getEntityController(EntityId entityId) {

        // Hmm, dunno, it might get duplicate hits etc. Not maybe smart design.
        // Maybe create the base interface methods as final, and it would be always valid to return the IEntityController wherever required
        for (IEntityWrapper<?> entityWrapper : entityWrappers.values()) {
            if (entityWrapper.isValidEntity(entityId)) {
                return entityWrapper.createController(entityId);
            }
        }

        // Hmm, I think this is safe, just create the general one
        return new EntityController(entityId, entityData, objectsController, mapController);
    }

    @Override
    public <T extends IEntityController> T getEntityController(EntityId entityId, Class<T> clazz) {
        IEntityWrapper<T> entityWrapper = getEntityWrapper(clazz);

        return entityWrapper.createController(entityId);
    }

    private <T extends IEntityController> IEntityWrapper<T> getEntityWrapper(Class<T> clazz) throws RuntimeException {
        IEntityWrapper<T> entityWrapper = (IEntityWrapper<T>) entityWrappers.get(clazz);
        if (entityWrapper == null) {
            throw new RuntimeException("No entity wrappers registered with type " + clazz + "!");
        }
        return entityWrapper;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        positionedEntities.release();
        mapTilesByEntities.clear();
        entityWrappers.clear();
        clearMatrix(entitiesByMapTile);
        clearMatrix(obstaclesByMapTile);
    }

    private static void clearMatrix(Set<EntityId>[][] matrix) {
        for (int x = 0; x < matrix.length; x++) {
            for (int y = 0; y < matrix[x].length; y++) {
                matrix[x][y] = null;
            }
        }
    }

    private static boolean isObstacle(EntityData entityData, EntityId id) {

        // Objects have solid obstacle property, but that in my opinion doesn't block the whole tile
        // More of a physics thingie that...
        // So only doors here now...
        DoorComponent doorComponent = entityData.getComponent(id, DoorComponent.class);
        return doorComponent != null && !doorComponent.blueprint;
    }

    @Override
    public boolean isTileBlocked(Point p, short playerId) {
        return isTileBlocked(p.x, p.y, playerId);
    }

    @Override
    public boolean isTileBlocked(IMapTileInformation mapTile, short playerId) {
        return isTileBlocked(mapTile.getX(), mapTile.getY(), playerId);
    }

    @Override
    public boolean isTileBlocked(int x, int y, short playerId) {
        Set<EntityId> entityIds = obstaclesByMapTile[x][y];
        if (entityIds.isEmpty()) {
            return false;
        }

        for (EntityId entityId : entityIds) {
            DoorComponent doorComponent = entityData.getComponent(entityId, DoorComponent.class);
            if (doorComponent == null) {
                continue;
            }

            if (doorComponent.locked) {
                return true;
            }

            Owner owner = entityData.getComponent(entityId, Owner.class);

            return owner == null || owner.ownerId != playerId;
        }

        return false;
    }

    @Override
    public Set<EntityId> getSensedEntities(EntityId entityId) {
        Senses senses = entityData.getComponent(entityId, Senses.class);
        if (senses == null) {
            return Collections.emptySet();
        }

        return sensedEntitiesByEntity.computeIfAbsent(entityId, (id) -> {
            Set<EntityId> sensedEntities = new HashSet<>();

            // Get creatures we sense
            IMapTileInformation tile = getEntityLocation(id);
            // TODO: Every creature has hearing & vision 4, so I can just cheat this in, but should fix eventually
            // https://github.com/tonihele/OpenKeeper/issues/261
            if (tile != null) {
                addSensedEntities(tile, (int) Math.max(senses.distanceCanHear, senses.distanceCanSee), sensedEntities);
            }

            // Remove us, the caller
            sensedEntities.remove(id);

            return sensedEntities;
        });
    }

    private void addSensedEntities(IMapTileInformation tile, int range, Set<EntityId> sensedEntities) {
        if (tile == null || mapController.getTerrain(tile).getFlags().contains(Terrain.TerrainFlag.SOLID)
                || range-- < 0) {
            return;
        }

        sensedEntities.addAll(getEntitiesInLocation(tile));

        addSensedEntities(mapController.getMapData().getTile(tile.getX() + 1, tile.getY()), range, sensedEntities);
        addSensedEntities(mapController.getMapData().getTile(tile.getX() - 1, tile.getY()), range, sensedEntities);
        addSensedEntities(mapController.getMapData().getTile(tile.getX(), tile.getY() + 1), range, sensedEntities);
        addSensedEntities(mapController.getMapData().getTile(tile.getX(), tile.getY() - 1), range, sensedEntities);
    }

}
