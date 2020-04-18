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
package toniarts.openkeeper.game.controller.map;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import toniarts.openkeeper.game.control.Control;
import toniarts.openkeeper.game.controller.IBuildOrSellRoom;
import toniarts.openkeeper.view.selection.SelectionArea;

/**
 *
 * @author ArchDemon
 */
public class BuildTileControl extends Control {

    private float tick = 0;
    public final float PERIOD = 0.3f;
    private IBuildOrSellRoom gameWorldController;
    private final Queue<Item> queue = new ArrayDeque<>();

    public BuildTileControl() {
        // nope
    }

    public BuildTileControl(final IBuildOrSellRoom gameWorldController) {
        this.gameWorldController = gameWorldController;
    }

    public boolean add(final SelectionArea area, final short playerId, final short room) {
        List<Item> items = new ArrayList<>();

        for (Set<Point> p : area) {
            items.add(new Item(p, playerId, room));
        }

        synchronized (this.queue) {
            return this.queue.addAll(items);
        }
    }

    @Override
    protected void updateControl(float tpf) {
        if (!isEnabled() || queue.isEmpty()) {
            return;
        }

        if (tick >= PERIOD) {
            tick -= PERIOD;
            synchronized (this.queue) {
                Item item = queue.poll();
                if (item.isBuild()) {
                    gameWorldController.build(item.getPoints(), item.getPlayerId(), item.getRoomId());
                } else {
                    gameWorldController.sell(item.getPoints(), item.getPlayerId());
                }
            }
        }

        tick += tpf;
    }

    public static class Item {

        private final Set<Point> points;
        private final short playerId;
        private final short roomId;

        public Item(Set<Point> points, short playerId, short roomId) {
            this.points = points;
            this.playerId = playerId;
            this.roomId = roomId;
        }

        public Set<Point> getPoints() {
            return points;
        }

        public short getPlayerId() {
            return playerId;
        }

        public short getRoomId() {
            return roomId;
        }

        public boolean isBuild() {
            return roomId != 0;
        }

    }
}
