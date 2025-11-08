package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

public class GuiLockIconButton extends GuiButton {
    private boolean locked = false;

    public GuiLockIconButton(int buttonId, int x, int y) {
        super(buttonId, x, y, 20, 20, "");
    }

    public boolean isLocked() {
        return this.locked;
    }

    public void setLocked(boolean state) {
        this.locked = state;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible) {
            mc.getTextureManager().bindTexture(GuiButton.BUTTON_TEXTURES);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            boolean flag = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
            Icon guilockiconbutton$icon;

            if (this.locked) {
                if (!this.enabled) {
                    guilockiconbutton$icon = Icon.LOCKED_DISABLED;
                } else if (flag) {
                    guilockiconbutton$icon = Icon.LOCKED_HOVER;
                } else {
                    guilockiconbutton$icon = Icon.LOCKED;
                }
            } else if (!this.enabled) {
                guilockiconbutton$icon = Icon.UNLOCKED_DISABLED;
            } else if (flag) {
                guilockiconbutton$icon = Icon.UNLOCKED_HOVER;
            } else {
                guilockiconbutton$icon = Icon.UNLOCKED;
            }

            this.drawTexturedModalRect(this.xPosition, this.yPosition, guilockiconbutton$icon.func_178910_a(), guilockiconbutton$icon.func_178912_b(), this.width, this.height);
        }
    }

    enum Icon {
        LOCKED(0, 146),
        LOCKED_HOVER(0, 166),
        LOCKED_DISABLED(0, 186),
        UNLOCKED(20, 146),
        UNLOCKED_HOVER(20, 166),
        UNLOCKED_DISABLED(20, 186);

        private final int field_178914_g;
        private final int field_178920_h;

        Icon(int p_i45537_3_, int p_i45537_4_) {
            this.field_178914_g = p_i45537_3_;
            this.field_178920_h = p_i45537_4_;
        }

        public int func_178910_a() {
            return this.field_178914_g;
        }

        public int func_178912_b() {
            return this.field_178920_h;
        }
    }
}
