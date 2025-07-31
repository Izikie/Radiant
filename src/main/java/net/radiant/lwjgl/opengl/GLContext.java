package net.radiant.lwjgl.opengl;


public class GLContext {

    private static ContextCapabilities contextCapabilities = new ContextCapabilities();

    public static ContextCapabilities getCapabilities() {
        return contextCapabilities;
    }
}
