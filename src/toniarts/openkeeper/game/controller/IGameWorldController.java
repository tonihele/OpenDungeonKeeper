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
package toniarts.openkeeper.game.controller;

import com.jme3.math.Vector2f;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.game.listener.PlayerActionListener;

/**
 * Controls the game world, map and the entities
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IGameWorldController {

    /**
     * Add a lump sum of gold to a player, distributes the gold to the available
     * rooms
     *
     * @param playerId for the player
     * @param sum the gold sum
     * @return returns a sum of gold that could not be added to player's gold
     */
    int addGold(short playerId, int sum);

    /**
     * Add a lump sum of gold to a player, distributes the gold to the available
     * rooms
     *
     * @param playerId for the player
     * @param p a point where to drop the gold, can be {@code  null}
     * @param sum the gold sum
     * @return returns a sum of gold that could not be added to player's gold
     */
    int addGold(short playerId, Point p, int sum);

    //    public Collection<IPlayerController> getPlayerControllers() {
    //        return playerControllers.values();
    //    }
    //
    //    public Collection<Keeper> getPlayers() {
    //        return players.values();
    //    }
    /**
     * If you want to get notified about player actiosns
     *
     * @param listener the listener
     */
    void addListener(PlayerActionListener listener);

    /**
     * Stop listening to player actions
     *
     * @param listener the listener
     */
    void removeListener(PlayerActionListener listener);

    /**
     * Substract gold from player
     *
     * @param amount the amount to try to substract
     * @param playerId the player id
     * @return amount of money that could not be substracted from the player
     */
    int substractGold(int amount, short playerId);

    /**
     * Get a map controller
     *
     * @return map controller
     */
    public IMapController getMapController();

    /**
     * Build a building to the wanted area
     *
     * @param start start location
     * @param end end location
     * @param playerId the player who is building the room
     * @param roomId the room ID to be build
     */
    public void build(Vector2f start, Vector2f end, short playerId, short roomId);

    /**
     * Sell a building from wanted area
     *
     * @param start start location
     * @param end end location
     * @param playerId the player who is selling the room
     */
    public void sell(Vector2f start, Vector2f end, short playerId);

    /**
     * Interact with given entity
     *
     * @param entity the entity to interact with
     * @param playerId the player who interacts
     */
    public void interact(EntityId entity, short playerId);

    /**
     * Pick up the given entity
     *
     * @param entity the entity to pick up
     * @param playerId the player who picks up
     */
    public void pickUp(EntityId entity, short playerId);

    /**
     * Drop the entity on a tile
     *
     * @param entity the entity to drop
     * @param tile tile to drop to
     * @param coordinates real world coordinates inside
     * @param dropOnEntity if there is already an entity at the position
     * @param playerId the player dropping this entity
     */
    public void drop(EntityId entity, Point tile, Vector2f coordinates, EntityId dropOnEntity, short playerId);

    /**
     * Get gold, substract the amount of gold and places it to the keeper's hand
     *
     * @param amount amount of gold to get
     * @param playerId the player who is getting the gold
     */
    public void getGold(int amount, short playerId);

    /**
     * Get the creatures controller
     *
     * @return creatures controller
     */
    public ICreaturesController getCreaturesController();

    /**
     * Get the objects controller
     *
     * @return objects controller
     */
    public IObjectsController getObjectsController();

    /**
     * Cast a keeper spell on target / location
     *
     * @param keeperSpellId the spell casted
     * @param target the target casted on (can be null)
     * @param tile the tile casted on
     * @param position the actual coordinates of the cast
     * @param playerId the player who is casting
     */
    public void castKeeperSpell(short keeperSpellId, EntityId target, Point tile, Vector2f position, short playerId);

    /**
     * Place a door (blueprint.. manufacturing order)
     *
     * @param doorId the door to place
     * @param tile the tile to place the door to
     * @param playerId the player placing the door
     */
    public void placeDoor(short doorId, Point tile, short playerId);

    /**
     * Place a trap (blueprint.. manufacturing order)
     *
     * @param trapId the door to place
     * @param tile the tile to place the trap to
     * @param playerId the player placing the trap
     */
    public void placeTrap(short trapId, Point tile, short playerId);
}
