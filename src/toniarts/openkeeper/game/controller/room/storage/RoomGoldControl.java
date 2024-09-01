/*
 * Copyright (C) 2014-2016 OpenKeeper
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
package toniarts.openkeeper.game.controller.room.storage;

import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.KwdFile;

/**
 * Not really a JME control currently. Manages how the gold places in the room.
 * Should be generalized to provide a control for any type of object owned by
 * the room. Either these controls or decorator pattern, lets see...
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class RoomGoldControl extends AbstractRoomObjectControl<Integer> {

    private int storedGold = 0;

    public RoomGoldControl(KwdFile kwdFile, IRoomController parent, IObjectsController objectsController, IGameTimer gameTimer) {
        super(kwdFile, parent, objectsController, gameTimer);
    }

    @Override
    public Integer addItem(Integer sum, Point p) {
        if (p != null && parent.isTileAccessible(null, p)) {
            sum = putGold(sum, p);
        }

        // Distribute gold evenly
        if (sum > 0) {
            List<Point> coordinates = parent.getRoomInstance().getCoordinates();
            for (Point coordinate : coordinates) {
                if (parent.isTileAccessible(null, coordinate)) {
                    sum = putGold(sum, coordinate);
                    if (sum == 0) {
                        break;
                    }
                }
            }
        }
        return sum;
    }

    private int putGold(int sum, Point p) {
        int pointStoredGold = 0;
        Collection<EntityId> goldPiles = objectsByCoordinate.get(p);
        Gold goldPile = null;
        if (goldPiles != null && !goldPiles.isEmpty()) {
            goldPile = objectsController.getEntityData().getComponent(goldPiles.iterator().next(), Gold.class);
            pointStoredGold = goldPile.gold;
        }
        if (pointStoredGold < getGoldPerObject()) {
            int goldToStore = Math.min(sum, getGoldPerObject() - pointStoredGold);
            pointStoredGold += goldToStore;
            sum -= goldToStore;
            storedGold += goldToStore;

            // Add the visuals
            if (goldPile == null) {
                EntityId entityId = objectsController.addRoomGold(parent.getRoomInstance().getOwnerId(), p.x, p.y, pointStoredGold, getGoldPerObject());
                if (goldPiles == null) {
                    goldPiles = new ArrayList<>(1);
                }
                if (entityId != null) {
                    goldPiles.add(entityId);
                    objectsByCoordinate.put(p, goldPiles);
                    setRoomStorageToItem(entityId, true);
                }
            } else {

                // Adjust the gold sum
                goldPile.gold = pointStoredGold;
            }

            // Add gold to player
            //parent.getWorldState().getGameState().getPlayer(parent.getRoomInstance().getOwnerId()).getGoldControl().addGold(goldToStore);
        }
        return sum;
    }

    @Override
    public int getCurrentCapacity() {
        return storedGold;
    }

    @Override
    public ObjectType getObjectType() {
        return ObjectType.GOLD;
    }

    @Override
    public void destroy() {

        // You can do this easier by deleting room gold at the end, but for the sake
        // of OCD I don't want to spawn in the new gold even logically before the old is deleted
        // Get the old gold
        Map<Point, Integer> storedGoldList = new HashMap<>(objectsByCoordinate.size());
        for (Entry<Point, Collection<EntityId>> entry : objectsByCoordinate.entrySet()) {
            for (EntityId entityId : entry.getValue()) {
                storedGoldList.put(entry.getKey(), objectsController.getEntityData().getComponent(entityId, Gold.class).gold);
            }
        }

        // Delete all gold
        removeAllObjects();

        // Create the loose gold
        if (!storedGoldList.isEmpty()) {
            for (Entry<Point, Integer> entry : storedGoldList.entrySet()) {
                objectsController.addLooseGold(parent.getRoomInstance().getOwnerId(), entry.getKey().x, entry.getKey().y, entry.getValue(), getGoldPerObject());
            }
        }
    }

    @Override
    public void removeItem(EntityId object) {
        super.removeItem(object);

        // Substract the gold from the player
        //parent.getWorldState().getGameState().getPlayer(parent.getRoomInstance().getOwnerId()).getGoldControl().subGold(object.getGold());
        Gold goldPile = objectsController.getEntityData().getComponent(object, Gold.class);
        storedGold -= goldPile.gold;
        if (goldPile.gold == 0) {
            objectsController.getEntityData().removeEntity(object);
        }
    }

    /**
     * Remove amount of gold from this room
     *
     * @param amount the amount
     * @return the amount that can't be removed
     */
    public int removeGold(int amount) {
        List<EntityId> objectsToRemove = new ArrayList<>();
        for (Collection<EntityId> goldPiles : objectsByCoordinate.values()) {
            if (!goldPiles.isEmpty()) {
                EntityId goldEntity = goldPiles.iterator().next();
                Gold goldPile = objectsController.getEntityData().getComponent(goldPiles.iterator().next(), Gold.class);
                int goldToRemove = Math.min(goldPile.gold, amount);
                amount -= goldToRemove;
                goldPile.gold = goldPile.gold - goldToRemove;

                // Substract the gold from the player
                //parent.getWorldState().getGameState().getPlayer(parent.getRoomInstance().getOwnerId()).getGoldControl().subGold(goldToRemove);
                storedGold -= goldToRemove;

                // Add to removal list if empty item
                if (goldPile.gold == 0) {
                    objectsToRemove.add(goldEntity);
                }

                if (amount == 0) {
                    break;
                }
            }
        }

        // Clean up, the amount of gold is already 0, so
        for (EntityId entityId : objectsToRemove) {
            removeItem(entityId);
        }

        return amount;
    }

    @Override
    public int getMaxCapacity() {
        return getObjectsPerTile() * getNumberOfAccessibleTiles() * getGoldPerObject();
    }

    protected abstract int getGoldPerObject();

    @Override
    protected int getObjectsPerTile() {
        return 1;
    }

    @Override
    public Collection<Point> getAvailableCoordinates() {
        return getCoordinates(); // Everything goes
    }

}
