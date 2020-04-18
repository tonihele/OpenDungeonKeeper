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

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.FastMath;
import com.jme3.network.serializing.serializers.FieldSerializer;
import java.awt.Point;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;
import toniarts.openkeeper.game.network.Transferable;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Tile;
import toniarts.openkeeper.tools.convert.map.Tile.BridgeTerrainType;

/**
 * A presentation of a single map tile
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Transferable(FieldSerializer.class)
public class MapTile implements Savable {

    private Map<Short, Boolean> selection = new HashMap<>();
    private Map<Short, Boolean> flashing = new HashMap<>();
    private int randomTextureIndex;
    private int health;
    private int maxHealth;
    private int gold;
    private int manaGain;

    /* Refers to the room this tile holds */
    private boolean destroyed = false;

    private short ownerId;
    private short terrainId;
    private BridgeTerrainType bridgeTerrainType;

    private Point p;
    private int index;

    public MapTile() {
        // For serialization
    }

    public MapTile(Tile tile, Terrain terrain, int x, int y, int index) {
        this.p = new Point(x, y);
        this.index = index;
        this.bridgeTerrainType = tile.getFlag();
        this.terrainId = tile.getTerrainId();
        this.ownerId = tile.getPlayerId();

        // The water/lava under the bridge is set only when there is an actual bridge, but we might as well set it here, it doesn't change
        if (terrain.getFlags().contains(Terrain.TerrainFlag.LAVA)) {
            this.bridgeTerrainType = BridgeTerrainType.LAVA;
        } else if (terrain.getFlags().contains(Terrain.TerrainFlag.WATER)) {
            this.bridgeTerrainType = BridgeTerrainType.WATER;
        }

        // Set attributes
        setAttributesFromTerrain(this, terrain);
    }

    public static void setAttributesFromTerrain(MapTile tile, Terrain terrain) {
        tile.health = terrain.getStartingHealth();
        tile.maxHealth = terrain.getMaxHealth();
        tile.gold = terrain.getGoldValue();
        tile.manaGain = terrain.getManaGain();

        // Randomize the texture index, the terrain can change for sure but the changed types have no random textures
        // But for the principle, let it be here
        if (terrain.getFlags().contains(Terrain.TerrainFlag.RANDOM_TEXTURE)) {
            tile.randomTextureIndex = FastMath.nextRandomInt(0, terrain.getTextureFrames() - 1);
        }
    }

    public boolean isSelected(short playerId) {
        if (selection != null) {
            return selection.getOrDefault(playerId, false);
        }
        return false;
    }

    public void setSelected(boolean selected, short playerId) {
        if (selection == null) {
            selection = new HashMap<>(4);
        }
        selection.put(playerId, selected);
    }

    public boolean isFlashed(short playerId) {
        if (flashing != null) {
            return flashing.getOrDefault(playerId, false);
        }
        return false;
    }

    public void setFlashed(boolean flashed, short playerId) {
        if (flashing == null) {
            flashing = new HashMap<>(4);
        }
        flashing.put(playerId, flashed);
    }

//    @Override
//    protected void setPlayerId(short playerId) {
//        super.setPlayerId(playerId);
//    }
//
//    @Override
//    protected void setTerrainId(short terrainId) {
//        super.setTerrainId(terrainId);
//        if (terrain.getTerrainId() != terrainId) {
//
//            // A change
//            terrain = kwdFile.getTerrain(getTerrainId());
//            setAttributesFromTerrain();
//
//            // If the terrain is not taggable anymore, reset the tagging data
//            if (!terrain.getFlags().contains(Terrain.TerrainFlag.TAGGABLE)) {
//                setSelected(false, (short) 0);
//            }
//            // FIXME realy need?
//            setFlashed(false);
//        }
//    }
    public short getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(short ownerId) {
        this.ownerId = ownerId;
    }

    public short getTerrainId() {
        return terrainId;
    }

    public void setTerrainId(short terrainId) {
        this.terrainId = terrainId;
    }

    public BridgeTerrainType getBridgeTerrainType() {
        return bridgeTerrainType;
    }

    public void setBridgeTerrainType(BridgeTerrainType bridgeTerrainType) {
        this.bridgeTerrainType = bridgeTerrainType;
    }

    public int getRandomTextureIndex() {
        return randomTextureIndex;
    }

    public void setRandomTextureIndex(int randomTextureIndex) {
        this.randomTextureIndex = randomTextureIndex;
    }

    public int getX() {
        return p.x;
    }

    public int getY() {
        return p.y;
    }

    /**
     * Get tile index in MapData 2D array as Point
     *
     * @return
     */
    public Point getLocation() {
        return p;
    }

    public int getIndex() {
        return index;
    }

    /**
     * Get the terrain type of this terrain
     *
     * @return the terrain
     */
//    public Terrain getTerrain() {
//        return terrain;
//    }
    public int getHealth() {
        return health;
    }

    public int getGold() {
        return gold;
    }

    /**
     * Set tile health, only internal usage
     *
     * @param health the health points to set
     */
    public void setHealth(int health) {
        this.health = health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setPoint(Point p) {
        this.p = p;
    }

    public void setGold(int gold) {
        this.gold = gold;
    }

//    public String getTooltip() {
//        return bundle.getString(Integer.toString(getTerrain().getTooltipStringId()))
//                .replaceAll("%37%", Integer.toString(getHealthPercent()))
//                .replaceAll("%66", Integer.toString(terrain.getManaGain()))
//                .replaceAll("%67", Integer.toString(gold));
//    }
//
    public Integer getHealthPercent() {
        return Math.round((float) health / maxHealth * 100);
    }

    public int getManaGain() {
        return manaGain;
    }

    /**
     * Apply damage to the tile
     *
     * @param damage amount of damage
     * @return true if the tile is "dead"
     */
//    public boolean applyDamage(int damage) {
//        health = Math.max(0, health - damage);
//        return (health == 0);
//    }
    /**
     * Apply healing to the tile
     *
     * @param healing amount of healing
     * @return true if the tile is at max
     */
//    public boolean applyHealing(int healing) {
//        health = (int) Math.min(getTerrain().getMaxHealth(), (long) health + healing);
//        return (health == getTerrain().getMaxHealth());
//    }
    /**
     * Mine for gold
     *
     * @param damage the amount of gold requested
     * @return the amount of gold got
     */
//    public int mineGold(int damage) {
//        int minedAmount = Math.min(damage, gold);
//        gold -= minedAmount;
//        return minedAmount;
//    }
    /**
     * Is tile at full health
     *
     * @return true if full health
     */
    public boolean isAtFullHealth() {
        return (health == maxHealth);
    }

    @Override
    public int hashCode() {
        return this.index;
    }

    @Override
    public String toString() {
        return "MapTile{ownerId=" + ownerId + ", p=" + p + '}';
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
        final MapTile other = (MapTile) obj;
        if (this.index != other.index) {
            return false;
        }
        return true;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
//        if (selection != null) {
        writeShortBooleanMap(out, selection, "selection");
//        }
//        if (flashing != null) {
        writeShortBooleanMap(out, flashing, "flashing");
//        }
        out.write(randomTextureIndex, "randomTextureIndex", 0);
        out.write(health, "health", 0);
        out.write(maxHealth, "maxHealth", 0);
        out.write(gold, "gold", 0);
        out.write(ownerId, "ownerId", Integer.valueOf(0).shortValue());
        out.write(terrainId, "terrainId", Integer.valueOf(0).shortValue());
        out.write(bridgeTerrainType, "bridgeTerrainType", null);
        out.write(destroyed, "destroyed", false);
    }

    private void writeShortBooleanMap(OutputCapsule out, Map<Short, Boolean> map, String name) throws IOException {
        short[] keys = null;
        boolean[] values = null;

        Short[] shortArray = map.keySet().toArray(new Short[0]);
        IntStream.range(0, shortArray.length).forEach(i -> keys[i] = shortArray[i]);

        Boolean[] booleanArray = map.values().toArray(new Boolean[0]);
        IntStream.range(0, booleanArray.length).forEach(i -> values[i] = booleanArray[i]);

        out.write(keys, name + "Keys", null);
        out.write(values, name + "Values", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        selection = readShortBooleanMap(in, "selection");
        flashing = readShortBooleanMap(in, "flashing");
        randomTextureIndex = in.readInt("randomTextureIndex", 0);
        health = in.readInt("health", 0);
        maxHealth = in.readInt("maxHealth", 0);
        gold = in.readInt("gold", 0);
        ownerId = in.readShort("ownerId", Integer.valueOf(0).shortValue());
        terrainId = in.readShort("terrainId", Integer.valueOf(0).shortValue());
        bridgeTerrainType = in.readEnum("bridgeTerrainType", BridgeTerrainType.class, null);
        destroyed = in.readBoolean("destroyed", false);
    }

    private Map<Short, Boolean> readShortBooleanMap(InputCapsule in, String name) throws IOException {
        short[] keys = in.readShortArray(name + "Keys", null);
        boolean[] values = in.readBooleanArray(name + "Values", null);
        Map<Short, Boolean> result = null;
        if (keys != null) {
            result = new HashMap<>(keys.length);
            for (int i = 0; i < keys.length; i++) {
                result.put(keys[i], values[i]);
            }
        }

        return result;
    }

}
