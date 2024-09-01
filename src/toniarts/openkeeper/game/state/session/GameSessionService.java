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
package toniarts.openkeeper.game.state.session;

import com.jme3.network.service.rmi.Asynchronous;
import com.simsilica.es.EntityData;
import toniarts.openkeeper.utils.Point;
import java.util.Collection;
import java.util.List;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.listener.PlayerListener;

/**
 * This is server's perspective of game flow things. The services we offer our
 * clients.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface GameSessionService extends PlayerListener, PlayerService {

    /**
     * Get the entity data
     *
     * @return the entity data
     */
    public EntityData getEntityData();

    /**
     * Sends the game data to the clients to allow them to load it up visually
     *
     * @param players the players
     */
    @Asynchronous
    public void sendGameData(Collection<Keeper> players);

    /**
     * Signals that the game should start
     */
    public void startGame();

    /**
     * Signals that map tiles have been changed
     *
     * @param updatedTiles the changed tiles
     */
    @Asynchronous
    public void updateTiles(List<Point> updatedTiles);

    /**
     * Map tiles should be set flashing
     *
     * @param points   the points that should be set flashing
     * @param enabled  flashing on or off
     * @param keeperId the keeper whose tiles are involved
     */
    @Asynchronous
    public void flashTiles(List<Point> points, boolean enabled, short keeperId);

}
