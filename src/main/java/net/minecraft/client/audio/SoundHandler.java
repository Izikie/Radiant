package net.minecraft.client.audio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

public class SoundHandler implements IResourceManagerReloadListener, ITickable {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoundHandler.class);
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(SoundList.class, new SoundListSerializer())
            .create();
    private static final ParameterizedType TYPE = new ParameterizedType() {
        @Override
        public Type[] getActualTypeArguments() {
            return new Type[]{String.class, SoundList.class};
        }

        @Override
        public Type getRawType() {
            return Map.class;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    };
    public static final SoundPoolEntry MISSING_SOUND = new SoundPoolEntry(new ResourceLocation("meta:missing_sound"), 0.0D, 0.0D, false);
    private final SoundRegistry sndRegistry = new SoundRegistry();
    private final SoundManager sndManager;
    private final IResourceManager mcResourceManager;

    public SoundHandler(IResourceManager manager, GameSettings gameSettingsIn) {
        this.mcResourceManager = manager;
        this.sndManager = new SoundManager(this, gameSettingsIn);
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        this.sndManager.reloadSoundSystem();
        this.sndRegistry.clearMap();

        for (String s : resourceManager.getResourceDomains()) {
            try {
                for (IResource iresource : resourceManager.getAllResources(new ResourceLocation(s, "sounds.json"))) {
                    try {
                        Map<String, SoundList> map = this.getSoundMap(iresource.getInputStream());

                        for (Entry<String, SoundList> entry : map.entrySet()) {
                            this.loadSoundResource(new ResourceLocation(s, entry.getKey()), entry.getValue());
                        }
                    } catch (RuntimeException exception) {
                        LOGGER.warn("Invalid sounds.json", exception);
                    }
                }
            } catch (IOException exception) {
            }
        }
    }

    protected Map<String, SoundList> getSoundMap(InputStream stream) {
        Map<String, SoundList> map;

        try {
            map = GSON.fromJson(new InputStreamReader(stream), TYPE);
        } finally {
            IOUtils.closeQuietly(stream);
        }

        return map;
    }

    private void loadSoundResource(ResourceLocation location, SoundList sounds) {
        boolean flag = !this.sndRegistry.containsKey(location);
        SoundEventAccessorComposite soundeventaccessorcomposite;

        if (!flag && !sounds.canReplaceExisting()) {
            soundeventaccessorcomposite = this.sndRegistry.getObject(location);
        } else {
            if (!flag) {
                LOGGER.debug("Replaced sound event location {}", location);
            }

            soundeventaccessorcomposite = new SoundEventAccessorComposite(location, 1.0D, 1.0D, sounds.getSoundCategory());
            this.sndRegistry.registerSound(soundeventaccessorcomposite);
        }

        for (final SoundList.SoundEntry soundlist$soundentry : sounds.getSoundList()) {
            String s = soundlist$soundentry.getSoundEntryName();
            ResourceLocation resourcelocation = new ResourceLocation(s);
            final String s1 = s.contains(":") ? resourcelocation.getResourceDomain() : location.getResourceDomain();
            ISoundEventAccessor<SoundPoolEntry> isoundeventaccessor;

            switch (soundlist$soundentry.getSoundEntryType()) {
                case FILE:
                    ResourceLocation resourcelocation1 = new ResourceLocation(s1, "sounds/" + resourcelocation.getResourcePath() + ".ogg");
                    InputStream inputstream = null;

                    try {
                        inputstream = this.mcResourceManager.getResource(resourcelocation1).getInputStream();
                    } catch (FileNotFoundException exception) {
                        LOGGER.warn("File {} does not exist, cannot add it to event {}", resourcelocation1, location);
                        continue;
                    } catch (IOException exception) {
                        LOGGER.warn("Could not load sound file {}, cannot add it to event {}", resourcelocation1, location, exception);
                        continue;
                    } finally {
                        IOUtils.closeQuietly(inputstream);
                    }

                    isoundeventaccessor = new SoundEventAccessor(new SoundPoolEntry(resourcelocation1, soundlist$soundentry.getSoundEntryPitch(), soundlist$soundentry.getSoundEntryVolume(), soundlist$soundentry.isStreaming()), soundlist$soundentry.getSoundEntryWeight());
                    break;

                case SOUND_EVENT:
                    isoundeventaccessor = new ISoundEventAccessor<>() {
                        final ResourceLocation field_148726_a = new ResourceLocation(s1, soundlist$soundentry.getSoundEntryName());

                        @Override
                        public int getWeight() {
                            SoundEventAccessorComposite soundeventaccessorcomposite1 = SoundHandler.this.sndRegistry.getObject(this.field_148726_a);
                            return soundeventaccessorcomposite1 == null ? 0 : soundeventaccessorcomposite1.getWeight();
                        }

                        @Override
                        public SoundPoolEntry cloneEntry() {
                            SoundEventAccessorComposite soundeventaccessorcomposite1 = SoundHandler.this.sndRegistry.getObject(this.field_148726_a);
                            return soundeventaccessorcomposite1 == null ? SoundHandler.MISSING_SOUND : soundeventaccessorcomposite1.cloneEntry();
                        }
                    };

                    break;
                default:
                    throw new IllegalStateException("IN YOU FACE");
            }

            soundeventaccessorcomposite.addSoundToEventPool(isoundeventaccessor);
        }
    }

    public SoundEventAccessorComposite getSound(ResourceLocation location) {
        return this.sndRegistry.getObject(location);
    }

    public void playSound(ISound sound) {
        this.sndManager.playSound(sound);
    }

    public void playDelayedSound(ISound sound, int delay) {
        this.sndManager.playDelayedSound(sound, delay);
    }

    public void setListener(EntityPlayer player, float p_147691_2_) {
        this.sndManager.setListener(player, p_147691_2_);
    }

    public void pauseSounds() {
        this.sndManager.pauseAllSounds();
    }

    public void stopSounds() {
        this.sndManager.stopAllSounds();
    }

    public void unloadSounds() {
        this.sndManager.unloadSoundSystem();
    }

    @Override
    public void update() {
        this.sndManager.updateAllSounds();
    }

    public void resumeSounds() {
        this.sndManager.resumeAllSounds();
    }

    public void setSoundLevel(SoundCategory category, float volume) {
        if (category == SoundCategory.MASTER && volume <= 0.0F) {
            this.stopSounds();
        }

        this.sndManager.setSoundCategoryVolume(category, volume);
    }

    public void stopSound(ISound p_147683_1_) {
        this.sndManager.stopSound(p_147683_1_);
    }

    public SoundEventAccessorComposite getRandomSoundFromCategories(SoundCategory... categories) {
        List<SoundEventAccessorComposite> list = new ArrayList<>();

        for (ResourceLocation resourcelocation : this.sndRegistry.getKeys()) {
            SoundEventAccessorComposite soundeventaccessorcomposite = this.sndRegistry.getObject(resourcelocation);

            if (ArrayUtils.contains(categories, soundeventaccessorcomposite.getSoundCategory())) {
                list.add(soundeventaccessorcomposite);
            }
        }

        if (list.isEmpty()) {
            return null;
        } else {
            return list.get((new Random()).nextInt(list.size()));
        }
    }

    public boolean isSoundPlaying(ISound sound) {
        return this.sndManager.isSoundPlaying(sound);
    }
}
