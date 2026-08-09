package net.minecraft.tileentity;

public class TileEntityDropper extends TileEntityDispenser {
    @Override
    public String getName() {
        return this.hasCustomName() ? this.customName : "container.dropper";
    }

    @Override
    public String guiID() {
        return "minecraft:dropper";
    }
}
