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

import com.jme3.network.serializing.serializers.EnumSerializer;
import java.util.EnumSet;
import java.util.List;
import javax.vecmath.Vector3f;
import toniarts.openkeeper.game.data.IIndexable;
import toniarts.openkeeper.game.data.ITriggerable;
import toniarts.openkeeper.game.network.Transferable;
import toniarts.openkeeper.tools.convert.IFlagEnum;
import toniarts.openkeeper.tools.convert.IValueEnum;
import toniarts.openkeeper.tools.convert.map.Thing.HeroParty.Objective;

/**
 * Container class for *Things.kld
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class Thing {

//    struct ActionPointBlock {
//        int32_t x00;
//        int32_t x04;
//        int32_t x08;
//        int32_t x0c;
//        int32_t x10;
//        int16_t x14;
//        uint8_t id; /* 16 */
//        uint8_t x17;
//        char name[32]; /* 18 */
//        };
    public static class ActionPoint extends Thing implements Comparable<ActionPoint>, ITriggerable, IIndexable {

        /**
         * ActionPoint flags
         */
        public enum ActionPointFlag implements IFlagEnum {

            HERO_LAIR(0x01),
            UNKNOWN_4(0x04),
            UNKNOWN_8(0x08),
            REVEAL_THROUGH_FOG_OF_WAR(0x10),
            TOOL_BOX(0x20),
            IGNORE_SOLID(0x40);

            private final long flagValue;

            private ActionPointFlag(long flagValue) {
                this.flagValue = flagValue;
            }

            @Override
            public long getFlagValue() {
                return flagValue;
            }
        };
        private int startX; // 0-based coordinate
        private int startY; // 0-based coordinate
        private int endX; // 0-based coordinate
        private int endY; // 0-based coordinate
        private int waitDelay;
        private EnumSet<ActionPointFlag> flags;
        private int triggerId;
        private short id; // 16
        private short nextWaypointId; // Is always another ActionPoint?
        private String name; // 18 <- Always just shit

        public int getStartX() {
            return startX;
        }

        protected void setStartX(int startX) {
            this.startX = startX;
        }

        public int getStartY() {
            return startY;
        }

        protected void setStartY(int startY) {
            this.startY = startY;
        }

        public int getEndX() {
            return endX;
        }

        protected void setEndX(int endX) {
            this.endX = endX;
        }

        public int getEndY() {
            return endY;
        }

        protected void setEndY(int endY) {
            this.endY = endY;
        }

        public int getWaitDelay() {
            return waitDelay;
        }

        protected void setWaitDelay(int waitDelay) {
            this.waitDelay = waitDelay;
        }

        public EnumSet<ActionPointFlag> getFlags() {
            return flags;
        }

        protected void setFlags(EnumSet<ActionPointFlag> flags) {
            this.flags = flags;
        }

        @Override
        public int getTriggerId() {
            return triggerId;
        }

        public void setTriggerId(int triggerId) {
            this.triggerId = triggerId;
        }

        @Override
        public short getId() {
            return id;
        }

        protected void setId(short id) {
            this.id = id;
        }

        public short getNextWaypointId() {
            return nextWaypointId;
        }

        protected void setNextWaypointId(short nextWaypointId) {
            this.nextWaypointId = nextWaypointId;
        }

        public String getName() {
            return name;
        }

        protected void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "[ID " + id + "] Action Point " + id
                    + " [" + (startX + 1) + "," + (startY + 1)
                    + "] - [" + (endX + 1) + "," + (endY + 1) + "]";
        }

        @Override
        public int compareTo(ActionPoint o) {
            return Short.compare(id, o.id);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 73 * hash + this.id;
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
            final ActionPoint other = (ActionPoint) obj;
            if (this.id != other.id) {
                return false;
            }
            return true;
        }
    }

    public abstract static class Creature extends Thing {

        /**
         * Creature flags
         */
        public enum CreatureFlag implements IFlagEnum {
            // ELITE contains in creatureId

            WILL_FIGHT(0x001),
            LEADER(0x002),
            FOLLOWER(0x004),
            WILL_BE_ATTACKED(0x008),
            RETURN_TO_HERO_LAIR(0x010),
            FREE_FRIENDS_ON_JAIL_BREAK(0x020),
            ACT_AS_DROPPED(0x040),
            START_AS_DYING(0x080);
            private final long flagValue;

            private CreatureFlag(long flagValue) {
                this.flagValue = flagValue;
            }

            @Override
            public long getFlagValue() {
                return flagValue;
            }
        };

        /**
         * Creature flags
         */
        public enum CreatureFlag2 implements IFlagEnum {

            DESTROY_ROOMS(0x001),
            I_AM_A_TOOL(0x002),
            DIES_INSTANTLY(0x004),
            I_AM_A_MERCENARY(0x008);
            private final long flagValue;

            private CreatureFlag2(long flagValue) {
                this.flagValue = flagValue;
            }

            @Override
            public long getFlagValue() {
                return flagValue;
            }
        };
        private int posX; // 0-based coordinate
        private int posY; // 0-based coordinate
        private int posZ; // ???
        private int goldHeld; // Percent
        private short creatureId; // 1a

        public int getPosX() {
            return posX;
        }

        protected void setPosX(int posX) {
            this.posX = posX;
        }

        public int getPosY() {
            return posY;
        }

        protected void setPosY(int posY) {
            this.posY = posY;
        }

        public int getPosZ() {
            return posZ;
        }

        protected void setPosZ(int posZ) {
            this.posZ = posZ;
        }

        public int getGoldHeld() {
            return goldHeld;
        }

        protected void setGoldHeld(int goldHeld) {
            this.goldHeld = goldHeld;
        }

        public short getCreatureId() {
            return creatureId;
        }

        protected void setCreatureId(short creatureId) {
            this.creatureId = creatureId;
        }
    }

    public static class KeeperCreature extends Creature implements ITriggerable {

        private short level;
        private EnumSet<Creature.CreatureFlag> flags; // Short, likely flags
        private int initialHealth; // Percent
        private int objectiveTargetActionPointId;
        private int triggerId;
        private short playerId; // player id

        public short getLevel() {
            return level;
        }

        protected void setLevel(short level) {
            this.level = level;
        }

        public EnumSet<CreatureFlag> getFlags() {
            return flags;
        }

        protected void setFlags(EnumSet<CreatureFlag> flags) {
            this.flags = flags;
        }

        public int getInitialHealth() {
            return initialHealth;
        }

        protected void setInitialHealth(int initialHealth) {
            this.initialHealth = initialHealth;
        }

        public int getObjectiveTargetActionPointId() {
            return objectiveTargetActionPointId;
        }

        protected void setObjectiveTargetActionPointId(int objectiveTargetActionPointId) {
            this.objectiveTargetActionPointId = objectiveTargetActionPointId;
        }

        @Override
        public int getTriggerId() {
            return triggerId;
        }

        protected void setTriggerId(int triggerId) {
            this.triggerId = triggerId;
        }

        public short getPlayerId() {
            return playerId;
        }

        protected void setPlayerId(short playerId) {
            this.playerId = playerId;
        }
    }

//    struct Thing11Block {
//        Thing11Block()
//        : x00(0), x04(0), x08(0), x0c(0), x0e(0), x0f(0), x10(0), x12(0), x13(0) { }
//        int32_t x00;
//        int32_t x04;
//        int32_t x08;
//        int16_t x0c;
//        uint8_t x0e;
//        uint8_t x0f; /* maybe padding */
//        int16_t x10;
//        uint8_t x12;
//        uint8_t x13;
//        };
    public static class Room extends Thing {

        public enum RoomType implements IValueEnum {

            PORTAL(12),
            DUNGEON_HEART(14),
            HERO_GATE_2X2(33),
            HERO_GATE_FRONT_END(34), // ??, in the 3D menu
            HERO_GATE_3X1(37),
            HERO_PORTAL(40);

            private RoomType(int id) {
                this.id = id;
            }

            @Override
            public int getValue() {
                return id;
            }
            private final int id;
        }

        public enum Direction implements IValueEnum {

            NORTH(0),
            EAST(2),
            SOUTH(4),
            WEST(6);

            private Direction(int id) {
                this.id = id;
            }

            @Override
            public int getValue() {
                return id;
            }
            private final int id;
        }
        private int posX; // 0-based coordinate
        private int posY; // 0-based coordinate
        private int x08;
        private int x0c;
        private Direction direction; // Hero gates have direction
        private short x0f; // maybe padding
        private int initialHealth; // Percentage
        private RoomType roomType; // 14 dh, 12 portal, 33 hero gate 2x2, 37 hero gate 3x1, 40 hero portal
        private short playerId;

        public int getPosX() {
            return posX;
        }

        protected void setPosX(int posX) {
            this.posX = posX;
        }

        public int getPosY() {
            return posY;
        }

        protected void setPosY(int posY) {
            this.posY = posY;
        }

        public int getX08() {
            return x08;
        }

        protected void setX08(int x08) {
            this.x08 = x08;
        }

        public int getX0c() {
            return x0c;
        }

        protected void setX0c(int x0c) {
            this.x0c = x0c;
        }

        public Direction getDirection() {
            return direction;
        }

        protected void setDirection(Direction direction) {
            this.direction = direction;
        }

        public short getX0f() {
            return x0f;
        }

        protected void setX0f(short x0f) {
            this.x0f = x0f;
        }

        public int getInitialHealth() {
            return initialHealth;
        }

        protected void setInitialHealth(int initialHealth) {
            this.initialHealth = initialHealth;
        }

        public RoomType getRoomType() {
            return roomType;
        }

        protected void setRoomType(RoomType roomType) {
            this.roomType = roomType;
        }

        public short getPlayerId() {
            return playerId;
        }

        protected void setPlayerId(short playerId) {
            this.playerId = playerId;
        }

        @Override
        public String toString() {
            return roomType + " [" + (posX + 1) + "," + (posY + 1) + "]";
        }
    }

    // Contains in GlobalVariables.kwd only
    public static class Camera extends Thing {

        public static int ID_POSSESION = 2;
        public static int ID_GAME = 3;

        public enum CameraFlag implements IFlagEnum {

            DISABLE_YAW(0x01), // fixed angleXY ?
            DISABLE_ROLL(0x02), // fixed angleYZ ?
            DISABLE_PITCH(0x04), // fixed angleXZ ?
            DISABLE_ZOOM(0x08),
            UNKNOWN_10(0x10), // fixed ViewDistance, Lens
            UNKNOWN_20(0x20), // fixed ViewDistance, Lens
            DISABLE_MOVE(0x40), // fixed Position
            DISABLE_CHANGE(0x80); // Never used. camera not enter and leave possession.

            private CameraFlag(long flagValue) {
                this.flagValue = flagValue;
            }

            @Override
            public long getFlagValue() {
                return flagValue;
            }

            private final long flagValue;
        };

        private Vector3f position;
        private Vector3f positionMinClipExtent;
        private Vector3f positionMaxClipExtent;
        private float viewDistanceValue; // Volume fog. Used in Possession.
        private float viewDistanceMin;
        private float viewDistanceMax;
        private float zoomValue; // in game zoom height
        private float zoomValueMin;
        private float zoomValueMax;
        private float lensValue; // field of view
        private float lensValueMin;
        private float lensValueMax;
        private EnumSet<CameraFlag> flags;
        private int angleYaw; // always 0
        private int angleRoll; // rotate camera around direction vector.
        private int anglePitch; // rotate camera around Left vector. 512 = front.

        /* 2 - possession camera
         * 3 - game camera
         * other never used ?
         */
        private short id;

        public Vector3f getPosition() {
            return position;
        }

        protected void setPosition(float x, float y, float z) {
            this.position = new Vector3f(x, y, z);
        }

        public Vector3f getPositionMinClipExtent() {
            return positionMinClipExtent;
        }

        protected void setPositionMinClipExtent(float x, float y, float z) {
            this.positionMinClipExtent = new Vector3f(x, y, z);
        }

        public Vector3f getPositionMaxClipExtent() {
            return positionMaxClipExtent;
        }

        protected void setPositionMaxClipExtent(float x, float y, float z) {
            this.positionMaxClipExtent = new Vector3f(x, y, z);
        }

        public float getViewDistanceValue() {
            return viewDistanceValue;
        }

        protected void setViewDistanceValue(float fog) {
            this.viewDistanceValue = fog;
        }

        public float getViewDistanceMin() {
            return viewDistanceMin;
        }

        protected void setViewDistanceMin(float distance) {
            this.viewDistanceMin = distance;
        }

        public float getViewDistanceMax() {
            return viewDistanceMax;
        }

        protected void setViewDistanceMax(float distance) {
            this.viewDistanceMax = distance;
        }

        public float getZoomValue() {
            return zoomValue;
        }

        protected void setZoomValue(float zoom) {
            this.zoomValue = zoom;
        }

        public float getZoomValueMin() {
            return zoomValueMin;
        }

        protected void setZoomValueMin(float zoom) {
            this.zoomValueMin = zoom;
        }

        public float getZoomValueMax() {
            return zoomValueMax;
        }

        protected void setZoomValueMax(float zoom) {
            this.zoomValueMax = zoom;
        }

        public float getLensValue() {
            return lensValue;
        }

        protected void setLensValue(float lens) {
            this.lensValue = lens;
        }

        public float getLensValueMin() {
            return lensValueMin;
        }

        protected void setLensValueMin(float lens) {
            this.lensValueMin = lens;
        }

        public float getLensValueMax() {
            return lensValueMax;
        }

        protected void setLensValueMax(float lens) {
            this.lensValueMax = lens;
        }

        public EnumSet<CameraFlag> getFlags() {
            return flags;
        }

        protected void setFlags(EnumSet<CameraFlag> flags) {
            this.flags = flags;
        }

        public int getAngleYaw() {
            return angleYaw;
        }

        protected void setAngleYaw(int angle) {
            this.angleYaw = angle;
        }

        public int getAngleRoll() {
            return angleRoll;
        }

        protected void setAngleRoll(int angle) {
            this.angleRoll = angle;
        }

        public int getAnglePitch() {
            return anglePitch;
        }

        protected void setAnglePitch(int angle) {
            this.anglePitch = angle;
        }

        public short getId() {
            return id;
        }

        protected void setId(short id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "Camera { "
                    + "position=" + position
                    + ", positionMinClipExtent=" + positionMinClipExtent
                    + ", positionMaxClipExtent=" + positionMaxClipExtent
                    + ", viewDistanceValue=" + viewDistanceValue
                    + ", viewDistanceMin=" + viewDistanceMin
                    + ", viewDistanceMax=" + viewDistanceMax
                    + ", zoomValue=" + zoomValue
                    + ", zoomValueMin=" + zoomValueMin
                    + ", zoomValueMax=" + zoomValueMax
                    + ", lensValue=" + lensValue
                    + ", lensValueMin=" + lensValueMin
                    + ", lensValueMax=" + lensValueMax
                    + ", flags=" + flags
                    + ", angleYaw=" + angleYaw + ", angleRoll=" + angleRoll + ", anglePitch=" + anglePitch
                    + ", id=" + id + " }";
        }
    }

//struct Thing08Block { /* hero party */
//    char name[32];
//    int16_t x20;
//    uint8_t x22;
//    int32_t x23; /* these two are unreferenced... */
//    int32_t x27;
//    HeroPartyData x2b[16];
//    };
    public static class HeroParty extends Thing implements Comparable<HeroParty>, ITriggerable, IIndexable {

        /**
         * This is really a subset of
         * {@link toniarts.openkeeper.tools.convert.map.Creature.JobType}, only
         * includes the job types a hero party can do. FIXME: maybe we should
         * unify these, just add a boolean whether it is available to parties
         * only
         */
        @Transferable(EnumSerializer.class)
        public enum Objective implements IValueEnum {

            NONE(0),
            DESTROY_ROOMS(11),
            DESTROY_WALLS(12),
            STEAL_GOLD(13),
            STEAL_SPELLS(14),
            STEAL_MAFUFACTURE_CRATES(17),
            KILL_CREATURES(18),
            KILL_PLAYER(19),
            WAIT(22),
            SEND_TO_ACTION_POINT(23),
            JAIL_BREAK(27);

            private Objective(int id) {
                this.id = id;
            }

            @Override
            public int getValue() {
                return id;
            }
            private final int id;
        }
        private String name; // Always shit
        private int triggerId;
        private short id; // I assume, autoincremental 0-based
        private int x23; // these two are unreferenced...
        private int x27;
        private List<GoodCreature> heroPartyMembers; // 16 at max

        public String getName() {
            return name;
        }

        protected void setName(String name) {
            this.name = name;
        }

        @Override
        public int getTriggerId() {
            return triggerId;
        }

        protected void setTriggerId(int triggerId) {
            this.triggerId = triggerId;
        }

        @Override
        public short getId() {
            return id;
        }

        protected void setId(short id) {
            this.id = id;
        }

        public int getX23() {
            return x23;
        }

        protected void setX23(int x23) {
            this.x23 = x23;
        }

        public int getX27() {
            return x27;
        }

        protected void setX27(int x27) {
            this.x27 = x27;
        }

        public List<GoodCreature> getHeroPartyMembers() {
            return heroPartyMembers;
        }

        protected void setHeroPartyMembers(List<GoodCreature> heroPartyMembers) {
            this.heroPartyMembers = heroPartyMembers;
        }

        @Override
        public String toString() {
            return "Hero Party " + (id + 1);
        }

        @Override
        public int compareTo(HeroParty o) {
            return Short.compare(id, o.id);
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 71 * hash + this.id;
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
            final HeroParty other = (HeroParty) obj;
            if (this.id != other.id) {
                return false;
            }
            return true;
        }

//        struct HeroPartyData {
//            int32_t x00;
//            int32_t x04;
//            int32_t x08;
//            int16_t goldHeld;
//            uint8_t x0e; /* level */
//            uint8_t x0f;
//            int32_t x10;
//            int32_t initialHealth;
//            int16_t x18; /* trigger root */
//            uint8_t x1a;
//            uint8_t x1b;
//            uint8_t x1c;
//            uint8_t x1d;
//            uint8_t x1e;
//            uint8_t x1f;
//            };
        /**
         * Represents the party members
         */
        public class HeroPartyData implements ITriggerable {

            private int x00;
            private int x04;
            private int x08;
            private int goldHeld;
            private short level; // level
            private short x0f;
            private int objectiveTargetActionPointId;
            private int initialHealth;
            private int triggerId; // trigger root
            private short objectiveTargetPlayerId;
            private Objective objective;
            private short creatureId;
            private short x1d;
            private short x1e;
            private short x1f;

            public int getX00() {
                return x00;
            }

            protected void setX00(int x00) {
                this.x00 = x00;
            }

            public int getX04() {
                return x04;
            }

            protected void setX04(int x04) {
                this.x04 = x04;
            }

            public int getX08() {
                return x08;
            }

            protected void setX08(int x08) {
                this.x08 = x08;
            }

            public int getGoldHeld() {
                return goldHeld;
            }

            protected void setGoldHeld(int goldHeld) {
                this.goldHeld = goldHeld;
            }

            public short getLevel() {
                return level;
            }

            protected void setLevel(short level) {
                this.level = level;
            }

            public short getX0f() {
                return x0f;
            }

            protected void setX0f(short x0f) {
                this.x0f = x0f;
            }

            public int getObjectiveTargetActionPointId() {
                return objectiveTargetActionPointId;
            }

            protected void setObjectiveTargetActionPointId(int objectiveTargetActionPointId) {
                this.objectiveTargetActionPointId = objectiveTargetActionPointId;
            }

            public int getInitialHealth() {
                return initialHealth;
            }

            protected void setInitialHealth(int initialHealth) {
                this.initialHealth = initialHealth;
            }

            @Override
            public int getTriggerId() {
                return triggerId;
            }

            protected void setTriggerId(int triggerId) {
                this.triggerId = triggerId;
            }

            public short getObjectiveTargetPlayerId() {
                return objectiveTargetPlayerId;
            }

            protected void setObjectiveTargetPlayerId(short objectiveTargetPlayerId) {
                this.objectiveTargetPlayerId = objectiveTargetPlayerId;
            }

            public Objective getObjective() {
                return objective;
            }

            protected void setObjective(Objective objective) {
                this.objective = objective;
            }

            public short getCreatureId() {
                return creatureId;
            }

            protected void setCreatureId(short creatureId) {
                this.creatureId = creatureId;
            }

            public short getX1d() {
                return x1d;
            }

            protected void setX1d(short x1d) {
                this.x1d = x1d;
            }

            public short getX1e() {
                return x1e;
            }

            protected void setX1e(short x1e) {
                this.x1e = x1e;
            }

            public short getX1f() {
                return x1f;
            }

            protected void setX1f(short x1f) {
                this.x1f = x1f;
            }
        }
    }

//    struct Thing10Block {
//        int32_t x00;
//        int32_t x04;
//        int32_t x08;
//        int32_t x0c;
//        int16_t x10;
//        int16_t x12;
//        int16_t x14[4];
//        uint8_t x1c;
//        uint8_t x1d;
//        uint8_t pad[6];
//        };
    public static class EffectGenerator extends Thing {

        private int posX; // 0-based coordinate
        private int posY; // 0-based coordinate
        private int x08;
        private int x0c;
        private int x10;
        private int x12;
        private List<Integer> effectIds; // 4
        private short frequency;
        private short id;
        private short pad[];

        public int getPosX() {
            return posX;
        }

        protected void setPosX(int posX) {
            this.posX = posX;
        }

        public int getPosY() {
            return posY;
        }

        protected void setPosY(int posY) {
            this.posY = posY;
        }

        public int getX08() {
            return x08;
        }

        protected void setX08(int x08) {
            this.x08 = x08;
        }

        public int getX0c() {
            return x0c;
        }

        protected void setX0c(int x0c) {
            this.x0c = x0c;
        }

        public int getX10() {
            return x10;
        }

        protected void setX10(int x10) {
            this.x10 = x10;
        }

        public int getX12() {
            return x12;
        }

        protected void setX12(int x12) {
            this.x12 = x12;
        }

        public List<Integer> getEffectIds() {
            return effectIds;
        }

        protected void setEffectIds(List<Integer> effectIds) {
            this.effectIds = effectIds;
        }

        public short getFrequency() {
            return frequency;
        }

        protected void setFrequency(short frequency) {
            this.frequency = frequency;
        }

        public short getId() {
            return id;
        }

        protected void setId(short id) {
            this.id = id;
        }

        public short[] getPad() {
            return pad;
        }

        protected void setPad(short[] pad) {
            this.pad = pad;
        }
    }

    public static class DeadBody extends Creature {

        private short playerId;

        public short getPlayerId() {
            return playerId;
        }

        protected void setPlayerId(short playerId) {
            this.playerId = playerId;
        }
    }

    public static class NeutralCreature extends Creature implements ITriggerable {

        private short level; // level
        private EnumSet<CreatureFlag> flags; // Short, likely flags
        private int initialHealth; // Percent
        private int triggerId;
        private short unknown1;

        public short getLevel() {
            return level;
        }

        protected void setLevel(short level) {
            this.level = level;
        }

        public EnumSet<CreatureFlag> getFlags() {
            return flags;
        }

        protected void setFlags(EnumSet<CreatureFlag> flags) {
            this.flags = flags;
        }

        public int getInitialHealth() {
            return initialHealth;
        }

        protected void setInitialHealth(int initialHealth) {
            this.initialHealth = initialHealth;
        }

        @Override
        public int getTriggerId() {
            return triggerId;
        }

        protected void setTriggerId(int triggerId) {
            this.triggerId = triggerId;
        }

        public short getUnknown1() {
            return unknown1;
        }

        protected void setUnknown1(short unknown1) {
            this.unknown1 = unknown1;
        }
    }

    public static class Door extends Thing implements ITriggerable {

        public enum DoorFlag implements IValueEnum {

            NONE(0),
            LOCKED(1),
            BLUEPRINT(2);

            private DoorFlag(int id) {
                this.id = id;
            }

            @Override
            public int getValue() {
                return id;
            }
            private final int id;
        }
        private int posX; // 0-based coordinate
        private int posY; // 0-based coordinate
        private int unknown1; // 4
        private int triggerId;
        private short doorId;
        private short playerId;
        private DoorFlag flag;
        private short unknown2[];  // 3

        public int getPosX() {
            return posX;
        }

        protected void setPosX(int posX) {
            this.posX = posX;
        }

        public int getPosY() {
            return posY;
        }

        protected void setPosY(int posY) {
            this.posY = posY;
        }

        public int getUnknown1() {
            return unknown1;
        }

        protected void setUnknown1(int unknown1) {
            this.unknown1 = unknown1;
        }

        @Override
        public int getTriggerId() {
            return triggerId;
        }

        protected void setTriggerId(int triggerId) {
            this.triggerId = triggerId;
        }

        public short getDoorId() {
            return doorId;
        }

        protected void setDoorId(short doorId) {
            this.doorId = doorId;
        }

        public short getPlayerId() {
            return playerId;
        }

        protected void setPlayerId(short playerId) {
            this.playerId = playerId;
        }

        public DoorFlag getFlag() {
            return flag;
        }

        protected void setFlag(DoorFlag flag) {
            this.flag = flag;
        }

        public short[] getUnknown2() {
            return unknown2;
        }

        protected void setUnknown2(short[] unknown2) {
            this.unknown2 = unknown2;
        }
    }

    public static class Object extends Thing implements ITriggerable {

        private int posX; // 0-based coordinate
        private int posY; // 0-based coordinate
        private short unknown1[]; // 4
        private int keeperSpellId; // Data 1
        private int moneyAmount; // Data 2
        private int triggerId;
        private short objectId;
        private short playerId;

        public int getPosX() {
            return posX;
        }

        protected void setPosX(int posX) {
            this.posX = posX;
        }

        public int getPosY() {
            return posY;
        }

        protected void setPosY(int posY) {
            this.posY = posY;
        }

        public short[] getUnknown1() {
            return unknown1;
        }

        protected void setUnknown1(short[] unknown1) {
            this.unknown1 = unknown1;
        }

        public int getKeeperSpellId() {
            return keeperSpellId;
        }

        protected void setKeeperSpellId(int keeperSpellId) {
            this.keeperSpellId = keeperSpellId;
        }

        public int getMoneyAmount() {
            return moneyAmount;
        }

        protected void setMoneyAmount(int moneyAmount) {
            this.moneyAmount = moneyAmount;
        }

        @Override
        public int getTriggerId() {
            return triggerId;
        }

        protected void setTriggerId(int triggerId) {
            this.triggerId = triggerId;
        }

        public short getObjectId() {
            return objectId;
        }

        protected void setObjectId(short objectId) {
            this.objectId = objectId;
        }

        public short getPlayerId() {
            return playerId;
        }

        protected void setPlayerId(short playerId) {
            this.playerId = playerId;
        }
    }

    public static class Trap extends Thing {

        private int posX; // 0-based coordinate
        private int posY; // 0-based coordinate
        private int unknown1; // 4
        private short numberOfShots;
        private short trapId;
        private short playerId;
        private short unknown2; // 1

        public int getPosX() {
            return posX;
        }

        protected void setPosX(int posX) {
            this.posX = posX;
        }

        public int getPosY() {
            return posY;
        }

        protected void setPosY(int posY) {
            this.posY = posY;
        }

        public int getUnknown1() {
            return unknown1;
        }

        protected void setUnknown1(int unknown1) {
            this.unknown1 = unknown1;
        }

        /**
         * Get number of shots, what ever that means... Seems that the info
         * about being blueprint is just saved here, when this is 0, then the
         * trap is a blueprint
         *
         * @return
         */
        public short getNumberOfShots() {
            return numberOfShots;
        }

        protected void setNumberOfShots(short numberOfShots) {
            this.numberOfShots = numberOfShots;
        }

        public short getTrapId() {
            return trapId;
        }

        protected void setTrapId(short trapId) {
            this.trapId = trapId;
        }

        public short getPlayerId() {
            return playerId;
        }

        protected void setPlayerId(short playerId) {
            this.playerId = playerId;
        }

        public short getUnknown2() {
            return unknown2;
        }

        protected void setUnknown2(short unknown2) {
            this.unknown2 = unknown2;
        }
    }

    public static class GoodCreature extends Creature implements ITriggerable {

        private short level; // level
        private EnumSet<CreatureFlag> flags; // Short, likely flags
        private int objectiveTargetActionPointId;
        private int initialHealth; // Percent
        private int triggerId;
        private short objectiveTargetPlayerId; // target objective
        private HeroParty.Objective objective;
        private short unknown1[];
        private EnumSet<Thing.Creature.CreatureFlag2> flags2;

        public short getLevel() {
            return level;
        }

        protected void setLevel(short level) {
            this.level = level;
        }

        public EnumSet<CreatureFlag> getFlags() {
            return flags;
        }

        protected void setFlags(EnumSet<CreatureFlag> flags) {
            this.flags = flags;
        }

        public int getObjectiveTargetActionPointId() {
            return objectiveTargetActionPointId;
        }

        protected void setObjectiveTargetActionPointId(int objectiveTargetActionPointId) {
            this.objectiveTargetActionPointId = objectiveTargetActionPointId;
        }

        public int getInitialHealth() {
            return initialHealth;
        }

        protected void setInitialHealth(int initialHealth) {
            this.initialHealth = initialHealth;
        }

        @Override
        public int getTriggerId() {
            return triggerId;
        }

        protected void setTriggerId(int triggerId) {
            this.triggerId = triggerId;
        }

        public short getObjectiveTargetPlayerId() {
            return objectiveTargetPlayerId;
        }

        protected void setObjectiveTargetPlayerId(short objectiveTargetPlayerId) {
            this.objectiveTargetPlayerId = objectiveTargetPlayerId;
        }

        public Objective getObjective() {
            return objective;
        }

        protected void setObjective(Objective objective) {
            this.objective = objective;
        }

        public short[] getUnknown1() {
            return unknown1;
        }

        protected void setUnknown1(short unknown1[]) {
            this.unknown1 = unknown1;
        }

        public EnumSet<CreatureFlag2> getFlags2() {
            return flags2;
        }

        protected void setFlags2(EnumSet<CreatureFlag2> flags2) {
            this.flags2 = flags2;
        }
    }
}
