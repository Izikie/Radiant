package org.lwjgl.opengl;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.LWJGLUtil;
import org.lwjgl.glfw.*;
import org.lwjgl.glfw.GLFWVidMode.Buffer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Objects;

public class Display {

    private static final org.lwjgl.opengl.DisplayMode desktop_mode;

    private static String title = "Game";

    private static long handle = MemoryUtil.NULL;

    private static boolean resizable = false;

    private static org.lwjgl.opengl.DisplayMode current_mode;

    private static int width = 0;

    private static int height = 0;

    private static final int x = -1;

    private static final int y = -1;

    private static boolean fullscreen;

    private static boolean window_resized = false;

    private static boolean window_created = false;

    private static GLFWWindowSizeCallback sizeCallback = null;

    private static ByteBuffer[] cached_icons = null;

    static {
        GLFWErrorCallback.createPrint(System.err).set();
        if (GLFW.glfwInit()) {
            new ExceptionInInitializerError("Unable to initialize GLFW");
        }
        GLFWVidMode vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
        desktop_mode = new org.lwjgl.opengl.DisplayMode(vidMode.width(), vidMode.height(), vidMode.redBits() + vidMode.greenBits() + vidMode.blueBits(), vidMode.refreshRate());
        current_mode = desktop_mode;
    }

    private Display() {
    }

    public static org.lwjgl.opengl.DisplayMode getDisplayMode() {
        return current_mode;
    }

    public static void setDisplayMode(org.lwjgl.opengl.DisplayMode mode) throws LWJGLException {
        setDisplayModeAndFullscreen(mode);
    }

    public static void setIcon(ByteBuffer[] icons) {
        if (cached_icons != icons) {
            cached_icons = new ByteBuffer[icons.length];
            for (int i = 0; i < icons.length; i++) {
                cached_icons[i] = BufferUtils.createByteBuffer(icons[i].capacity());
                int old_position = icons[i].position();
                cached_icons[i].put(icons[i]);
                icons[i].position(old_position);
                cached_icons[i].flip();
            }
        }

        if (isCreated()) GLFW.glfwSetWindowIcon(handle, iconsToGLFWBuffer(cached_icons));
    }

    public static org.lwjgl.opengl.DisplayMode getDesktopDisplayMode() {
        return desktop_mode;
    }

    private static GLFWImage.Buffer iconsToGLFWBuffer(ByteBuffer[] icons) {
        GLFWImage.Buffer buffer = GLFWImage.create(icons.length);
        for (ByteBuffer icon : icons) {
            int size = icon.limit() / 4;
            int dimension = (int) Math.sqrt(size);
            try (GLFWImage image = GLFWImage.malloc()) {
                buffer.put(image.set(dimension, dimension, icon));
            }
        }
        buffer.flip();
        return buffer;
    }

    public static void update() {
        update(true);
    }

    public static void update(boolean processMessages) {
        window_resized = false;
        GLFW.glfwPollEvents();
        if (processMessages) {
            if (Mouse.isCreated()) {
                Mouse.poll();
            }
            if (Keyboard.isCreated()) {
                Keyboard.poll();
            }
        }
        swapBuffers();
    }

    public static void swapBuffers() {
        GLFW.glfwSwapBuffers(handle);
    }

    public static void create() throws LWJGLException {
        create(new PixelFormat());
    }

    public static void create(PixelFormat pixelFormat) throws LWJGLException {

        if (GLFW.glfwPlatformSupported(GLFW.GLFW_PLATFORM_WAYLAND)) {
            GLFW.glfwInitHint(GLFW.GLFW_PLATFORM, GLFW.GLFW_PLATFORM_WAYLAND);
        }

        if (GLFW.glfwGetPlatform() == GLFW.GLFW_PLATFORM_WAYLAND) {
            GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
        }

        GLFW.glfwWindowHint(GLFW.GLFW_ACCUM_ALPHA_BITS, pixelFormat.getAccumulationBitsPerPixel());
        GLFW.glfwWindowHint(GLFW.GLFW_ALPHA_BITS, pixelFormat.getAlphaBits());
        GLFW.glfwWindowHint(GLFW.GLFW_AUX_BUFFERS, pixelFormat.getAuxBuffers());
        GLFW.glfwWindowHint(GLFW.GLFW_DEPTH_BITS, pixelFormat.getDepthBits());
        GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES, pixelFormat.getSamples());
        GLFW.glfwWindowHint(GLFW.GLFW_STENCIL_BITS, pixelFormat.getStencilBits());
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        handle = GLFW.glfwCreateWindow(current_mode.getWidth(), current_mode.getHeight(), title, MemoryUtil.NULL, MemoryUtil.NULL);
        if (handle == MemoryUtil.NULL) {
            throw new LWJGLException("Display could not be created");
        }
        sizeCallback = GLFWWindowSizeCallback.create(Display::resizeCallback);
        GLFW.glfwSetWindowSizeCallback(handle, sizeCallback);
        GLFW.glfwMakeContextCurrent(handle);
        createWindow();
        GL.createCapabilities();
    }

    public static void setDisplayModeAndFullscreen(org.lwjgl.opengl.DisplayMode mode) {
        setDisplayModeAndFullscreenInternal(mode.isFullscreenCapable(), mode);
    }

    private static void setDisplayModeAndFullscreenInternal(boolean fullscreen, org.lwjgl.opengl.DisplayMode mode) {
        org.lwjgl.opengl.DisplayMode old_mode = current_mode;
        current_mode = mode;
        boolean was_fullscreen = isFullscreen();
        Display.fullscreen = fullscreen;
        if (was_fullscreen != isFullscreen() || !mode.equals(old_mode)) {
            if (!isCreated())
                return;
            destroyWindow();
            createWindow();
        }
    }

    private static void createWindow() {
        if (isCreated()) {
            return;
        }
        // Configure GLFW
        GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_RESIZABLE, resizable ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        window_created = true;

        if (isFullscreen()) {
            GLFW.glfwSetWindowMonitor(handle, GLFW.glfwGetPrimaryMonitor(), x, y, current_mode.getWidth(), current_mode.getHeight(), current_mode.getFrequency());
        } else {
            GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, 1);
            GLFW.glfwSetWindowMonitor(handle, MemoryUtil.NULL, x, y, current_mode.getWidth(), current_mode.getHeight(), current_mode.getFrequency());
        }

        width = current_mode.getWidth();
        height = current_mode.getHeight();
        GLFW.glfwSetWindowPos(handle, getWindowX(), getWindowY());
        // create general callbacks
        initControls();

        // set cached window icon if exists
        setIcon(Objects.requireNonNullElseGet(cached_icons, () -> new ByteBuffer[]{LWJGLUtil.LWJGLIcon32x32, LWJGLUtil.LWJGLIcon16x16}));
        GLFW.glfwShowWindow(handle);
        GLFW.glfwFocusWindow(handle);
    }

    static boolean getPrivilegedBoolean(final String property_name) {
        return Boolean.getBoolean(property_name);
    }

    private static void initControls() {
        if (!getPrivilegedBoolean("org.org.lwjgl.opengl.Display.noinput")) {
            if (!Mouse.isCreated() && !getPrivilegedBoolean("org.org.lwjgl.opengl.Display.nomouse")) {
                try {
                    Mouse.create();
                } catch (LWJGLException e) {
                    e.printStackTrace(System.err);
                }
            }
            if (!Keyboard.isCreated() && !getPrivilegedBoolean("org.org.lwjgl.opengl.Display.nokeyboard")) {
                try {
                    Keyboard.create();
                } catch (LWJGLException e) {
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    public static org.lwjgl.opengl.DisplayMode[] getAvailableDisplayModes() {
        long primaryMonitor = GLFW.glfwGetPrimaryMonitor();
        if (primaryMonitor == MemoryUtil.NULL) {
            return new org.lwjgl.opengl.DisplayMode[0];
        }

        Buffer videoModes = GLFW.glfwGetVideoModes(primaryMonitor);
        if (videoModes == null || videoModes.capacity() == 0) {
            return new org.lwjgl.opengl.DisplayMode[0];
        }

        HashSet<org.lwjgl.opengl.DisplayMode> modes = new HashSet<>(videoModes.capacity());
        for (int i = 0; i < videoModes.capacity(); i++) {
            GLFWVidMode mode = videoModes.get(i);
            modes.add(new org.lwjgl.opengl.DisplayMode(
                    mode.width(),
                    mode.height(),
                    mode.redBits() + mode.blueBits() + mode.greenBits(),
                    mode.refreshRate()
            ));
        }

        org.lwjgl.opengl.DisplayMode[] filteredModes = new DisplayMode[modes.size()];
        return modes.toArray(filteredModes);
    }

    private static void resizeCallback(long window, int width, int height) {
        if (window == handle) {
            window_resized = true;
            Display.width = width;
            Display.height = height;
        }
    }

    private static void destroyWindow() {
        if (!isCreated()) {
            return;
        }
        if (Mouse.isCreated()) {
            Mouse.destroy();
        }
        if (Keyboard.isCreated()) {
            Keyboard.destroy();
        }
        // Hide the window while maintaining it's context
        //GLFW.glfwHideWindow(handle);
        window_created = false;
    }

    public static void destroy() {
        destroyWindow();
        sizeCallback.free();
        GLFW.glfwTerminate();
        try (GLFWErrorCallback callback = GLFW.glfwSetErrorCallback(null)) {
            if (callback != null) {
                callback.free();
            }
        }
    }

    public static boolean isCreated() {
        return window_created;
    }

    public static boolean isCloseRequested() {
        return GLFW.glfwWindowShouldClose(handle);
    }

    public static boolean isActive() {
        return GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_ICONIFIED) == 0;
    }

    public static void setResizable(boolean isResizable) {
        resizable = isResizable;
        if (isCreated()) {
            GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, resizable ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        }
    }

    public static void sync(int fps) {
        Sync.sync(fps);
    }

    public static void setTitle(String newTitle) {
        title = newTitle;
        if (isCreated()) GLFW.glfwSetWindowTitle(handle, title);
    }

    public static void setVSyncEnabled(boolean enabled) {
        if (!isCreated()) {
            return;
        }
        GLFW.glfwSwapInterval(enabled ? 1 : 0);
    }

    private static int getWindowX() {
        if (!isFullscreen()) {
            // if no display location set, center window
            return Math.max(0, (desktop_mode.getWidth() - current_mode.getWidth()) / 2);
        } else {
            return 0;
        }
    }

    private static int getWindowY() {
        if (!isFullscreen()) {
            // if no display location set, center window
            return Math.max(0, (desktop_mode.getHeight() - current_mode.getHeight()) / 2);
        } else {
            return 0;
        }
    }

    public static int getX() {
        if (isFullscreen()) {
            return 0;
        }
        return x;
    }

    public static int getY() {
        if (isFullscreen()) {
            return 0;
        }
        return y;
    }

    public static void setFullscreen(boolean fullscreen) {
        setDisplayModeAndFullscreenInternal(fullscreen, current_mode);
    }

    public static boolean wasResized() {
        return window_resized;
    }

    public static String getTitle() {
        return Display.title;
    }

    public static long getHandle() {
        return Display.handle;
    }

    public static int getWidth() {
        return Display.width;
    }

    public static int getHeight() {
        return Display.height;
    }

    public static boolean isFullscreen() {
        return Display.fullscreen;
    }
}