package net.minecraft.client.audio;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.radiant.audio.ISoundSource;
import net.radiant.audio.SoundSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;

public class SoundManager {
    private static final Marker LOG_MARKER = MarkerFactory.getMarker("SOUNDS");
    private static final Logger LOGGER = LoggerFactory.getLogger(SoundManager.class);
    private final SoundHandler sndHandler;
    private final GameSettings options;
    private SoundSystemStarterThread sndSystem;
    private boolean loaded;
    private int playTime = 0;
    private final BiMap<String, ISound> playingSounds = HashBiMap.create();
    private final Map<ISound, String> invPlayingSounds;
    private final Map<ISound, SoundPoolEntry> playingSoundPoolEntries;
    private final Multimap<SoundCategory, String> categorySounds;
    private final List<ITickableSound> tickableSounds;
    private final Map<ISound, Integer> delayedSounds;
    private final Map<String, Integer> playingSoundsStopTime;

    public SoundManager(SoundHandler handler, GameSettings options) {
        this.invPlayingSounds = this.playingSounds.inverse();
        this.playingSoundPoolEntries = new HashMap<>();
        this.categorySounds = HashMultimap.create();
        this.tickableSounds = new ArrayList<>();
        this.delayedSounds = new HashMap<>();
        this.playingSoundsStopTime = new HashMap<>();
        this.sndHandler = handler;
        this.options = options;
    }

    public void reloadSoundSystem() {
        this.unloadSoundSystem();
        this.loadSoundSystem();
    }

    private synchronized void loadSoundSystem() {
        if (!this.loaded) {
            try {
                (new Thread(() -> {
                    this.sndSystem = new SoundSystemStarterThread();
                    this.sndSystem.init();
                    this.loaded = true;
                    this.sndSystem.setMasterVolume(this.options.getSoundLevel(SoundCategory.MASTER));
                    LOGGER.info(LOG_MARKER, "Sound engine started");
                }, "Sound Library Loader")).start();
            } catch (RuntimeException exception) {
                LOGGER.error(LOG_MARKER, "Error starting SoundSystem. Turning off sounds & music", exception);
                this.options.setSoundLevel(SoundCategory.MASTER, 0.0F);
                this.options.saveOptions();
            }
        }
    }

    private float getSoundCategoryVolume(SoundCategory category) {
        return category != null && category != SoundCategory.MASTER ? this.options.getSoundLevel(category) : 1.0F;
    }

    public void setSoundCategoryVolume(SoundCategory category, float volume) {
        if (this.loaded) {
            if (category == SoundCategory.MASTER) {
                this.sndSystem.setMasterVolume(volume);
            } else {
                for (String s : this.categorySounds.get(category)) {
                    ISound isound = this.playingSounds.get(s);
                    float f = this.getNormalizedVolume(isound, this.playingSoundPoolEntries.get(isound), category);

                    if (f <= 0.0F) {
                        this.stopSound(isound);
                    } else {
                        this.sndSystem.setVolume(s, f);
                    }
                }
            }
        }
    }

    public void unloadSoundSystem() {
        if (this.loaded) {
            this.stopAllSounds();
            this.sndSystem.cleanup();
            this.loaded = false;
        }
    }

    public void stopAllSounds() {
        if (this.loaded) {
            for (String s : this.playingSounds.keySet()) {
                this.sndSystem.stop(s);
            }

            this.playingSounds.clear();
            this.delayedSounds.clear();
            this.tickableSounds.clear();
            this.categorySounds.clear();
            this.playingSoundPoolEntries.clear();
            this.playingSoundsStopTime.clear();
        }
    }

    public void updateAllSounds() {
        ++this.playTime;

        for (ITickableSound itickablesound : this.tickableSounds) {
            itickablesound.update();

            if (itickablesound.isDonePlaying()) {
                this.stopSound(itickablesound);
            } else {
                String s = this.invPlayingSounds.get(itickablesound);
                this.sndSystem.setVolume(s, this.getNormalizedVolume(itickablesound, this.playingSoundPoolEntries.get(itickablesound), this.sndHandler.getSound(itickablesound.getSoundLocation()).getSoundCategory()));
                this.sndSystem.setPitch(s, this.getNormalizedPitch(itickablesound, this.playingSoundPoolEntries.get(itickablesound)));
                this.sndSystem.setPosition(s, itickablesound.getXPosF(), itickablesound.getYPosF(), itickablesound.getZPosF());
            }
        }

        Iterator<Entry<String, ISound>> iterator = this.playingSounds.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<String, ISound> entry = iterator.next();
            String s1 = entry.getKey();
            ISound isound = entry.getValue();

            if (!this.sndSystem.playing(s1)) {
                int i = this.playingSoundsStopTime.get(s1);

                if (i <= this.playTime) {
                    int j = isound.getRepeatDelay();

                    if (isound.canRepeat() && j > 0) {
                        this.delayedSounds.put(isound, this.playTime + j);
                    }
                    iterator.remove();
                    LOGGER.debug(LOG_MARKER, "Removed channel {} because it's not playing anymore", s1);
                    this.sndSystem.removeSource(s1);
                    this.playingSoundsStopTime.remove(s1);
                    this.playingSoundPoolEntries.remove(isound);

                    try {
                        this.categorySounds.remove(this.sndHandler.getSound(isound.getSoundLocation()).getSoundCategory(), s1);
                    } catch (RuntimeException _) {
                    }

                    if (isound instanceof ITickableSound) {
                        this.tickableSounds.remove(isound);
                    }
                }
            }
        }

        Iterator<Entry<ISound, Integer>> iterator1 = this.delayedSounds.entrySet().iterator();

        while (iterator1.hasNext()) {
            Entry<ISound, Integer> entry1 = iterator1.next();

            if (this.playTime >= entry1.getValue()) {
                ISound isound1 = entry1.getKey();

                if (isound1 instanceof ITickableSound iTickableSound) {
                    iTickableSound.update();
                }

                this.playSound(isound1);
                iterator1.remove();
            }
        }
    }

    public boolean isSoundPlaying(ISound sound) {
        if (!this.loaded) {
            return false;
        } else {
            String s = this.invPlayingSounds.get(sound);
            return s != null && (this.sndSystem.playing(s) || this.playingSoundsStopTime.containsKey(s) && this.playingSoundsStopTime.get(s) <= this.playTime);
        }
    }

    public void stopSound(ISound sound) {
        if (this.loaded) {
            String s = this.invPlayingSounds.get(sound);

            if (s != null) {
                this.sndSystem.stop(s);
            }
        }
    }

    public void playSound(ISound p_sound) {
        if (this.loaded) {
            if (this.sndSystem.getMasterVolume() <= 0.0F) {
                LOGGER.debug(LOG_MARKER, "Skipped playing soundEvent: {}, master volume was zero", new Object[]{p_sound.getSoundLocation()});
            } else {
                SoundEventAccessorComposite soundeventaccessorcomposite = this.sndHandler.getSound(p_sound.getSoundLocation());

                if (soundeventaccessorcomposite == null) {
                    LOGGER.warn(LOG_MARKER, "Unable to play unknown soundEvent: {}", new Object[]{p_sound.getSoundLocation()});
                } else {
                    SoundPoolEntry soundpoolentry = soundeventaccessorcomposite.cloneEntry();

                    if (soundpoolentry == SoundHandler.MISSING_SOUND) {
                        LOGGER.warn(LOG_MARKER, "Unable to play empty soundEvent: {}", new Object[]{soundeventaccessorcomposite.getSoundEventLocation()});
                    } else {
                        float f = p_sound.getVolume();
                        float f1 = 16.0F;

                        if (f > 1.0F) {
                            f1 *= f;
                        }

                        SoundCategory soundcategory = soundeventaccessorcomposite.getSoundCategory();
                        float f2 = this.getNormalizedVolume(p_sound, soundpoolentry, soundcategory);
                        double d0 = this.getNormalizedPitch(p_sound, soundpoolentry);
                        ResourceLocation resourcelocation = soundpoolentry.getSoundPoolEntryLocation();

                        if (f2 == 0.0F) {
                            LOGGER.debug(LOG_MARKER, "Skipped playing sound {}, volume was zero.", new Object[]{resourcelocation});
                        } else {
                            boolean flag = p_sound.canRepeat() && p_sound.getRepeatDelay() == 0;
                            String s = MathHelper.getRandomUuid(ThreadLocalRandom.current()).toString();

                            try (InputStream stream = Minecraft.get().getResourceManager().getResource(resourcelocation).getInputStream()) {
                                if (soundpoolentry.isStreamingSound()) {
                                    this.sndSystem.loadStreamingSound(s, stream, flag, p_sound.getXPosF(), p_sound.getYPosF(), p_sound.getZPosF(), p_sound.getAttenuationType().getTypeInt(), f1);
                                } else {
                                    this.sndSystem.loadStaticSound(s, stream, flag, p_sound.getXPosF(), p_sound.getYPosF(), p_sound.getZPosF(), p_sound.getAttenuationType().getTypeInt(), f1);
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                            LOGGER.debug(LOG_MARKER, "Playing sound {} for event {} as channel {}", soundpoolentry.getSoundPoolEntryLocation(), soundeventaccessorcomposite.getSoundEventLocation(), s);
                            this.sndSystem.setPitch(s, (float) d0);
                            this.sndSystem.setVolume(s, f2);
                            this.sndSystem.play(s);
                            this.playingSoundsStopTime.put(s, this.playTime + 20);
                            this.playingSounds.put(s, p_sound);
                            this.playingSoundPoolEntries.put(p_sound, soundpoolentry);

                            if (soundcategory != SoundCategory.MASTER) {
                                this.categorySounds.put(soundcategory, s);
                            }

                            if (p_sound instanceof ITickableSound iTickableSound) {
                                this.tickableSounds.add(iTickableSound);
                            }
                        }
                    }
                }
            }
        }
    }

    private float getNormalizedPitch(ISound sound, SoundPoolEntry entry) {
        return (float) MathHelper.clamp(sound.getPitch() * entry.getPitch(), 0.5D, 2.0D);
    }

    private float getNormalizedVolume(ISound sound, SoundPoolEntry entry, SoundCategory category) {
        return (float) MathHelper.clamp(sound.getVolume() * entry.getVolume(), 0.0D, 1.0D) * this.getSoundCategoryVolume(category);
    }

    public void pauseAllSounds() {
        for (String s : this.playingSounds.keySet()) {
            LOGGER.debug(LOG_MARKER, "Pausing channel {}", new Object[]{s});
            this.sndSystem.pause(s);
        }
    }

    public void resumeAllSounds() {
        for (String s : this.playingSounds.keySet()) {
            LOGGER.debug(LOG_MARKER, "Resuming channel {}", new Object[]{s});
            this.sndSystem.pause(s);
        }
    }

    public void playDelayedSound(ISound sound, int delay) {
        this.delayedSounds.put(sound, this.playTime + delay);
    }

    private static URL getURLForSoundResource(final ResourceLocation resource) {
        String protocol = "mcsounddomain";
        String domain = resource.getResourceDomain();
        String path = resource.getResourcePath();

        String uriString = String.format("%s:%s:%s", protocol, domain, path);

        URLStreamHandler urlStreamHandler = new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL url) {
                return new URLConnection(url) {
                    @Override
                    public void connect() {
                        // No connection logic required
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        return Minecraft.get().getResourceManager()
                                .getResource(resource).getInputStream();
                    }
                };
            }
        };

        try {
            return URL.of(URI.create(uriString), urlStreamHandler);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to create URL for sound resource", exception);
        }
    }


    public void setListener(EntityPlayer player, float p_148615_2_) {
        if (this.loaded && player != null) {
            float f = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * p_148615_2_;
            float f1 = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * p_148615_2_;
            double d0 = player.prevPosX + (player.posX - player.prevPosX) * p_148615_2_;
            double d1 = player.prevPosY + (player.posY - player.prevPosY) * p_148615_2_ + player.getEyeHeight();
            double d2 = player.prevPosZ + (player.posZ - player.prevPosZ) * p_148615_2_;
            float f2 = MathHelper.cos((f1 + 90.0F) * 0.017453292F);
            float f3 = MathHelper.sin((f1 + 90.0F) * 0.017453292F);
            float f4 = MathHelper.cos(-f * 0.017453292F);
            float f5 = MathHelper.sin(-f * 0.017453292F);
            float f6 = MathHelper.cos((-f + 90.0F) * 0.017453292F);
            float f7 = MathHelper.sin((-f + 90.0F) * 0.017453292F);
            float f8 = f2 * f4;
            float f9 = f3 * f4;
            float f10 = f2 * f6;
            float f11 = f3 * f6;
            this.sndSystem.setListenerPosition((float) d0, (float) d1, (float) d2);
            this.sndSystem.setListenerOrientation(f8, f5, f9, f10, f7, f11);
        }
    }

    static class SoundSystemStarterThread extends SoundSystem {

        public static final Object SYNC = new Object();

        private SoundSystemStarterThread() {
        }

        public boolean playing(String p_playing_1_) {
            synchronized (SYNC) {
                ISoundSource source = this.getSound(p_playing_1_);
                return source != null && (source.playing() || source.paused());
            }
        }
    }
}
