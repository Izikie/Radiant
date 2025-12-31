package net.radiant.audio;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.AL11.alSpeedOfSound;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_decode_memory;

public class SoundSystem {

    private static final Logger LOGGER = LoggerFactory.getLogger(SoundSystem.class);
    private final Map<String, ISoundSource> sources = new HashMap<>();
    private long device;
    private long context;
    private float masterVolume = 1.0f;

    // ---------------- Initialization ----------------
    public void init() {
        device = alcOpenDevice((ByteBuffer) null);
        if (device == 0) {
            throw new IllegalStateException("Failed to open OpenAL device");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            int[] attribs = {0};
            context = alcCreateContext(device, attribs);
            alcMakeContextCurrent(context);
        }
        AL.createCapabilities(org.lwjgl.openal.ALC.createCapabilities(device));
        LOGGER.info("OpenAL initialized");
    }

    public void destroy() {
        cleanup();
        alcDestroyContext(context);
        alcCloseDevice(device);
    }

    public float getMasterVolume() {
        return masterVolume;
    }

    // ---------------- Master Volume ----------------
    public void setMasterVolume(float volume) {
        masterVolume = Math.clamp(volume, 0f, 1f);
        for (ISoundSource src : sources.values()) {
            src.updateVolumeWithMaster(masterVolume);
        }
    }

    // ---------------- Listener ----------------
    public void setListenerPosition(float x, float y, float z) {
        alListener3f(AL_POSITION, x, y, z);
    }

    public void setListenerVelocity(float vx, float vy, float vz) {
        alListener3f(AL_VELOCITY, vx, vy, vz);
    }

    public void setListenerOrientation(float atX, float atY, float atZ, float upX, float upY, float upZ) {
        float[] orientation = {atX, atY, atZ, upX, upY, upZ};
        alListenerfv(AL_ORIENTATION, orientation);
    }

    public void setDoppler(float factor, float speedOfSound) {
        alDopplerFactor(factor);
        alSpeedOfSound(speedOfSound);
    }

    // ---------------- Sound Loading ----------------
    public void loadStaticSound(String name, Path oggFile) throws IOException {
        loadStaticSound(name, Files.newInputStream(oggFile));
    }

    public void loadStaticSound(String name, InputStream is) throws IOException {
        byte[] data = is.readAllBytes();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer channels = stack.mallocInt(1);
            IntBuffer sampleRate = stack.mallocInt(1);
            ShortBuffer pcm = decodeOgg(data, channels, sampleRate);
            SoundSource src = new SoundSource(pcm, channels.get(0), sampleRate.get(0));
            sources.put(name, src);
        }
    }

    public void loadStreamingSound(String name, Path oggFile) throws IOException {
        loadStreamingSound(name, Files.newInputStream(oggFile));
    }

    public void loadStreamingSound(String name, InputStream is) throws IOException {
        byte[] data = is.readAllBytes();
        ByteBuffer buffer = BufferUtils.createByteBuffer(data.length);
        buffer.put(data).flip();
        StreamingSound music = new StreamingSound(buffer);
        sources.put(name, music);
    }

    private ShortBuffer decodeOgg(byte[] oggData, IntBuffer channels, IntBuffer sampleRate) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(oggData.length);
        buffer.put(oggData).flip();
        return stb_vorbis_decode_memory(buffer, channels, sampleRate);
    }

    // ---------------- Loading with Parameters ----------------
    public void loadStaticSound(String sourceName, InputStream stream, boolean toLoop, float x, float y, float z,
                                int attModel, float distOrRoll) throws IOException {
        byte[] data = stream.readAllBytes();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer channels = stack.mallocInt(1);
            IntBuffer sampleRate = stack.mallocInt(1);
            ShortBuffer pcm = decodeOgg(data, channels, sampleRate);
            SoundSource src = new SoundSource(pcm, channels.get(0), sampleRate.get(0));

            src.setPosition(x, y, z);
            src.setLooping(toLoop);
            applyAttenuation(src, attModel, distOrRoll);

            sources.put(sourceName, src);
        }
    }

    public void loadStreamingSound(String sourceName, InputStream stream, boolean toLoop, float x, float y, float z,
                                   int attModel, float distOrRoll) throws IOException {
        byte[] data = stream.readAllBytes();
        ByteBuffer buffer = BufferUtils.createByteBuffer(data.length);
        buffer.put(data).flip();
        StreamingSound music = new StreamingSound(buffer);

        music.setPosition(x, y, z);
        music.setLooping(toLoop);
        applyAttenuation(music, attModel, distOrRoll);

        sources.put(sourceName, music);
    }

    private void applyAttenuation(ISoundSource src, int attModel, float distOrRoll) {
        switch (attModel) {
            case 0 -> src.setAttenuation(distOrRoll, distOrRoll * 2, 1.0f);
            case 1 -> src.setAttenuation(distOrRoll, distOrRoll * 2, 0.5f);
            default -> src.setAttenuation(1, 100, 1);
        }
    }

    // ---------------- Playback ----------------
    public ISoundSource getSound(String name) {
        return sources.get(name);
    }

    public void play(String name) {
        ISoundSource src = getSound(name);
        if (src != null) {
            src.play();
        }
    }

    public void stop(String name) {
        ISoundSource src = getSound(name);
        if (src != null) {
            src.stop();
        }
    }

    public void pause(String name) {
        ISoundSource src = getSound(name);
        if (src != null) {
            src.pause();
        }
    }

    public void setVolume(String name, float volume) {
        ISoundSource src = getSound(name);
        if (src != null) {
            src.setVolume(volume);
        }
    }

    public void setPitch(String name, float pitch) {
        ISoundSource src = getSound(name);
        if (src != null) {
            src.setPitch(pitch);
        }
    }

    public void setLooping(String name, boolean loop) {
        ISoundSource src = getSound(name);
        if (src != null) {
            src.setLooping(loop);
        }
    }

    public void setPosition(String name, float x, float y, float z) {
        ISoundSource src = getSound(name);
        if (src != null) {
            src.setPosition(x, y, z);
        }
    }

    public void removeSource(String name) {
        ISoundSource src = sources.remove(name);
        if (src != null) {
            src.cleanup();
        }
    }

    public void cleanup() {
        for (ISoundSource src : sources.values()) {
            src.cleanup();
        }
        sources.clear();
    }

}
