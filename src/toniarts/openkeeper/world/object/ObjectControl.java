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
package toniarts.openkeeper.world.object;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.gui.CursorFactory;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.world.TileData;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.control.IInteractiveControl;
import toniarts.openkeeper.world.creature.CreatureControl;
import toniarts.openkeeper.world.room.control.RoomObjectControl;

/**
 * Control for object
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Deprecated
public class ObjectControl extends HighlightControl implements IInteractiveControl {

    public enum ObjectState {

        NORMAL, PICKED_UP, OWNED_BY_CREATURE, STORED_IN_ROOM, REMOVED;
    }

    protected final WorldState worldState;
    protected final GameObject object;
    private final String tooltip;
    protected TileData tile;
    private ObjectState state = ObjectState.NORMAL;

    // Owners
    protected RoomObjectControl roomObjectControl;
    private CreatureControl creature;
    protected short pickedUpBy;

    public ObjectControl(TileData tile, GameObject object, WorldState worldState) {
        super();

        this.worldState = worldState;
        this.object = object;
        this.tile = tile;

        // Strings
        tooltip = Utils.getMainTextResourceBundle().getString(Integer.toString(object.getTooltipStringId()));
    }

    @Override
    protected void controlUpdate(float tpf) {

    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

    }

    @Override
    public String getTooltip(short playerId) {
        return tooltip;
    }

    /**
     * Remove object
     */
    public void removeObject() {

        // Physically from the view
        Spatial us = getSpatial();
        us.removeFromParent();

        // From the things registry
        worldState.getThingLoader().onObjectRemoved(this);

        // If we belong to a creature, remove us
        if (creature != null) {
            creature.removeObject(this);
        }

        // If we belong to a room, remove us
        if (roomObjectControl != null) {
            roomObjectControl.removeItem(this);
        }
        state = ObjectState.REMOVED;
    }

    public void setCreature(CreatureControl creature) {
        this.creature = creature;
        if (creature != null) {
            setState(ObjectState.OWNED_BY_CREATURE);
        }
    }

    public void setRoomObjectControl(RoomObjectControl roomObjectControl) {
        this.roomObjectControl = roomObjectControl;
        if (roomObjectControl != null) {
            setState(ObjectState.STORED_IN_ROOM);
        }
    }

    protected ArtResource getResource() {
        if (isAdditionalResources()) {
            int resourceIndex = FastMath.nextRandomInt(0, getResourceCount() - 1);
            if (resourceIndex == object.getAdditionalResources().size()) {
                return object.getMeshResource();
            }
            return object.getAdditionalResources().get(resourceIndex);
        }
        return object.getMeshResource();
    }

    public int getOrientation() {
        if (object.getMaxAngle() == 0) {
            return 0;
        }

        // Take a random angle
        return FastMath.nextRandomInt(0, object.getMaxAngle());
    }

    protected boolean isAdditionalResources() {
        return !object.getAdditionalResources().isEmpty();
    }

    protected int getResourceCount() {
        return object.getAdditionalResources().size() + 1;
    }

    @Override
    public boolean isPickable(short playerId) {
        return object.getFlags().contains(GameObject.ObjectFlag.CAN_BE_PICKED_UP);
    }

    @Override
    public boolean isInteractable(short playerId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public IInteractiveControl pickUp(short playerId) {
        setEnabled(false);
        pickedUpBy = playerId;
        setState(ObjectState.PICKED_UP);

        // If we are a part of room, we need to detach
        if (roomObjectControl != null) {
            roomObjectControl.removeItem(this);
        } else {

            // We are part of the world
            worldState.getThingLoader().onObjectRemoved(this);
        }

        // Remove from view
        getSpatial().removeFromParent();

        return this;
    }

    @Override
    public boolean interact(short playerId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onHover() {

    }

    @Override
    public void onHoverStart() {
        super.onHoverStart();
    }

    @Override
    public void onHoverEnd() {
        super.onHoverEnd();
    }

    @Override
    public CursorFactory.CursorType getInHandCursor() {
        return CursorFactory.CursorType.HOLD_THING;
    }

    @Override
    public ArtResource getInHandMesh() {
        return object.getInHandMeshResource();
    }

    @Override
    public ArtResource getInHandIcon() {
        return object.getGuiIconResource();
    }

    @Override
    public DroppableStatus getDroppableStatus(TileData tile, short playerId) {
        return !tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.SOLID)
                && (object.getFlags().contains(GameObject.ObjectFlag.CAN_BE_DROPPED_ON_ANY_LAND)
                || ((tile.getPlayerId() == getOwnerId()
                && tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.OWNABLE))))
                ? DroppableStatus.DROPPABLE : DroppableStatus.NOT_DROPPABLE;
    }

    @Override
    public void drop(TileData tile, Vector2f coordinates, IInteractiveControl control) {
        if (control != null && control instanceof CreatureControl && control.getOwnerId() == getOwnerId()) {
            if (((CreatureControl) control).giveObject(this)) {
                return;
            }
        }

        // Drop the item to the world state
        this.tile = tile;
        state = ObjectState.NORMAL;
        worldState.dropObject(this, tile, coordinates, control);
    }

    @Override
    public short getOwnerId() {
        switch (state) {
            case STORED_IN_ROOM:
            case NORMAL: {
                return tile.getPlayerId();
            }
            case OWNED_BY_CREATURE: {
                return creature.getOwnerId();
            }
            case PICKED_UP: {
                return pickedUpBy;
            }
        }
        return 0;
    }

    /**
     * Get the object coordinates, in tile coordinates
     *
     * @return the tile coordinates
     */
    public Point getObjectCoordinates() {
        return WorldUtils.vectorToPoint(getSpatial().getWorldTranslation());
    }

    public ObjectState getState() {
        return state;
    }

    protected void setState(ObjectState state) {
        this.state = state;
    }

    public TileData getTile() {
        return tile;
    }

    public GameObject getObject() {
        return object;
    }

    public boolean isPickableByPlayerCreature(short playerId) {
        return state == ObjectState.NORMAL && getTile().getPlayerId() == playerId;
    }

    public void creaturePicksUp(CreatureControl creature) {
        state = ObjectState.OWNED_BY_CREATURE;
        this.creature = creature;
        roomObjectControl = null;

        // Attach the object to the creature
        creature.giveObject(this);
    }

}
