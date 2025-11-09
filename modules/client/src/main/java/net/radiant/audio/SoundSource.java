package net.radiant.audio;

import java.nio.ShortBuffer;

import static org.lwjgl.openal.AL10.*;

public class SoundSource implements ISoundSource {

    private final int buffer, source;
    private float baseVolume = 1.0f;

    SoundSource(ShortBuffer pcm, int channels, int sampleRate) {
        buffer = alGenBuffers();
        source = alGenSources();
        int format = channels == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;
        alBufferData(buffer, format, pcm, sampleRate);
        alSourcei(source, AL_BUFFER, buffer);
    }

    @Override
    public void play() {
        alSourcePlay(source);
    }

    @Override
    public void stop() {
        alSourceStop(source);
    }

    @Override
    public void pause() {
        alSourcePause(source);
    }

    @Override
    public void setVolume(float volume) {
        baseVolume = volume;
    }

    @Override
    public void setPitch(float pitch) {
        alSourcef(source, AL_PITCH, pitch);
    }

    @Override
    public void updateVolumeWithMaster(float masterVolume) {
        alSourcef(source, AL_GAIN, baseVolume * masterVolume);
    }

    @Override
    public void setLooping(boolean loop) {
        alSourcei(source, AL_LOOPING, loop ? 1 : 0);
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
    public void cleanup() {
        alDeleteSources(source);
        alDeleteBuffers(buffer);
    }

    @Override
    public boolean playing() {
        return alGetSourcei(source, AL_SOURCE_STATE) == AL_PLAYING;
    }

    @Override
    public boolean paused() {
        return alGetSourcei(source, AL_SOURCE_STATE) == AL_PAUSED;
    }
}
