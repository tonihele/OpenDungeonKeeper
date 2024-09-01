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
package toniarts.openkeeper.game.data;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import toniarts.openkeeper.tools.convert.map.AI.AIType;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.utils.Point;

/**
 * Your FRIENDLY neighborhood Keeper, or not
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Keeper implements Comparable<Keeper>, IIndexable, Savable {

    private boolean ai;
    private AIType aiType = AIType.MASTER_KEEPER;

    private int gold;
    private int goldMined;
    private int mana;
    private int manaGain;
    private int manaLoose;
    private int maxMana;
    private Point dungeonHeartLocation;
    private boolean possession;
    private List<ResearchableEntity> availableRooms = new ArrayList<>();
    private List<ResearchableEntity> availableSpells = new ArrayList<>();
    private List<Short> availableCreatures = new ArrayList<>();
    private List<ResearchableEntity> availableDoors = new ArrayList<>();
    private List<ResearchableEntity> availableTraps = new ArrayList<>();
    private ResearchableEntity currentResearch;

    private transient Player player;
    private short id;
    private boolean destroyed = false;
    private Set<Short> allies = HashSet.newHashSet(4);

    public Keeper() {

    }

    public Keeper(boolean ai, short id) {
        this.ai = ai;
        this.id = id;
    }

    public Keeper(Player player) {
        this.player = player;
        this.id = player.getPlayerId();
    }

    @Override
    public short getId() {
        return id;
    }

    public void setId(short id) {
        this.id = id;
    }

    public Player getPlayer() {
        return player;
    }

    public int getGoldMined() {
        return goldMined;
    }

    public void setGoldMined(int goldMined) {
        this.goldMined = goldMined;
    }

    public void setManaGain(int manaGain) {
        this.manaGain = manaGain;
    }

    public int getManaGain() {
        return manaGain;
    }

    public void setManaLoose(int manaLoose) {
        this.manaLoose = manaLoose;
    }

    public int getManaLoose() {
        return manaLoose;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    @Override
    public String toString() {
        return Short.toString(id);
    }

    /**
     * Check whether the player is destroyed or not
     *
     * @return is destroyed
     */
    public boolean isDestroyed() {
        return destroyed;
    }

    public AIType getAiType() {
        return aiType;
    }

    public void setAiType(AIType aiType) {
        this.aiType = aiType;
    }

    public boolean isAi() {
        return ai;
    }

    /**
     * Is the given player an enemy of ours
     *
     * @param playerId the other player
     * @return is the player enemy of ours
     * @see #isAlly(short)
     */
    public boolean isEnemy(short playerId) {
        if (playerId == id || playerId == Player.NEUTRAL_PLAYER_ID || id == Player.NEUTRAL_PLAYER_ID) {
            return false; // Neutral player has no enemies
        }
        return !allies.contains(playerId);
    }

    /**
     * Is the player an ally of us. If not, it is not necessarily the enemy. But
     * we do not share vision, battles etc. with non-allies
     *
     * @param playerId the other player
     * @return is the player an ally of ours
     * @see #isEnemy(short)
     */
    public boolean isAlly(short playerId) {
        return id == playerId || allies.contains(playerId);
    }

    /**
     * Create alliance between two players, remember to set both players as
     * allies
     *
     * @param playerId the player to form alliance with
     */
    public void createAlliance(short playerId) {
        allies.add(playerId);
    }

    /**
     * Breaks alliance between two players, remember to break the alliance on
     * both players
     *
     * @param playerId the player to break alliance with
     */
    public void breakAlliance(short playerId) {
        allies.remove(playerId);
    }

    public int getGold() {
        return gold;
    }

    public int getMana() {
        return mana;
    }

    public void setGold(int gold) {
        this.gold = gold;
    }

    public void setMana(int mana) {
        this.mana = mana;
    }

    public int getMaxMana() {
        return maxMana;
    }

    public void setMaxMana(int maxMana) {
        this.maxMana = maxMana;
    }

    public Point getDungeonHeartLocation() {
        return dungeonHeartLocation;
    }

    public void setDungeonHeartLocation(Point dungeonHeartLocation) {
        this.dungeonHeartLocation = dungeonHeartLocation;
    }

    public boolean isPossession() {
        return possession;
    }

    public void setPossession(boolean possession) {
        this.possession = possession;
    }

    public List<ResearchableEntity> getAvailableRooms() {
        return availableRooms;
    }

    public List<ResearchableEntity> getAvailableSpells() {
        return availableSpells;
    }

    public List<Short> getAvailableCreatures() {
        return availableCreatures;
    }

    public List<ResearchableEntity> getAvailableDoors() {
        return availableDoors;
    }

    public List<ResearchableEntity> getAvailableTraps() {
        return availableTraps;
    }

    public void setCurrentResearch(ResearchableEntity currentResearch) {
        this.currentResearch = currentResearch;
    }

    public ResearchableEntity getCurrentResearch() {
        return currentResearch;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(id, "playerId", Integer.valueOf(0).shortValue());
        out.write(ai, "ai", false);
        out.write(aiType, "aiType", null);
        out.write(gold, "gold", 0);
        out.write(goldMined, "goldMined", 0);
        out.write(mana, "mana", 0);
        out.write(manaGain, "manaGain", 0);
        out.write(manaLoose, "manaLoose", 0);
        out.write(maxMana, "maxMana", 0);
        out.write(dungeonHeartLocation != null ? dungeonHeartLocation.x : 0, "dungeonHeartLocationX", 0);
        out.write(dungeonHeartLocation != null ? dungeonHeartLocation.y : 0, "dungeonHeartLocationY", 0);
        out.writeSavableArrayList(new ArrayList(availableRooms), "availableRooms", null);
        out.writeSavableArrayList(new ArrayList(availableSpells), "availableSpells", null);
        out.write(toPrimitiveShortArray(availableCreatures), "availableCreatures", new short[0]);
        out.writeSavableArrayList(new ArrayList(availableDoors), "availableDoors", null);
        out.writeSavableArrayList(new ArrayList(availableTraps), "availableTraps", null);
        out.write(currentResearch, "currentResearch", null);
        out.write(destroyed, "destroyed", false);
        out.write(toPrimitiveShortArray(allies), "allies", new short[0]);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        id = in.readShort("playerId", id);
        ai = in.readBoolean("ai", ai);
        aiType = in.readEnum("aiType", AIType.class, aiType);
        gold = in.readInt("gold", gold);
        goldMined = in.readInt("goldMined", goldMined);
        mana = in.readInt("mana", mana);
        manaGain = in.readInt("manaGain", manaGain);
        manaLoose = in.readInt("manaLoose", manaLoose);
        maxMana = in.readInt("maxMana", maxMana);
        int x = in.readInt("dungeonHeartLocationX", 0);
        int y = in.readInt("dungeonHeartLocationY", 0);
        dungeonHeartLocation = new Point(x, y);
        availableRooms = in.readSavableArrayList("availableRooms", new ArrayList<>());
        availableSpells = in.readSavableArrayList("availableSpells", new ArrayList<>());
        availableCreatures = toReferenceList(in.readShortArray("availableCreatures", new short[0]));
        availableDoors = in.readSavableArrayList("availableDoors", new ArrayList<>());
        availableTraps = in.readSavableArrayList("availableTraps", new ArrayList<>());
        currentResearch = (ResearchableEntity) in.readSavable("currentResearch", null);
        destroyed = in.readBoolean("destroyed", destroyed);
        allies = new HashSet<>(toReferenceList(in.readShortArray("allies", new short[0])));
    }

    private static short[] toPrimitiveShortArray(Collection<Short> list) {
        short[] primList = new short[list.size()];
        int i = 0;
        for (Short value : list) {
            primList[i] = value;

            i++;
        }

        return primList;
    }

    private static List<Short> toReferenceList(short[] shortArray) {
        List<Short> list = new ArrayList(shortArray.length);
        for (short value : shortArray) {
            list.add(value);
        }

        return list;
    }

    @Override
    public int compareTo(Keeper keeper) {
        return Short.compare(id, keeper.id);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + this.id;
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
        final Keeper other = (Keeper) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

}
