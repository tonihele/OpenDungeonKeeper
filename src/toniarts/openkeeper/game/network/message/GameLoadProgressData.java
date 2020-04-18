/*
 * Copyright (C) 2014-2018 OpenKeeper
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

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.serializers.FieldSerializer;
import toniarts.openkeeper.game.network.Transferable;

/**
 * The game loading progress data
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Transferable(FieldSerializer.class)
public class GameLoadProgressData extends AbstractMessage {

    private short keeperId;
    private float progress;

    public GameLoadProgressData() {
        super(false);
    }

    public GameLoadProgressData(short keeperId, float progress) {
        super(false);
        this.keeperId = keeperId;
        this.progress = progress;
    }

    public short getKeeperId() {
        return keeperId;
    }

    public void setKeeperId(short keeperId) {
        this.keeperId = keeperId;
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

}
