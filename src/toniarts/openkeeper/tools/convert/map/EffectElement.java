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
package toniarts.openkeeper.tools.convert.map;

import java.util.EnumSet;
import javax.annotation.Nullable;
import toniarts.openkeeper.tools.convert.IFlagEnum;
import toniarts.openkeeper.utils.Color;
import toniarts.openkeeper.world.effect.IEffect;

/**
 * Container class for EffectElements.kwd
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class EffectElement implements Comparable<EffectElement>, IEffect {

    /**
     * Effect element flags
     */
    public enum EffectElementFlag implements IFlagEnum {

        DIE_WHEN_HIT_SOLID(0x0001),
        SHRINK(0x0002),
        EXPAND(0x0004),
        FADE(0x0008),
        DIRECTIONAL_FRICTION(0x0010),
        CAN_BE_DISTURBED(0x0020),
        ROTATE_TO_MOVEMENT_DIRECTION(0x0040),
        /**
         * Triggers even when not in player's view
         */
        ALWAYS_TRIGGER(0x0200);
        private final long flagValue;

        private EffectElementFlag(long flagValue) {
            this.flagValue = flagValue;
        }

        @Override
        public long getFlagValue() {
            return flagValue;
        }
    };
//    struct EffectElementBlock {
//        char m_sName[32];
//        ArtResource m_kArtResource;
//        int32_t mass; /* 74 */
//        uint32_t air_friction; /* 78 */
//        uint32_t elasticity; /* 7c */
//        int32_t min_speed_xy; /* 80 */
//        int32_t max_speed_xy; /* 84 */
//        int32_t min_speed_yz; /* 88 says it's yz, but appears to only be Z */
//        int32_t max_speed_yz; /* 8c z speed at end */
//        uint32_t min_scale; /* 90 */
//        uint32_t max_scale; /* 94 */
//        uint32_t scale_ratio; /* 98 */
//        uint32_t flags; /* 9c */
//        uint16_t m_wId; /* a0 */
//        uint16_t min_hp; /* a2 */
//        uint16_t max_hp; /* a4 */
//        uint16_t death_element; /* a6 */
//        uint16_t hit_solid_element; /* a8 */
//        uint16_t hit_water_element; /* aa */
//        uint16_t hit_lava_element; /* ac */
//        uint8_t color[3]; /* ae */
//        uint8_t random_color_index; /* b1 */
//        uint8_t table_color_index; /* b2 */
//        uint8_t fade_percentage; /* b3 */
//        uint16_t next_effect; /* b4 */
//        };
    private String name;
    private ArtResource artResource;
    private float mass; // 74
    private float airFriction; // 78
    private float elasticity; // 7c
    private float minSpeedXy; // 80
    private float maxSpeedXy; // 84
    private float minSpeedYz; // 88 says it's yz, but appears to only be Z
    private float maxSpeedYz; // 8c z speed at end
    private float minScale; // 90
    private float maxScale; // 94
    private float scaleRatio; // 98
    private EnumSet<EffectElementFlag> flags; // 9c
    private int effectElementId; // a0
    private int minHp; // a2
    private int maxHp; // a4
    private int deathElementId; // a6, EffectElementId
    private int hitSolidElementId; // a8, EffectElementId
    private int hitWaterElementId; // aa, EffectElementId
    private int hitLavaElementId; // ac, EffectElementId
    private Color color; // ae
    private short randomColorIndex; // b1
    private short tableColorIndex; // b2
    private short fadePercentage; // b3
    private int nextEffectId; // b4

    @Override
    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    @Override
    @Nullable
    public ArtResource getArtResource() {
        return artResource;
    }

    protected void setArtResource(ArtResource artResource) {
        this.artResource = artResource;
    }

    @Override
    public float getMass() {
        return mass;
    }

    protected void setMass(float mass) {
        this.mass = mass;
    }

    @Override
    public float getAirFriction() {
        return airFriction;
    }

    protected void setAirFriction(float airFriction) {
        this.airFriction = airFriction;
    }

    @Override
    public float getElasticity() {
        return elasticity;
    }

    protected void setElasticity(float elasticity) {
        this.elasticity = elasticity;
    }

    @Override
    public float getMinSpeedXy() {
        return minSpeedXy;
    }

    protected void setMinSpeedXy(float minSpeedXy) {
        this.minSpeedXy = minSpeedXy;
    }

    @Override
    public float getMaxSpeedXy() {
        return maxSpeedXy;
    }

    protected void setMaxSpeedXy(float maxSpeedXy) {
        this.maxSpeedXy = maxSpeedXy;
    }

    @Override
    public float getMinSpeedYz() {
        return minSpeedYz;
    }

    protected void setMinSpeedYz(float minSpeedYz) {
        this.minSpeedYz = minSpeedYz;
    }

    @Override
    public float getMaxSpeedYz() {
        return maxSpeedYz;
    }

    protected void setMaxSpeedYz(float maxSpeedYz) {
        this.maxSpeedYz = maxSpeedYz;
    }

    @Override
    public float getMinScale() {
        return minScale;
    }

    protected void setMinScale(float minScale) {
        this.minScale = minScale;
    }

    @Override
    public float getMaxScale() {
        return maxScale;
    }

    protected void setMaxScale(float maxScale) {
        this.maxScale = maxScale;
    }

    public float getScaleRatio() {
        return scaleRatio;
    }

    protected void setScaleRatio(float scaleRatio) {
        this.scaleRatio = scaleRatio;
    }

    public EnumSet<EffectElementFlag> getFlags() {
        return flags;
    }

    protected void setFlags(EnumSet<EffectElementFlag> flags) {
        this.flags = flags;
    }

    public int getEffectElementId() {
        return effectElementId;
    }

    protected void setEffectElementId(int effectElementId) {
        this.effectElementId = effectElementId;
    }

    @Override
    public int getMinHp() {
        return minHp;
    }

    protected void setMinHp(int minHp) {
        this.minHp = minHp;
    }

    @Override
    public int getMaxHp() {
        return maxHp;
    }

    protected void setMaxHp(int maxHp) {
        this.maxHp = maxHp;
    }

    public int getDeathElementId() {
        return deathElementId;
    }

    protected void setDeathElementId(int deathElementId) {
        this.deathElementId = deathElementId;
    }

    public int getHitSolidElementId() {
        return hitSolidElementId;
    }

    protected void setHitSolidElementId(int hitSolidElementId) {
        this.hitSolidElementId = hitSolidElementId;
    }

    public int getHitWaterElementId() {
        return hitWaterElementId;
    }

    protected void setHitWaterElementId(int hitWaterElementId) {
        this.hitWaterElementId = hitWaterElementId;
    }

    public int getHitLavaElementId() {
        return hitLavaElementId;
    }

    protected void setHitLavaElementId(int hitLavaElementId) {
        this.hitLavaElementId = hitLavaElementId;
    }

    public Color getColor() {
        return color;
    }

    protected void setColor(Color color) {
        this.color = color;
    }

    public short getRandomColorIndex() {
        return randomColorIndex;
    }

    protected void setRandomColorIndex(short randomColorIndex) {
        this.randomColorIndex = randomColorIndex;
    }

    public short getTableColorIndex() {
        return tableColorIndex;
    }

    protected void setTableColorIndex(short tableColorIndex) {
        this.tableColorIndex = tableColorIndex;
    }

    public short getFadePercentage() {
        return fadePercentage;
    }

    protected void setFadePercentage(short fadePercentage) {
        this.fadePercentage = fadePercentage;
    }

    public int getNextEffectId() {
        return nextEffectId;
    }

    protected void setNextEffectId(int nextEffectId) {
        this.nextEffectId = nextEffectId;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(EffectElement o) {
        return Integer.compare(effectElementId, o.effectElementId);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + this.effectElementId;
        return hash;
    }

    @Override
    public boolean equals(java.lang.Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final EffectElement other = (EffectElement) obj;

        return this.effectElementId == other.effectElementId;
    }
}
