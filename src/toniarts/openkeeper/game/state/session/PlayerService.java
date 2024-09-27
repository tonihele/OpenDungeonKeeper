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
package toniarts.openkeeper.game.state.session;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.tools.convert.map.TriggerAction;

/**
 * Handles player related requests
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface PlayerService {

    /**
     * Set widescreen mode to the player
     *
     * @param enable set widescreen on/off
     * @param playerId the player to set to
     */
    public void setWidescreen(boolean enable, short playerId);

    /**
     * Play a speech on the player
     *
     * @param speechId the speech ID
     * @param showText show subtitles
     * @param introduction whether this is an introduction or not
     * @param pathId camera path
     * @param playerId the player to hear the speech
     */
    public void playSpeech(int speechId, boolean showText, boolean introduction, int pathId, short playerId);

    /**
     * Is any of the players in UI transition (cinematic playing)
     *
     * @return true if in transition
     */
    public boolean isInTransition();

    /**
     * Do an UI transition, a cinematic
     *
     * @param pathId the camera path to use
     * @param start starting coordinates
     * @param playerId the player ID who should play the transition
     */
    public void doTransition(short pathId, Vector3f start, short playerId);

    /**
     * Flash UI button for player
     *
     * @param buttonType the button type
     * @param targetId the ID of the record, type specified by
     * {@link #TriggerAction.MakeType}
     * @param targetButtonType if the button type is
     * {@link TriggerAction.MakeType#MISC_BUTTON} this specifies the button
     * @param enabled turn flashing on/off ?
     * @param time time to flash
     * @param playerId the player ID whose button should flash
     */
    public void flashButton(TriggerAction.MakeType buttonType, short targetId, TriggerAction.ButtonType targetButtonType, boolean enabled, int time, short playerId);

    /**
     * Rotate camera around a point
     *
     * @param point the point to rotate around
     * @param relative relative to current position?
     * @param angle rotation angle
     * @param time time to rotate
     * @param playerId the player ID whose camera should rotate
     */
    public void rotateViewAroundPoint(Vector3f point, boolean relative, int angle, int time, short playerId);

    /**
     * Show info message for player
     *
     * @param textId the text ID
     * @param playerId the player ID who should get the message
     */
    public void showMessage(int textId, short playerId);

    /**
     * Zoom camera to a point
     *
     * @param point the point to zoom to
     * @param playerId the player ID whose camera should be moved
     */
    public void zoomViewToPoint(Vector3f point, short playerId);

    /**
     * Zoom camera to a entity
     *
     * @param entityId the entity to zoom to
     * @param playerId the player ID whose camera should be moved
     */
    public void zoomViewToEntity(EntityId entityId, short playerId);

    /**
     * Sets the game as paused
     *
     * @param paused true is paused
     */
    public void setGamePaused(boolean paused);

    /**
     * Show unit flower of an entity
     *
     * @param entityId the entity of which flower to show
     * @param interval show for how many seconds
     * @param playerId the player ID who should see the flower
     */
    public void showUnitFlower(EntityId entityId, int interval, short playerId);

    /**
     * Set player possession mode on/off
     *
     * @param target possession target, null if possession ends
     * @param playerId player ID that posesses
     */
    public void setPossession(EntityId target, short playerId);

}
