package net.radiant.audio;

import org.lwjgl.openal.AL;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;

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
import static org.lwjgl.stb.STBVorbis.*;

public class SoundSystem {
    private long device;
    private long context;
    private float masterVolume = 1.0f;
    private final Map<String, ISoundSource> staticSources = new HashMap<>();
    private final Map<String, ISoundSource> streamingSources = new HashMap<>();

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
        System.out.println("OpenAL initialized");
    }

    public void destroy() {
        cleanup(); // system-level cleanup
        alcDestroyContext(context);
        alcCloseDevice(device);
    }

    // ---------------- Master Volume ----------------
    public void setMasterVolume(float volume) {
        masterVolume = Math.max(0f, Math.min(1f, volume));
        for (ISoundSource src : staticSources.values()) {
            src.updateVolumeWithMaster(masterVolume);
        }
        for (ISoundSource s : streamingSources.values()) {
            s.updateVolumeWithMaster(masterVolume);
        }
    }

    public float getMasterVolume() {
        return masterVolume;
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
            staticSources.put(name, src);
        }
    }

    public void loadStreamingSound(String name, Path oggFile) throws IOException {
        loadStreamingSound(name, Files.newInputStream(oggFile));
    }

    public void loadStreamingSound(String name, InputStream is) throws IOException {
        byte[] data = is.readAllBytes();
        StreamingSound music = new StreamingSound(ByteBuffer.wrap(data));
        streamingSources.put(name, music);
    }

    private ShortBuffer decodeOgg(byte[] oggData, IntBuffer channels, IntBuffer sampleRate) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(oggData.length);
        buffer.put(oggData).flip();
        return stb_vorbis_decode_memory(buffer, channels, sampleRate);
    }

    // ---------------- Playback ----------------
    public ISoundSource getSound(String name) {
        if (staticSources.containsKey(name)) {
            return staticSources.get(name);
        }
        return streamingSources.get(name);
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
        ISoundSource src = staticSources.remove(name);
        if (src == null) {
            src = streamingSources.remove(name);
        }
        if (src != null) {
            src.cleanup();
        }
    }

    public void cleanup() {
        for (ISoundSource src : staticSources.values()) {
            src.cleanup();
        }
        staticSources.clear();
        for (ISoundSource src : streamingSources.values()) {
            src.cleanup();
        }
        streamingSources.clear();
    }

    // ---------------- Interface ----------------
    public interface ISoundSource {
        void play();

        void stop();

        void pause();        // new

        void setVolume(float volume);

        void setPitch(float pitch);

        void setLooping(boolean loop);

        void setPosition(float x, float y, float z);

        void setVelocity(float vx, float vy, float vz);

        void setAttenuation(float refDist, float maxDist, float rolloff);

        void updateVolumeWithMaster(float masterVolume);

        void cleanup();

        boolean playing();

        boolean paused();
    }

    // ---------------- SoundSource ----------------
    public class SoundSource implements ISoundSource {
        private final int buffer;
        private final int source;
        private float baseVolume = 1.0f;

        SoundSource(ShortBuffer pcm, int channels, int sampleRate) {
            buffer = alGenBuffers();
            source = alGenSources();
            int format = channels == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;
            alBufferData(buffer, format, pcm, sampleRate);
            alSourcei(source, AL_BUFFER, buffer);
        }

        public void play() {
            alSourcePlay(source);
        }

        public void stop() {
            alSourceStop(source);
        }

        public void pause() {
            alSourcePause(source);
        }

        public void setVolume(float volume) {
            baseVolume = volume;
            updateVolumeWithMaster(masterVolume);
        }

        public void setPitch(float pitch) {
            alSourcef(source, AL_PITCH, pitch);
        }

        public void updateVolumeWithMaster(float masterVolume) {
            alSourcef(source, AL_GAIN, baseVolume * masterVolume);
        }

        public void setLooping(boolean loop) {
            alSourcei(source, AL_LOOPING, loop ? 1 : 0);
        }

        public void setPosition(float x, float y, float z) {
            alSource3f(source, AL_POSITION, x, y, z);
        }

        public void setVelocity(float vx, float vy, float vz) {
            alSource3f(source, AL_VELOCITY, vx, vy, vz);
        }

        public void setAttenuation(float refDist, float maxDist, float rolloff) {
            alSourcef(source, AL_REFERENCE_DISTANCE, refDist);
            alSourcef(source, AL_MAX_DISTANCE, maxDist);
            alSourcef(source, AL_ROLLOFF_FACTOR, rolloff);
        }

        public void cleanup() {
            alDeleteSources(source);
            alDeleteBuffers(buffer);
        }

        public boolean playing() {
            return alGetSourcei(source, AL_SOURCE_STATE) == AL_PLAYING;
        }

        public boolean paused() {
            return alGetSourcei(source, AL_SOURCE_STATE) == AL_PAUSED;
        }
    }

    // ---------------- StreamingSound ----------------
    public class StreamingSound implements ISoundSource, Runnable {
        private static final int BUFFER_COUNT = 4;
        private static final int BUFFER_SIZE = 8192;

        private final int source;
        private final int[] buffers = new int[BUFFER_COUNT];
        private long vorbis;
        private int channels;
        private int sampleRate;
        private boolean running = true;
        private boolean looping = true;
        private float baseVolume = 1.0f;

        public StreamingSound(ByteBuffer fileData) {
            source = alGenSources();
            for (int i = 0; i < BUFFER_COUNT; i++) {
                buffers[i] = alGenBuffers();
            }

            try (MemoryStack stack = MemoryStack.stackPush()) {
                int[] error = new int[1];
                vorbis = stb_vorbis_open_memory(fileData, error, null);
                if (vorbis == 0) {
                    throw new RuntimeException("Failed to open OGG stream, error: " + error[0]);
                }

                STBVorbisInfo info = STBVorbisInfo.malloc(stack);
                stb_vorbis_get_info(vorbis, info);
                channels = info.channels();
                sampleRate = info.sample_rate();
            }

            for (int buf : buffers) {
                if (!streamBuffer(buf)) {
                    break;
                }
            }
            alSourceQueueBuffers(source, buffers);
        }

        private boolean streamBuffer(int bufferId) {
            ShortBuffer pcm = ShortBuffer.allocate(BUFFER_SIZE * channels);
            int samples = stb_vorbis_get_samples_short_interleaved(vorbis, channels, pcm);
            if (samples <= 0) {
                return false;
            }

            pcm.limit(samples * channels);
            int format = channels == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;
            alBufferData(bufferId, format, pcm, sampleRate);
            return true;
        }

        public void play() {
            running = true;
            alSourcePlay(source);
            new Thread(this, "StreamingSoundThread").start();
        }

        public void stop() {
            running = false;
            alSourceStop(source);
        }

        public void pause() {
            running = false;
            alSourcePause(source);
        }

        public void setVolume(float volume) {
            baseVolume = volume;
            updateVolumeWithMaster(masterVolume);
        }

        public void setPitch(float pitch) {
            alSourcef(source, AL_PITCH, pitch);
        }

        public void updateVolumeWithMaster(float masterVolume) {
            alSourcef(source, AL_GAIN, baseVolume * masterVolume);
        }

        public void setLooping(boolean loop) {
            looping = loop;
        }

        public void setPosition(float x, float y, float z) {
            alSource3f(source, AL_POSITION, x, y, z);
        }

        public void setVelocity(float vx, float vy, float vz) {
            alSource3f(source, AL_VELOCITY, vx, vy, vz);
        }

        public void setAttenuation(float refDist, float maxDist, float rolloff) {
            alSourcef(source, AL_REFERENCE_DISTANCE, refDist);
            alSourcef(source, AL_MAX_DISTANCE, maxDist);
            alSourcef(source, AL_ROLLOFF_FACTOR, rolloff);
        }

        @Override
        public void run() {
            while (running) {
                int processed = alGetSourcei(source, AL_BUFFERS_PROCESSED);
                for (int i = 0; i < processed; i++) {
                    int buf = alSourceUnqueueBuffers(source);
                    if (!streamBuffer(buf)) {
                        if (looping) {
                            alSourceRewind(source);
                        }
                        continue;
                    }
                    alSourceQueueBuffers(source, buf);
                }
                if (alGetSourcei(source, AL_SOURCE_STATE) != AL_PLAYING) {
                    alSourcePlay(source);
                }
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ignored) {
                }
            }
        }

        public void cleanup() {
            running = false;
            for (int buf : buffers) {
                alDeleteBuffers(buf);
            }
            alDeleteSources(source);
        }

        public boolean playing() {
            return alGetSourcei(source, AL_SOURCE_STATE) == AL_PLAYING;
        }

        public boolean paused() {
            return alGetSourcei(source, AL_SOURCE_STATE) == AL_PAUSED;
        }
    }
}

