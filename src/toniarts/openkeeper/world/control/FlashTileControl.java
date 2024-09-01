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
package toniarts.openkeeper.world.control;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.world.WorldState;

/**
 *
 * @author ArchDemon
 */
@Deprecated
public class FlashTileControl extends AbstractControl {

    private float tick = 0;
    public final float FLASH_PERIOD = 0.5f;
    private boolean flashed = false;
    private WorldState worldState;
    private final List<Point> points = new ArrayList<>();
    private Main app;

    public FlashTileControl() {
    }

    public FlashTileControl(final WorldState worldState, final Main app) {
        this.worldState = worldState;
        this.app = app;
    }

    public boolean attach(List<Point> points, boolean enabled) {
        if (enabled) {
            return this.points.addAll(points);
        } else {
            app.enqueue(() -> {

                worldState.getMapLoader().flashTile(false, points);

                return null;
            });
            return this.points.removeAll(points);
        }
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (!enabled || points.isEmpty()) {
            return;
        }

        if (tick > FLASH_PERIOD) {
            tick -= FLASH_PERIOD;
            flashed = !flashed;
            worldState.getMapLoader().flashTile(flashed, new ArrayList<>(points));
        }

        tick += tpf;
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }
}
