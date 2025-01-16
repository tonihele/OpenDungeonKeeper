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

import com.jme3.math.Vector2f;
import com.jme3.scene.Node;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Tile;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.world.creature.CreatureControl;

/**
 * Wrapper for a map tile
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Deprecated
public final class TileData extends Tile {

    private boolean selected = false;
    private boolean flashed = false;
    private short selectedByPlayerId = 0;
    private Integer randomTextureIndex;
    private Terrain terrain;
    private int health;
    private int gold;
    private final Point p;
    private final int index;
    private final KwdFile kwdFile;
    private final List<CreatureControl> creatures = new ArrayList<>();
    private Node sideNode;
    private Node topNode;

    protected TileData(KwdFile kwdFile, Tile tile, Terrain terrain, int x, int y, int index) {
        this.p = new Point(x, y);
        this.index = index;
        this.kwdFile = kwdFile;
        this.terrain = terrain;
        this.setFlag(tile.getFlag());
        this.setPlayerId(tile.getPlayerId());
        this.setTerrainId(tile.getTerrainId());
        this.setUnknown(tile.getUnknown());

        // The water/lava under the bridge is set only when there is an actual bridge, but we might as well set it here, it doesn't change
        if (terrain.getFlags().contains(Terrain.TerrainFlag.LAVA)) {
            this.setFlag(BridgeTerrainType.LAVA);
        } else if (terrain.getFlags().contains(Terrain.TerrainFlag.WATER)) {
            this.setFlag(BridgeTerrainType.WATER);
        }

        // Set attributes
        setAttributesFromTerrain();
    }

    public boolean isSelected() {
        return selected;
    }

    protected void setSelected(boolean selected, short playerId) {
        this.selected = selected;
        selectedByPlayerId = playerId;
    }

    public boolean isFlashed() {
        return flashed;
    }

    public void setFlashed(boolean flashed) {
        this.flashed = flashed;
    }

    public short getSelectedByPlayerId() {
        return selectedByPlayerId;
    }

    public boolean isSelectedByPlayerId(short playerId) {
        return (selected && selectedByPlayerId == playerId);
    }

    @Override
    protected void setPlayerId(short playerId) {
        super.setPlayerId(playerId);
    }

    @Override
    protected void setTerrainId(short terrainId) {
        super.setTerrainId(terrainId);
        if (terrain.getTerrainId() != terrainId) {

            // A change
            terrain = kwdFile.getTerrain(getTerrainId());
            setAttributesFromTerrain();

            // If the terrain is not taggable anymore, reset the tagging data
            if (!terrain.getFlags().contains(Terrain.TerrainFlag.TAGGABLE)) {
                setSelected(false, (short) 0);
            }
            // FIXME realy need?
            setFlashed(false);
        }
    }

    public Integer getRandomTextureIndex() {
        return randomTextureIndex;
    }

    protected void setRandomTextureIndex(Integer randomTextureIndex) {
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
     * @return
     */
    public Point getLocation() {
        return p;
    }

    /**
     * Get real tile position in 3D World as Vector2f
     * @return
     */
    public Vector2f getWorldLocation() {
        return new Vector2f(p.x * MapLoader.TILE_WIDTH, p.y * MapLoader.TILE_WIDTH);
    }

    public int getIndex() {
        return index;
    }

    /**
     * Get the terrain type of this terrain
     *
     * @return the terrain
     */
    public Terrain getTerrain() {
        return terrain;
    }

    private void setAttributesFromTerrain() {
        health = terrain.getStartingHealth();
        gold = terrain.getGoldValue();
    }

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
    protected void setHealth(int health) {
        this.health = health;
    }

    public String getTooltip() {
        return Utils.getMainTextResourceBundle().getString(Integer.toString(getTerrain().getTooltipStringId()))
                .replaceAll("%37%", Integer.toString(getHealthPercent()))
                .replaceAll("%66", Integer.toString(terrain.getManaGain()))
                .replaceAll("%67", Integer.toString(gold));
    }

    protected Integer getHealthPercent() {
        return Math.round((float) health / terrain.getMaxHealth() * 100);
    }

    /**
     * Apply damage to the tile
     *
     * @param damage amount of damage
     * @return true if the tile is "dead"
     */
    public boolean applyDamage(int damage) {
        health = Math.max(0, health - damage);
        return (health == 0);
    }

    /**
     * Apply healing to the tile
     *
     * @param healing amount of healing
     * @return true if the tile is at max
     */
    public boolean applyHealing(int healing) {
        health = (int) Math.min(getTerrain().getMaxHealth(), (long) health + healing);
        return (health == getTerrain().getMaxHealth());
    }

    /**
     * Mine for gold
     *
     * @param damage the amount of gold requested
     * @return the amount of gold got
     */
    public int mineGold(int damage) {
        int minedAmount = Math.min(damage, gold);
        gold -= minedAmount;
        return minedAmount;
    }

    /**
     * Is tile at full health
     *
     * @return true if full health
     */
    public boolean isAtFullHealth() {
        return (health == getTerrain().getMaxHealth());
    }

    /**
     * Clears the tile creature record
     */
    public void clearCreatures() {
        creatures.clear();
    }

    /**
     * Add a creature to the tile creature record
     *
     * @param creature the creature to add
     */
    public void addCreature(CreatureControl creature) {
        creatures.add(creature);
    }

    /**
     * Get list of creatures currently wondering at this tile
     *
     * @return creatures at this tile
     */
    public List<CreatureControl> getCreatures() {
        return creatures;
    }

    @Nullable
    public Node getSideNode() {
        return sideNode;
    }

    public void setSideNode(Node node) {
        this.sideNode = node;
    }

    @Nullable
    public Node getTopNode() {
        return topNode;
    }

    public void setTopNode(Node node) {
        this.topNode = node;
    }
}
