package net.minecraft.client.gui;

import net.minecraft.block.BlockTallGrass;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.FlatGeneratorInfo;
import net.minecraft.world.gen.FlatLayerInfo;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class GuiFlatPresets extends GuiScreen {
    private static final List<LayerItem> FLAT_WORLD_PRESETS = new ArrayList<>();
    private final GuiCreateFlatWorld parentScreen;
    private String presetsTitle;
    private String presetsShare;
    private String field_146436_r;
    private ListSlot field_146435_s;
    private GuiButton field_146434_t;
    private GuiTextField field_146433_u;

    public GuiFlatPresets(GuiCreateFlatWorld p_i46318_1_) {
        this.parentScreen = p_i46318_1_;
    }

    public void initGui() {
        this.buttonList.clear();
        Keyboard.enableRepeatEvents(true);
        this.presetsTitle = I18n.format("createWorld.customize.presets.title");
        this.presetsShare = I18n.format("createWorld.customize.presets.share");
        this.field_146436_r = I18n.format("createWorld.customize.presets.list");
        this.field_146433_u = new GuiTextField(2, this.fontRendererObj, 50, 40, this.width - 100, 20);
        this.field_146435_s = new ListSlot();
        this.field_146433_u.setMaxStringLength(1230);
        this.field_146433_u.setText(this.parentScreen.func_146384_e());
        this.buttonList.add(this.field_146434_t = new GuiButton(0, this.width / 2 - 155, this.height - 28, 150, 20, I18n.format("createWorld.customize.presets.select")));
        this.buttonList.add(new GuiButton(1, this.width / 2 + 5, this.height - 28, 150, 20, I18n.format("gui.cancel")));
        this.func_146426_g();
    }

    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        this.field_146435_s.handleMouseInput();
    }

    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        this.field_146433_u.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (!this.field_146433_u.textboxKeyTyped(typedChar, keyCode)) {
            super.keyTyped(typedChar, keyCode);
        }
    }

    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0 && this.func_146430_p()) {
            this.parentScreen.func_146383_a(this.field_146433_u.getText());
            this.mc.displayGuiScreen(this.parentScreen);
        } else if (button.id == 1) {
            this.mc.displayGuiScreen(this.parentScreen);
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.field_146435_s.drawScreen(mouseX, mouseY, partialTicks);
        this.drawCenteredString(this.fontRendererObj, this.presetsTitle, this.width / 2, 8, 16777215);
        this.drawString(this.fontRendererObj, this.presetsShare, 50, 30, 10526880);
        this.drawString(this.fontRendererObj, this.field_146436_r, 50, 70, 10526880);
        this.field_146433_u.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    public void updateScreen() {
        this.field_146433_u.updateCursorCounter();
        super.updateScreen();
    }

    public void func_146426_g() {
        this.field_146434_t.enabled = this.func_146430_p();
    }

    private boolean func_146430_p() {
        return this.field_146435_s.field_148175_k > -1 && this.field_146435_s.field_148175_k < FLAT_WORLD_PRESETS.size() || this.field_146433_u.getText().length() > 1;
    }

    private static void func_146425_a(String p_146425_0_, Item p_146425_1_, BiomeGenBase p_146425_2_, FlatLayerInfo... p_146425_3_) {
        func_175354_a(p_146425_0_, p_146425_1_, 0, p_146425_2_, null, p_146425_3_);
    }

    private static void func_146421_a(String p_146421_0_, Item p_146421_1_, BiomeGenBase p_146421_2_, List<String> p_146421_3_, FlatLayerInfo... p_146421_4_) {
        func_175354_a(p_146421_0_, p_146421_1_, 0, p_146421_2_, p_146421_3_, p_146421_4_);
    }

    private static void func_175354_a(String p_175354_0_, Item p_175354_1_, int p_175354_2_, BiomeGenBase p_175354_3_, List<String> p_175354_4_, FlatLayerInfo... p_175354_5_) {
        FlatGeneratorInfo flatgeneratorinfo = new FlatGeneratorInfo();

        for (int i = p_175354_5_.length - 1; i >= 0; --i) {
            flatgeneratorinfo.getFlatLayers().add(p_175354_5_[i]);
        }

        flatgeneratorinfo.setBiome(p_175354_3_.biomeID);
        flatgeneratorinfo.func_82645_d();

        if (p_175354_4_ != null) {
            for (String s : p_175354_4_) {
                flatgeneratorinfo.getWorldFeatures().put(s, new HashMap<>());
            }
        }

        FLAT_WORLD_PRESETS.add(new LayerItem(p_175354_1_, p_175354_2_, p_175354_0_, flatgeneratorinfo.toString()));
    }

    static {
        func_146421_a("Classic Flat", Item.getItemFromBlock(Blocks.GRASS), BiomeGenBase.PLAINS, List.of("village"), new FlatLayerInfo(1, Blocks.GRASS), new FlatLayerInfo(2, Blocks.DIRT), new FlatLayerInfo(1, Blocks.BEDROCK));
        func_146421_a("Tunnelers' Dream", Item.getItemFromBlock(Blocks.STONE), BiomeGenBase.EXTREME_HILLS, Arrays.asList("biome_1", "dungeon", "decoration", "stronghold", "mineshaft"), new FlatLayerInfo(1, Blocks.GRASS), new FlatLayerInfo(5, Blocks.DIRT), new FlatLayerInfo(230, Blocks.STONE), new FlatLayerInfo(1, Blocks.BEDROCK));
        func_146421_a("Water World", Items.WATER_BUCKET, BiomeGenBase.DEEP_OCEAN, Arrays.asList("biome_1", "oceanmonument"), new FlatLayerInfo(90, Blocks.WATER), new FlatLayerInfo(5, Blocks.SAND), new FlatLayerInfo(5, Blocks.DIRT), new FlatLayerInfo(5, Blocks.STONE), new FlatLayerInfo(1, Blocks.BEDROCK));
        func_175354_a("Overworld", Item.getItemFromBlock(Blocks.TALL_GRASS), BlockTallGrass.EnumType.GRASS.getMeta(), BiomeGenBase.PLAINS, Arrays.asList("village", "biome_1", "decoration", "stronghold", "mineshaft", "dungeon", "lake", "lava_lake"), new FlatLayerInfo(1, Blocks.GRASS), new FlatLayerInfo(3, Blocks.DIRT), new FlatLayerInfo(59, Blocks.STONE), new FlatLayerInfo(1, Blocks.BEDROCK));
        func_146421_a("Snowy Kingdom", Item.getItemFromBlock(Blocks.SNOW_LAYER), BiomeGenBase.ICE_PLAINS, Arrays.asList("village", "biome_1"), new FlatLayerInfo(1, Blocks.SNOW_LAYER), new FlatLayerInfo(1, Blocks.GRASS), new FlatLayerInfo(3, Blocks.DIRT), new FlatLayerInfo(59, Blocks.STONE), new FlatLayerInfo(1, Blocks.BEDROCK));
        func_146421_a("Bottomless Pit", Items.FEATHER, BiomeGenBase.PLAINS, Arrays.asList("village", "biome_1"), new FlatLayerInfo(1, Blocks.GRASS), new FlatLayerInfo(3, Blocks.DIRT), new FlatLayerInfo(2, Blocks.COBBLESTONE));
        func_146421_a("Desert", Item.getItemFromBlock(Blocks.SAND), BiomeGenBase.DESERT, Arrays.asList("village", "biome_1", "decoration", "stronghold", "mineshaft", "dungeon"), new FlatLayerInfo(8, Blocks.SAND), new FlatLayerInfo(52, Blocks.SANDSTONE), new FlatLayerInfo(3, Blocks.STONE), new FlatLayerInfo(1, Blocks.BEDROCK));
        func_146425_a("Redstone Ready", Items.REDSTONE, BiomeGenBase.DESERT, new FlatLayerInfo(52, Blocks.SANDSTONE), new FlatLayerInfo(3, Blocks.STONE), new FlatLayerInfo(1, Blocks.BEDROCK));
    }

    static class LayerItem {
        public final Item field_148234_a;
        public final int field_179037_b;
        public final String field_148232_b;
        public final String field_148233_c;

        public LayerItem(Item p_i45518_1_, int p_i45518_2_, String p_i45518_3_, String p_i45518_4_) {
            this.field_148234_a = p_i45518_1_;
            this.field_179037_b = p_i45518_2_;
            this.field_148232_b = p_i45518_3_;
            this.field_148233_c = p_i45518_4_;
        }
    }

    class ListSlot extends GuiSlot {
        public int field_148175_k = -1;

        public ListSlot() {
            super(GuiFlatPresets.this.mc, GuiFlatPresets.this.width, GuiFlatPresets.this.height, 80, GuiFlatPresets.this.height - 37, 24);
        }

        private void func_178054_a(int p_178054_1_, int p_178054_2_, Item p_178054_3_, int p_178054_4_) {
            this.func_148173_e(p_178054_1_ + 1, p_178054_2_ + 1);
            GlStateManager.enableRescaleNormal();
            RenderHelper.enableGUIStandardItemLighting();
            GuiFlatPresets.this.itemRender.renderItemIntoGUI(new ItemStack(p_178054_3_, 1, p_178054_4_), p_178054_1_ + 2, p_178054_2_ + 2);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
        }

        private void func_148173_e(int p_148173_1_, int p_148173_2_) {
            this.func_148171_c(p_148173_1_, p_148173_2_, 0, 0);
        }

        private void func_148171_c(int p_148171_1_, int p_148171_2_, int p_148171_3_, int p_148171_4_) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.mc.getTextureManager().bindTexture(Gui.STAT_ICONS);
            float f = 0.0078125F;
            float f1 = 0.0078125F;
            int i = 18;
            int j = 18;
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldrenderer = tessellator.getWorldRenderer();
            worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
            worldrenderer.pos((p_148171_1_), (p_148171_2_ + 18), GuiFlatPresets.this.zLevel).tex(((p_148171_3_) * 0.0078125F), ((p_148171_4_ + 18) * 0.0078125F)).endVertex();
            worldrenderer.pos((p_148171_1_ + 18), (p_148171_2_ + 18), GuiFlatPresets.this.zLevel).tex(((p_148171_3_ + 18) * 0.0078125F), ((p_148171_4_ + 18) * 0.0078125F)).endVertex();
            worldrenderer.pos((p_148171_1_ + 18), (p_148171_2_), GuiFlatPresets.this.zLevel).tex(((p_148171_3_ + 18) * 0.0078125F), ((p_148171_4_) * 0.0078125F)).endVertex();
            worldrenderer.pos((p_148171_1_), (p_148171_2_), GuiFlatPresets.this.zLevel).tex(((p_148171_3_) * 0.0078125F), ((p_148171_4_) * 0.0078125F)).endVertex();
            tessellator.draw();
        }

        protected int getSize() {
            return GuiFlatPresets.FLAT_WORLD_PRESETS.size();
        }

        protected void elementClicked(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY) {
            this.field_148175_k = slotIndex;
            GuiFlatPresets.this.func_146426_g();
            GuiFlatPresets.this.field_146433_u.setText(GuiFlatPresets.FLAT_WORLD_PRESETS.get(GuiFlatPresets.this.field_146435_s.field_148175_k).field_148233_c);
        }

        protected boolean isSelected(int slotIndex) {
            return slotIndex == this.field_148175_k;
        }

        protected void drawBackground() {
        }

        protected void drawSlot(int entryID, int p_180791_2_, int p_180791_3_, int p_180791_4_, int mouseXIn, int mouseYIn) {
            LayerItem guiflatpresets$layeritem = GuiFlatPresets.FLAT_WORLD_PRESETS.get(entryID);
            this.func_178054_a(p_180791_2_, p_180791_3_, guiflatpresets$layeritem.field_148234_a, guiflatpresets$layeritem.field_179037_b);
            GuiFlatPresets.this.fontRendererObj.drawString(guiflatpresets$layeritem.field_148232_b, p_180791_2_ + 18 + 5, p_180791_3_ + 6, 16777215);
        }
    }
}
