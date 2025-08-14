package net.minecraft.util.input;

import net.minecraft.client.settings.KeyBinding;
import net.radiant.lwjgl.input.Keyboard;
import net.radiant.lwjgl.input.Mouse;
import net.radiant.lwjgl.opengl.Display;

public class MouseHelper {
    public int deltaX;
    public int deltaY;

    public void grabMouseCursor() {
        this.updateKeyBindState();
        Mouse.setGrabbed(true);
        this.deltaX = 0;
        this.deltaY = 0;
    }

    public void ungrabMouseCursor() {
        Mouse.setCursorPosition(Display.getWidth() / 2, Display.getHeight() / 2);
        Mouse.setGrabbed(false);
    }

    public void mouseXYChange() {
        this.deltaX = Mouse.getDX();
        this.deltaY = Mouse.getDY();
    }

    // IMPROVEMENT: Modern KeyBind Handling
    private void updateKeyBindState() {
        for (KeyBinding keybinding : KeyBinding.getKeybindArray()) {
            try {
                final int keyCode = keybinding.getKeyCode();
                KeyBinding.setKeyBindState(keyCode, keyCode < 256 && Keyboard.isKeyDown(keyCode));
            } catch (IndexOutOfBoundsException ignored) {
            }
        }
    }
}
