
package net.minecraft.client.gui.resourcepack;

import com.google.common.collect.Lists;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptionButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.*;
import net.minecraft.util.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// TODO: Implement sorting by search, A-Z, Z-A
public class GuiScreenResourcePacks extends GuiScreen {
    private final GuiScreen parentScreen;
    private final List<ResourcePackListEntry> availableResourcePacks = new ArrayList<>();
    private final List<ResourcePackListEntry> selectedResourcePacks = new ArrayList<>();
    private GuiResourcePackAvailable availableResourcePacksList;
    private GuiResourcePackSelected selectedResourcePacksList;
    private boolean changed = false;

    public GuiScreenResourcePacks(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    public void initGui() {
        this.buttonList.add(new GuiButton(0, this.width / 2 - 204, this.height - 26, 30, 20, "A-Z"));
        this.buttonList.add(new GuiButton(1, this.width / 2 - 204 + 34, this.height - 26, 30, 20, "Z-A"));
        this.buttonList.add(new GuiButton(2, this.width / 2 - 56, this.height - 26, 52, 20, "Refresh"));

        this.buttonList.add(new GuiOptionButton(3, this.width / 2 + 25, this.height - 26, I18n.format("resourcePack.openFolder")));
        this.buttonList.add(new GuiOptionButton(4, this.width / 2 + 25, this.height - 48, I18n.format("gui.done")));

        if (!this.changed) {
            loadResourcePacks();
        }

        setupResourcePackLists();
    }

    private void loadResourcePacks() {
        this.availableResourcePacks.clear();
        this.selectedResourcePacks.clear();
        ResourcePackRepository resourcePackRepository = this.mc.getResourcePackRepository();
        resourcePackRepository.updateRepositoryEntriesAll();
        List<ResourcePackRepository.Entry> allEntries = new ArrayList<>(resourcePackRepository.getRepositoryEntriesAll());
        allEntries.removeAll(resourcePackRepository.getRepositoryEntries());

        for (ResourcePackRepository.Entry entry : allEntries) {
            this.availableResourcePacks.add(new ResourcePackListEntryFound(this, entry));
        }

        for (ResourcePackRepository.Entry entry : Lists.reverse(resourcePackRepository.getRepositoryEntries())) {
            this.selectedResourcePacks.add(new ResourcePackListEntryFound(this, entry));
        }

        this.selectedResourcePacks.add(new ResourcePackListEntryDefault(this));
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
        if (!this.changed) return;

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
        this.mc.refreshResources();
    }

    protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled) return;

        switch (button.id) {
            case 0 -> { // Sort A-Z
            }
            case 1 -> { // Sort Z-A
            }
            case 2 -> { // Refresh
                this.changed = false;
                loadResourcePacks();
                setupResourcePackLists();
            }
            case 3 -> { // Open Resource Pack Folder
                Util.openFolder(mc.getResourcePackRepository().getDirResourcepacks());
            }
            case 4 -> { // Done
                applyResourcePackChanges();
                this.mc.displayGuiScreen(this.parentScreen);
            }
        }
    }

    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        this.selectedResourcePacksList.handleMouseInput();
        this.availableResourcePacksList.handleMouseInput();
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.availableResourcePacksList.mouseClicked(mouseX, mouseY, mouseButton);
        this.selectedResourcePacksList.mouseClicked(mouseX, mouseY, mouseButton);
    }

    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawBackground(0);
        this.availableResourcePacksList.drawScreen(mouseX, mouseY, partialTicks);
        this.selectedResourcePacksList.drawScreen(mouseX, mouseY, partialTicks);
        this.drawCenteredString(this.fontRendererObj, I18n.format("resourcePack.title"), this.width / 2, 16, 16777215);
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
}
