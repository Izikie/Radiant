
package net.minecraft.client.gui.resourcepack;

import com.google.common.collect.Lists;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.resourcepack.api.GuiResourcePackAvailable;
import net.minecraft.client.gui.resourcepack.api.GuiResourcePackSelected;
import net.minecraft.client.resources.*;
import net.minecraft.util.Util;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

// TODO: Implement sorting by search, A-Z, Z-A
public class GuiScreenResourcePacks extends GuiScreen {

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

    public void initGui() {
        this.buttonList.add(new GuiButton(0, this.width / 2 - 204, this.height - 26, 30, 20, "A-Z"));
        this.buttonList.add(new GuiButton(1, this.width / 2 - 204 + 34, this.height - 26, 30, 20, "Z-A"));
        this.buttonList.add(new GuiButton(2, this.width / 2 - 56, this.height - 26, 52, 20, "Refresh"));

        this.buttonList.add(new GuiOptionButton(3, this.width / 2 + 25, this.height - 26, I18n.format("resourcePack.openFolder")));
        this.buttonList.add(new GuiOptionButton(4, this.width / 2 + 25, this.height - 48, I18n.format("gui.done")));

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
        loadingFuture = repository.loadFromCacheAndCheckChangesProgressive(entry -> {
            this.mc.addScheduledTask(() -> addResourcePackEntry(entry));
        }).thenRun(() -> {
            this.mc.addScheduledTask(() -> {
                this.isLoading = false;
            });
        }).exceptionally(throwable -> {
            LoggerFactory.getLogger(GuiScreenResourcePacks.class).warn("Cache loading failed, falling back to sync", throwable);
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
            List<ResourcePackRepository.Entry> correctOrder = Lists.reverse(selectedEntries);
            int myIndex = correctOrder.indexOf(entry);

            boolean inserted = false;

            for (int i = 0; i < this.selectedResourcePacks.size(); i++) {
                ResourcePackListEntry existing = this.selectedResourcePacks.get(i);

                if (existing instanceof ResourcePackListEntryFound) {
                    ResourcePackRepository.Entry existingEntry = ((ResourcePackListEntryFound) existing)
                            .func_148318_i();
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

        for (ResourcePackRepository.Entry entry : Lists.reverse(resourcePackRepository.getRepositoryEntries())) {
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
        if (!this.changed)
            return;

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

    protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled)
            return;

        switch (button.id) {
            case 0 -> { // Sort A-Z
            }
            case 1 -> { // Sort Z-A
            }
            case 2 -> { // Refresh
                if (!isLoading) {
                    this.changed = false;
                    loadResourcePacks();
                }
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
        searchBox.mouseClicked(mouseX, mouseY, mouseButton);
        this.availableResourcePacksList.mouseClicked(mouseX, mouseY, mouseButton);
        this.selectedResourcePacksList.mouseClicked(mouseX, mouseY, mouseButton);
    }

    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (mc.world != null) {
            this.drawDefaultBackground();
        } else {
            this.drawBackground(0);
        }

        this.availableResourcePacksList.drawScreen(mouseX, mouseY, partialTicks);
        this.selectedResourcePacksList.drawScreen(mouseX, mouseY, partialTicks);
        
        if (isLoading) {
            Gui.drawCenteredString(this.fontRendererObj, "Loading...", this.width / 2, this.height - 60, 8421504);
        }

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
}
