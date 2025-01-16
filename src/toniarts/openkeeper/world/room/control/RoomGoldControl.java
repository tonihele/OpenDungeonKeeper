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
package toniarts.openkeeper.world.room.control;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.world.ThingLoader;
import toniarts.openkeeper.world.creature.CreatureControl;
import toniarts.openkeeper.world.object.GoldObjectControl;
import toniarts.openkeeper.world.room.GenericRoom;

/**
 * Not really a JME control currently. Manages how the gold places in the room.
 * Should be generalized to provide a control for any type of object owned by
 * the room. Either these controls or decorator pattern, lets see...
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Deprecated
public abstract class RoomGoldControl extends RoomObjectControl<GoldObjectControl, Integer> {

    private int storedGold = 0;

    public RoomGoldControl(GenericRoom parent) {
        super(parent);
    }

    @Override
    public Integer addItem(Integer sum, Point p, ThingLoader thingLoader, CreatureControl creature) {
        if (p != null) {
            sum = putGold(sum, p, thingLoader);
        }
        if (sum > 0) {
            List<Point> coordinates = parent.getRoomInstance().getCoordinates();
            for (Point coordinate : coordinates) {
                if (parent.isTileAccessible(null, null, coordinate.x, coordinate.y)) {
                    sum = putGold(sum, coordinate, thingLoader);
                    if (sum == 0) {
                        break;
                    }
                }
            }
        }
        return sum;
    }

    private int putGold(int sum, Point p, ThingLoader thingLoader) {
        int pointStoredGold = 0;
        Collection<GoldObjectControl> goldPiles = objectsByCoordinate.get(p);
        GoldObjectControl goldPile = null;
        if (goldPiles != null && !goldPiles.isEmpty()) {
            goldPile = goldPiles.iterator().next();
            pointStoredGold = goldPile.getGold();
        }
        if (pointStoredGold < getGoldPerObject()) {
            int goldToStore = Math.min(sum, getGoldPerObject() - pointStoredGold);
            pointStoredGold += goldToStore;
            sum -= goldToStore;
            storedGold += goldToStore;

            // Add the visuals
            if (goldPile == null) {
                GoldObjectControl object = thingLoader.addRoomGold(p,
                        parent.getRoomInstance().getOwnerId(), goldToStore, getGoldPerObject());
                if (goldPiles == null) {
                    goldPiles = new ArrayList<>(1);
                }
                if (object != null) {
                    goldPiles.add(object);
                    objectsByCoordinate.put(p, goldPiles);
                    object.setRoomObjectControl(this);
                }
            } else {

                // Adjust the gold sum
                goldPile.setGold(pointStoredGold);
            }

            // Add gold to player
//            parent.getWorldState().getGameState().getPlayer(parent.getRoomInstance().getOwnerId()).getGoldControl().addGold(goldToStore);
        }
        return sum;
    }

    @Override
    public int getCurrentCapacity() {
        return storedGold;
    }

    @Override
    public GenericRoom.ObjectType getObjectType() {
        return GenericRoom.ObjectType.GOLD;
    }

    @Override
    public void destroy() {
        List<Entry<Point, Collection<GoldObjectControl>>> storedGoldList = new ArrayList<>(objectsByCoordinate.entrySet());

        // Delete all gold
        removeAllObjects();

        // Create the loose gold
        if (!storedGoldList.isEmpty()) {
            ThingLoader thingLoader = parent.getWorldState().getThingLoader();
            for (Entry<Point, Collection<GoldObjectControl>> entry : storedGoldList) {
                thingLoader.addLooseGold(entry.getKey(), parent.getRoomInstance().getOwnerId(),
                        entry.getValue().iterator().next().getGold());
            }
        }
    }

    @Override
    public void removeItem(GoldObjectControl object) {
        super.removeItem(object);

        // Substract the gold from the player
//        parent.getWorldState().getGameState().getPlayer(parent.getRoomInstance().getOwnerId()).getGoldControl().subGold(object.getGold());
        storedGold -= object.getGold();
        if (object.getGold() == 0) {
            object.removeObject();
        }
    }

    /**
     * Remove amount of gold from this room
     *
     * @param amount the amount
     * @return the amount that can't be removed
     */
    public int removeGold(int amount) {
        List<GoldObjectControl> objectsToRemove = new ArrayList<>();
        for (Collection<GoldObjectControl> goldObjectControls : objectsByCoordinate.values()) {
            if (!goldObjectControls.isEmpty()) {
                GoldObjectControl goldObjectControl = goldObjectControls.iterator().next();
                int goldToRemove = Math.min(goldObjectControl.getGold(), amount);
                amount -= goldToRemove;
                goldObjectControl.setGold(goldObjectControl.getGold() - goldToRemove);

                // Substract the gold from the player
//                parent.getWorldState().getGameState().getPlayer(parent.getRoomInstance().getOwnerId()).getGoldControl().subGold(goldToRemove);
                storedGold -= goldToRemove;

                // Add to removal list if empty item
                if (goldObjectControl.getGold() == 0) {
                    objectsToRemove.add(goldObjectControl);
                }

                if (amount == 0) {
                    break;
                }
            }
        }

        // Clean up, the amount of gold is already 0, so
        for (GoldObjectControl goldObjectControl : objectsToRemove) {
            removeItem(goldObjectControl);
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
