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
 * Simple creature component
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Transferable(FieldSerializer.class)
public class CreatureComponent implements EntityComponent {

    public String name;
    public String bloodType;
    public float height;
    public int fear;
    public int pay;
    public int maxGoldHeld;
    public int hungerFill;
    public int manaGenPrayer;
    public int researchPerSecond; // TODO: Researcher component?
    public int manufacturePerSecond; // TODO: Manufacturer component?
    public int decomposeValue;
    public float speed;
    public float runSpeed;
    public float tortureTimeToConvert;
    public int posessionManaCost;
    public float stunDuration;
    public boolean worker;
    public short creatureId;

    public CreatureComponent() {

    }

    public CreatureComponent(CreatureComponent creatureComponent) {
        this.name = creatureComponent.name;
        this.bloodType = creatureComponent.bloodType;
        this.height = creatureComponent.height;
        this.fear = creatureComponent.fear;
        this.pay = creatureComponent.pay;
        this.maxGoldHeld = creatureComponent.maxGoldHeld;
        this.hungerFill = creatureComponent.hungerFill;
        this.manaGenPrayer = creatureComponent.manaGenPrayer;
        this.researchPerSecond = creatureComponent.researchPerSecond;
        this.manufacturePerSecond = creatureComponent.manufacturePerSecond;
        this.decomposeValue = creatureComponent.decomposeValue;
        this.speed = creatureComponent.speed;
        this.runSpeed = creatureComponent.runSpeed;
        this.tortureTimeToConvert = creatureComponent.tortureTimeToConvert;
        this.posessionManaCost = creatureComponent.posessionManaCost;
        this.stunDuration = creatureComponent.stunDuration;
        this.worker = creatureComponent.worker;
        this.creatureId = creatureComponent.creatureId;
    }

}
