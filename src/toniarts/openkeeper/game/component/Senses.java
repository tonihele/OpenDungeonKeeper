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

/**
 * Senses, sensory system... Seeing & hearing.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Transferable(FieldSerializer.class)
public class Senses implements EntityComponent {

    public float distanceCanHear;
    public float distanceCanSee;

    public Senses() {
        // For serialization
    }

    public Senses(float distanceCanHear, float distanceCanSee) {
        this.distanceCanHear = distanceCanHear;
        this.distanceCanSee = distanceCanSee;
    }

    public Senses(Senses senses) {
        this.distanceCanHear = senses.distanceCanHear;
        this.distanceCanSee = senses.distanceCanSee;
    }

}
