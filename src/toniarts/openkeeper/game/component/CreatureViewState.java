/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.game.component;

import com.jme3.network.serializing.serializers.FieldSerializer;
import com.simsilica.es.EntityComponent;
import toniarts.openkeeper.game.network.Transferable;
import toniarts.openkeeper.tools.convert.map.Creature.AnimationType;

/**
 * Determines that the entity should be viewed as an creature. Visual
 * presentation only.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Transferable(FieldSerializer.class)
public class CreatureViewState implements EntityComponent {

    public short creatureId;
    public double stateStartTime;
    public AnimationType state; // TODO: Proper enum, not this

    public CreatureViewState() {
        // For serialization
    }

    public CreatureViewState(short creatureId, double stateStartTime, AnimationType state) {
        this.creatureId = creatureId;
        this.stateStartTime = stateStartTime;
        this.state = state;
    }

}
