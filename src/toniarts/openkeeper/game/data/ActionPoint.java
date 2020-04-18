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
package toniarts.openkeeper.game.data;

import com.jme3.math.Vector2f;
import java.awt.Point;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import toniarts.openkeeper.game.control.Container;
import toniarts.openkeeper.tools.convert.map.Thing;

/**
 * Represents an action point, areas of intrests in a map
 *
 * @author ArchDemon
 */
public class ActionPoint extends Container implements ITriggerable, Iterable<Point> {

    private final int id;
    private final int triggerId;
    private final Point start;
    private final Point end;
    private final EnumSet<Thing.ActionPoint.ActionPointFlag> flags;
    private final int waitDelay;
    private final int nextWaypointId;
    private final Vector2f center;
    private List<Point> points;

    public ActionPoint(Thing.ActionPoint acionPoint) {
        id = acionPoint.getId();
        start = new Point(acionPoint.getStartX(), acionPoint.getStartY());
        end = new Point(acionPoint.getEndX(), acionPoint.getEndY());
        waitDelay = acionPoint.getWaitDelay();
        nextWaypointId = acionPoint.getNextWaypointId();
        flags = acionPoint.getFlags();
        triggerId = acionPoint.getTriggerId();
        center = new Vector2f((start.x + end.x) / 2, (start.y + end.y) / 2);
    }

    public int getId() {
        return id;
    }

    @Override
    public int getTriggerId() {
        return triggerId;
    }

    public Point getStart() {
        return start;
    }

    public Point getEnd() {
        return end;
    }

    public EnumSet<Thing.ActionPoint.ActionPointFlag> getFlags() {
        return flags;
    }

    public int getWaitDelay() {
        return waitDelay;
    }

    public int getNextWaypointId() {
        return nextWaypointId;
    }

    public Vector2f getCenter() {
        return center;
    }

    public List<Point> getPoints() {
        if (points == null) {
            points = new ArrayList<>((end.x - start.x + 1) * (end.y - start.y + 1));
            for (int x = start.x; x <= end.x; x++) {
                for (int y = start.y; y <= end.y; y++) {
                    points.add(new Point(x, y));
                }
            }
        }
        return points;
    }

    @Override
    public String toString() {
        return "ActionPoint { id=" + id + ", triggerId=" + triggerId + ", start=" + start + ", end="
                + end + ", flags=" + flags + ", waitDelay=" + waitDelay + ", nextWaypointId=" + nextWaypointId + " }";
    }

    @Override
    public Iterator<Point> iterator() {
        return new ActionPointIterator();
    }

    private class ActionPointIterator implements Iterator<Point> {

        private Point cursor;

        @Override
        public boolean hasNext() {
            return !ActionPoint.this.end.equals(cursor);
        }

        @Override
        public Point next() {

            if (cursor == null) {
                cursor = (Point) ActionPoint.this.start.clone();
                return cursor;
            }

            cursor.x++;
            if (cursor.x > end.x) {
                cursor.x = start.x;
                cursor.y++;
            }

            return cursor;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void forEachRemaining(Consumer<? super Point> consumer) {
            throw new UnsupportedOperationException();
        }
    }
}
