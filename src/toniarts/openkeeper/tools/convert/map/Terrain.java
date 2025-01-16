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
import toniarts.openkeeper.game.data.IFpsSoundable;
import toniarts.openkeeper.game.data.ISoundable;
import toniarts.openkeeper.tools.convert.IFlagEnum;
import toniarts.openkeeper.utils.Color;

/**
 * Container class for the Terrain & TerrainBlock
 *
 * @author Wizand Petteri Loisko petteri.loisko@gmail.com, Toni Helenius
 * <helenius.toni@gmail.com>
 *
 * Thank you https://github.com/werkt
 */
public class Terrain implements Comparable<Terrain>, ISoundable, IFpsSoundable {

    /**
     * Terrain flags
     */
    public enum TerrainFlag implements IFlagEnum {

        SOLID(0x00000001),
        IMPENETRABLE(0x00000002),
        OWNABLE(0x00000004),
        TAGGABLE(0x00000008),
        ROOM(0x00000010), // is room terrain
        ATTACKABLE(0x00000020),
        TORCH(0x00000040), // has torch?
        WATER(0x00000080),
        LAVA(0x00000100),
        ALWAYS_EXPLORED(0x00000200),
        PLAYER_COLOURED_PATH(0x00000400),
        PLAYER_COLOURED_WALL(0x00000800),
        CONSTRUCTION_TYPE_WATER(0x00001000),
        CONSTRUCTION_TYPE_QUAD(0x00002000),
        UNEXPLORE_IF_DUG_BY_ANOTHER_PLAYER(0x00004000),
        FILL_INABLE(0x00008000),
        ALLOW_ROOM_WALLS(0x00010000),
        DECAY(0x00020000),
        RANDOM_TEXTURE(0x00040000),
        TERRAIN_COLOR_RED(0x00080000), // If this is set, add 256 to the red value of terrain light
        TERRAIN_COLOR_GREEN(0x00100000), // If this is set, add 256 to the green value of terrain light
        TERRAIN_COLOR_BLUE(0x00200000), // If this is set, add 256 to the blue value of terrain light
        DWARF_CAN_DIG_THROUGH(0x00800000),
        REVEAL_THROUGH_FOG_OF_WAR(0x01000000),
        AMBIENT_COLOR_RED(0x02000000), // If this is set, add 256 to the red value of ambient light
        AMBIENT_COLOR_GREEN(0x04000000), // If this is set, add 256 to the green value of ambient light
        AMBIENT_COLOR_BLUE(0x08000000), // If this is set, add 256 to the blue value of ambient light
        TERRAIN_LIGHT(0x10000000),
        AMBIENT_LIGHT(0x20000000);
        private final long flagValue;

        private TerrainFlag(long flagValue) {
            this.flagValue = flagValue;
        }

        @Override
        public long getFlagValue() {
            return flagValue;
        }
    };
    //
    // struct Terrain { char name[32]; /* 0
    //
    //  ArtResource complete; /* 20 */
    //  ArtResource side; /* 74 */
    //  ArtResource top; /* c8 */
    //  ArtResource tagged; /* 11c */
    //  StringIds string_ids; /* 170 */
    //  uint32_t unk188; /* 188 */
    //  uint32_t light_height; /* 18c */
    //  uint32_t flags; /* 190 */
    //  uint16_t damage; /* 194 */
    //  uint16_t editorTextureId; /* 196 */
    //  uint16_t unk198; /* 198 */
    //  uint16_t gold_value; /* 19a */
    //  uint16_t mana_gain; /* 19c */
    //  uint16_t max_mana_gain; /* 19e */
    //  uint16_t unk1a0; /* 1a0 */
    //  uint16_t unk1a2; /* 1a2 */
    //  uint16_t unk1a4; /* 1a4 */
    //  uint16_t unk1a6; /* 1a6 */
    //  uint16_t unk1a8; /* 1a8 */
    //  uint16_t unk1aa; /* 1aa */
    //  uint16_t unk1ac; /* 1ac */
    //  uint16_t unk1ae[16]; /* 1ae */
    //  uint8_t wibble_h; /* 1ce */
    //  uint8_t lean_h[3]; /* 1cf */
    //  uint8_t wibble_v; /* 1d2 */
    //  uint8_t lean_v[3]; /* 1d3 */
    //  uint8_t id; /* 1d6 */
    //  uint16_t starting_health; /* 1d7 */
    //  uint8_t max_health_type; /* 1d9 */
    //  uint8_t destroyed_type; /* 1da */
    //  uint8_t terrain_light[3]; /* 1db */
    //  uint8_t texture_frames; /* 1de */
    //  char str1[32]; /* 1df */
    //  uint16_t max_health; /* 1ff */
    //  uint8_t ambient_light[3]; /* 201 */
    //  char str2[32]; /* 204 */
    //  uint32_t unk224; /* 224 */
    //};
    private String name;
    private ArtResource completeResource; // 20
    private ArtResource sideResource; // 74
    private ArtResource topResource; // c8
    private ArtResource taggedTopResource; // 11c
    private StringId stringIds; // 170
    private float depth; // 188
    private float lightHeight; // 18c, fixed point
    private EnumSet<TerrainFlag> flags; // 190
    private int damage; // 194
    private int editorTextureId; // 196 Data\editor\Graphics\TerrainIcons.bmp
    private int unk198; // 198
    private int goldValue; // 19a
    private int manaGain; // 19c
    private int maxManaGain; // 19e
    private int tooltipStringId; // 1a0
    private int nameStringId; // 1a2
    private int maxHealthEffectId; // 1a4
    private int destroyedEffectId; // 1a6
    private int generalDescriptionStringId; // 1a8
    private int strengthStringId; // 1aa
    private int weaknessStringId; // 1ac
    private int[] unk1ae; // 1ae
    private short wibbleH; // 1ce
    private short[] leanH; // 1cf
    private short wibbleV; // 1d2
    private short[] leanV; // 1d3
    private short terrainId; // 1d6
    private int startingHealth; // 1d7
    private short maxHealthTypeTerrainId; // 1d9
    private short destroyedTypeTerrainId; // 1da
    private Color terrainLight; // 1db
    private short textureFrames; // 1de
    private String soundCategory; // 1df
    private int maxHealth; // 1ff
    private Color ambientLight; // 201
    private String soundCategoryFirstPerson; // 204
    private int unk224; // 224

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public float getDepth() {
        return depth;
    }

    protected void setDepth(float depth) {
        this.depth = depth;
    }

    public float getLightHeight() {
        return lightHeight;
    }

    protected void setLightHeight(float lightHeight) {
        this.lightHeight = lightHeight;
    }

    public EnumSet<TerrainFlag> getFlags() {
        return flags;
    }

    protected void setFlags(EnumSet<TerrainFlag> flags) {
        this.flags = flags;
    }

    public int getDamage() {
        return damage;
    }

    protected void setDamage(int damage) {
        this.damage = damage;
    }

    public int getEditorTextureId() {
        return editorTextureId;
    }

    protected void setEditorTextureId(int editorTextureId) {
        this.editorTextureId = editorTextureId;
    }

    public int getUnk198() {
        return unk198;
    }

    protected void setUnk198(int unk198) {
        this.unk198 = unk198;
    }

    public int getGoldValue() {
        return goldValue;
    }

    protected void setGoldValue(int goldValue) {
        this.goldValue = goldValue;
    }

    public int getManaGain() {
        return manaGain;
    }

    protected void setManaGain(int manaGain) {
        this.manaGain = manaGain;
    }

    public int getMaxManaGain() {
        return maxManaGain;
    }

    protected void setMaxManaGain(int maxManaGain) {
        this.maxManaGain = maxManaGain;
    }

    public int getTooltipStringId() {
        return tooltipStringId;
    }

    protected void setTooltipStringId(int tooltipStringId) {
        this.tooltipStringId = tooltipStringId;
    }

    public int getNameStringId() {
        return nameStringId;
    }

    protected void setNameStringId(int nameStringId) {
        this.nameStringId = nameStringId;
    }

    public int getMaxHealthEffectId() {
        return maxHealthEffectId;
    }

    protected void setMaxHealthEffectId(int maxHealthEffectId) {
        this.maxHealthEffectId = maxHealthEffectId;
    }

    public int getDestroyedEffectId() {
        return destroyedEffectId;
    }

    protected void setDestroyedEffectId(int destroyedEffectId) {
        this.destroyedEffectId = destroyedEffectId;
    }

    public int getGeneralDescriptionStringId() {
        return generalDescriptionStringId;
    }

    protected void setGeneralDescriptionStringId(int generalDescriptionStringId) {
        this.generalDescriptionStringId = generalDescriptionStringId;
    }

    public int getStrengthStringId() {
        return strengthStringId;
    }

    protected void setStrengthStringId(int strengthStringId) {
        this.strengthStringId = strengthStringId;
    }

    public int getWeaknessStringId() {
        return weaknessStringId;
    }

    protected void setWeaknessStringId(int weaknessStringId) {
        this.weaknessStringId = weaknessStringId;
    }

    public int[] getUnk1ae() {
        return unk1ae;
    }

    protected void setUnk1ae(int[] unk1ae) {
        this.unk1ae = unk1ae;
    }

    public short getWibbleH() {
        return wibbleH;
    }

    protected void setWibbleH(short wibbleH) {
        this.wibbleH = wibbleH;
    }

    public short[] getLeanH() {
        return leanH;
    }

    protected void setLeanH(short[] leanH) {
        this.leanH = leanH;
    }

    public short getWibbleV() {
        return wibbleV;
    }

    protected void setWibbleV(short wibbleV) {
        this.wibbleV = wibbleV;
    }

    public short[] getLeanV() {
        return leanV;
    }

    protected void setLeanV(short[] leanV) {
        this.leanV = leanV;
    }

    public short getTerrainId() {
        return terrainId;
    }

    protected void setTerrainId(short terrainId) {
        this.terrainId = terrainId;
    }

    public int getStartingHealth() {
        return startingHealth;
    }

    protected void setStartingHealth(int startingHealth) {
        this.startingHealth = startingHealth;
    }

    public short getMaxHealthTypeTerrainId() {
        return maxHealthTypeTerrainId;
    }

    protected void setMaxHealthTypeTerrainId(short maxHealthTypeTerrainId) {
        this.maxHealthTypeTerrainId = maxHealthTypeTerrainId;
    }

    public short getDestroyedTypeTerrainId() {
        return destroyedTypeTerrainId;
    }

    protected void setDestroyedTypeTerrainId(short destroyedTypeTerrainId) {
        this.destroyedTypeTerrainId = destroyedTypeTerrainId;
    }

    public Color getTerrainLight() {
        return terrainLight;
    }

    protected void setTerrainLight(Color terrainLight) {
        this.terrainLight = terrainLight;
    }

    public short getTextureFrames() {
        return textureFrames;
    }

    protected void setTextureFrames(short textureFrames) {
        this.textureFrames = textureFrames;
    }

    @Override
    public String getSoundCategory() {
        return soundCategory;
    }

    protected void setSoundCategory(String soundCategory) {
        this.soundCategory = soundCategory;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    protected void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
    }

    public Color getAmbientLight() {
        return ambientLight;
    }

    protected void setAmbientLight(Color ambientLight) {
        this.ambientLight = ambientLight;
    }

    @Override
    public String getSoundCategoryFirstPerson() {
        return soundCategoryFirstPerson;
    }

    protected void setSoundCategoryFirstPerson(String soundCategoryFirstPerson) {
        this.soundCategoryFirstPerson = soundCategoryFirstPerson;
    }

    public int getUnk224() {
        return unk224;
    }

    protected void setUnk224(int unk224) {
        this.unk224 = unk224;
    }

    public ArtResource getCompleteResource() {
        return completeResource;
    }

    protected void setCompleteResource(ArtResource completeResource) {
        this.completeResource = completeResource;
    }

    public ArtResource getSideResource() {
        return sideResource;
    }

    protected void setSideResource(ArtResource sideResource) {
        this.sideResource = sideResource;
    }

    public ArtResource getTopResource() {
        return topResource;
    }

    protected void setTopResource(ArtResource topResource) {
        this.topResource = topResource;
    }

    public ArtResource getTaggedTopResource() {
        return taggedTopResource;
    }

    protected void setTaggedTopResource(ArtResource taggedTopResource) {
        this.taggedTopResource = taggedTopResource;
    }

    public StringId getStringIds() {
        return stringIds;
    }

    protected void setStringIds(StringId stringIds) {
        this.stringIds = stringIds;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(Terrain o) {
        return Short.compare(terrainId, o.terrainId);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + this.terrainId;
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
        final Terrain other = (Terrain) obj;
        if (this.terrainId != other.terrainId) {
            return false;
        }
        return true;
    }
}
