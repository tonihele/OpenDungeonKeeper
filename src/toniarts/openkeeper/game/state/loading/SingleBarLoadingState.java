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
package toniarts.openkeeper.game.state.loading;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import toniarts.openkeeper.utils.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import toniarts.openkeeper.Main;

/**
 * Loading state with a single loading bar
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class SingleBarLoadingState extends LoadingState implements IPlayerLoadingProgress {

    private static final String[] availableScreens = {"LoadingScreen1024x768.png", "LoadingScreen1280x1024.png",
        "LoadingScreen1600x1200.png", "LoadingScreen400x300.png",
        "LoadingScreen512x384.png", "LoadingScreen640x480.png",
        "LoadingScreen800x600.png"};
    private static final List<Integer> availableWidths = new ArrayList<>(availableScreens.length);
    private static final Map<Integer, String> screens = HashMap.newHashMap(availableScreens.length);
    private static final Color BAR_COLOR = new Color(237, 100, 42);
    private Geometry progressBar;

    static {

        // Select by width
        Pattern p = Pattern.compile("LoadingScreen(?<width>\\d+)x(?<height>\\d+)\\.png");
        for (String screen : availableScreens) {
            Matcher m = p.matcher(screen);
            m.matches();
            int width = Integer.parseInt(m.group("width"));
            availableWidths.add(width);
            screens.put(width, screen);
        }
        Collections.sort(availableWidths);
    }

    public SingleBarLoadingState(final Main app, String name) {
        super(app, name);
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);

        // Set the loading bar
        progressBar = new Geometry("ProgressBar", new Quad(0, imageHeight * BAR_HEIGHT));
        progressBar.setLocalTranslation((Main.getUserSettings().getAppSettings().getWidth() - imageWidth) / 2 + imageWidth * BAR_X,
                imageHeight - ((Main.getUserSettings().getAppSettings().getHeight() - imageHeight) / 2 + imageHeight * BAR_Y) - imageHeight * BAR_HEIGHT, 0);
        Material mat = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(BAR_COLOR.getRed() / 255f, BAR_COLOR.getGreen() / 255f, BAR_COLOR.getBlue() / 255f, BAR_COLOR.getAlpha() / 255f));
        progressBar.setMaterial(mat);
        this.app.getGuiNode().attachChild(progressBar);
    }

    @Override
    protected Texture getLoadingScreenTexture() {

        // Use binary search to get the nearest resolution index
        int index = Collections.binarySearch(availableWidths, Main.getUserSettings().getAppSettings().getWidth());
        if (index < 0) {
            index = Math.min(availableWidths.size() - 1, ~index + 1);
        }

        // Load up the texture, there are few localized ones available
        String screen = screens.get(availableWidths.get(index));
        TextureKey texKey = new TextureKey(getLocalizedLoadingScreenTextureFolder() + screen);
        return assetManager.loadTexture(texKey);
    }

    /**
     * Set the loading progress
     *
     * @param progress 0.0 to 1.0, 1 being complete
     */
    public void setProgress(final float progress) {

        // Since this method is called from another thread,
        // we enqueue the changes to the progressbar to the update loop thread
        if (initialized && this.progress != Math.round(progress * 100)) {
            this.progress = Math.round(progress * 100);
            app.enqueue(() -> {

                // Adjust the progress bar
                Quad q = (Quad) progressBar.getMesh();
                q.updateGeometry(imageWidth * BAR_WIDTH * progress, q.getHeight());
            });
        }
    }

    @Override
    public void setProgress(float progress, short playerId) {
        setProgress(progress); // We only have one player
    }

    @Override
    public void cleanup() {

        // Remove the title screen
        if (progressBar != null) {
            app.getGuiNode().detachChild(progressBar);
        }

        super.cleanup();
    }

}
