package net.radiant.audio;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.stb.STBVorbis.*;

public class StreamingSound implements ISoundSource, Runnable {

    private static final int BUFFER_COUNT = 4;
    private static final int BUFFER_SIZE = 0x8000; // tune this
    private static final int MAX_WAIT_TIME = BUFFER_COUNT + 5;

    private final int source;
    private final int[] buffers = new int[BUFFER_COUNT];
    private final @SuppressWarnings({"unused", "FieldCanBeLocal"}) ByteBuffer fileDataRef; // keep the ByteBuffer alive
    private final int channels;
    private final int sampleRate;
    // threading & control
    private final Object vorbisLock = new Object();
    private long decoder;
    private volatile boolean threadRunning = false;
    private volatile boolean playRequested = false;
    private volatile boolean looping = true;
    private Thread streamThread;

    private float baseVolume = 1.0f;

    public StreamingSound(ByteBuffer fileData) {
        this.fileDataRef = fileData;

        source = alGenSources();
        for (int i = 0; i < BUFFER_COUNT; i++) {
            buffers[i] = alGenBuffers();
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            int[] err = new int[1];
            decoder = stb_vorbis_open_memory(fileData, err, null);
            if (decoder == 0L) {
                throw new RuntimeException("stb_vorbis_open_memory failed, err=" + err[0]);
            }

            STBVorbisInfo info = STBVorbisInfo.malloc(stack);
            stb_vorbis_get_info(decoder, info);
            channels = info.channels();
            sampleRate = info.sample_rate();
        }

        // initial fill of all buffers (best-effort)
        for (int buf : buffers) {
            if (!streamBuffer(buf)) {
                break;
            }
        }
        alSourceQueueBuffers(source, buffers);
    }

    /**
     * Decode into a *local* ShortBuffer and upload it. Must be called only while decoder is valid.
     */
    private boolean streamBuffer(int bufferId) {
        // local pcm prevents shared/native memory races
        ShortBuffer pcm = BufferUtils.createShortBuffer(BUFFER_SIZE * channels);

        // guard access to decoder with lock so close() cannot race
        synchronized (vorbisLock) {
            if (decoder == 0L) {
                return false; // decoder already closed -> no decode
            }
            int samples = stb_vorbis_get_samples_short_interleaved(decoder, channels, pcm);
            if (samples <= 0) {
                return false; // EOF or error
            }
            pcm.limit(samples * channels);
        }

        int format = (channels == 1) ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;
        alBufferData(bufferId, format, pcm, sampleRate);
        return true;
    }

    // ---------------- ISoundSource ----------------
    @Override
    public synchronized void play() {
        playRequested = true;
        if (streamThread == null || !streamThread.isAlive()) {
            threadRunning = true;
            streamThread = new Thread(this, "StreamingSoundThread");
            streamThread.setDaemon(true);
            streamThread.start();
        }

        // if data is queued and source not playing, start it
        int queued = alGetSourcei(source, AL_BUFFERS_QUEUED);
        int state = alGetSourcei(source, AL_SOURCE_STATE);
        if (queued > 0 && state != AL_PLAYING) {
            alSourcePlay(source);
        }
    }

    @Override
    public synchronized void pause() {
        playRequested = false;
        alSourcePause(source);
        // keep threadRunning true so thread can stay alive (you might prefer to let it sleep)
    }

    @Override
    public synchronized void stop() {
        playRequested = false;
        alSourceStop(source);
        // do not close decoder here — call cleanup() to permanently destroy the stream
    }

    @Override
    public void setVolume(float volume) {
        baseVolume = volume;
        updateVolumeWithMaster(1.0f);
    }

    @Override
    public void setPitch(float pitch) {
        alSourcef(source, AL_PITCH, pitch);
    }

    @Override
    public void setLooping(boolean loop) {
        looping = loop;
    }

    @Override
    public void setPosition(float x, float y, float z) {
        alSource3f(source, AL_POSITION, x, y, z);
    }

    @Override
    public void setVelocity(float vx, float vy, float vz) {
        alSource3f(source, AL_VELOCITY, vx, vy, vz);
    }

    @Override
    public void setAttenuation(float refDist, float maxDist, float rolloff) {
        alSourcef(source, AL_REFERENCE_DISTANCE, refDist);
        alSourcef(source, AL_MAX_DISTANCE, maxDist);
        alSourcef(source, AL_ROLLOFF_FACTOR, rolloff);
    }

    @Override
    public void updateVolumeWithMaster(float masterVolume) {
        alSourcef(source, AL_GAIN, baseVolume * masterVolume);
    }

    @Override
    public boolean playing() {
        return alGetSourcei(source, AL_SOURCE_STATE) == AL_PLAYING && playRequested;
    }

    @Override
    public boolean paused() {
        return !playRequested && alGetSourcei(source, AL_SOURCE_STATE) == AL_PAUSED;
    }

    @Override
    public void run() {
        try {
            while (threadRunning) {
                if (!playRequested) {
                    Thread.sleep(MAX_WAIT_TIME);
                    continue;
                }

                // Process completed buffers
                int processed = alGetSourcei(source, AL_BUFFERS_PROCESSED);
                int queuedBefore = alGetSourcei(source, AL_BUFFERS_QUEUED);
                boolean wasUnderrun = (queuedBefore == 0);

                for (int i = 0; i < processed; i++) {
                    int buf = alSourceUnqueueBuffers(source);

                    boolean filled = streamBuffer(buf);

                    if (!filled && looping) {
                        synchronized (vorbisLock) {
                            if (decoder != 0L) {
                                stb_vorbis_seek_start(decoder);
                                filled = streamBuffer(buf);
                            }
                        }
                    }

                    if (filled) {
                        alSourceQueueBuffers(source, buf);
                    }
                }

                int queuedAfter = alGetSourcei(source, AL_BUFFERS_QUEUED);
                int state = alGetSourcei(source, AL_SOURCE_STATE);
                if (wasUnderrun && queuedAfter > 0 && state != AL_PLAYING && playRequested) {
                    alSourcePlay(source);
                }

                if (state != AL_PLAYING && queuedAfter > 0 && playRequested) {
                    alSourcePlay(source);
                }

                int adaptive = Math.max(2, Math.min(MAX_WAIT_TIME, MAX_WAIT_TIME - queuedAfter));
                Thread.sleep(adaptive);
            }
        } catch (InterruptedException _) {

        }
    }

    /**
     * Permanently stop & free resources. Waits for the stream thread to exit, then closes decoder.
     */
    @Override
    public synchronized void cleanup() {
        // request thread to stop and playback to stop
        playRequested = false;
        threadRunning = false;
        alSourceStop(source);

        // join the thread so it cannot be in the middle of STB calls
        if (streamThread != null) {
            try {
                streamThread.join(2000);
            } catch (InterruptedException _) {
            }
            streamThread = null;
        }

        // safe to close decoder now (we hold no lock necessary because thread has exited)
        synchronized (vorbisLock) {
            if (decoder != 0L) {
                stb_vorbis_close(decoder);
                decoder = 0L; // only set after close
            }
        }

        // delete OpenAL objects
        for (int buf : buffers) {
            alDeleteBuffers(buf);
        }
        alDeleteSources(source);
    }
}
