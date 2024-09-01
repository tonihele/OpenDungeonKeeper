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
package toniarts.openkeeper.world;

import toniarts.openkeeper.utils.Point;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Tile;

/**
 * This is just a wrapper for the MAP file really. I really wish they are clean
 * of any engine related code. Just read only "beans". So these wrappers will
 * serve the engine and can be saved etc.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Deprecated
public final class MapData {

    private final int width, height;
    private final KwdFile kwdFile;
    private final TileData[][] tiles;

    public MapData(KwdFile kwdFile) {
        this.kwdFile = kwdFile;
        width = this.kwdFile.getMap().getWidth();
        height = this.kwdFile.getMap().getHeight();
        // Duplicate the map
        this.tiles = new TileData[getWidth()][getHeight()];
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                Tile tile = this.kwdFile.getMap().getTile(x, y);
                tiles[x][y] = new TileData(kwdFile, tile, kwdFile.getTerrain(tile.getTerrainId()), x, y, y * getWidth() + x);
            }
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    protected void setTile(int x, int y, @Nonnull TileData tile) {
        this.tiles[x][y] = tile;
    }

    /**
     * Get the tile data at x & y
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return the tile data
     */
    @Nullable
    public TileData getTile(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return null;
        }
        return this.tiles[x][y];
    }

    /**
     * Get the tile data at point
     *
     * @param p Point
     * @return the tile data
     */
    @Nullable
    public TileData getTile(Point p) {
        if (p != null) {
            return getTile(p.x, p.y);
        }

        return null;
    }
}
