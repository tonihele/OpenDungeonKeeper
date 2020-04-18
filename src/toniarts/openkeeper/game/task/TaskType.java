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
package toniarts.openkeeper.game.task;

import com.jme3.network.serializing.serializers.EnumSerializer;
import toniarts.openkeeper.game.network.Transferable;

/**
 * Type of task
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Transferable(EnumSerializer.class)
public enum TaskType {

    CLAIM_LAIR,
    GO_TO_SLEEP,
    RESEARCH_SPELL,
    GO_TO_LOCATION,
    KILL_PLAYER,
    CAPTURE_ENEMY_CREATURE,
    CARRY_CREATURE_TO_LAIR,
    CARRY_CREATURE_TO_JAIL,
    CARRY_GOLD_TO_TREASURY,
    CLAIM_ROOM,
    CLAIM_TILE,
    CLAIM_WALL,
    DIG_TILE,
    FETCH_OBJECT,
    REPAIR_WALL,
    RESCUE_CREATURE,
    GO_TO_EAT
}
