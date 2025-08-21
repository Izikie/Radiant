package net.radiant.audio;

import java.nio.ByteBuffer;

import static org.lwjgl.openal.AL10.AL_FORMAT_MONO16;
import static org.lwjgl.openal.AL10.AL_FORMAT_STEREO16;

public class PCMBuffer {
    final ByteBuffer data; // little-endian, interleaved 16-bit
    final int channels;
    final int sampleRate;
    final int alFormat; // AL_FORMAT_MONO16/AL_FORMAT_STEREO16

    PCMBuffer(ByteBuffer data, int channels, int sampleRate) {
        this.data = data;
        this.channels = channels;
        this.sampleRate = sampleRate;
        this.alFormat = (channels == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16);
    }
}
