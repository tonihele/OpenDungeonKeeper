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
package toniarts.openkeeper.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import toniarts.openkeeper.utils.Point;

/**
 * Holds an entity instance, series of coordinates that together form an entity
 * area
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 * @param <T> Room, Terrain
 */
public class EntityInstance<T> {

    private final T entity;
    private final List<Point> coordinates = new ArrayList<>();
    private Point matrixStartPoint;
    private boolean[][] matrix;
    private int minX = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE;
    private int minY = Integer.MAX_VALUE;
    private int maxY = Integer.MIN_VALUE;

    public EntityInstance(T entity) {
        if (entity == null) {
            throw new RuntimeException("Entity can not be null");
        }
        this.entity = entity;
    }

    public T getEntity() {
        return entity;
    }

    public void addCoordinate(Point p) {
        coordinates.add(~Collections.binarySearch(coordinates, p, new PointComparator()), p);
        minX = Math.min(p.x, minX);
        maxX = Math.max(p.x, maxX);
        minY = Math.min(p.y, minY);
        maxY = Math.max(p.y, maxY);
        matrixStartPoint = null;
        matrix = null;
    }

    public void addCoordinates(Collection<Point> points) {
        for (Point p : points) {
            addCoordinate(p);
        }
    }

    public void removeCoordinate(Point p) {
        coordinates.remove(Collections.binarySearch(coordinates, p, new EntityInstance.PointComparator()));
        minX = Math.max(p.x, minX);
        maxX = Math.min(p.x, maxX);
        minY = Math.max(p.y, minY);
        maxY = Math.min(p.y, maxY);
        matrixStartPoint = null;
        matrix = null;
    }

    public boolean hasCoordinate(Point p) {
        return Collections.binarySearch(coordinates, p, new PointComparator()) >= 0;
    }

    public Point getCenter() {
        return new Point((minX + maxX) / 2, (minY + maxY) / 2);
    }

    /**
     * Get the coordinates as a sorted list
     *
     * @return the coordinates list
     * @see #getCoordinatesAsMatrix()
     */
    public List<Point> getCoordinates() {
        return coordinates;
    }

    /**
     * Some building are nowhere near squares, the matrix will help in building
     * such rooms<br>
     * The matrix is build in room constraints, so you need to add the first
     * coordinate to the matrix coordinate to get the real world coordinate
     *
     * @return coordinates as matrix, true value signifies presense of the room
     * instance in the coordinate
     * @see #getCoordinates()
     * @see #getMatrixStartPoint()
     */
    public boolean[][] getCoordinatesAsMatrix() {
        if (matrix == null) {
            int width = maxX - minX + 1;
            int height = coordinates.get(coordinates.size() - 1).y - getMatrixStartPoint().y + 1;
            matrix = new boolean[width][height];
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    matrix[x][y] = (Collections.binarySearch(coordinates, new Point(getMatrixStartPoint().x + x, getMatrixStartPoint().y + y), new PointComparator()) > -1);
                }
            }
        }
        return matrix;
    }

    /**
     * If you have acquired the matrix already, you can get the matrix start
     * point (the corner) from here. Otherwise you can't know what real
     * coordinates the matrix cells represent.
     *
     * @return the upper left corner point of the matrix as real map coordinate
     * @see #getCoordinatesAsMatrix()
     */
    public Point getMatrixStartPoint() {
        if (matrixStartPoint == null) {
            matrixStartPoint = new Point(minX, coordinates.get(0).y);
        }
        return matrixStartPoint;
    }

    @Override
    public String toString() {
        return entity.toString();
    }

    /**
     * Sorts coordinates so that they are in order (helps room
     * construction?)<br>
     * The order is "natural", starting from origin
     */
    private static final class PointComparator implements Comparator<Point> {

        @Override
        public int compare(Point o1, Point o2) {
            int result = Integer.compare(o1.y, o2.y);
            if (result == 0) {
                result = Integer.compare(o1.x, o2.x);
            }
            return result;
        }
    }

    /**
     * Translates a world tile coordinate to local room coordinate, stating at
     * the top left corner
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @return the local coordinate
     */
    public Point worldCoordinateToLocalCoordinate(int x, int y) {
        return new Point(x - getMatrixStartPoint().x, y - getMatrixStartPoint().y);
    }

    /**
     * Translates a world tile coordinate to local room coordinate, stating at
     * the top left corner
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @return the local coordinate
     */
    public Point localCoordinateToWorldCoordinate(int x, int y) {
        return new Point(x + getMatrixStartPoint().x, y + getMatrixStartPoint().y);
    }
}
