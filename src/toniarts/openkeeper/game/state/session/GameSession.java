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

import com.jme3.math.Vector2f;
import com.jme3.network.service.rmi.Asynchronous;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.game.state.CheatState;

/**
 * Clients view on game service
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface GameSession {

    /**
     * Get the game entity data
     *
     * @return entity data
     */
    public EntityData getEntityData();

    /**
     * Signal that we are ready and loaded up
     */
    @Asynchronous
    public void loadComplete();

    /**
     * Our game loading status update
     *
     * @param progress our current progress
     */
    @Asynchronous
    public void loadStatus(float progress);

    /**
     * Mark us ready to start receiving game updates
     */
    @Asynchronous
    public void markReady();

    /**
     * Build a building to the wanted area
     *
     * @param start start coordinates
     * @param end end coordinates
     * @param roomId room to build
     */
    @Asynchronous
    public void build(Vector2f start, Vector2f end, short roomId);

    /**
     * Sell building(s) from the wanted area
     *
     * @param start start coordinates
     * @param end end coordinates
     */
    @Asynchronous
    public void sell(Vector2f start, Vector2f end);

    /**
     * Set some tiles selected/undelected
     *
     * @param start start coordinates
     * @param end end coordinates
     * @param select select or unselect
     */
    @Asynchronous
    public void selectTiles(Vector2f start, Vector2f end, boolean select);

    /**
     * Interact with given entity
     *
     * @param entity the entity
     */
    @Asynchronous
    public void interact(EntityId entity);

    /**
     * Pick up given entity
     *
     * @param entity the entity
     */
    @Asynchronous
    public void pickUp(EntityId entity);

    /**
     * Drop the entity on a tile
     *
     * @param entity the entity to drop
     * @param tile tile to drop to
     * @param coordinates real world coordinates inside
     * @param dropOnEntity if there is already an entity at the position
     */
    @Asynchronous
    public void drop(EntityId entity, Point tile, Vector2f coordinates, EntityId dropOnEntity);

    /**
     * Get gold... Rather instantly pick up a lump sum of gold deducted from
     * your account
     *
     * @param amount amount of gold to get
     */
    @Asynchronous
    public void getGold(int amount);

    /**
     * Signals that any UI transition has ended
     */
    @Asynchronous
    public void transitionEnd();

    /**
     * Request for pausing the game
     */
    @Asynchronous
    public void pauseGame();

    /**
     * Request for resuming the game
     */
    @Asynchronous
    public void resumeGame();

    /**
     * We quit!
     */
    public void exitGame();

    /**
     * Trigger a cheat
     *
     * @param cheat the cheat to trigger
     */
    @Asynchronous
    public void triggerCheat(CheatState.CheatType cheat);

    /**
     * Cast a keeper spell
     *
     * @param keeperSpellId the spell to cast
     * @param target entity to cast the spell on (can be null)
     * @param tile the tile to cast on
     * @param position the actual position of the cast
     */
    @Asynchronous
    public void castKeeperSpell(short keeperSpellId, EntityId target, Point tile, Vector2f position);

    /**
     * Place a door
     *
     * @param doorId the door ID to place
     * @param tile the tile to place door on
     */
    @Asynchronous
    public void placeDoor(short doorId, Point tile);

    /**
     * Place a trap
     *
     * @param trapId the trap ID to place
     * @param tile the tile to place trap on
     */
    @Asynchronous
    public void placeTrap(short trapId, Point tile);

}
