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
package toniarts.openkeeper.gui.nifty.jme;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;
import toniarts.openkeeper.utils.Color;

/**
 * Creates a progress indicator, 2D image to the GUI
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ProgressIndicatorPicture extends Picture {

    public ProgressIndicatorPicture(String name) {
        super(name);
    }

    @Override
    public void setTexture(AssetManager assetManager, Texture2D tex, boolean useAlpha) {
        initMaterial(assetManager);

        super.setTexture(assetManager, tex, useAlpha);
    }

    public void setColor(AssetManager assetManager, Color color, boolean useAlpha) {
        initMaterial(assetManager);

        getMaterial().getAdditionalRenderState().setBlendMode(useAlpha ? RenderState.BlendMode.Alpha : RenderState.BlendMode.Off);
        getMaterial().setColor("Color", new ColorRGBA(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f));
    }

    public void setProgress(AssetManager assetManager, float progress) {
        initMaterial(assetManager);

        getMaterial().setFloat("Progress", progress);
    }

    private void initMaterial(AssetManager assetManager) {
        if (getMaterial() == null) {
            Material mat = new Material(assetManager, "MatDefs/Gui/ProgressIndicator.j3md");
            mat.setColor("Color", ColorRGBA.White);

            setMaterial(mat);
        }
    }


}
