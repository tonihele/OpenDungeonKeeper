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
package toniarts.openkeeper.game.map;

import com.jme3.math.FastMath;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;
import java.util.HashMap;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Mana;
import toniarts.openkeeper.game.component.MapTile;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Tile.BridgeTerrainType;

/**
 * A presentation of a single map tile
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class MapTileController extends AbstractMapTileInformation implements IMapTileController {

    private final EntityData entityData;

    public MapTileController(EntityId entityId, EntityData entityData) {
        super(entityId);

        this.entityData = entityData;
    }

    public static void setAttributesFromTerrain(EntityData entityData, IMapTileInformation mapTile, Terrain terrain) {
        setAttributesFromTerrain(entityData, mapTile.getEntityId(), entityData.getComponent(mapTile.getEntityId(), MapTile.class), terrain);
    }

    protected static void setAttributesFromTerrain(EntityData entityData, EntityId entity, MapTile mapTileComponent, Terrain terrain) {

        // Health
        Health health = new Health(terrain.getStartingHealth(), terrain.getMaxHealth());

        // Gold
        Gold gold = new Gold(terrain.getGoldValue(), terrain.getGoldValue());

        // Mana
        Mana mana = new Mana(terrain.getManaGain());

        // Randomize the texture index, the terrain can change for sure but the changed types have no random textures
        // But for the principle, let it be here
        if (terrain.getFlags().contains(Terrain.TerrainFlag.RANDOM_TEXTURE)) {
            mapTileComponent.randomTextureIndex = FastMath.nextRandomInt(0, terrain.getTextureFrames() - 1);
        }

        entityData.setComponents(entity, health, gold, mana, mapTileComponent);
    }

    @Override
    public void setSelected(boolean selected, short playerId) {
        MapTile mapTileComponent = new MapTile(getEntityComponent(MapTile.class));
        if (mapTileComponent.selection == null) {
            mapTileComponent.selection = HashMap.newHashMap(4);
        }
        mapTileComponent.selection.put(playerId, selected);
        entityData.setComponent(entityId, mapTileComponent);
    }

    @Override
    public void setFlashed(boolean flashed, short playerId) {
        MapTile mapTileComponent = new MapTile(getEntityComponent(MapTile.class));
        if (mapTileComponent.flashing == null) {
            mapTileComponent.flashing = HashMap.newHashMap(4);
        }
        mapTileComponent.flashing.put(playerId, flashed);
        entityData.setComponent(entityId, mapTileComponent);
    }

    @Override
    public void setOwnerId(short ownerId) {
        entityData.setComponent(entityId, new Owner(ownerId, ownerId));
    }

    @Override
    public void setTerrainId(short terrainId) {
        MapTile mapTileComponent = new MapTile(getEntityComponent(MapTile.class));
        mapTileComponent.terrainId = terrainId;
        entityData.setComponent(entityId, mapTileComponent);
    }

    @Override
    public void setBridgeTerrainType(BridgeTerrainType bridgeTerrainType) {
        MapTile mapTileComponent = new MapTile(getEntityComponent(MapTile.class));
        mapTileComponent.bridgeTerrainType = bridgeTerrainType;
        entityData.setComponent(entityId, mapTileComponent);
    }

    @Override
    public void setRandomTextureIndex(int randomTextureIndex) {
        MapTile mapTileComponent = new MapTile(getEntityComponent(MapTile.class));
        mapTileComponent.randomTextureIndex = randomTextureIndex;
        entityData.setComponent(entityId, mapTileComponent);
    }

    /**
     * Set tile health, only internal usage
     *
     * @param health the health points to set
     */
    @Override
    public void setHealth(int health) {
        Health healthComponent = new Health(getEntityComponent(Health.class));
        healthComponent.health = health;
        entityData.setComponent(entityId, healthComponent);
    }

    @Override
    public void setMaxHealth(int maxHealth) {
        Health healthComponent = new Health(getEntityComponent(Health.class));
        healthComponent.maxHealth = maxHealth;
        entityData.setComponent(entityId, healthComponent);
    }

    @Override
    public void setIndex(int index) {
        MapTile mapTileComponent = new MapTile(getEntityComponent(MapTile.class));
        mapTileComponent.index = index;
        entityData.setComponent(entityId, mapTileComponent);
    }

    @Override
    public void setPoint(Point p) {
        MapTile mapTileComponent = new MapTile(getEntityComponent(MapTile.class));
        mapTileComponent.p = p;
        entityData.setComponent(entityId, mapTileComponent);
    }

    @Override
    public void setGold(int gold) {
        Gold goldComponent = new Gold(getEntityComponent(Gold.class));
        goldComponent.gold = gold;
        entityData.setComponent(entityId, goldComponent);
    }

    @Override
    protected <T extends EntityComponent> T getEntityComponent(Class<T> type) {
        return entityData.getComponent(entityId, type);
    }

}
