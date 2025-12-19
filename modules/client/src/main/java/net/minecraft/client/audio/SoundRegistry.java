package net.minecraft.client.audio;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.RegistrySimple;

import java.util.HashMap;
import java.util.Map;

public class SoundRegistry extends RegistrySimple<ResourceLocation, SoundEventAccessorComposite> {
    private Map<ResourceLocation, SoundEventAccessorComposite> soundRegistry;

    @Override
    protected Map<ResourceLocation, SoundEventAccessorComposite> createUnderlyingMap() {
        this.soundRegistry = new HashMap<>();
        return this.soundRegistry;
    }

    public void registerSound(SoundEventAccessorComposite p_148762_1_) {
        this.putObject(p_148762_1_.getSoundEventLocation(), p_148762_1_);
    }

    public void clearMap() {
        this.soundRegistry.clear();
    }
}
