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
package toniarts.openkeeper.tools.convert;

import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;
import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.tools.convert.conversion.ConversionTaskManager;
import toniarts.openkeeper.tools.convert.conversion.task.ConvertFonts;
import toniarts.openkeeper.tools.convert.conversion.task.ConvertHiScores;
import toniarts.openkeeper.tools.convert.conversion.task.ConvertMapThumbnails;
import toniarts.openkeeper.tools.convert.conversion.task.ConvertModels;
import toniarts.openkeeper.tools.convert.conversion.task.ConvertMouseCursors;
import toniarts.openkeeper.tools.convert.conversion.task.ConvertPaths;
import toniarts.openkeeper.tools.convert.conversion.task.ConvertSounds;
import toniarts.openkeeper.tools.convert.conversion.task.ConvertTexts;
import toniarts.openkeeper.tools.convert.conversion.task.ConvertTextures;
import toniarts.openkeeper.tools.convert.conversion.task.IConversionTask;
import toniarts.openkeeper.tools.convert.conversion.task.IConversionTaskUpdate;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.utils.PathUtils;
import toniarts.openkeeper.utils.SettingUtils;

/**
 *
 * Converts all the assets from the original game to our game directory<br>
 * In formats supported by our engine<br>
 * Since we own no copyright to these and cannot distribute these, these wont go
 * into our JAR files. We need an custom resource locator for JME
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class AssetsConverter implements IConversionTaskUpdate {

    /**
     * Conversion process enum, contains conversion version and dependency to
     * other conversions
     */
    public enum ConvertProcess {

        TEXTURES(7, new ConvertProcess[]{}),
        MODELS(10, new ConvertProcess[]{TEXTURES}),
        MOUSE_CURSORS(4, new ConvertProcess[]{}),
        MUSIC_AND_SOUNDS(4, new ConvertProcess[]{}),
        INTERFACE_TEXTS(3, new ConvertProcess[]{}),
        PATHS(4, new ConvertProcess[]{}),
        HI_SCORES(2, new ConvertProcess[]{}),
        FONTS(5, new ConvertProcess[]{}),
        MAP_THUMBNAILS(3, new ConvertProcess[]{TEXTURES});

        private ConvertProcess(int version, ConvertProcess[] dependencies) {
            this.version = version;
            this.dependencies = dependencies;
        }

        public int getVersion() {
            return this.version;
        }

        public ConvertProcess[] getDependencies() {
            return dependencies;
        }

        public String getSettingName() {
            String[] names = this.toString().toLowerCase().split(" ");
            StringBuilder name = new StringBuilder();
            for (String item : names) {
                name.append(Character.toUpperCase(item.charAt(0))).append(item.substring(1));
            }
            return name + "Version";
        }

        @Override
        public String toString() {
            return super.toString().replace('_', ' ');
        }

        private final int version;
        private final ConvertProcess[] dependencies;
    }

    private static final Logger logger = System.getLogger(AssetsConverter.class.getName());

    private static final boolean OVERWRITE_DATA = true; // Not exhausting your SDD :) or our custom graphics
    private static final String ASSETS_FOLDER = "assets" + File.separator + "Converted";
    private static final String ABSOLUTE_ASSETS_FOLDER = getCurrentFolder() + ASSETS_FOLDER + File.separator;

    public static final String SOUNDS_FOLDER = "Sounds";
    public static final String MATERIALS_FOLDER = "Materials";
    public static final String MODELS_FOLDER = "Models";
    public static final String TEXTURES_FOLDER = "Textures";
    public static final String SPRITES_FOLDER = "Sprites";
    public static final String MAP_THUMBNAILS_FOLDER = "Thumbnails";
    private static final String INTERFACE_FOLDER = "Interface" + File.separator;
    public static final String MOUSE_CURSORS_FOLDER = INTERFACE_FOLDER + "Cursors";
    private static final String FONTS_FOLDER = INTERFACE_FOLDER + "Fonts";
    private static final String TEXTS_FOLDER = INTERFACE_FOLDER + "Texts";
    public static final String PATHS_FOLDER = INTERFACE_FOLDER + "Paths";

    private final String dungeonKeeperFolder;
    private final AssetManager assetManager;

    public AssetsConverter(String dungeonKeeperFolder, AssetManager assetManager) {
        this.dungeonKeeperFolder = dungeonKeeperFolder;
        this.assetManager = assetManager;
    }

    public static void main(String[] args) {

        String dk2Folder = args.length > 0 ? args[0] : PathUtils.getDKIIFolder();

        // First and foremost, the folder
        if (!PathUtils.checkDkFolder(dk2Folder)) {
            logger.log(Level.ERROR, "DKII folder not found or valid!");
            return;
        }

        // If the folder is ok, check the conversion
        if (!AssetsConverter.isConversionNeeded(SettingUtils.getInstance().getSettings())) {
            logger.log(Level.INFO, "Assets are up-to-date!");
            return;
        }

        logger.log(Level.INFO, "Need to convert the assets!");

        var assetManager = JmeSystem.newAssetManager(Thread.currentThread().getContextClassLoader().getResource("com/jme3/asset/Desktop.cfg"));
        assetManager.registerLocator(AssetsConverter.getAssetsFolder(), FileLocator.class);

        var converter = new AssetsConverter(dk2Folder, assetManager) {
            @Override
            public void onUpdateStatus(Integer currentProgress, Integer totalProgress, ConvertProcess process) {
                System.out.printf("Converting %s: %d/%d\n", process, currentProgress, totalProgress);
            }

            @Override
            public void onComplete(ConvertProcess process) {
                System.out.printf("Completed %s\n", process);
            }

            @Override
            public void onError(Exception ex, ConvertProcess process) {
                System.err.printf("Error in %s: %s\n", process, ex.getMessage());
            }
        };
        converter.convertAssets();
        SettingUtils.getInstance().saveSettings();
    }

    /**
     * Checks if asset conversion is needed
     *
     * @param settings the application settings
     * @return true if asset conversion is needed
     */
    public static boolean isConversionNeeded(AppSettings settings) {
        for (ConvertProcess item : ConvertProcess.values()) {
            if (isConversionNeeded(item, settings)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a specific asset conversion is needed
     *
     * @param convertProcess the process to check
     * @param settings the application settings
     * @return true if asset conversion is needed
     */
    public static boolean isConversionNeeded(ConvertProcess convertProcess, AppSettings settings) {
        String key = convertProcess.getSettingName();
        return convertProcess.getVersion() > settings.getInteger(key);
    }

    /**
     * Get a list of asset conversion processes that should be executed
     *
     * @param settings the application settings
     * @return list of conversion processes
     */
    public static List<ConvertProcess> getConversionNeeded(AppSettings settings) {
        List<ConvertProcess> processes = new ArrayList<>();
        for (ConvertProcess item : ConvertProcess.values()) {
            if (isConversionNeeded(item, settings)) {
                processes.add(item);
            }
        }

        return processes;
    }

    private static synchronized void setConversionComplete(ConvertProcess process, AppSettings settings) {
        settings.putInteger(process.getSettingName(), process.getVersion());
    }

    /**
     * Convert all the original DK II assets to our formats and copy to our
     * working folder
     *
     * @return true if the conversion process was successful
     */
    public boolean convertAssets() {
        long start = System.currentTimeMillis();
        String currentFolder = getCurrentFolder();
        logger.log(Level.INFO, "Starting asset convertion from DK II folder: {0}", dungeonKeeperFolder);
        logger.log(Level.INFO, "Current folder set to: {0}", currentFolder);

        // Create an assets folder
        String assetFolder = currentFolder.concat(ASSETS_FOLDER).concat(File.separator);

        // Create task manager for taking care of the conversion workflow
        ConversionTaskManager conversionTaskManager = new ConversionTaskManager();
        AppSettings settings = Main.getSettings();
        for (ConvertProcess conversion : ConvertProcess.values()) {
            conversionTaskManager.addTask(conversion,
                    () -> {
                        IConversionTask task = createTask(conversion, assetFolder);
                        task.addListener(new IConversionTaskUpdate() {

                            @Override
                            public void onUpdateStatus(Integer currentProgress, Integer totalProgress, ConvertProcess process) {
                                AssetsConverter.this.onUpdateStatus(currentProgress, totalProgress, process);
                            }

                            @Override
                            public void onComplete(ConvertProcess process) {

                                // Mark the conversion complete
                                setConversionComplete(process, settings);

                                AssetsConverter.this.onComplete(process);
                            }

                            @Override
                            public void onError(Exception ex, ConvertProcess process) {
                                AssetsConverter.this.onError(ex, process);
                            }

                        });
                        return task;
                    },
                    isConversionNeeded(conversion, settings));
        }
        boolean success = conversionTaskManager.executeTasks();

        // Log the time taken
        long duration = System.currentTimeMillis() - start;
        logger.log(Level.INFO, "Conversion took {0} seconds!", TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS));

        return success;
    }

    private IConversionTask createTask(ConvertProcess conversion, String currentFolder) {
        switch (conversion) {
            case TEXTURES:
                return new ConvertTextures(dungeonKeeperFolder, currentFolder.concat(TEXTURES_FOLDER).concat(File.separator), OVERWRITE_DATA);
            case MODELS:
                return new ConvertModels(dungeonKeeperFolder, currentFolder.concat(MODELS_FOLDER).concat(File.separator), OVERWRITE_DATA, assetManager);
            case MOUSE_CURSORS:
                return new ConvertMouseCursors(dungeonKeeperFolder, currentFolder.concat(MOUSE_CURSORS_FOLDER).concat(File.separator), OVERWRITE_DATA);
            case MUSIC_AND_SOUNDS:
                return new ConvertSounds(dungeonKeeperFolder, currentFolder.concat(SOUNDS_FOLDER).concat(File.separator), OVERWRITE_DATA);
            case INTERFACE_TEXTS:
                return new ConvertTexts(dungeonKeeperFolder, currentFolder.concat(TEXTS_FOLDER).concat(File.separator), OVERWRITE_DATA);
            case PATHS:
                return new ConvertPaths(dungeonKeeperFolder, currentFolder.concat(PATHS_FOLDER).concat(File.separator), OVERWRITE_DATA);
            case HI_SCORES:
                return new ConvertHiScores(dungeonKeeperFolder, OVERWRITE_DATA);
            case FONTS:
                return new ConvertFonts(dungeonKeeperFolder, currentFolder.concat(FONTS_FOLDER).concat(File.separator), OVERWRITE_DATA);
            case MAP_THUMBNAILS:
                return new ConvertMapThumbnails(dungeonKeeperFolder, currentFolder.concat(MAP_THUMBNAILS_FOLDER).concat(File.separator), OVERWRITE_DATA);
        }

        throw new IllegalArgumentException("Conversion " + conversion + " not implemented!");
    }

    /**
     * Get the current folder
     *
     * @return the current folder
     */
    public static String getCurrentFolder() {
        String currentFolder = Paths.get("").toAbsolutePath().toString();
        return PathUtils.fixFilePath(currentFolder);
    }

    /**
     * Get the assets root folder
     *
     * @return the assets folder
     */
    public static String getAssetsFolder() {
        return ABSOLUTE_ASSETS_FOLDER;
    }

    /**
     * Generates a map thumbnail out of the given map file
     *
     * @param kwd map file
     * @param destination the folder to save to
     * @throws IOException may fail
     */
    public static void genererateMapThumbnail(KwdFile kwd, String destination) throws IOException {
        ConvertMapThumbnails.genererateMapThumbnail(kwd, destination);
    }

}
