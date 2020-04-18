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
package toniarts.openkeeper.game.component;

import com.jme3.network.serializing.serializers.FieldSerializer;
import com.simsilica.es.EntityComponent;
import toniarts.openkeeper.game.network.Transferable;
import toniarts.openkeeper.tools.convert.map.GameObject;

/**
 * Determines that the entity should be viewed as an object. Visual presentation
 * only.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Transferable(FieldSerializer.class)
public class ObjectViewState implements EntityComponent {

    public short objectId;
    public GameObject.State state;
    public GameObjectAnimState animState = GameObjectAnimState.MESH_RESOURCE;
    public boolean visible;

    public ObjectViewState() {
        // For serialization
    }

    public ObjectViewState(short objectId, GameObject.State state, GameObjectAnimState animState, boolean visible) {
        this.objectId = objectId;
        this.state = state;
        this.animState = animState;
        this.visible = visible;
    }

    @Override
    public String toString() {
        return "ObjectViewState{" + "objectId=" + objectId + ", state=" + state + '}';
    }

    /**
     * Game objects have 5 resources, this specifies what we should use
     */
    public enum GameObjectAnimState {
        MESH_RESOURCE,
        ADDITIONAL_RESOURCE_1,
        ADDITIONAL_RESOURCE_2,
        ADDITIONAL_RESOURCE_3,
        ADDITIONAL_RESOURCE_4
    }

}
