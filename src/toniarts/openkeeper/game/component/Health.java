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
 * Simple health component, essentially without this, you are dead
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Transferable(FieldSerializer.class)
public class Health implements EntityComponent {

    public int ownLandHealthIncrease;
    public int health;
    public int maxHealth;
    public boolean unconscious;

    public Health() {
        // For serialization
    }

    public Health(int ownLandHealthIncrease, int health, int maxHealth, boolean unconscious) {
        this.ownLandHealthIncrease = ownLandHealthIncrease;
        this.health = health;
        this.maxHealth = maxHealth;
        this.unconscious = unconscious;
    }

    public Health(Health health) {
        this.ownLandHealthIncrease = health.ownLandHealthIncrease;
        this.health = health.health;
        this.maxHealth = health.maxHealth;
        this.unconscious = health.unconscious;
    }

}
