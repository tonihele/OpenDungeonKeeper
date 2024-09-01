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
import java.util.List;
import toniarts.openkeeper.game.data.IIndexable;
import toniarts.openkeeper.game.data.ISoundable;
import toniarts.openkeeper.tools.convert.IFlagEnum;
import toniarts.openkeeper.tools.convert.IValueEnum;
import toniarts.openkeeper.utils.Color;

/**
 * Container class for the Rooms
 *
 * @author Wizand Petteri Loisko petteri.loisko@gmail.com, Toni Helenius
 * <helenius.toni@gmail.com>
 *
 * Thank you https://github.com/werkt
 */
public class Room implements Comparable<Room>, ISoundable, IIndexable {

    /**
     * Room flags
     */
    public enum RoomFlag implements IFlagEnum {

        PLACEABLE_ON_WATER(0x0001),
        PLACEABLE_ON_LAVA(0x0002),
        PLACEABLE_ON_LAND(0x0004),
        HAS_WALLS(0x0008),
        CENTRE(0x0010), // Placement
        SPECIAL_TILES(0x0020), // Placement
        NORMAL_TILES(0x0040), // Placement
        BUILDABLE(0x0080),
        SPECIAL_WALLS(0x0100), // Placement
        ATTACKABLE(0x0200),
        UNKNOWN(0x0400), // FIXME unknown flag. Hero Gate [ Final ], Hero Gate [ Tile ] [ not used ]
        UNKNOWN_2(0x0800), // FIXME unknown flag
        HAS_FLAME(0x1000),
        IS_GOOD(0x2000);
        private final long flagValue;

        private RoomFlag(long flagValue) {
            this.flagValue = flagValue;
        }

        @Override
        public long getFlagValue() {
            return flagValue;
        }
    };

    public enum TileConstruction implements IValueEnum {

        COMPLETE(1),
        QUAD(2),
        _3_BY_3(3),
        _3_BY_3_ROTATED(4),
        NORMAL(5),
        CENTER_POOL(6),
        DOUBLE_QUAD(7),
        _5_BY_5_ROTATED(8),
        HERO_GATE(9),
        HERO_GATE_TILE(10),
        HERO_GATE_2_BY_2(11),
        HERO_GATE_FRONT_END(12),
        HERO_GATE_3_BY_1(13);

        private TileConstruction(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }
    //struct RoomBlock {
    //  char name[32]; /* 0 */
    //  ArtResource gui_icon; /* 20 */
    //  ArtResource room_icon; /* 74 */
    //  ArtResource complete; /* c8 */
    //  ArtResource ref[7]; /* 11c */
    //  uint32_t unk1; /* 368 - very likely flags */
    //  uint16_t unk2; /* 36c */
    //  uint16_t intensity; /* 36e */
    //  uint32_t unk3; /* 370 */
    //  uint16_t x374;
    //  uint16_t x376;
    //  uint16_t x378;
    //  uint16_t x37a;
    //  uint16_t x37c;
    //  uint16_t x37e;
    //  uint16_t unk5; /* 380 */
    //  uint16_t effects[8]; /* 382 */
    //  uint8_t id; /* 392 */
    //  uint8_t unk7; /* 393 */
    //  uint8_t terrain; /* 394 */
    //  uint8_t tile_construction; /* 395 */
    //  uint8_t unk8; /* 396 */
    //  uint8_t torch_color[3]; /* 397 */
    //  uint8_t objects[8]; /* 39a */
    //  char sound_category[32]; /* 3a2 */
    //  uint8_t x3c2; /* 3c2 */
    //  uint8_t x3c3; /* 3c3 */
    //  uint16_t unk10; /* 3c4 */
    //  uint8_t unk11; /* 3c6 */
    //  ArtResource torch; /* 3c7 */
    //  uint8_t x41b; /* 41b */
    //  uint8_t x41c; /* 41c */
    //  short x41d; /* 41d */
    //};
    private String name; // 0
    private ArtResource guiIcon; // 20
    private ArtResource editorIcon; // 74
    private ArtResource completeResource; // c8
    private ArtResource straightResource; // 11c
    private ArtResource insideCornerResource;
    private ArtResource unknownResource; // always null
    private ArtResource outsideCornerResource;
    private ArtResource wallResource;
    private ArtResource capResource; // Capture?
    private ArtResource ceilingResource;
    private float ceilingHeight; // 368 - very likely flags
    private int researchTime; // 36c
    private int torchIntensity; // 36e
    private EnumSet<RoomFlag> flags; // 370
    private int tooltipStringId;
    private int nameStringId;
    private int cost;
    private int fightEffectId;
    private int generalDescriptionStringId;
    private int strengthStringId;
    private float torchHeight;
    private List<Integer> effects; // 382, in editor there are just 6
    private short roomId; // 392
    private short returnPercentage; // 393
    private short terrainId; // 394
    private TileConstruction tileConstruction; // 395
    private short createdCreatureId; // 396
    private Color torchColor; // 397
    private List<Short> objects; // 39a, in editor there are just 6
    private String soundCategory; // 3a2
    private short orderInEditor; // 3c2
    private float torchRadius;
    private ArtResource torch; // 3c7
    private short recommendedSizeX; // 41b
    private short recommendedSizeY; // 41c
    private short healthGain; // 41d

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public float getCeilingHeight() {
        return ceilingHeight;
    }

    protected void setCeilingHeight(float ceilingHeight) {
        this.ceilingHeight = ceilingHeight;
    }

    /**
     *
     * @return maybe. Not changed in editor
     */
    public int getResearchTime() {
        return researchTime;
    }

    protected void setResearchTime(int researchTime) {
        this.researchTime = researchTime;
    }

    public int getTorchIntensity() {
        return torchIntensity;
    }

    protected void setTorchIntensity(int torchIntensity) {
        this.torchIntensity = torchIntensity;
    }

    public EnumSet<RoomFlag> getFlags() {
        return flags;
    }

    protected void setFlags(EnumSet<RoomFlag> flags) {
        this.flags = flags;
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

    public int getCost() {
        return cost;
    }

    protected void setCost(int cost) {
        this.cost = cost;
    }

    public int getFightEffectId() {
        return fightEffectId;
    }

    protected void setFightEffectId(int fightEffectId) {
        this.fightEffectId = fightEffectId;
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

    public float getTorchRadius() {
        return torchRadius;
    }

    protected void setTorchRadius(float torchRadius) {
        this.torchRadius = torchRadius;
    }

    public List<Integer> getEffects() {
        return effects;
    }

    protected void setEffects(List<Integer> effects) {
        this.effects = effects;
    }

    public short getRoomId() {
        return roomId;
    }

    protected void setRoomId(short roomId) {
        this.roomId = roomId;
    }

    public short getReturnPercentage() {
        return returnPercentage;
    }

    protected void setReturnPercentage(short returnPercentage) {
        this.returnPercentage = returnPercentage;
    }

    public short getTerrainId() {
        return terrainId;
    }

    protected void setTerrainId(short terrainId) {
        this.terrainId = terrainId;
    }

    public TileConstruction getTileConstruction() {
        return tileConstruction;
    }

    protected void setTileConstruction(TileConstruction tileConstruction) {
        this.tileConstruction = tileConstruction;
    }

    public short getCreatedCreatureId() {
        return createdCreatureId;
    }

    protected void setCreatedCreatureId(short createdCreatureId) {
        this.createdCreatureId = createdCreatureId;
    }

    public Color getTorchColor() {
        return torchColor;
    }

    protected void setTorchColor(Color torchColor) {
        this.torchColor = torchColor;
    }

    public List<Short> getObjects() {
        return objects;
    }

    protected void setObjects(List<Short> objects) {
        this.objects = objects;
    }

    @Override
    public String getSoundCategory() {
        return soundCategory;
    }

    protected void setSoundCategory(String soundCategory) {
        this.soundCategory = soundCategory;
    }

    public short getOrderInEditor() {
        return orderInEditor;
    }

    protected void setOrderInEditor(short orderInEditor) {
        this.orderInEditor = orderInEditor;
    }

    public float getTorchHeight() {
        return torchHeight;
    }

    protected void setTorchHeight(float torchHeight) {
        this.torchHeight = torchHeight;
    }

    public short getRecommendedSizeX() {
        return recommendedSizeX;
    }

    protected void setRecommendedSizeX(short recommendedSizeX) {
        this.recommendedSizeX = recommendedSizeX;
    }

    public short getRecommendedSizeY() {
        return recommendedSizeY;
    }

    protected void setRecommendedSizeY(short recommendedSizeY) {
        this.recommendedSizeY = recommendedSizeY;
    }

    public short getHealthGain() {
        return healthGain;
    }

    protected void setHealthGain(short healthGain) {
        this.healthGain = healthGain;
    }

    public ArtResource getGuiIcon() {
        return guiIcon;
    }

    protected void setGuiIcon(ArtResource guiIcon) {
        this.guiIcon = guiIcon;
    }

    public ArtResource getEditorIcon() {
        return editorIcon;
    }

    protected void setEditorIcon(ArtResource editorIcon) {
        this.editorIcon = editorIcon;
    }

    public ArtResource getCompleteResource() {
        return completeResource;
    }

    protected void setCompleteResource(ArtResource completeResource) {
        this.completeResource = completeResource;
    }

    public ArtResource getStraightResource() {
        return straightResource;
    }

    protected void setStraightResource(ArtResource straightResource) {
        this.straightResource = straightResource;
    }

    public ArtResource getInsideCornerResource() {
        return insideCornerResource;
    }

    protected void setInsideCornerResource(ArtResource insideCornerResource) {
        this.insideCornerResource = insideCornerResource;
    }

    /**
     *
     * @return always null
     */
    public ArtResource getUnknownResource() {
        return unknownResource;
    }

    protected void setUnknownResource(ArtResource unknownResource) {
        this.unknownResource = unknownResource;
    }

    public ArtResource getOutsideCornerResource() {
        return outsideCornerResource;
    }

    protected void setOutsideCornerResource(ArtResource outsideCornerResource) {
        this.outsideCornerResource = outsideCornerResource;
    }

    public ArtResource getWallResource() {
        return wallResource;
    }

    protected void setWallResource(ArtResource wallResource) {
        this.wallResource = wallResource;
    }

    public ArtResource getCapResource() {
        return capResource;
    }

    protected void setCapResource(ArtResource capResource) {
        this.capResource = capResource;
    }

    public ArtResource getCeilingResource() {
        return ceilingResource;
    }

    protected void setCeilingResource(ArtResource ceilingResource) {
        this.ceilingResource = ceilingResource;
    }

    public ArtResource getTorch() {
        return torch;
    }

    protected void setTorch(ArtResource torch) {
        this.torch = torch;
    }

    @Override
    public short getId() {
        return roomId;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(Room o) {
        return Short.compare(orderInEditor, o.orderInEditor);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 19 * hash + this.roomId;
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
        final Room other = (Room) obj;
        if (this.roomId != other.roomId) {
            return false;
        }
        return true;
    }
}
