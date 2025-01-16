/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.view.map;

import com.jme3.math.FastMath;
import toniarts.openkeeper.utils.Point;
import java.util.List;

/**
 * Single wall section
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public record WallSection(toniarts.openkeeper.view.map.WallSection.WallDirection direction, List<Point> coordinates) {

    public enum WallDirection {

        EAST(-FastMath.HALF_PI), NORTH(0), WEST(FastMath.HALF_PI), SOUTH(FastMath.PI);

        WallDirection(float angle) {
            this.angle = angle;
        }

        public float getAngle() {
            return angle;
        }

        private final float angle;
    }

}
