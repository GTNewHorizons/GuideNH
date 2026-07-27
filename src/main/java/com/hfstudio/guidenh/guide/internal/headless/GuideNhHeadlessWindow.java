package com.hfstudio.guidenh.guide.internal.headless;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;

import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

/**
 * Handles hiding the client window for headless rendering (screenshots, etc.).
 *
 * <p>Two-tier strategy:
 * <ol>
 *   <li><b>installEarly()</b> — If {@code DisplayEvents} exists (lwjgl3ify &ge; 3.0.21),
 *       registers a pre-window-create listener that sets
 *       {@code SDL_PROP_WINDOW_CREATE_HIDDEN_BOOLEAN} so the window is never shown.
 *   <li><b>hideNow()</b> — After {@code Display.create()} has occurred (the 3.0.20
 *       fallback, or if the early listener was missed), calls
 *       {@code SDL_HideWindow(Display.sdlWindow)} directly.
 * </ol>
 *
 * <p>All failures are logged as warnings only — window hiding must never crash the
 * screenshot pipeline. When JVM property {@code -Dguidenh.headlessRender} is not
 * {@code true}, every method returns immediately with zero side effects.
 *
 * <p>All LWJGL3 / lwjgl3ify classes are accessed reflectively to avoid compile-time
 * dependency on jars that are only present at runtime.
 */
public final class GuideNhHeadlessWindow {

    private static final String PROPERTY_NAME = "guidenh.headlessRender";
    private static final String TAG = "[GuideNhHeadlessWindow]";
    private static final boolean HEADLESS = Boolean.getBoolean(PROPERTY_NAME);

    private GuideNhHeadlessWindow() {}

    /**
     * Earliest hook — call from {@code FMLPreInitializationEvent}.
     *
     * <p>If {@code -Dguidenh.headlessRender=true} and the {@code DisplayEvents} API is
     * present at runtime (lwjgl3ify &ge; 3.0.21), dynamically registers a
     * pre-window-create listener that makes the window invisible from birth.
     */
    public static void installEarly() {
        if (!HEADLESS) {
            return;
        }

        try {
            Class<?> displayEventsClass = Class.forName("me.eigenraven.lwjgl3ify.api.DisplayEvents");
            Method addListener = displayEventsClass.getMethod("addPreWindowCreateListener", Consumer.class);

            // Resolve SDL bindings reflectively
            Class<?> sdlVideo = Class.forName("org.lwjgl.sdl.SDLVideo");
            Class<?> sdlProps = Class.forName("org.lwjgl.sdl.SDLProperties");
            String hiddenKey = (String) sdlVideo.getField("SDL_PROP_WINDOW_CREATE_HIDDEN_BOOLEAN").get(null);
            Method setBoolProp = sdlProps.getMethod(
                "SDL_SetBooleanProperty", int.class, CharSequence.class, boolean.class);

            Object consumerProxy = Proxy.newProxyInstance(
                GuideNhHeadlessWindow.class.getClassLoader(),
                new Class<?>[] { Consumer.class },
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if ("accept".equals(method.getName()) && args != null && args.length == 1) {
                            Object ctx = args[0];
                            int props = (int) ctx.getClass().getMethod("props").invoke(ctx);
                            setBoolProp.invoke(null, props, hiddenKey, true);
                            GuideDebugLog.infoAlways(
                                "{} Set SDL_PROP_WINDOW_CREATE_HIDDEN_BOOLEAN via DisplayEvents listener (props={})",
                                TAG, props);
                        }
                        return null;
                    }
                });

            addListener.invoke(null, consumerProxy);
            GuideDebugLog.infoAlways(
                "{} Registered pre-window-create listener via DisplayEvents API", TAG);
        } catch (ClassNotFoundException e) {
            GuideDebugLog.warnAlways(
                "{} DisplayEvents not found (lwjgl3ify <= 3.0.20), will use fallback hideNow()", TAG);
        } catch (Exception e) {
            GuideDebugLog.warnAlways(
                "{} Failed to register pre-window-create listener: {}", TAG, e.getMessage());
        }
    }

    /**
     * Fallback / second-chance hook — call after {@code Display.create()} has
     * occurred (FML init phase). Hides the SDL window directly.
     */
    public static void hideNow() {
        if (!HEADLESS) {
            return;
        }

        try {
            Class<?> displayClass = Class.forName("org.lwjglx.opengl.Display");
            long window = displayClass.getField("sdlWindow").getLong(null);
            if (window != 0L) {
                Class<?> sdlVideo = Class.forName("org.lwjgl.sdl.SDLVideo");
                sdlVideo.getMethod("SDL_HideWindow", long.class).invoke(null, window);
                GuideDebugLog.infoAlways(
                    "{} Window hidden via SDL_HideWindow (sdlWindow={})", TAG, window);
            } else {
                GuideDebugLog.warnAlways(
                    "{} sdlWindow is 0, cannot hide window", TAG);
            }
        } catch (Exception e) {
            GuideDebugLog.warnAlways(
                "{} Failed to hide window: {}", TAG, e.getMessage());
        }
    }
}
