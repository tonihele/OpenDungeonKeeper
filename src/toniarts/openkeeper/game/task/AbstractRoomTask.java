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
package toniarts.openkeeper.game.task;

import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.navigation.INavigationService;

/**
 * A base of a task that involves a room
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class AbstractRoomTask extends AbstractTileTask {

    protected final IRoomController room;

    public AbstractRoomTask(final INavigationService navigationService, final IMapController mapController, Point p, short playerId, IRoomController room) {
        super(navigationService, mapController, p, playerId);

        this.room = room;
    }

    protected IRoomController getRoom() {
        return room;
    }

    @Override
    public boolean isValid(ICreatureController creature) {

        // See that the room exists and has capacity etc.
        return room.getRoomInstance().getOwnerId() == playerId && !room.isDestroyed() && !getRoomObjectControl().isFullCapacity();
    }

    protected abstract ObjectType getRoomObjectType();

    protected IRoomObjectControl getRoomObjectControl() {
        return getRoom().getObjectControl(getRoomObjectType());
    }

}
