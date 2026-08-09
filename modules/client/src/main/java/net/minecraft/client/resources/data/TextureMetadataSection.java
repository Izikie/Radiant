package net.minecraft.client.resources.data;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;

public record TextureMetadataSection(boolean textureBlur, boolean textureClamp,
                                     IntList listMipmaps) implements IMetadataSection {

    @Override
    public IntList listMipmaps() {
        return IntLists.unmodifiable(this.listMipmaps);
    }
}
