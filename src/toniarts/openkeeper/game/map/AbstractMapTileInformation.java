/*
 * Copyright (C) 2014-2020 OpenKeeper
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

import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;
import java.util.Objects;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Mana;
import toniarts.openkeeper.game.component.MapTile;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.tools.convert.map.Tile.BridgeTerrainType;

/**
 * A presentation of a single map tile. Gets data from specified entity
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class AbstractMapTileInformation implements IMapTileInformation {

    protected final EntityId entityId;

    public AbstractMapTileInformation(EntityId entityId) {
        this.entityId = entityId;
    }

    protected abstract <T extends EntityComponent> T getEntityComponent(Class<T> type);

    @Override
    public boolean isSelected(short playerId) {
        MapTile mapTileComponent = getEntityComponent(MapTile.class);
        if (mapTileComponent.selection != null) {
            return mapTileComponent.selection.getOrDefault(playerId, false);
        }
        return false;
    }

    @Override
    public boolean isFlashed(short playerId) {
        MapTile mapTileComponent = getEntityComponent(MapTile.class);
        if (mapTileComponent.flashing != null) {
            return mapTileComponent.flashing.getOrDefault(playerId, false);
        }
        return false;
    }

    @Override
    public short getOwnerId() {
        return getEntityComponent(Owner.class).ownerId;
    }

    @Override
    public short getTerrainId() {
        return getEntityComponent(MapTile.class).terrainId;
    }

    @Override
    public BridgeTerrainType getBridgeTerrainType() {
        return getEntityComponent(MapTile.class).bridgeTerrainType;
    }

    @Override
    public int getRandomTextureIndex() {
        return getEntityComponent(MapTile.class).randomTextureIndex;
    }

    @Override
    public int getX() {
        return getLocation().x;
    }

    @Override
    public int getY() {
        return getLocation().y;
    }

    /**
     * Get tile index in MapData 2D array as Point
     *
     * @return
     */
    @Override
    public Point getLocation() {
        return getEntityComponent(MapTile.class).p;
    }

    @Override
    public int getIndex() {
        return getEntityComponent(MapTile.class).index;
    }

    @Override
    public int getHealth() {
        return getEntityComponent(Health.class).health;
    }

    @Override
    public int getGold() {
        return getEntityComponent(Gold.class).gold;
    }

    @Override
    public int getMaxHealth() {
        return getEntityComponent(Health.class).maxHealth;
    }

    @Override
    public Integer getHealthPercent() {
        Health health = getEntityComponent(Health.class);
        return Math.round((float) health.health / health.maxHealth * 100);
    }

    @Override
    public int getManaGain() {
        return getEntityComponent(Mana.class).manaGeneration;
    }

    /**
     * Is tile at full health
     *
     * @return true if full health
     */
    @Override
    public boolean isAtFullHealth() {
        Health health = getEntityComponent(Health.class);
        return (health.health == health.maxHealth);
    }

    @Override
    public EntityId getEntityId() {
        return entityId;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + Objects.hashCode(this.entityId);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AbstractMapTileInformation other = (AbstractMapTileInformation) obj;
        if (!Objects.equals(this.entityId, other.entityId)) {
            return false;
        }
        return true;
    }

}
