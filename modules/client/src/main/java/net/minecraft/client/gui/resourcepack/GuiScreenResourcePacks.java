package net.minecraft.client.gui.resourcepack;


import net.minecraft.client.gui.*;
import net.minecraft.client.gui.resourcepack.api.GuiResourcePackAvailable;
import net.minecraft.client.gui.resourcepack.api.GuiResourcePackSelected;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.*;
import net.minecraft.util.Util;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

// TODO: fix the top/bottom header thing like controls
public class GuiScreenResourcePacks extends GuiScreen {

    private static final Logger LOGGER = LoggerFactory.getLogger(GuiScreenResourcePacks.class);

    private final GuiScreen parentScreen;
    private final List<ResourcePackListEntry> availableResourcePacks = new ArrayList<>();
    private final List<ResourcePackListEntry> selectedResourcePacks = new ArrayList<>();
    private GuiResourcePackAvailable availableResourcePacksList;
    private GuiResourcePackSelected selectedResourcePacksList;
    private boolean changed = false;
    private boolean isLoading = false;
    private CompletableFuture<Void> loadingFuture = null;
    public static GuiTextField searchBox;

    public GuiScreenResourcePacks(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        this.buttonList.add(new GuiButton(0, this.width / 2 - 56, this.height - 26, 52, 20, "Refresh"));

        this.buttonList.add(new GuiOptionButton(1, this.width / 2 + 25, this.height - 26, I18n.format("resourcePack.openFolder")));
        this.buttonList.add(new GuiOptionButton(2, this.width / 2 + 25, this.height - 48, I18n.format("gui.done")));

        searchBox = new GuiTextField(5, this.fontRendererObj, this.width / 2 - 204, this.height - 46, 200, 16);

        if (!this.changed) {
            loadResourcePacks();
        }

        setupResourcePackLists();
    }

    private void loadResourcePacks() {
        if (isLoading) {
            return;
        }

        this.isLoading = true;
        this.availableResourcePacks.clear();
        this.selectedResourcePacks.clear();

        this.selectedResourcePacks.add(new ResourcePackListEntryDefault(this));
        setupResourcePackLists();

        if (loadingFuture != null) {
            loadingFuture.cancel(true);
        }

        ResourcePackRepository repository = this.mc.getResourcePackRepository();
        loadingFuture = repository
                .loadFromCacheAndCheckChangesProgressive(entry -> this.mc.addScheduledTask(() -> addResourcePackEntry(entry)))
                .thenRun(() -> this.mc.addScheduledTask(() -> this.isLoading = false))
                .exceptionally(throwable -> {
                    LOGGER.warn("Cache loading failed, falling back to sync", throwable);
                    this.mc.addScheduledTask(() -> {
                        loadResourcePacksSync();
                        this.isLoading = false;
                    });
                    return null;
                });
    }

    private void addResourcePackEntry(ResourcePackRepository.Entry entry) {
        ResourcePackListEntry listEntry = new ResourcePackListEntryFound(this, entry);

        List<ResourcePackRepository.Entry> selectedEntries = this.mc.getResourcePackRepository().getRepositoryEntries();
        if (selectedEntries.contains(entry)) {
            List<ResourcePackRepository.Entry> correctOrder = selectedEntries.reversed();
            int myIndex = correctOrder.indexOf(entry);

            boolean inserted = false;

            for (int i = 0; i < this.selectedResourcePacks.size(); i++) {
                ResourcePackListEntry existing = this.selectedResourcePacks.get(i);

                if (existing instanceof ResourcePackListEntryFound existingFound) {
                    ResourcePackRepository.Entry existingEntry = existingFound.func_148318_i();
                    int otherIndex = correctOrder.indexOf(existingEntry);

                    if (otherIndex > myIndex) {
                        this.selectedResourcePacks.add(i, listEntry);
                        inserted = true;
                        break;
                    }
                } else if (existing instanceof ResourcePackListEntryDefault) {
                    this.selectedResourcePacks.add(i, listEntry);
                    inserted = true;
                    break;
                }
            }

            if (!inserted) {
                this.selectedResourcePacks.add(listEntry);
            }
        } else {
            this.availableResourcePacks.add(listEntry);
        }
    }

    private void loadResourcePacksSync() {
        ResourcePackRepository resourcePackRepository = this.mc.getResourcePackRepository();
        resourcePackRepository.updateRepositoryEntriesAll();
        populateResourcePackLists();
    }

    private void populateResourcePackLists() {
        ResourcePackRepository resourcePackRepository = this.mc.getResourcePackRepository();
        List<ResourcePackRepository.Entry> allEntries = new ArrayList<>(resourcePackRepository.getRepositoryEntriesAll());
        allEntries.removeAll(resourcePackRepository.getRepositoryEntries());

        for (ResourcePackRepository.Entry entry : allEntries) {
            this.availableResourcePacks.add(new ResourcePackListEntryFound(this, entry));
        }

        for (ResourcePackRepository.Entry entry : resourcePackRepository.getRepositoryEntries().reversed()) {
            this.selectedResourcePacks.add(new ResourcePackListEntryFound(this, entry));
        }

        this.selectedResourcePacks.add(new ResourcePackListEntryDefault(this));

        setupResourcePackLists();
    }

    private void setupResourcePackLists() {
        this.availableResourcePacksList = new GuiResourcePackAvailable(this.mc, 200, this.height, this.availableResourcePacks);
        this.availableResourcePacksList.setSlotXBoundsFromLeft(this.width / 2 - 4 - 200);
        this.availableResourcePacksList.registerScrollButtons(7, 8);

        this.selectedResourcePacksList = new GuiResourcePackSelected(this.mc, 200, this.height, this.selectedResourcePacks);
        this.selectedResourcePacksList.setSlotXBoundsFromLeft(this.width / 2 + 4);
        this.selectedResourcePacksList.registerScrollButtons(7, 8);
    }

    private void applyResourcePackChanges() {
        if (!this.changed) {
            return;
        }

        List<ResourcePackRepository.Entry> entries = new ArrayList<>();
        for (ResourcePackListEntry entry : this.selectedResourcePacks) {
            if (entry instanceof ResourcePackListEntryFound foundEntry) {
                entries.add(foundEntry.func_148318_i());
            }
        }

        Collections.reverse(entries);
        this.mc.getResourcePackRepository().setRepositories(entries);
        this.mc.gameSettings.resourcePacks.clear();
        this.mc.gameSettings.incompatibleResourcePacks.clear();

        for (ResourcePackRepository.Entry entry : entries) {
            this.mc.gameSettings.resourcePacks.add(entry.getResourcePackName());

            if (entry.func_183027_f() != 1) {
                this.mc.gameSettings.incompatibleResourcePacks.add(entry.getResourcePackName());
            }
        }

        this.mc.gameSettings.saveOptions();
        this.mc.refreshResourcesFromCache();
    }

    @Override
    public void updateScreen() {
        searchBox.updateCursorCounter();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        searchBox.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled) {
            return;
        }

        switch (button.id) {
            case 0 -> { // Refresh
                if (!isLoading) {
                    this.changed = false;
                    loadResourcePacks();
                }
            }
            // Open Resource Pack Folder
            case 1 -> Util.openFolder(mc.getResourcePackRepository().getDirResourcepacks());
            case 2 -> { // Done
                applyResourcePackChanges();
                this.mc.displayGuiScreen(this.parentScreen);
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        this.selectedResourcePacksList.handleMouseInput();
        this.availableResourcePacksList.handleMouseInput();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        searchBox.mouseClicked(mouseX, mouseY, mouseButton);
        this.availableResourcePacksList.mouseClicked(mouseX, mouseY, mouseButton);
        this.selectedResourcePacksList.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        this.availableResourcePacksList.drawScreen(mouseX, mouseY, partialTicks);
        this.selectedResourcePacksList.drawScreen(mouseX, mouseY, partialTicks);

        Tessellator tessellator = Tessellator.get();
        WorldRenderer renderer = tessellator.getWorldRenderer();

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ZERO, GL11.GL_ONE);
        GlStateManager.disableAlpha();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.disableTexture2D();

        int i1 = 4; // Magic BS SweedNumber
        int bottom = this.height - 50;

        // Top Gradient
        renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        renderer.pos(0, 32 + i1, 0.0D).tex(0.0D, 1.0D).color(0, 0, 0, 0).endVertex();
        renderer.pos(this.width, 32 + i1, 0.0D).tex(1.0D, 1.0D).color(0, 0, 0, 0).endVertex();
        renderer.pos(this.width, 32, 0.0D).tex(1.0D, 0.0D).color(0, 0, 0, 255).endVertex();
        renderer.pos(0, 32, 0.0D).tex(0.0D, 0.0D).color(0, 0, 0, 255).endVertex();
        tessellator.draw();

        // Bottom Gradient
        renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        renderer.pos(0, bottom, 0.0D).tex(0.0D, 1.0D).color(0, 0, 0, 255).endVertex();
        renderer.pos(this.width, bottom, 0.0D).tex(1.0D, 1.0D).color(0, 0, 0, 255).endVertex();
        renderer.pos(this.width, bottom - i1, 0.0D).tex(1.0D, 0.0D).color(0, 0, 0, 0).endVertex();
        renderer.pos(0, bottom - i1, 0.0D).tex(0.0D, 0.0D).color(0, 0, 0, 0).endVertex();
        tessellator.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();

        this.overlayBackground(0, 0, this.width, 32, 255, 255);
        this.overlayBackground(0, bottom, this.width, this.height, 255, 255);

        searchBox.drawTextBox();

        Gui.drawCenteredString(this.fontRendererObj, I18n.format("resourcePack.title"), this.width / 2, 16, 16777215);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    public boolean hasResourcePackEntry(ResourcePackListEntry entry) {
        return this.selectedResourcePacks.contains(entry);
    }

    public List<ResourcePackListEntry> getListContaining(ResourcePackListEntry entry) {
        return this.hasResourcePackEntry(entry) ? this.selectedResourcePacks : this.availableResourcePacks;
    }

    public List<ResourcePackListEntry> getAvailableResourcePacks() {
        return this.availableResourcePacks;
    }

    public List<ResourcePackListEntry> getSelectedResourcePacks() {
        return this.selectedResourcePacks;
    }

    public void markChanged() {
        this.changed = true;
    }

    protected void overlayBackground(int startX, int startY, int endX, int endY, int startAlpha, int endAlpha) {
        Tessellator tessellator = Tessellator.get();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        this.mc.getTextureManager().bindTexture(Gui.OPTIONS_BACKGROUND);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        worldrenderer.pos(startX, endY, 0.0D).tex(0.0D, endY / 32.0F).color(64, 64, 64, endAlpha).endVertex();
        worldrenderer.pos(startX + this.width, endY, 0.0D).tex(this.width / 32.0F, endY / 32.0F).color(64, 64, 64, endAlpha).endVertex();
        worldrenderer.pos(startX + this.width, startY, 0.0D).tex(this.width / 32.0F, startY / 32.0F).color(64, 64, 64, startAlpha).endVertex();
        worldrenderer.pos(startX, startY, 0.0D).tex(0.0D, startY / 32.0F).color(64, 64, 64, startAlpha).endVertex();
        tessellator.draw();
    }
}
