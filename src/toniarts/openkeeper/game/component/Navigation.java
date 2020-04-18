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
package toniarts.openkeeper.game.component;

import com.badlogic.gdx.math.Vector2;
import com.jme3.network.serializing.serializers.FieldSerializer;
import com.simsilica.es.EntityComponent;
import java.awt.Point;
import java.util.List;
import toniarts.openkeeper.game.network.Transferable;

/**
 * An entity class marking... well.. target of navigation with full path
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Transferable(FieldSerializer.class)
public class Navigation implements EntityComponent {

    public Point target;
    public Point faceTarget;
    public List<Vector2> navigationPath;

    public Navigation() {
        // For serialization
    }

    public Navigation(Point target, Point faceTarget, List<Vector2> navigationPath) {
        this.target = target;
        this.faceTarget = faceTarget;
        this.navigationPath = navigationPath;
    }

}
