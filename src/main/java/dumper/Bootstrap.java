package dumper;

import game.GameSpec;
import game.VERSION;
import game.save.GameLoader;
import init.INIT;
import init.paths.PATHS;
import init.paths.PATHS.PATHS_BASE;
import init.settings.S;
import launcher.LSettings;
import snake2d.CORE;
import snake2d.CORE.GlJob;
import snake2d.PreLoader;
import snake2d.SOUND_CORE.AUDIO_GAIN_TYPE;
import util.error.ErrorHandler;
import util.spritecomposer.Initer;
import util.text.D;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Path;

/**
 * Full headless bootstrap of the game engine. Mirrors {@code MainProcess.main()}
 * up to (but NOT including) {@code Menu.start()}, then drives the save-load flow
 * from {@code GameLoader.getState()} programmatically.
 *
 * Must run under {@code xvfb-run} — {@code CORE.create()} opens a real GLFW
 * window via LWJGL.
 */
public final class Bootstrap {

    private static boolean engineUp = false;

    public static synchronized void initEngine() {
        if (engineUp) return;

        PreLoader.load(VERSION.VERSION_STRING, PATHS_BASE.PRELOADER, PATHS_BASE.ICON_FOLDER + "Icon64.png");
        CORE.init(new ErrorHandler());

        LSettings ls = new LSettings();
        String l = ls.lang.get();
        PATHS.init(ls.mods.get(), l != null && l.length() > 0 ? l : null, ls.easy.get() == 1);

        D.init();

        // Menu.start() does this; we replicate without entering the game loop.
        CORE.create(S.get().make());

        // Silence audio. Engine still inits OpenAL but plays at gain 0.
        CORE.getSoundCore().setGain(0.0, AUDIO_GAIN_TYPE.MASTER);

        // Menu.RESOURCES ctor runs `new INIT()` inside a GlJob+Initer — the Initer
        // wraps it so `util.spritecomposer.Resources.c` is set before UI/font
        // composition runs. Without that wrapper, UI ctor NPEs in ComposerThings.
        new GlJob() {
            @Override
            public void doJob() {
                new Initer() {
                    @Override
                    public void createAssets() throws IOException {
                        new INIT();
                    }
                }.get("menu", PATHS.textureSize(), 0);
            }
        }.perform();

        // CORE.start() normally creates the Updater (a daemon Thread) and runs the
        // game loop. We won't run the loop, but GameSaver ctor (via GAME.create)
        // reads CORE.getUpdateInfo() which dereferences CORE.updater. Inject a
        // non-running Updater via reflection so the dereference succeeds.
        installInertUpdater();

        engineUp = true;
    }

    private static void installInertUpdater() {
        try {
            Class<?> updaterCls = Class.forName("snake2d.Updater");
            Class<?> ctorCls = Class.forName("snake2d.CORE_STATE$Constructor");
            Constructor<?> updaterCtor = updaterCls.getDeclaredConstructor(ctorCls);
            updaterCtor.setAccessible(true);
            // Pass null — Updater only reads it inside run(), which we never invoke
            // (we don't start the thread).
            Object updater = updaterCtor.newInstance(new Object[]{null});

            Field f = CORE.class.getDeclaredField("updater");
            f.setAccessible(true);
            f.set(null, updater);
        } catch (Exception e) {
            throw new RuntimeException("Failed to install inert Updater", e);
        }
    }

    /**
     * Mirrors {@link game.save.GameLoader#getState()} up through
     * {@code GAME.saver().load(fg)}. After this returns, the global STATS class
     * (static singletons) is populated. Caller pulls values via {@code STATS.POP()} etc.
     *
     * Returns the parsed {@link GameSpec} for callers building a meta block.
     * The spec is read twice: once here (then closed), and again by the
     * {@code GameLoader} internally. Cheap on save files this size.
     */
    public static GameSpec loadSave(Path savePath) throws Exception {
        initEngine();
        GameSpec spec;
        try (SaveReader reader = new SaveReader(savePath)) {
            spec = reader.spec();
        }
        new GameLoader(savePath).getState();
        return spec;
    }

    private Bootstrap() {}
}
