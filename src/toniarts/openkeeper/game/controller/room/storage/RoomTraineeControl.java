/*
 * Copyright (C) 2014-2023 OpenKeeper
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
import java.util.List;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Holds out the trainees populating a room
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class RoomTraineeControl extends AbstractRoomObjectControl<EntityId> {

    protected RoomTraineeControl(KwdFile kwdFile, IRoomController parent, IObjectsController objectsController, IGameTimer gameTimer) {
        super(kwdFile, parent, objectsController, gameTimer);
    }

    @Override
    public int getCurrentCapacity() {
        return objectsByCoordinate.size();
    }

    @Override
    protected int getObjectsPerTile() {
        return 1;
    }

    @Override
    public ObjectType getObjectType() {
        return ObjectType.TRAINEE;
    }

    @Override
    public EntityId addItem(EntityId trainee, Point p) {
        setRoomStorageToItem(trainee, false);

        return trainee;
    }

    @Override
    public void destroy() {

        // TODO: The trainee can't do his/her job
    }

    @Override
    public void captured(short playerId) {

    }

    @Override
    protected Collection<Point> getCoordinates() {

        // Only furniture
        List<Point> coordinates = new ArrayList<>(parent.getFloorFurnitureCount() + parent.getWallFurnitureCount());
        for (EntityId oc : parent.getFloorFurniture()) {
            coordinates.add(WorldUtils.vectorToPoint(objectsController.getEntityData().getComponent(oc, Position.class).position));
        }
        for (EntityId oc : parent.getWallFurniture()) {
            coordinates.add(WorldUtils.vectorToPoint(objectsController.getEntityData().getComponent(oc, Position.class).position));
        }

        return coordinates;
    }

}
