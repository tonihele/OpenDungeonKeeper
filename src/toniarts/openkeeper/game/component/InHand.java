/*
 * Copyright (C) 2014-2018 OpenKeeper
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

/**
 * Tags an entity to be on keeper's hand
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Transferable(FieldSerializer.class)
public class InHand implements EntityComponent {

    public int index;
    /**
     * The player whose hand this is, lets imagine you could pick up someone
     * else's stuff
     */
    public short playerId;
    public ViewType type;
    /**
     * Of the record type
     */
    public short id;

    public InHand() {
        // For serialization
    }

    public InHand(int index, short playerId, ViewType type, short id) {
        this.index = index;
        this.playerId = playerId;
        this.type = type;
        this.id = id;
    }

}
