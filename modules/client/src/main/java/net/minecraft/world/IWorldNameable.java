package net.minecraft.world;

import net.minecraft.util.chat.IChatComponent;

public interface IWorldNameable {
    String getName();

    boolean hasCustomName();

    IChatComponent getDisplayName();
}
