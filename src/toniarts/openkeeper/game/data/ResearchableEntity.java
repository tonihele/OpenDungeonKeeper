/*
 * Copyright (C) 2014-2019 OpenKeeper
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
import com.jme3.network.serializing.serializers.FieldSerializer;
import java.io.IOException;
import toniarts.openkeeper.game.network.Transferable;

/**
 * Represents a researchable item such as keeper spell, room, trap, etc
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Transferable(FieldSerializer.class)
public class ResearchableEntity implements Savable {

    protected short id;
    protected ResearchableType researchableType;
    protected boolean discovered = false;
    protected int research = 0;
    protected boolean upgraded = false;

    public ResearchableEntity() {
    }

    public ResearchableEntity(short id, ResearchableType researchableType) {
        this(id, false, researchableType);
    }

    public ResearchableEntity(short id, boolean discovered, ResearchableType researchableType) {
        this.id = id;
        this.discovered = discovered;
        this.researchableType = researchableType;
    }

    public short getId() {
        return id;
    }

    public boolean isDiscovered() {
        return discovered;
    }

    public void setDiscovered(boolean discovered) {
        this.discovered = discovered;
    }

    public int getResearch() {
        return research;
    }

    public void setResearch(int research) {
        this.research = research;
    }

    public ResearchableType getResearchableType() {
        return researchableType;
    }

    public boolean isUpgraded() {
        return upgraded;
    }

    public void setUpgraded(boolean upgraded) {
        this.upgraded = upgraded;
    }

    public boolean isUpgradedable() {
        return false;
    }

    @Override
    public String toString() {
        return "ResearchableEntity{" + "id=" + id + ", researchableType=" + researchableType + ", discovered=" + discovered + ", research=" + research + '}';
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + this.id;
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
        final ResearchableEntity other = (ResearchableEntity) obj;
        if (this.id != other.id) {
            return false;
        }
        if (this.researchableType != other.researchableType) {
            return false;
        }
        return true;
    }


    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(id, "id", Integer.valueOf(0).shortValue());
        out.write(researchableType, "researchableType", null);
        out.write(discovered, "discovered", false);
        out.write(research, "research", 0);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        id = in.readShort("id", id);
        researchableType = in.readEnum("researchableType", ResearchableType.class, null);
        discovered = in.readBoolean("discovered", discovered);
        research = in.readInt("research", research);
    }

}
