package net.radiant.audio;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;

import java.io.Closeable;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.stb.STBVorbis.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Minimal PaulsCode-style replacement using LWJGL3 OpenAL + STB Vorbis.
 * - Preload short sounds into buffers (SFX)
 * - Stream long OGG files for music via queued OpenAL buffers
 */
public class SoundSystem implements Closeable {
    // ===== OpenAL device/context =====
    private long device;
    private long context;

    // ===== SFX: id -> AL buffer =====
    private final Map<String, Integer> buffers = new ConcurrentHashMap<>();
    // Active sources by id (both sfx/music use sources). You can map differently if desired.
    private final Map<String, Integer> sources = new ConcurrentHashMap<>();

    // ===== Music streaming management =====
    private final Map<String, MusicStream> music = new ConcurrentHashMap<>();
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "SoundSystem-Stream");
        t.setDaemon(true);
        return t;
    });

    // ===== Config defaults =====
    public static final int STREAM_BUFFER_COUNT = 4;            // number of queued buffers
    public static final int STREAM_CHUNK_SAMPLES = 0x8000;      // samples per buffer (per channel)
    // Larger chunks = fewer callbacks/CPU, smaller = lower latency on seeks/stop.

    // ===== Lifecycle =====
    public void init() {
        device = alcOpenDevice((String) null);
        if (device == NULL) {
            throw new IllegalStateException("Failed to open OpenAL device");
        }

        context = alcCreateContext(device, new int[]{0});
        if (context == NULL) {
            throw new IllegalStateException("Failed to create OpenAL context");
        }

        if (!alcMakeContextCurrent(context)) {
            throw new IllegalStateException("Failed to make OpenAL context current");
        }

        // Load capabilities
        ALCCapabilities alcCaps = ALC.createCapabilities(device);
        AL.createCapabilities(alcCaps);
    }

    @Override
    public void close() {
        // Stop all music streams first
        for (String id : new ArrayList<>(music.keySet())) {
            stopMusic(id);
        }
        streamExecutor.shutdownNow();

        // Stop & delete all sources
        for (int src : sources.values()) {
            alSourceStop(src);
            alDeleteSources(src);
        }
        sources.clear();

        // Delete all buffers
        for (int buf : buffers.values()) {
            alDeleteBuffers(buf);
        }
        buffers.clear();

        // Tear down OpenAL
        if (context != NULL) {
            alcDestroyContext(context);
        }
        if (device != NULL) {
            alcCloseDevice(device);
        }
        context = 0;
        device = 0;
    }

    // ===== Listener controls =====
    public void setListenerPosition(float x, float y, float z) {
        alListener3f(AL_POSITION, x, y, z);
    }

    /** forward(x,y,z) then up(x,y,z) */
    public void setListenerOrientation(float fx, float fy, float fz, float ux, float uy, float uz) {
        float[] ori = new float[]{fx, fy, fz, ux, uy, uz};
        alListenerfv(AL_ORIENTATION, ori);
    }

    public void setListenerGain(float gain) {
        alListenerf(AL_GAIN, gain);
    }

    // ===== SFX (preloaded) =====
    public void loadSound(String id, Path oggFile) {
        if (buffers.containsKey(id)) {
            // Replace existing
            alDeleteBuffers(buffers.remove(id));
        }
        int buffer = loadOggToBuffer(oggFile);
        buffers.put(id, buffer);
    }

    public void unloadSound(String id) {
        Integer buf = buffers.remove(id);
        if (buf != null) {
            alDeleteBuffers(buf);
        }
    }

    /** Play a preloaded SFX. */
    public int playSound(String id, boolean loop, float x, float y, float z, float gain, float pitch) {
        Integer buffer = buffers.get(id);
        if (buffer == null) {
            throw new IllegalArgumentException("Sound not loaded: " + id);
        }

        int src = alGenSources();
        alSourcei(src, AL_BUFFER, buffer);
        alSourcei(src, AL_LOOPING, loop ? AL_TRUE : AL_FALSE);
        alSource3f(src, AL_POSITION, x, y, z);
        alSourcef(src, AL_GAIN, gain);
        alSourcef(src, AL_PITCH, pitch);
        alSourcePlay(src);

        // Track by id (last wins). If you want multiple instances per id, store a list instead.
        Integer old = sources.put(id, src);
        if (old != null && old != src) {
            alSourceStop(old);
            alDeleteSources(old);
        }
        return src;
    }

    public void stopSound(String id) {
        Integer src = sources.remove(id);
        if (src != null) {
            alSourceStop(src);
            alDeleteSources(src);
        }
    }

    public void setSoundGain(String id, float gain) {
        Integer src = sources.get(id);
        if (src != null) {
            alSourcef(src, AL_GAIN, gain);
        }
    }

    public void setSoundPitch(String id, float pitch) {
        Integer src = sources.get(id);
        if (src != null) {
            alSourcef(src, AL_PITCH, pitch);
        }
    }

    public void setSoundPosition(String id, float x, float y, float z) {
        Integer src = sources.get(id);
        if (src != null) {
            alSource3f(src, AL_POSITION, x, y, z);
        }
    }

    public void pauseSound(String id) {
        Integer src = sources.get(id);
        if (src != null) {
            alSourcePause(src);
        }
    }

    public void resumeSound(String id) {
        Integer src = sources.get(id);
        if (src != null) {
            alSourcePlay(src);
        }
    }

    // ===== Music (streaming OGG) =====

    /** Start (or restart) streaming music under an id. If already exists, it is replaced. */
    public void playMusic(String id, Path oggFile, boolean loop, float gain) {
        stopMusic(id);
        MusicStream ms = new MusicStream(oggFile, loop, gain);
        music.put(id, ms);
        sources.put(id, ms.getSourceId());
        streamExecutor.submit(ms);
    }

    public void stopMusic(String id) {
        MusicStream ms = music.remove(id);
        if (ms != null) {
            ms.close();
        }
        Integer src = sources.remove(id);
        if (src != null) {
            alSourceStop(src);
            alDeleteSources(src);
        }
    }

    public void pauseMusic(String id) {
        MusicStream ms = music.get(id);
        if (ms != null) {
            ms.pause();
        }
    }

    public void resumeMusic(String id) {
        MusicStream ms = music.get(id);
        if (ms != null) {
            ms.resume();
        }
    }

    public void setMusicGain(String id, float gain) {
        MusicStream ms = music.get(id);
        if (ms != null) {
            ms.setGain(gain);
        }
    }

    private int loadOggToBuffer(Path file) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer error = stack.mallocInt(1);
            long decoder = stb_vorbis_open_filename(file.toString(), error, null);
            if (decoder == NULL) {
                throw new RuntimeException("Failed to open OGG " + file + " (error " + error.get(0) + ")");
            }

            STBVorbisInfo info = STBVorbisInfo.malloc(stack);
            stb_vorbis_get_info(decoder, info);
            int channels = info.channels();
            int rate = info.sample_rate();

            int totalSamples = stb_vorbis_stream_length_in_samples(decoder);
            // total samples *per channel*
            int totalInterleaved = Math.max(1, totalSamples) * channels;

            ShortBuffer pcm = stack.mallocShort(totalInterleaved);
            int got = stb_vorbis_get_samples_short_interleaved(decoder, channels, pcm);
            stb_vorbis_close(decoder);

            // got is number of samples PER CHANNEL actually written.
            pcm.limit(got * channels);

            int buffer = alGenBuffers();
            int format = (channels == 1) ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;
            alBufferData(buffer, format, pcm, rate);
            return buffer;
        }
    }

    // ===== Small helper =====
    public static void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}

