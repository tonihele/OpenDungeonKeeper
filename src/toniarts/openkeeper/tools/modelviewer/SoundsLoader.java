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
package toniarts.openkeeper.tools.modelviewer;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.scene.Node;
import java.io.File;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.sound.SoundCategory;
import toniarts.openkeeper.game.sound.SoundFile;
import toniarts.openkeeper.game.sound.SoundGroup;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.Utils;

/**
 *
 * @author archdemon
 */
public class SoundsLoader {
    
    private static final Logger logger = System.getLogger(SoundsLoader.class.getName());
    
    private static final Map<String, SoundCategory> CACHE = new HashMap<>();

    private SoundsLoader() {
        // nope
    }

    @Nullable
    public static SoundCategory load(final String soundCategory) {
        return load(soundCategory, true);
    }

    @Nullable
    public static SoundCategory load(final String category, final boolean useGlobal) {

        if (category == null || category.isEmpty()) {
            return null;
        } else if (CACHE.containsKey(category)) {
            return CACHE.get(category);
        }

        SoundCategory result = null;
        try {
            result = new SoundCategory(category, useGlobal);
            CACHE.put(category, result);
//            temp(category, useGlobal);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Sound category {0} does not exist", category);
        }

        return result;
    }

    @Nullable
    protected static SoundGroup getGroup(final String category, final int id) {
        SoundCategory sc = SoundsLoader.load(category);
        if (sc == null) {
            return null;
        }

        if (!sc.hasGroup(id)) {
            logger.log(Level.WARNING, "Sound group {0} does not exist in category {1} ", new Object[]{id, category});
            return null;
        }

        return sc.getGroup(id);
    }

    @Nullable
    public static SoundFile getAudioFile(final String category, final int id) {
        SoundGroup sg = getGroup(category, id);
        if (sg != null) {
            return Utils.getRandomItem(sg.getFiles());
        }

        return null;
    }

    @Nullable
    public static AudioNode getAudioNode(final AssetManager assetManager, final SoundFile file) {
        if (file == null) {
            return null;
        }

        return new AudioNode(assetManager,
                AssetsConverter.SOUNDS_FOLDER + File.separator + file.getFilename(),
                AudioData.DataType.Buffer);
    }

    @Nullable
    public static AudioNode getAudioNode(final AssetManager assetManager, final String category, int id) {
        SoundGroup sg = getGroup(category, id);
        if (sg != null) {
            SoundFile file = Utils.getRandomItem(sg.getFiles());
            return getAudioNode(assetManager, file);
        }

        return null;
    }

    public static void playSound(final AudioNode node) {
        if (node != null) {
            float volume = Main.getUserSettings().getFloat(Settings.Setting.MASTER_VOLUME);
            node.setVolume(volume);
            node.setLooping(false);
            node.setPositional(false);
            node.play();
        }
    }

    public static void playSound(final Node parent, final AudioNode node, final Point p) {
        if (node != null) {
            float volume = Main.getUserSettings().getFloat(Settings.Setting.MASTER_VOLUME);
            node.setVolume(volume);
            node.setPositional(true);
            node.setReverbEnabled(false);
            node.setLocalTranslation(p.x, 0, p.y);
            parent.attachChild(node);
            node.play();
        }
    }

    /**
     * Prints sound files in System.out. For debug only
     *
     * @deprecated @param category
     * @param useGlobal
     */
    @Deprecated
    public static void temp(final String category, final boolean useGlobal) {
        List<SoundFile> result = new ArrayList<>();

        SoundCategory sc = load(category, useGlobal);
        System.out.println(category);
        if (sc != null) {
            for (SoundGroup sa : sc.getGroups().values()) {
                result.addAll(sa.getFiles());
            }
        }
        Collections.sort(result);

        for (SoundFile soundFile : result) {
            System.out.println("\t" + soundFile);
        }
    }

}
