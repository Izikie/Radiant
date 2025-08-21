package net.radiant.audio;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

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
public class SoundSystem {
    private long device;
    private long context;

    private final Map<String, Integer> buffers = new HashMap<>();
    private final Map<String, Integer> sources = new HashMap<>();

    public SoundSystem() {
        // Init OpenAL
        device = alcOpenDevice((String) null);
        if (device == NULL) {
            throw new IllegalStateException("Failed to open OpenAL device");
        }
        context = alcCreateContext(device, new int[]{0});
        alcMakeContextCurrent(context);
        ALCCapabilities alcCaps = ALC.createCapabilities(device);
        AL.createCapabilities(alcCaps);
    }

    /** Load a short sound effect into memory. */
    public void loadSound(String identifier, Path file) {
        int buffer = loadOggToBuffer(file);
        buffers.put(identifier, buffer);
    }

    /** Play a sound effect already loaded. */
    public void play(String identifier) {
        Integer buffer = buffers.get(identifier);
        if (buffer == null) {
            return;
        }
        int src = alGenSources();
        sources.put(identifier, src);
        alSourcei(src, AL_BUFFER, buffer);
        alSourcePlay(src);
    }

    public void stop(String identifier) {
        Integer src = sources.remove(identifier);
        if (src != null) {
            alSourceStop(src);
            alDeleteSources(src);
        }
    }

    public void setVolume(String identifier, float gain) {
        Integer src = sources.get(identifier);
        if (src != null) {
            alSourcef(src, AL_GAIN, gain);
        }
    }

    public void setPitch(String identifier, float pitch) {
        Integer src = sources.get(identifier);
        if (src != null) {
            alSourcef(src, AL_PITCH, pitch);
        }
    }

    public void setPosition(String identifier, float x, float y, float z) {
        Integer src = sources.get(identifier);
        if (src != null) {
            alSource3f(src, AL_POSITION, x, y, z);
        }
    }

    public void cleanup() {
        for (int src : sources.values()) {
            alDeleteSources(src);
        }
        for (int buf : buffers.values()) {
            alDeleteBuffers(buf);
        }
        alcDestroyContext(context);
        alcCloseDevice(device);
    }

    private int loadOggToBuffer(Path file) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer error = stack.mallocInt(1);
            long decoder = stb_vorbis_open_filename(file.toString(), error, null);
            if (decoder == NULL) {
                throw new RuntimeException("Failed to open OGG: " + file);
            }
            STBVorbisInfo info = STBVorbisInfo.malloc(stack);
            stb_vorbis_get_info(decoder, info);
            int channels = info.channels();
            int sampleRate = info.sample_rate();
            int samples = stb_vorbis_stream_length_in_samples(decoder);
            ShortBuffer pcm = stack.mallocShort(samples * channels);
            stb_vorbis_get_samples_short_interleaved(decoder, channels, pcm);
            stb_vorbis_close(decoder);
            int buffer = alGenBuffers();
            int format = (channels == 1) ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;
            alBufferData(buffer, format, pcm, sampleRate);
            return buffer;
        }
    }
}

