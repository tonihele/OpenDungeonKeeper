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
package toniarts.openkeeper.game.controller;

import com.jme3.math.Vector2f;
import toniarts.openkeeper.view.selection.SelectionArea;

/**
 * Holds together all game related player actions
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IPlayerActions {

    /**
     * Build a building to the wanted area
     *
     * @param area
     * @param playerId the player, the new owner
     * @param roomId room to build
     */
    public void build(SelectionArea area, short playerId, short roomId);

    /**
     * Sell building(s) from the wanted area
     *
     * @param area
     * @param playerId the player, the seller
     */
    public void sell(SelectionArea area, short playerId);

    /**
     * Set some tiles selected/undelected
     *
     * @param area
     * @param playerId the player who selected the tile
     */
    public void selectTiles(SelectionArea area, short playerId);

}
