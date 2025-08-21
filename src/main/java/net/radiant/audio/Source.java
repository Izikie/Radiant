package net.radiant.audio;

public class Source {
    enum Kind {STATIC, STREAM}

    final int id;
    final Kind kind;
    final int alSource;
    int[] streamBuffers; // for STREAM
    StreamedVorbis stream; // for STREAM
    boolean looping;

    private Source(int id, Kind kind, int alSource) {
        this.id = id;
        this.kind = kind;
        this.alSource = alSource;
    }

    static Source staticSource(int id, int alSource, int buffer) {
        return new Source(id, Kind.STATIC, alSource);
    }

    static Source streamingSource(int id, int alSource, int[] buffers, StreamedVorbis stream, boolean looping) {
        Source s = new Source(id, Kind.STREAM, alSource);
        s.streamBuffers = buffers;
        s.stream = stream;
        s.looping = looping;
        return s;
    }
}
