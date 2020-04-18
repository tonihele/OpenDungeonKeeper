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

import com.jme3.network.serializing.serializers.EnumSerializer;
import toniarts.openkeeper.game.network.Transferable;

/**
 * Type of researchable entity
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Transferable(EnumSerializer.class)
public enum ResearchableType {

    ROOM, SPELL, DOOR, TRAP;

}
