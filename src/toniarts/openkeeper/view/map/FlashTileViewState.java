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

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import toniarts.openkeeper.utils.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import toniarts.openkeeper.Main;

/**
 * Simple app state that keeps tile flashing
 *
 * @author ArchDemon
 */
public class FlashTileViewState extends AbstractAppState {

    private float tick = 0;
    public static final float FLASH_PERIOD = 0.5f;
    private boolean flashed = false;
    private final MapViewController mapViewController;
    private final Set<Point> points = new HashSet<>();
    private Main app;

    public FlashTileViewState(final MapViewController mapViewController) {
        this.mapViewController = mapViewController;
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);

        this.app = (Main) app;
    }

    public boolean attach(List<Point> points, boolean enabled) {
        if (enabled) {
            return this.points.addAll(points);
        } else {
            app.enqueue(() -> {

                mapViewController.flashTile(false, points);

                return null;
            });
            return this.points.removeAll(points);
        }
    }

    @Override
    public void update(float tpf) {
        if (!initialized || points.isEmpty()) {
            return;
        }

        if (tick > FLASH_PERIOD) {
            tick -= FLASH_PERIOD;
            flashed = !flashed;
            mapViewController.flashTile(flashed, new ArrayList<>(points));
        }

        tick += tpf;
    }

}
