package net.minecraft.client.renderer;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import java.util.Collection;

public abstract class InventoryEffectRenderer extends GuiContainer {
    public InventoryEffectRenderer(Container inventorySlotsIn) {
        super(inventorySlotsIn);
    }

    public void initGui() {
        super.initGui();
        this.updateActivePotionEffects();
    }

    // IMPROVEMENT: 1.7 Potion Positioning
    protected void updateActivePotionEffects() {
        this.guiLeft = (this.width - this.xSize) / 2;
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        if (this.hasActivePotionEffects()) {
            this.drawActivePotionEffects();
        }
    }

    private void drawActivePotionEffects() {
        int x = this.guiLeft - 124;
        int y = this.guiTop;
        Collection<PotionEffect> collection = this.mc.player.getActivePotionEffects();

        if (collection.isEmpty())
            return;

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableLighting();
        int l = 33;

        if (collection.size() > 5) {
            l = 132 / (collection.size() - 1);
        }

        for (PotionEffect effect : this.mc.player.getActivePotionEffects()) {
            Potion potion = Potion.POTION_TYPES[effect.getPotionID()];
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.mc.getTextureManager().bindTexture(INVENTORY_BACKGROUND);
            this.drawTexturedModalRect(x, y, 0, 166, 140, 32);

            if (potion.hasStatusIcon()) {
                int iconIndex = potion.getStatusIconIndex();
                this.drawTexturedModalRect(x + 6, y + 7, iconIndex % 8 * 18, 198 + iconIndex / 8 * 18, 18, 18);
            }

            String name = I18n.format(potion.getName());

            if (effect.getAmplifier() == 1) {
                name += " " + I18n.format("enchantment.level.2");
            } else if (effect.getAmplifier() == 2) {
                name += " " + I18n.format("enchantment.level.3");
            } else if (effect.getAmplifier() == 3) {
                name += " " + I18n.format("enchantment.level.4");
            }

            this.fontRendererObj.drawStringWithShadow(name, (x + 10 + 18), (y + 6), 16777215);
            String duration = Potion.getDurationString(effect);
            this.fontRendererObj.drawStringWithShadow(duration, (x + 10 + 18), (y + 6 + 10), 8355711);
            y += l;
        }
    }

    private boolean hasActivePotionEffects() {
        return !this.mc.player.getActivePotionEffects().isEmpty();
    }
}
