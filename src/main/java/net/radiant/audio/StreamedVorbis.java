package net.radiant.audio;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;
import org.lwjgl.BufferUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.lwjgl.openal.AL10.AL_FORMAT_MONO16;
import static org.lwjgl.openal.AL10.AL_FORMAT_STEREO16;

public class StreamedVorbis implements Closeable {
    private final InputStream in;
    private final SyncState sync = new SyncState();
    private final StreamState stream = new StreamState();
    private final Page page = new Page();
    private final Packet packet = new Packet();
    private final Info info = new Info();
    private final Comment comment = new Comment();
    private final DspState dsp = new DspState();
    private final Block block = new Block(dsp);
    private boolean headersReady = false;
    private boolean eos = false;

    StreamedVorbis(InputStream in) {
        this.in = in;
    }

    void init() throws IOException {
        sync.init();
        setupHeaders();
    }

    ByteBuffer readSamples(int samplesPerChannel) throws IOException {
        if (eos) {
            return null;
        }
        ByteBuffer out = BufferUtils.createByteBuffer(samplesPerChannel * info.channels * 2).order(ByteOrder.LITTLE_ENDIAN);
        float[][][] pcmf = new float[1][][];
        int[] idx = new int[info.channels];
        while (out.hasRemaining()) {
            int result = sync.pageout(page);
            if (result == 0) {
                if (!refill()) {
                    break;
                }
                continue;
            }
            if (result == -1) {
                continue;
            }
            stream.pagein(page);
            while (true) {
                result = stream.packetout(packet);
                if (result == 0) {
                    break;
                }
                if (result == -1) {
                    continue;
                }
                if (block.synthesis(packet) == 0) {
                    dsp.synthesis_blockin(block);
                }
                int samples;
                while ((samples = dsp.synthesis_pcmout(pcmf, idx)) > 0) {
                    int bout = Math.min(samples, samplesPerChannel);
                    for (int ch = 0; ch < info.channels; ch++) {
                        float[] data = pcmf[0][ch];
                        int p = idx[ch];
                        for (int i = 0; i < bout; i++) {
                            SoundEngine.writeSample16(out, data[p + i]);
                        }
                    }
                    dsp.synthesis_read(bout);
                    if (!out.hasRemaining()) {
                        break;
                    }
                }
            }
            if (page.eos() != 0) {
                eos = true;
                break;
            }
        }
        out.flip();
        return out;
    }

    void rewind() throws IOException {
        // If underlying stream supports mark/reset, use it. Otherwise, caller should recreate via supplier.
        if (in.markSupported()) {
            try {
                in.reset();
                eos = false;
                sync.clear();
                stream.clear();
                dsp.clear();
                block.clear();
                sync.init();
                setupHeaders();
            } catch (IOException ex) {
                throw new IOException("Stream does not support reset; recreate StreamedVorbis", ex);
            }
        } else {
            throw new IOException("Cannot rewind a non-resettable InputStream. Recreate decoder.");
        }
    }

    int getSampleRate() {
        return info.rate;
    }

    int getALFormat() {
        return info.channels == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;
    }

    private void setupHeaders() throws IOException {
        if (headersReady) {
            return;
        }
        // As in VorbisDecoder.setupHeaders
        while (true) {
            int result = sync.pageout(page);
            if (result == 1) {
                break;
            }
            if (!refill()) {
                throw new IOException("EOF before first Ogg page");
            }
        }
        stream.init(page.serialno());
        stream.pagein(page);

        info.init();
        comment.init();
        if (stream.packetout(packet) != 1) {
            throw new IOException("Missing Vorbis identification header");
        }
        if (info.synthesis_headerin(comment, packet) < 0) {
            throw new IOException("Not a Vorbis stream");
        }
        for (int i = 0; i < 2; ) {
            int result = sync.pageout(page);
            if (result == 0) {
                if (!refill()) {
                    throw new IOException("EOF in Vorbis headers");
                }
                continue;
            }
            if (result == -1) {
                continue;
            }
            stream.pagein(page);
            while (i < 2 && stream.packetout(packet) == 1) {
                info.synthesis_headerin(comment, packet);
                i++;
            }
        }
        dsp.synthesis_init(info);
        block.init(dsp);
        headersReady = true;
    }

    private boolean refill() throws IOException {
        int idx = sync.buffer(4096);
        int r = in.read(sync.data, idx, 4096);
        if (r <= 0) {
            sync.wrote(0);
            return false;
        }
        sync.wrote(r);
        return true;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
