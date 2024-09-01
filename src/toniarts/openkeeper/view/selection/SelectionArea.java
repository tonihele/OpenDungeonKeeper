package toniarts.openkeeper.view.selection;

import com.jme3.math.Vector2f;
import toniarts.openkeeper.utils.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * @author 7willuwe : Philip Willuweit
 */
public class SelectionArea implements Iterable<List<Point>> {

    private Vector2f start = new Vector2f();
    private Vector2f end = new Vector2f();
    private float scale = 1;

    public SelectionArea(Vector2f start, Vector2f end) {
        this.start = start;
        this.end = end;
    }

    public SelectionArea(float appScaled) {
        this.scale = appScaled;
        this.start = new Vector2f(Vector2f.ZERO);
        this.end = new Vector2f(Vector2f.ZERO);
    }

    /**
     * For single square use only
     *
     * @param appScaled
     * @param start Start position
     * @param end End position
     */
    public SelectionArea(float appScaled, Vector2f start, Vector2f end) {
        this.start = start;
        this.end = end;
        this.scale = appScaled;
    }

    /**
     * @return the start
     */
    public Vector2f getStart() {
        return new Vector2f(Math.min(start.x, end.x), Math.min(start.y, end.y));
    }

    /**
     * Get the real starting coordinates, the first click
     *
     * @return
     */
    public Vector2f getRealStart() {
        return start;
    }

    /**
     * @param position the start to set
     */
    public void setStart(Vector2f position) {
        start.set(position);
        end.set(position);
    }

    /**
     * @return the end
     */
    public Vector2f getEnd() {
        return new Vector2f(Math.max(start.x, end.x), Math.max(start.y, end.y));
    }

    /**
     * Get the real ending coordinates, the click release
     *
     * @return
     */
    public Vector2f getRealEnd() {
        return end;
    }

    /**
     * @param position the end to set
     */
    public void setEnd(Vector2f position) {
        end.set(position);
    }

    /**
     * @return the scale
     */
    public float getScale() {
        return scale;
    }

    /**
     * @param scale the scale to set
     */
    public void setScale(float scale) {
        this.scale = scale;
    }

    public Vector2f getCenter() {
        return new Vector2f((end.x + start.x) / 2, (start.y + end.y) / 2);
    }

    /**
     * @return the delta x axis
     */
    public float getDeltaX() {
        return (Math.abs(end.x - start.x) + 1) / scale;
    }

    /**
     * @return the delta y axis
     */
    public float getDeltaY() {
        return (Math.abs(end.y - start.y) + 1) / scale;
    }

    @Override
    public Iterator<List<Point>> iterator() {
        return new SelectionArea.AreaIterator();
    }

    /**
     * An optimized version of AbstractList.Itr
     */
    private class AreaIterator implements Iterator<List<Point>> {

        private final Point cursor = WorldUtils.vectorToPoint(SelectionArea.this.getStart());
        private final Point start = WorldUtils.vectorToPoint(SelectionArea.this.getStart());
        private final Point end = WorldUtils.vectorToPoint(SelectionArea.this.getEnd());

        @Override
        public boolean hasNext() {
            return cursor.x != (end.x + 1) && cursor.y != (end.y + 1);
        }

        @Override
        public List<Point> next() {
            List result = new ArrayList<>();

            while (cursor.y >= start.y && cursor.x <= end.x) {
                check();
                result.add(new Point(cursor.x, cursor.y));

                cursor.x++;
                cursor.y--;
            }

            if (cursor.y < start.y) {
                cursor.y = start.y + (cursor.x - start.x);
                cursor.x = start.x;
            }

            if (cursor.x > end.x) {
                cursor.y += (cursor.x - start.x) + 1;
                cursor.x = start.x;
            }

            if (cursor.y > end.y) {
                cursor.x = start.x + (cursor.y - end.y);
                cursor.y = end.y;
            }

            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void forEachRemaining(Consumer<? super List<Point>> consumer) {
            throw new UnsupportedOperationException();
        }

        private void check() {
            if (cursor.x > end.x || cursor.x < start.x
                    || cursor.y > end.y || cursor.y < start.y) {
                throw new NoSuchElementException();
            }
        }
    }
}
