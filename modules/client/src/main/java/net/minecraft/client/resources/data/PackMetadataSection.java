package net.minecraft.client.resources.data;

import net.minecraft.util.chat.IChatComponent;

public record PackMetadataSection(IChatComponent packDescription, int packFormat) implements IMetadataSection {
}
