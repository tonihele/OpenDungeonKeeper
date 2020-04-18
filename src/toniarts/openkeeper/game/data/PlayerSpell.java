/*
 * Copyright (C) 2014-2016 OpenKeeper
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
import com.jme3.network.serializing.serializers.FieldSerializer;
import java.io.IOException;
import toniarts.openkeeper.game.network.Transferable;

/**
 * Player's spell (Keeper Spell)
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Transferable(FieldSerializer.class)
public class PlayerSpell extends ResearchableEntity {

    public PlayerSpell() {
    }

    public PlayerSpell(short keeperSpellId) {
        this(keeperSpellId, false);
    }

    public PlayerSpell(short keeperSpellId, boolean discovered) {
        super(keeperSpellId, discovered, ResearchableType.SPELL);
    }

    @Override
    public boolean isUpgradedable() {
        return true;
    }

    @Override
    public String toString() {
        return "PlayerSpell{" + "keeperSpellId=" + id + ", discovered=" + discovered + ", upgraded=" + upgraded + ", research=" + research + '}';
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);

        OutputCapsule out = ex.getCapsule(this);
        out.write(upgraded, "upgraded", false);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);

        InputCapsule in = im.getCapsule(this);
        upgraded = in.readBoolean("upgraded", upgraded);
    }

}
