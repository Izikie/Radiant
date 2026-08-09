package net.minecraft.client.resources.data;

import net.minecraft.client.resources.Language;

import java.util.Collection;

public record LanguageMetadataSection(Collection<Language> languages) implements IMetadataSection {
}
