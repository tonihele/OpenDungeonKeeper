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
package toniarts.openkeeper;

import com.jme3.app.DebugKeysAppState;
import com.jme3.app.DetailedProfilerState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.app.state.VideoRecorderAppState;
import com.jme3.asset.AssetEventListener;
import com.jme3.asset.AssetKey;
import com.jme3.asset.TextureKey;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.light.AmbientLight;
import com.jme3.math.ColorRGBA;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.opengl.GLRenderer;
import com.jme3.system.AppSettings;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.render.batch.BatchRenderConfiguration;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.ResourceBundle;
import javax.imageio.ImageIO;
import toniarts.openkeeper.audio.plugins.MP2Loader;
import toniarts.openkeeper.cinematics.CameraSweepDataLoader;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.sound.GlobalCategory;
import toniarts.openkeeper.game.state.MainMenuState;
import toniarts.openkeeper.game.state.SoundState;
import toniarts.openkeeper.game.state.loading.TitleScreenState;
import toniarts.openkeeper.game.state.session.LocalGameSession;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.modelviewer.SoundsLoader;
import toniarts.openkeeper.utils.PathUtils;
import toniarts.openkeeper.utils.SettingUtils;
import toniarts.openkeeper.video.MovieState;

/**
 * Main entry point of OpenKeeper
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class Main extends SimpleApplication {

    private static final Logger logger = System.getLogger(Main.class.getName());

    private static final String VERSION = "*ALPHA*";
    public static final String TITLE = "OpenKeeper";
    private static final String USER_HOME_FOLDER = System.getProperty("user.home").concat(File.separator).concat(".").concat(TITLE).concat(File.separator);
    private static final String SCREENSHOTS_FOLDER = USER_HOME_FOLDER.concat("SCRSHOTS").concat(File.separator);
    private static Map<String, String> params;
    private static boolean debug;

    private NiftyJmeDisplay niftyDisplay;
    private byte[] gameUiXml;

    private Main() {
        super(new StatsAppState(), new DebugKeysAppState());
    }

    public static void main(String[] args) throws Exception {

        // Create main application instance
        parseArguments(args);
        debug = params.containsKey("debug");
        final Main app = new Main();
        app.setPauseOnLostFocus(false);

        // Read settings and convert resources if needed
        app.showSettings = false;
        initSettings(app);

        // Check assets
        AssetsConverter.main(new String[0]);
        app.start();
    }

    /**
     * Parse application parameters
     *
     * @param args the arguments list
     */
    private static void parseArguments(String[] args) {
        params = HashMap.newHashMap(args.length);

        // Go through the params
        int i = 0;
        while (i < args.length) {

            // Skip values etc
            if (!args[i].startsWith("-")) {
                i++;
                continue;
            }

            // If the next parameter doesn't have a "-", it is the value for the param
            String value = null;
            if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                value = args[i + 1];
            }

            // Add it
            params.put(args[i].substring(1).toLowerCase(), value);

            i++;
        }
    }

    private static void initSettings(Main app) {

        // Create some folders
        try {
            Files.createDirectories(Paths.get(USER_HOME_FOLDER));
        } catch (IOException ex) {
            logger.log(Level.ERROR, "Failed to create folder " + USER_HOME_FOLDER + "!", ex);
        }
        try {
            Files.createDirectories(Paths.get(SCREENSHOTS_FOLDER));
        } catch (IOException ex) {
            logger.log(Level.ERROR, "Failed to create folder " + SCREENSHOTS_FOLDER + "!", ex);
        }

        // Init the user settings (which in JME are app settings)
        app.settings = Settings.getInstance().getAppSettings();
    }

    /**
     * The user settings, main settings
     *
     * @return the user settings
     */
    public static Settings getUserSettings() {
        return Settings.getInstance();
    }

    /**
     * Get the application settings, global OpenKeeper settings, nothing much
     * here
     *
     * @see #getUserSettings()
     * @return application settings
     */
    public static AppSettings getSettings() {
        return SettingUtils.getInstance().getSettings();
    }

    @Override
    public void simpleInitApp() {

        if (debug)
            ((GLRenderer)renderer).setDebugEnabled(true); // get debug names for GL objects

        // Distribution locator
        getAssetManager().registerLocator(AssetsConverter.getAssetsFolder(), FileLocator.class);

        // Init nifty while in render thread so it will get initialized before it is updated, otherwise we might hit a rare race-condition
        Nifty nifty = getNifty();

        // Initiate the title screen
        TitleScreenState gameLoader = new TitleScreenState(this, "Title Screen") {

            @Override
            public void onLoad() {
                try {
                    // Asset loaders
                    // Sound
                    getAssetManager().registerLoader(MP2Loader.class, MP2Loader.FILE_EXTENSION);
                    // Camera sweep files
                    getAssetManager().registerLoader(CameraSweepDataLoader.class, CameraSweepDataLoader.FILE_EXTENSION);

                    // Set the anisotropy asset listener
                    setAnisotropy();

                    // Allow people to take screenshots
                    ScreenshotAppState screenShotState = new ScreenshotAppState(SCREENSHOTS_FOLDER);
                    getStateManager().attach(screenShotState);

                    // Recording video
                    if (params.containsKey("record")) {
                        float quality = Settings.getInstance().getFloat(Settings.Setting.RECORDER_QUALITY);
                        int frameRate = Settings.getInstance().getInteger(Settings.Setting.RECORDER_FPS);
                        getSettings().setFrameRate(frameRate);
                        VideoRecorderAppState recorder = new VideoRecorderAppState(quality, frameRate);
                        String folder = params.get("record");
                        if (folder == null) {
                            folder = SCREENSHOTS_FOLDER;
                        }

                        folder = PathUtils.fixFilePath(folder).concat(getSettings().getTitle()).concat("-").concat(String.valueOf(System.currentTimeMillis() / 1000)).concat(".avi");
                        recorder.setFile(new File(folder));

                        getStateManager().attach(recorder);
                    }

                    // Nifty
                    nifty.setGlobalProperties(new Properties());
                    nifty.getGlobalProperties().setProperty("MULTI_CLICK_TIME", "1");
                    nifty.getGlobalProperties().setProperty("VERSION", VERSION);
                    setupNiftyResourceBundles(nifty);
                    setupNiftySound(nifty);

                    // Load the XMLs, since we also validate them, Nifty will read them twice
                    byte[] mainMenuUiXml = PathUtils.readInputStream(Main.this.getClass().getResourceAsStream("/Interface/MainMenu.xml"));
                    gameUiXml = PathUtils.readInputStream(Main.this.getClass().getResourceAsStream("/Interface/GameHUD.xml"));
                    List<Map.Entry<String, byte[]>> guiXmls = new ArrayList<>(2);
                    guiXmls.add(Map.entry("Interface/MainMenu.xml", mainMenuUiXml));
                    guiXmls.add(Map.entry("Interface/GameHUD.xml", gameUiXml));

                    // Validate the XML, great for debuging purposes
                    for (Map.Entry<String, byte[]> xml : guiXmls) {
                        try {
                            nifty.validateXml(new ByteArrayInputStream(xml.getValue()));
                        } catch (Exception e) {
                            throw new RuntimeException("GUI file " + xml.getKey() + " failed to validate!", e);
                        }
                    }

                    // Initialize persistent app states
                    MainMenuState mainMenuState = new MainMenuState(!params.containsKey("level"), assetManager, Main.this);
                    DetailedProfilerState detailedProfilerState = new DetailedProfilerState();
                    detailedProfilerState.setEnabled(false); // F6
                    getStateManager().getState(StatsAppState.class).toggleStats(); // F5

                    getStateManager().attach(new SoundState(false));
                    loadSounds();

                    getStateManager().attach(mainMenuState);
                    getStateManager().attach(detailedProfilerState);

                    // Eventually we are going to use Nifty, the XML files take some time to parse
                    nifty.addXml(new ByteArrayInputStream(mainMenuUiXml));
                } catch (Exception e) {
                    logger.log(Level.ERROR, "Failed to load the game!", e);
                    app.stop();
                }
            }

            @Override
            public void onLoadComplete() {

                // FIXME: We need ambient light, but it may be different for different states. There just seems to be a bug in BatchNodes concerning the removal of the light. So this is temporary perhaps
                AmbientLight al = new AmbientLight();
                al.setColor(ColorRGBA.White);
                rootNode.addLight(al);

                if (params.containsKey("nomovies") || params.containsKey("level")) {
                    startGame();
                } else {
                    // The fireworks!
                    playIntro();
                }
            }

            private void loadSounds() {
                for (String cat : GlobalCategory.getCategories()) {
                    SoundsLoader.load(cat);
                }
            }

            private void setupNiftyResourceBundles(Nifty nifty) {

                // For main menu
                nifty.addResourceBundle("menu", Main.getResourceBundle("Interface/Texts/Text"));
                nifty.addResourceBundle("speech", Main.getResourceBundle("Interface/Texts/Speech"));
                nifty.addResourceBundle("mpd1", Main.getResourceBundle("Interface/Texts/LEVELMPD1_BRIEFING"));
                nifty.addResourceBundle("mpd2", Main.getResourceBundle("Interface/Texts/LEVELMPD2_BRIEFING"));
                nifty.addResourceBundle("mpd3", Main.getResourceBundle("Interface/Texts/LEVELMPD3_BRIEFING"));
                nifty.addResourceBundle("mpd4", Main.getResourceBundle("Interface/Texts/LEVELMPD4_BRIEFING"));
                nifty.addResourceBundle("mpd5", Main.getResourceBundle("Interface/Texts/LEVELMPD5_BRIEFING"));
                nifty.addResourceBundle("mpd6", Main.getResourceBundle("Interface/Texts/LEVELMPD6_BRIEFING"));
            }
        };
        this.stateManager.attach(gameLoader);
    }

    public static void setupNiftySound(Nifty nifty) {
        Settings s = getUserSettings();

        float musicVolume = s.getFloat(Settings.Setting.MASTER_VOLUME) * s.getFloat(Settings.Setting.MUSIC_VOLUME);
        nifty.getSoundSystem().setMusicVolume(s.getBoolean(Settings.Setting.VOICE_ENABLED) ? musicVolume : 0);
        nifty.getSoundSystem().setSoundVolume(s.getFloat(Settings.Setting.MASTER_VOLUME));
    }

    public byte[] getGameUiXml() {
        return gameUiXml;
    }

    /**
     * Adds an asset listener to the asset manager that automatically sets
     * anisotropy level to any textures loaded
     */
    private void setAnisotropy() {
        AssetEventListener asl = new AssetEventListener() {
            @Override
            public void assetLoaded(AssetKey key) {
            }

            @Override
            public void assetRequested(AssetKey key) {
                if (key.getExtension().equals("png") || key.getExtension().equals("jpg") || key.getExtension().equals("dds")) {
                    TextureKey tkey = (TextureKey) key;
                    tkey.setAnisotropy(Settings.getInstance().getInteger(Settings.Setting.ANISOTROPY));
                }
            }

            @Override
            public void assetDependencyNotFound(AssetKey parentKey, AssetKey dependentAssetKey) {
            }
        };
        assetManager.addAssetEventListener(asl);
    }

    /**
     * Get the application icons
     *
     * @return array of application icons
     */
    public static BufferedImage[] getApplicationIcons() {
        ImageIO.setUseCache(false);
        try {
            return new BufferedImage[]{
                readIcon("/Icons/openkeeper256.png"),
                readIcon("/Icons/openkeeper256.png"),
                readIcon("/Icons/openkeeper128.png"),
                readIcon("/Icons/openkeeper64.png"),
                readIcon("/Icons/openkeeper48.png"),
                readIcon("/Icons/openkeeper32.png"),
                readIcon("/Icons/openkeeper24.png"),
                readIcon("/Icons/openkeeper16.png")
            };
        } catch (IOException ex) {
            logger.log(Level.ERROR, "Failed to load the application icons!", ex);
        }
        return null;
    }

    private static BufferedImage readIcon(String path) throws IOException {
        try (InputStream is = Main.class.getResourceAsStream(path);
                BufferedInputStream bis = new BufferedInputStream(is)) {
            return ImageIO.read(bis);
        }
    }

    /**
     * Gets the DK II folder, convenience method (this is also stored in the
     * settings)
     *
     * @return the DK II folder
     */
    public static String getDkIIFolder() {
        return PathUtils.getDKIIFolder();
    }

    @Override
    public void restart() {
        try {
            settings = Settings.getInstance().getAppSettings();
            super.restart();

            // FIXME: This should go to handle error
            try {

                // Continue to save the settings
                Settings.getInstance().save();
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Can not save the settings!", ex);
            }

        } catch (Exception e) {
            // Failed to restart
//            initSettings(this);
//            restart();
        }
    }

    /**
     * (re-)Sets scene processors to the view port
     */
    public void setViewProcessors() {

        // Clear the old ones
        viewPort.clearProcessors();

        // Add SSAO
        if (Settings.getInstance().getBoolean(Settings.Setting.SSAO)) {
            FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
            SSAOFilter ssaoFilter = new SSAOFilter(Settings.getInstance().getFloat(Settings.Setting.SSAO_SAMPLE_RADIUS),
                    Settings.getInstance().getFloat(Settings.Setting.SSAO_INTENSITY),
                    Settings.getInstance().getFloat(Settings.Setting.SSAO_SCALE),
                    Settings.getInstance().getFloat(Settings.Setting.SSAO_BIAS));
            fpp.addFilter(ssaoFilter);
            viewPort.addProcessor(fpp);
        }
    }

    /**
     * Gets current locale's resource bundle (UTF-8)
     *
     * @param baseName base name of the bundle
     * @return the resource bundle
     */
    public static ResourceBundle getResourceBundle(String baseName) {
        Path file = Paths.get(AssetsConverter.getAssetsFolder());
        try {
            URL[] urls = {file.toUri().toURL()};
            ClassLoader loader = new URLClassLoader(urls);
            return ResourceBundle.getBundle(baseName, Locale.getDefault(), loader);
        } catch (Exception e) {
            logger.log(Level.ERROR, "Failed to locate the resource bundle " + baseName + " in " + file + "!", e);
        }

        // Works only from the IDE
        return ResourceBundle.getBundle(baseName);
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }

    /**
     * Starts the game, opens up the start menu / level
     */
    private void startGame() {
        if (params.containsKey("level")) {
            try {
                LocalGameSession.createLocalGame(params.get("level"), false, stateManager, this);
            } catch (IOException ex) {
                throw new RuntimeException("Failed to start the game!", ex);
            }
        } else {

            // Enable the start menu
            stateManager.getState(MainMenuState.class).setEnabled(true);
        }
    }

    /**
     * Plays the intro movies, after which the game is started
     */
    private void playIntro() {

        // The intro sequence
        Queue<String> introSequence = new ArrayDeque<>(2);
        try {
            introSequence.add(PathUtils.getRealFileName(getDkIIFolder(), PathUtils.DKII_MOVIES_FOLDER + "BullfrogIntro.tgq"));
        } catch (IOException ex) {
            logger.log(Level.INFO, "Could not find the Bullfrog intro!", ex);
        }
        try {
            introSequence.add(PathUtils.getRealFileName(getDkIIFolder(), PathUtils.DKII_MOVIES_FOLDER + "INTRO.TGQ"));
        } catch (IOException ex) {
            logger.log(Level.INFO, "Could not find the game intro!", ex);
        }
        playMovie(introSequence);
    }

    /**
     * Plays single movie, and advances to the next or if not available, starts
     * the game
     *
     * @param introSequence movie queue
     */
    private void playMovie(final Queue<String> introSequence) {

        String movieFile = introSequence.poll();
        if (movieFile != null) {
            try {
                MovieState movieState = new MovieState(movieFile) {
                    @Override
                    protected void onPlayingEnd() {
                        playMovie(introSequence);
                    }
                };
                stateManager.attach(movieState);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to initiate playing " + movieFile + "!", e);

                // Continue with the movie list
                playMovie(introSequence);
            }
        } else {
            startGame();
        }
    }

    /**
     * Get Nifty, warning this also initializes and attaches Nifty (if not
     * initialized already) and is not synchronized
     *
     * @return the Nifty instance
     */
    private NiftyJmeDisplay getNiftyDisplay() {
        if (niftyDisplay == null) {

            // Batching
            BatchRenderConfiguration config = new BatchRenderConfiguration();
            config.atlasHeight = 2048;
            config.atlasWidth = 2048;
            config.fillRemovedImagesInAtlas = false;
            config.disposeImagesBetweenScreens = false;
            config.useHighQualityTextures = true;

            // Init Nifty
            niftyDisplay = NiftyJmeDisplay.newNiftyJmeDisplay(assetManager,
                    inputManager,
                    getAudioRenderer(),
                    getGuiViewPort(), config);

            // Attach the nifty display to the gui view port as a processor
            getGuiViewPort().addProcessor(niftyDisplay);
        }
        return niftyDisplay;
    }

    public Nifty getNifty() {
        return getNiftyDisplay().getNifty();
    }

    /**
     * Whether the debug flag is on
     *
     * @return debug
     */
    public static boolean isDebug() {
        return debug;
    }

}
