/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.game.network.message;

import com.jme3.network.serializing.serializers.FieldSerializer;
import java.util.Collection;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.MapData;
import toniarts.openkeeper.game.network.Transferable;

/**
 * Wrapper for game data
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Transferable(FieldSerializer.class)
public class GameData {

    private Collection<Keeper> players;
    private MapData mapData;

    public GameData() {
    }

    public GameData(Collection<Keeper> players, MapData mapData) {
        this.players = players;
        this.mapData = mapData;
    }

    public MapData getMapData() {
        return mapData;
    }

    public Collection<Keeper> getPlayers() {
        return players;
    }

    public void setMapData(MapData mapData) {
        this.mapData = mapData;
    }

    public void setPlayers(Collection<Keeper> players) {
        this.players = players;
    }

}
