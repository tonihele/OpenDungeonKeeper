/*
 * Copyright (C) 2014-2020 OpenKeeper
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
package toniarts.openkeeper.game.map;

import toniarts.openkeeper.utils.Point;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Iterates trough the given map container
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 * @param <T> the container map tile type
 */
public class MapIterator<T extends IMapTileInformation> implements Iterator<T> {

    private final Point cursor = new Point();
    private final IMapDataInformation<T> mapDataInformation;

    MapIterator(final IMapDataInformation<T> mapDataInformation) {
        this.mapDataInformation = mapDataInformation;
    }

    @Override
    public boolean hasNext() {
        return cursor.x < mapDataInformation.getWidth() && cursor.y < mapDataInformation.getHeight();
    }

    @Override
    public T next() {
        if (cursor.y >= mapDataInformation.getHeight() || cursor.x >= mapDataInformation.getWidth()) {
            throw new IndexOutOfBoundsException();
        }
        T result = mapDataInformation.getTile(cursor);
        cursor.x++;
        if (cursor.x >= mapDataInformation.getWidth()) {
            cursor.x = 0;
            cursor.y++;
        }
        return result;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forEachRemaining(Consumer<? super T> consumer) {
        throw new UnsupportedOperationException();
    }

}
