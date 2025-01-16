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
import toniarts.openkeeper.game.data.PlayerSpell;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.world.ThingLoader;
import toniarts.openkeeper.world.creature.CreatureControl;
import toniarts.openkeeper.world.object.ObjectControl;
import toniarts.openkeeper.world.object.SpellBookObjectControl;
import toniarts.openkeeper.world.room.GenericRoom;

/**
 * Holds out the spell books
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Deprecated
public abstract class RoomSpellBookControl extends RoomObjectControl<SpellBookObjectControl, PlayerSpell> {

    private int storedSpellBooks = 0;

    public RoomSpellBookControl(GenericRoom parent) {
        super(parent);
    }

    @Override
    public int getCurrentCapacity() {
        return storedSpellBooks;
    }

    @Override
    protected int getObjectsPerTile() {
        return 2; // 2 spell books per floor furniture tile
    }

    @Override
    public GenericRoom.ObjectType getObjectType() {
        return GenericRoom.ObjectType.SPELL_BOOK;
    }

    @Override
    public PlayerSpell addItem(PlayerSpell value, Point p, ThingLoader thingLoader, CreatureControl creature) {
        Collection<SpellBookObjectControl> spellBooks = null;
        if (p != null) {
            spellBooks = objectsByCoordinate.get(p);
            if (spellBooks != null && spellBooks.size() == getObjectsPerTile()) {
                return value; // Already max amount of books there
            }
        } else {

            // Find a spot
            Collection<Point> coordinates = getAvailableCoordinates();
            for (Point coordinate : coordinates) {
                p = new Point(coordinate.x, coordinate.y);
                spellBooks = objectsByCoordinate.get(p);
                if (spellBooks == null || spellBooks.size() < getObjectsPerTile()) {
                    break;
                }
            }
        }
        SpellBookObjectControl object = thingLoader.addRoomSpellBook(p, value, creature.getOwnerId());
        if (spellBooks == null) {
            spellBooks = new ArrayList<>(getObjectsPerTile());
        }
        spellBooks.add(object);
        objectsByCoordinate.put(p, spellBooks);
        object.setRoomObjectControl(this);
        storedSpellBooks++;
        return null;
    }

    @Override
    public void destroy() {

        // The keeper has no more access to the spells
        // TODO: how
    }

    @Override
    protected Collection<Point> getCoordinates() {

        // Only floor furniture
        List<Point> coordinates = new ArrayList<>(parent.getFloorFurnitureCount());
        for (ObjectControl oc : parent.getFloorFurniture()) {
            coordinates.add(oc.getObjectCoordinates());
        }
        return coordinates;
    }

}
