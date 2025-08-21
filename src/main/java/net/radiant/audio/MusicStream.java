package net.radiant.audio;

import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;

import java.io.Closeable;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.radiant.audio.SoundSystem.*;
import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.stb.STBVorbis.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class MusicStream implements Runnable, Closeable {
    private final Path file;
    private final boolean loop;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean paused = new AtomicBoolean(false);

    private long decoder;         // STB handle
    private OggInfo info;
    private final int[] queueBuffers = new int[STREAM_BUFFER_COUNT];
    private final int sourceId;

    public MusicStream(Path file, boolean loop, float initialGain) {
        this.file = file;
        this.loop = loop;

        // create decoder
        openDecoder();

        // create source + buffers
        sourceId = alGenSources();
        alSourcef(sourceId, AL_GAIN, initialGain);
        alSourcei(sourceId, AL_LOOPING, AL_FALSE); // we loop manually at stream end

        alGenBuffers(queueBuffers);
        // Prime the queue
        int queued = 0;
        for (; queued < queueBuffers.length; queued++) {
            if (!fillAndQueue(queueBuffers[queued])) {
                break;
            }
        }
        if (queued == 0) {
            // nothing decoded — treat as silent
            running.set(false);
            return;
        }
        alSourcePlay(sourceId);
    }

    private void openDecoder() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer err = stack.mallocInt(1);
            decoder = stb_vorbis_open_filename(file.toString(), err, null);
            if (decoder == NULL) {
                throw new RuntimeException("Failed to open music OGG " + file + " (error " + err.get(0) + ")");
            }
            STBVorbisInfo vi = STBVorbisInfo.malloc(stack);
            stb_vorbis_get_info(decoder, vi);
            info = new OggInfo(vi.channels(), vi.sample_rate());
        }
    }

    /** Decode up to STREAM_CHUNK_SAMPLES and queue into the given AL buffer. Returns false if end-of-stream and nothing queued. */
    private boolean fillAndQueue(int alBuffer) {
        ShortBuffer pcm;
        int samplesGot;
        try (MemoryStack stack = stackPush()) {
            pcm = stack.mallocShort(STREAM_CHUNK_SAMPLES * info.channels);
            samplesGot = stb_vorbis_get_samples_short_interleaved(decoder, info.channels, pcm);
            // samplesGot is per-channel; clamp buffer limit to interleaved sample count
            pcm.limit(samplesGot * info.channels);
        }

        if (samplesGot <= 0) {
            return false;
        }

        int format = (info.channels == 1) ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;
        alBufferData(alBuffer, format, pcm, info.sampleRate);
        alSourceQueueBuffers(sourceId, alBuffer);
        return true;
    }

    @Override
    public void run() {
        // Streaming loop: unqueue processed buffers, refill, handle looping, and keep playing
        while (running.get()) {
            if (paused.get()) {
                sleepQuiet(10);
                continue;
            }

            int processed = alGetSourcei(sourceId, AL_BUFFERS_PROCESSED);
            while (processed-- > 0) {
                int buf = alSourceUnqueueBuffers(sourceId);
                // Try to refill
                if (!fillAndQueue(buf)) {
                    // EOS reached
                    if (loop) {
                        // Seek back to start and try again
                        stb_vorbis_seek_start(decoder);
                        if (!fillAndQueue(buf)) {
                            // File really empty? stop.
                            running.set(false);
                            break;
                        }
                    } else {
                        // Do not re-queue — let playback drain
                    }
                }
            }

            // If source stopped but we still have queued data, restart (can happen after pause or buffer underrun)
            int state = alGetSourcei(sourceId, AL_SOURCE_STATE);
            if (state != AL_PLAYING) {
                int queued = alGetSourcei(sourceId, AL_BUFFERS_QUEUED);
                if (queued > 0 && !paused.get()) {
                    alSourcePlay(sourceId);
                } else if (queued == 0 && !loop) {
                    // fully drained and not looping => exit
                    running.set(false);
                }
            }

            sleepQuiet(10);
        }
    }

    void pause() {
        paused.set(true);
        alSourcePause(sourceId);
    }

    void resume() {
        paused.set(false);
        alSourcePlay(sourceId);
    }

    void setGain(float gain) {
        alSourcef(sourceId, AL_GAIN, gain);
    }

    @Override
    public void close() {
        running.set(false);
        // Give thread a moment to exit its loop if we're on another thread
        // (If called from its own thread via stopMusic, it will drop through.)
        // We won't join here since we run in a cached daemon pool.
        // Stop and clear source queue
        alSourceStop(sourceId);
        // Unqueue any remaining
        int queued = alGetSourcei(sourceId, AL_BUFFERS_QUEUED);
        while (queued-- > 0) {
            alSourceUnqueueBuffers(sourceId);
        }
        alDeleteBuffers(queueBuffers);
        if (decoder != 0) {
            stb_vorbis_close(decoder);
            decoder = 0;
        }
    }

    public int getSourceId() {
        return sourceId;
    }
}
