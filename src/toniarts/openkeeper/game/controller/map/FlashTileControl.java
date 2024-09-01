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
package toniarts.openkeeper.game.controller.map;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import toniarts.openkeeper.utils.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.game.control.Control;
import toniarts.openkeeper.game.controller.IMapController;

/**
 * Purpose of this class is to flash certain map tiles for given time.
 *
 * @author ArchDemon
 */
public class FlashTileControl extends Control {

    private float time;
    private List<Point> points;
    private short playerId;
    private IMapController mapController;

    public FlashTileControl() {
    }

    public FlashTileControl(List<Point> points, short playerId, int time) {
        this.points = points;
        this.playerId = playerId;
        this.time = time;
    }

    @Override
    protected void updateControl(float tpf) {
        if (time < 0) {
            mapController.unFlashTiles(points, playerId);
            parent.removeControl(this);

            return;
        }
        time -= tpf;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);

        OutputCapsule oc = ex.getCapsule(this);
        oc.writeSavableArrayList(new ArrayList<>(points), "points", null);
        oc.write(playerId, "playerId", 0);
        oc.write(time, "time", 0);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);

        InputCapsule ic = im.getCapsule(this);
        points = ic.readSavableArrayList("points", new ArrayList<>());
        playerId = ic.readShort("playerId", (short) 0);
        time = ic.readFloat("time", 0);
    }
}
