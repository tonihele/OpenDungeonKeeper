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
package toniarts.openkeeper.game.component;

import com.jme3.network.serializing.serializers.FieldSerializer;
import com.simsilica.es.EntityComponent;
import toniarts.openkeeper.game.network.Transferable;

/**
 * Creature experience level stuff
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Transferable(FieldSerializer.class)
public class CreatureExperience implements EntityComponent {

    public int level;
    public int experience;
    public int experienceToNextLevel;
    public int experiencePerSecond;
    public int experiencePerSecondTraining;

    public CreatureExperience() {
        // For serialization
    }

    public CreatureExperience(int level, int experience, int experienceToNextLevel, int experiencePerSecond, int experiencePerSecondTraining) {
        this.level = level;
        this.experience = experience;
        this.experienceToNextLevel = experienceToNextLevel;
        this.experiencePerSecond = experiencePerSecond;
        this.experiencePerSecondTraining = experiencePerSecondTraining;
    }

    public CreatureExperience(CreatureExperience creatureExperience) {
        this.level = creatureExperience.level;
        this.experience = creatureExperience.experience;
        this.experienceToNextLevel = creatureExperience.experienceToNextLevel;
        this.experiencePerSecond = creatureExperience.experiencePerSecond;
        this.experiencePerSecondTraining = creatureExperience.experiencePerSecondTraining;
    }

}
