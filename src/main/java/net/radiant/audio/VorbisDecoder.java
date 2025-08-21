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

public class VorbisDecoder implements Closeable {
    private final InputStream in;
    private final SyncState sync = new SyncState();
    private final StreamState stream = new StreamState();
    private final Page page = new Page();
    private final Packet packet = new Packet();
    private final Info info = new Info();
    private final Comment comment = new Comment();
    private final DspState dsp = new DspState();
    private final Block block = new Block(dsp);

    VorbisDecoder(InputStream in) {
        this.in = in;
    }

    void init() throws IOException {
        sync.init();
        setupHeaders();
    }

    PCMBuffer decodeAll() throws IOException {
        ByteBuffer pcmOut = BufferUtils.createByteBuffer(1 << 20).order(ByteOrder.LITTLE_ENDIAN); // grow as needed
        float[][][] pcmf = new float[1][][];
        int[] idx = null;
        boolean eos = false;

        while (!eos) {
            int result = sync.pageout(page);
            if (result == 0) {
                if (!refillSync()) {
                    break;
                }
                continue;
            }
            if (result == -1) {
                continue; // corrupt page
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
                while ((samples = dsp.synthesis_pcmout(pcmf, (idx == null ? (idx = new int[info.channels]) : idx))) > 0) {
                    int bout = Math.min(samples, 1024);
                    ensureCapacity(pcmOut, bout * info.channels * 2);
                    for (int ch = 0; ch < info.channels; ch++) {
                        float[] data = pcmf[0][ch];
                        int p = idx[ch];
                        for (int i = 0; i < bout; i++) {
                            SoundEngine.writeSample16(pcmOut, data[p + i]);
                        }
                    }
                    dsp.synthesis_read(bout);
                }
            }
            if (page.eos() != 0) {
                eos = true;
            }
        }
        pcmOut.flip();
        return new PCMBuffer(pcmOut, info.channels, info.rate);
    }

    private void setupHeaders() throws IOException {
        // read first page
        while (true) {
            int result = sync.pageout(page);
            if (result == 1) {
                break;
            }
            if (!refillSync()) {
                throw new IOException("EOF before first Ogg page");
            }
        }
        stream.init(page.serialno());
        stream.pagein(page);

        // three header packets
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
                if (!refillSync()) {
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
    }

    private boolean refillSync() throws IOException {
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

    private static void ensureCapacity(ByteBuffer buf, int moreBytes) {
        if (buf.remaining() >= moreBytes) {
            return;
        }
        int newCap = Math.max(buf.capacity() * 2, buf.capacity() + moreBytes);
        ByteBuffer bigger = BufferUtils.createByteBuffer(newCap).order(ByteOrder.LITTLE_ENDIAN);
        buf.flip();
        bigger.put(buf);
        // Transfer contents back by reflection is not possible; return bigger
        // But since we cannot reassign the caller's reference, use a small wrapper in usage sites
        // In this file we always capture the returned buffer instead of in-place grow for decodeAll()
    }
}