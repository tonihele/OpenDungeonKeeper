/*
 * Copyright (C) 2014-2019 OpenKeeper
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
package toniarts.openkeeper.gui.nifty.jme;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.texture.Texture2D;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.Parameters;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.screen.Screen;
import toniarts.openkeeper.utils.Color;

/**
 * Draws a kinda progress effect image on top of a Nifty element
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ResearchEffectControl extends AbstractNiftyJmeControl {

    public static final String CONTROL_NAME = "researchEffect";

    private ProgressIndicatorPicture picture;
    private boolean initialized = false;
    private float research = 0;
    private Color color;
    private String image;

    @Override
    public void bind(Nifty nifty, Screen screen, Element element, Parameters parameter) {
        super.bind(nifty, screen, element, parameter);

        String colorString = parameter.get("color");
        if (colorString != null && !colorString.isEmpty()) {
            color = new Color(Integer.parseInt(colorString), true);
        }
        image = parameter.getWithDefault("image", "");
        if (image.isEmpty()) {
            image = null;
        }

        // Sanity check
        if (color == null && image == null) {
            throw new IllegalArgumentException("Need to specify either color or image!");
        }
    }


    @Override
    public void initJme(SimpleApplication app) {
        super.initJme(app);

        picture = new ProgressIndicatorPicture("ResearchIndicator");
        picture.setHeight(getControlHeight());
        picture.setWidth(getControlWidth());
        picture.setPosition(getControlX(), getControlY());
        picture.addControl(new SimpleJmeControl(this));
    }

    @Override
    protected void cleanup() {
        if (initialized) {
            getApp().getGuiNode().detachChild(picture);
        }
    }

    public void setResearch(float research) {
        this.research = research;

        if (!initialized && research > 0) {
            initialize();
        } else {
            picture.setProgress(getApp().getAssetManager(), research);
        }
    }

    private void initialize() {
        initialized = true;
        AssetManager assetManager = getApp().getAssetManager();

        // Make sure we are drawing on the right spot
        picture.setHeight(getControlHeight());
        picture.setWidth(getControlWidth());
        picture.setPosition(getControlX(), getControlY());
        picture.setProgress(assetManager, research);

        // Set the color or the picture
        if (image != null) {
            TextureKey key = new TextureKey(image, true);
            Texture2D tex = (Texture2D) assetManager.loadTexture(key);
            picture.setTexture(assetManager, tex, true);
        } else {
            picture.setColor(assetManager, color, true);
        }

        // Attach to scene
        getApp().getGuiNode().attachChild(picture);
    }

}
