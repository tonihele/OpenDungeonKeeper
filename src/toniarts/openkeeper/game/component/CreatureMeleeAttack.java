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
import toniarts.openkeeper.game.network.Transferable;
import toniarts.openkeeper.tools.convert.map.Creature;

/**
 * Creature melee attack
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Transferable(FieldSerializer.class)
public class CreatureMeleeAttack extends Attack {

    public int attackType;
    public int damage;

    public CreatureMeleeAttack() {
        // For serialization
    }

    public CreatureMeleeAttack(int attackType, int damage, float rechargeTime, float range) {
        super(rechargeTime, range);
        this.attackType = attackType;
        this.damage = damage;
    }

    public CreatureMeleeAttack(CreatureMeleeAttack attack, double attackStartTime) {
        super(attack.rechargeTime, attack.range);
        this.attackType = attack.attackType;
        this.damage = attack.damage;
        this.attactStartTime = attackStartTime;
    }

    public CreatureMeleeAttack(CreatureMeleeAttack creatureMeleeAttack) {
        super(creatureMeleeAttack.rechargeTime, creatureMeleeAttack.range);
        this.attackType = creatureMeleeAttack.attackType;
        this.damage = creatureMeleeAttack.damage;
        this.attactStartTime = creatureMeleeAttack.attactStartTime;
    }

    public final Creature.AttackType getAttackType() {

        /**
         * The enum is not final class, so FieldSerializer wont serialize it.
         * Easiest for us now is just to emulate the serialization like this
         */
        return Creature.AttackType.class.getEnumConstants()[attackType];
    }

}
