package net.minecraft.client.gui.resourcepack.api;

import net.minecraft.client.resources.ResourcePackListEntry;

import java.util.function.Predicate;

public class ResourcePackFilter implements Predicate<ResourcePackListEntry> {

    private final String buffer;

    public ResourcePackFilter(String buffer) {
        this.buffer = buffer.toLowerCase();
    }

    @Override
    public boolean test(ResourcePackListEntry entry) {
        String name = stripColorCodes(entry.getName()).toLowerCase();
        return name.contains(buffer);
    }

    private String stripColorCodes(String text) {
        return text.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
    }
}