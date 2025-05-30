package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.network.LanServerDetector;

import java.util.ArrayList;
import java.util.List;

public class ServerSelectionList extends GuiListExtended {
    private final GuiMultiplayer owner;
    private final List<ServerListEntryNormal> serverListInternet = new ArrayList<>();
    private final List<ServerListEntryLanDetected> serverListLan = new ArrayList<>();
    private final IGuiListEntry lanScanEntry = new ServerListEntryLanScan();
    private int selectedSlotIndex = -1;

    public ServerSelectionList(GuiMultiplayer ownerIn, Minecraft mcIn, int widthIn, int heightIn, int topIn, int bottomIn, int slotHeightIn) {
        super(mcIn, widthIn, heightIn, topIn, bottomIn, slotHeightIn);
        this.owner = ownerIn;
    }

    public IGuiListEntry getListEntry(int index) {
        if (index < this.serverListInternet.size()) {
            return this.serverListInternet.get(index);
        } else {
            index = index - this.serverListInternet.size();

            if (index == 0) {
                return this.lanScanEntry;
            } else {
                --index;
                return this.serverListLan.get(index);
            }
        }
    }

    protected int getSize() {
        return this.serverListInternet.size() + 1 + this.serverListLan.size();
    }

    public void setSelectedSlotIndex(int selectedSlotIndexIn) {
        this.selectedSlotIndex = selectedSlotIndexIn;
    }

    protected boolean isSelected(int slotIndex) {
        return slotIndex == this.selectedSlotIndex;
    }

    public int func_148193_k() {
        return this.selectedSlotIndex;
    }

    public void func_148195_a(ServerList p_148195_1_) {
        this.serverListInternet.clear();

        for (int i = 0; i < p_148195_1_.countServers(); ++i) {
            this.serverListInternet.add(new ServerListEntryNormal(this.owner, p_148195_1_.getServerData(i)));
        }
    }

    public void func_148194_a(List<LanServerDetector.LanServer> p_148194_1_) {
        this.serverListLan.clear();

        for (LanServerDetector.LanServer lanserverdetector$lanserver : p_148194_1_) {
            this.serverListLan.add(new ServerListEntryLanDetected(this.owner, lanserverdetector$lanserver));
        }
    }

    protected int getScrollBarX() {
        return super.getScrollBarX() + 30;
    }

    public int getListWidth() {
        return super.getListWidth() + 85;
    }
}
