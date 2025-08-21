// API overview:
//   SoundEngine engine = new SoundEngine();
//   engine.init();
//   int sfx = engine.loadStaticOgg(() -> new FileInputStream("ding.ogg"));
//   int music = engine.createStreamingOgg(() -> new FileInputStream("bg.ogg"), true);
//   engine.play(sfx);
//   engine.play(music);
//   // each frame:
//   engine.update();
//   // controls:
//   engine.setGain(music, 0.5f);
//   engine.setPosition(sfx, 0,0,0);
//   engine.setLooping(sfx, false);
//   // shutdown:
//   engine.dispose();

package net.radiant.audio;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class SoundEngine implements Closeable {
    // ===================== OpenAL bootstrap =====================
    private long device = NULL;
    private long context = NULL;

    private boolean initialized = false;

    // Keep track of sources we manage
    private final Map<Integer, Source> sources = new HashMap<>();
    private int nextId = 1;

    // Recommended streaming buffer sizes
    private static final int STREAM_BUFFER_SAMPLES = 4096; // per channel
    private static final int STREAM_QUEUE_BUFFERS = 4;     // number of rotating buffers

    public void init() {
        if (initialized) {
            return;
        }
        device = alcOpenDevice((ByteBuffer) null);
        if (device == NULL) {
            throw new IllegalStateException("Failed to open OpenAL device");
        }

        int[] attrs = {ALC_REFRESH, 60, ALC_SYNC, 0, 0};
        context = alcCreateContext(device, attrs);
        if (context == NULL) {
            throw new IllegalStateException("Failed to create OpenAL context");
        }
        alcMakeContextCurrent(context);
        org.lwjgl.openal.AL.createCapabilities(org.lwjgl.openal.ALC.createCapabilities(device));

        // default listener
        alListener3f(AL_POSITION, 0, 0, 0);
        alListener3f(AL_VELOCITY, 0, 0, 0);
        float[] ori = {0, 0, -1, 0, 1, 0}; // at, up
        alListenerfv(AL_ORIENTATION, ori);

        initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    // ===================== Static (whole-file) playback =====================
    public int loadStaticOgg(Supplier<InputStream> oggSupplier) {
        ensureInit();
        try (InputStream in = oggSupplier.get()) {
            VorbisDecoder dec = new VorbisDecoder(in);
            dec.init();
            PCMBuffer pcm = dec.decodeAll();

            int buffer = alGenBuffers();
            alBufferData(buffer, pcm.alFormat, pcm.data, pcm.sampleRate);

            int src = alGenSources();
            alSourcei(src, AL_BUFFER, buffer);
            alSourcef(src, AL_GAIN, 1.0f);
            alSourcef(src, AL_PITCH, 1.0f);

            int id = nextId++;
            sources.put(id, Source.staticSource(id, src, buffer));
            return id;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load OGG", e);
        }
    }

    // ===================== Streaming playback =====================
    public int createStreamingOgg(Supplier<InputStream> oggSupplier, boolean looping) {
        ensureInit();
        try {
            StreamedVorbis stream = new StreamedVorbis(oggSupplier.get());
            stream.init();

            int src = alGenSources();
            alSourcef(src, AL_GAIN, 1.0f);
            alSourcef(src, AL_PITCH, 1.0f);

            int[] buffers = new int[STREAM_QUEUE_BUFFERS];
            alGenBuffers(buffers);

            // Prime the queue
            for (int buffer : buffers) {
                ByteBuffer chunk = stream.readSamples(STREAM_BUFFER_SAMPLES);
                if (chunk == null || !chunk.hasRemaining()) {
                    break;
                }
                alBufferData(buffer, stream.getALFormat(), chunk, stream.getSampleRate());
                alSourceQueueBuffers(src, buffer);
            }

            int id = nextId++;
            Source s = Source.streamingSource(id, src, buffers, stream, looping);
            sources.put(id, s);
            return id;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create streaming OGG", e);
        }
    }

    public void play(int id) {
        Source s = require(id);
        if (s.kind == Source.Kind.STATIC) {
            alSourcePlay(s.alSource);
        } else {
            // Start streaming if not already queued; ensure playing state
            int state = alGetSourcei(s.alSource, AL_SOURCE_STATE);
            if (state != AL_PLAYING) {
                alSourcePlay(s.alSource);
            }
        }
    }

    public void pause(int id) {
        alSourcePause(require(id).alSource);
    }

    public void stop(int id) throws IOException {
        Source s = require(id);
        alSourceStop(s.alSource);
        if (s.kind == Source.Kind.STREAM) {
            // flush queue to reset
            int queued = alGetSourcei(s.alSource, AL_BUFFERS_QUEUED);
            for (int i = 0; i < queued; i++) {
                int buf = alSourceUnqueueBuffers(s.alSource);
                // will be refilled in next update()
            }
            s.stream.rewind();
        }
    }

    public void setGain(int id, float gain) {
        alSourcef(require(id).alSource, AL_GAIN, gain);
    }

    public void setPitch(int id, float pitch) {
        alSourcef(require(id).alSource, AL_PITCH, pitch);
    }

    public void setLooping(int id, boolean looping) {
        require(id).looping = looping;
    }

    public void setPosition(int id, float x, float y, float z) {
        alSource3f(require(id).alSource, AL_POSITION, x, y, z);
    }

    public void setVelocity(int id, float x, float y, float z) {
        alSource3f(require(id).alSource, AL_VELOCITY, x, y, z);
    }

    public void remove(int id) {
        Source s = sources.remove(id);
        if (s == null) {
            return;
        }
        if (s.kind == Source.Kind.STATIC) {
            int buf = alGetSourcei(s.alSource, AL_BUFFER);
            alSourceStop(s.alSource);
            alDeleteSources(s.alSource);
            if (buf != 0) {
                alDeleteBuffers(buf);
            }
        } else {
            alSourceStop(s.alSource);
            // unqueue and delete stream buffers
            int processed = alGetSourcei(s.alSource, AL_BUFFERS_QUEUED);
            for (int i = 0; i < processed; i++) {
                int b = alSourceUnqueueBuffers(s.alSource);
                if (b != 0) {
                    alDeleteBuffers(b);
                }
            }
            alDeleteSources(s.alSource);
            try {
                s.stream.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Pump streaming sources. Call once per frame/tick.
     */
    public void update() {
        for (Source s : sources.values()) {
            if (s.kind != Source.Kind.STREAM) {
                continue;
            }

            int processed = alGetSourcei(s.alSource, AL_BUFFERS_PROCESSED);
            while (processed-- > 0) {
                int buf = alSourceUnqueueBuffers(s.alSource);

                // Read next chunk
                try {
                    ByteBuffer chunk = s.stream.readSamples(STREAM_BUFFER_SAMPLES);
                    if (chunk == null || !chunk.hasRemaining()) {
                        // End of stream
                        if (s.looping) {
                            s.stream.rewind();
                            chunk = s.stream.readSamples(STREAM_BUFFER_SAMPLES);
                        }
                    }

                    if (chunk != null && chunk.hasRemaining()) {
                        alBufferData(buf, s.stream.getALFormat(), chunk, s.stream.getSampleRate());
                        alSourceQueueBuffers(s.alSource, buf);
                    } else {
                        // No data to queue — drop buffer
                        alDeleteBuffers(buf);
                    }
                } catch (IOException e) {
                    // Drop buffer on decode error to keep engine running
                    alDeleteBuffers(buf);
                }
            }

            // If we ran dry, try to restart playback
            int queued = alGetSourcei(s.alSource, AL_BUFFERS_QUEUED);
            int state = alGetSourcei(s.alSource, AL_SOURCE_STATE);
            if (queued > 0 && state != AL_PLAYING) {
                alSourcePlay(s.alSource);
            }
        }
    }

    // ===================== Cleanup =====================
    @Override
    public void close() {
        dispose();
    }

    public void dispose() {
        if (!initialized) {
            return;
        }
        // Delete all sources/buffers
        for (Integer id : new ArrayList<>(sources.keySet())) {
            remove(id);
        }

        alcMakeContextCurrent(NULL);
        if (context != NULL) {
            alcDestroyContext(context);
        }
        if (device != NULL) {
            alcCloseDevice(device);
        }
        context = device = NULL;
        initialized = false;
    }

    // ===================== Helpers =====================
    private void ensureInit() {
        if (!initialized) {
            throw new IllegalStateException("Call init() first");
        }
    }

    private Source require(int id) {
        Source s = sources.get(id);
        if (s == null) {
            throw new IllegalArgumentException("Unknown source id: " + id);
        }
        return s;
    }

    // ===================== PCM helpers =====================
    static void writeSample16(ByteBuffer out, float sample) {
        int val = (int) (sample * 32767.0f);
        if (val > 32767) {
            val = 32767;
        } else if (val < -32768) {
            val = -32768;
        }
        out.put((byte) (val & 0xFF));
        out.put((byte) ((val >>> 8) & 0xFF));
    }
}

