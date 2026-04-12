package com.example.spaceinvaders_activity;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.SoundPool;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Generates procedural sound effects using PCM synthesis.
 * Sounds are generated as WAV data and loaded into SoundPool for playback.
 * No external audio files needed for new skills/abilities.
 */
public class SFXGenerator {

    private static final int SAMPLE_RATE = 22050;

    // SoundPool IDs for generated sounds
    public int homingMissileID = -1;
    public int chainLightningID = -1;
    public int shieldReflectID = -1;
    public int barrageFireID = -1;
    public int blackHoleID = -1;
    public int allyDroneFireID = -1;
    public int bossAppearID = -1;
    public int timeStopID = -1;
    public int supernovaID = -1;
    public int freezeWaveID = -1;
    public int powerUpCollectID = -1;
    public int zoneTransitionID = -1;
    public int criticalHitID = -1;
    public int warpID = -1;
    public int bomberExplodeID = -1;

    /**
     * Generate all procedural sounds and load into the SoundPool.
     * Call once during initialization.
     */
    public void generateAll(SoundPool soundPool) {
        // Each sound is a short PCM16 waveform generated mathematically
        homingMissileID = loadGenerated(soundPool, generateSweep(200, 800, 0.15f, 0.6f));
        chainLightningID = loadGenerated(soundPool, generateNoiseBurst(0.2f, 0.5f, 800));
        shieldReflectID = loadGenerated(soundPool, generateSweep(1000, 300, 0.12f, 0.5f));
        barrageFireID = loadGenerated(soundPool, generateRapidPops(0.25f, 6));
        blackHoleID = loadGenerated(soundPool, generateSweep(80, 40, 0.5f, 0.4f));
        allyDroneFireID = loadGenerated(soundPool, generateTone(1200, 0.08f, 0.3f));
        bossAppearID = loadGenerated(soundPool, generateBossAppear());
        timeStopID = loadGenerated(soundPool, generateSweepWithEcho(400, 100, 0.4f));
        supernovaID = loadGenerated(soundPool, generateSupernova());
        freezeWaveID = loadGenerated(soundPool, generateSweep(2000, 500, 0.3f, 0.4f));
        powerUpCollectID = loadGenerated(soundPool, generateArpeggio(new int[]{400, 800, 1200}, 0.2f));
        zoneTransitionID = loadGenerated(soundPool, generateChord(new int[]{300, 375, 450}, 0.6f));
        criticalHitID = loadGenerated(soundPool, generateSweep(500, 1500, 0.1f, 0.5f));
        warpID = loadGenerated(soundPool, generateSweep(800, 200, 0.2f, 0.4f));
        bomberExplodeID = loadGenerated(soundPool, generateNoiseBurst(0.3f, 0.7f, 200));
    }

    // --- Sound generation methods ---

    /** Frequency sweep from startHz to endHz */
    private byte[] generateSweep(float startHz, float endHz, float durationSec, float volume) {
        int numSamples = (int) (SAMPLE_RATE * durationSec);
        short[] samples = new short[numSamples];

        for (int i = 0; i < numSamples; i++) {
            float t = (float) i / SAMPLE_RATE;
            float progress = (float) i / numSamples;
            float freq = startHz + (endHz - startHz) * progress;
            float envelope = 1.0f - progress; // Fade out
            float val = (float) Math.sin(2 * Math.PI * freq * t) * envelope * volume;
            samples[i] = (short) (val * Short.MAX_VALUE);
        }
        return toWav(samples);
    }

    /** White noise burst with lowpass decay */
    private byte[] generateNoiseBurst(float durationSec, float volume, float filterFreq) {
        int numSamples = (int) (SAMPLE_RATE * durationSec);
        short[] samples = new short[numSamples];
        float prev = 0;
        float alpha = filterFreq / (filterFreq + SAMPLE_RATE);

        for (int i = 0; i < numSamples; i++) {
            float progress = (float) i / numSamples;
            float envelope = 1.0f - progress;
            float noise = (float) (Math.random() * 2 - 1);
            prev = prev + alpha * (noise - prev); // Simple lowpass
            float val = prev * envelope * volume;
            // Add crackle
            if (Math.random() < 0.05) val *= 2;
            samples[i] = (short) (Math.max(-1, Math.min(1, val)) * Short.MAX_VALUE);
        }
        return toWav(samples);
    }

    /** Rapid pops (for barrage fire) */
    private byte[] generateRapidPops(float durationSec, int popCount) {
        int numSamples = (int) (SAMPLE_RATE * durationSec);
        short[] samples = new short[numSamples];
        int samplesPerPop = numSamples / popCount;

        for (int p = 0; p < popCount; p++) {
            int start = p * samplesPerPop;
            for (int i = 0; i < samplesPerPop && (start + i) < numSamples; i++) {
                float t = (float) i / SAMPLE_RATE;
                float envelope = Math.max(0, 1.0f - (float) i / (samplesPerPop * 0.3f));
                float val = (float) Math.sin(2 * Math.PI * 800 * t) * envelope * 0.5f;
                samples[start + i] = (short) (val * Short.MAX_VALUE);
            }
        }
        return toWav(samples);
    }

    /** Simple sine tone */
    private byte[] generateTone(float freq, float durationSec, float volume) {
        int numSamples = (int) (SAMPLE_RATE * durationSec);
        short[] samples = new short[numSamples];

        for (int i = 0; i < numSamples; i++) {
            float t = (float) i / SAMPLE_RATE;
            float envelope = 1.0f - (float) i / numSamples;
            float val = (float) Math.sin(2 * Math.PI * freq * t) * envelope * volume;
            samples[i] = (short) (val * Short.MAX_VALUE);
        }
        return toWav(samples);
    }

    /** Boss appear: descending sweep + rumble */
    private byte[] generateBossAppear() {
        int numSamples = (int) (SAMPLE_RATE * 0.8f);
        short[] samples = new short[numSamples];

        for (int i = 0; i < numSamples; i++) {
            float t = (float) i / SAMPLE_RATE;
            float progress = (float) i / numSamples;
            // Descending sweep
            float freq = 600 - 400 * progress;
            float sweep = (float) Math.sin(2 * Math.PI * freq * t);
            // Low rumble
            float rumble = (float) Math.sin(2 * Math.PI * 50 * t);
            float envelope = 1.0f - progress * 0.7f;
            float val = (sweep * 0.4f + rumble * 0.3f) * envelope;
            samples[i] = (short) (val * Short.MAX_VALUE);
        }
        return toWav(samples);
    }

    /** Sweep with echo effect */
    private byte[] generateSweepWithEcho(float startHz, float endHz, float durationSec) {
        int numSamples = (int) (SAMPLE_RATE * durationSec);
        short[] samples = new short[numSamples];
        int echoDelay = SAMPLE_RATE / 10; // 100ms echo

        for (int i = 0; i < numSamples; i++) {
            float t = (float) i / SAMPLE_RATE;
            float progress = (float) i / numSamples;
            float freq = startHz + (endHz - startHz) * progress;
            float envelope = 1.0f - progress;
            float val = (float) Math.sin(2 * Math.PI * freq * t) * envelope * 0.4f;
            // Add echo from earlier sample
            if (i >= echoDelay) {
                val += samples[i - echoDelay] / (float) Short.MAX_VALUE * 0.3f;
            }
            samples[i] = (short) (Math.max(-1, Math.min(1, val)) * Short.MAX_VALUE);
        }
        return toWav(samples);
    }

    /** Supernova: noise + low sweep + sine chord */
    private byte[] generateSupernova() {
        int numSamples = (int) (SAMPLE_RATE * 1.0f);
        short[] samples = new short[numSamples];

        for (int i = 0; i < numSamples; i++) {
            float t = (float) i / SAMPLE_RATE;
            float progress = (float) i / numSamples;
            // Explosion noise
            float noise = (float) (Math.random() * 2 - 1) * Math.max(0, 1 - progress * 2);
            // Low sweep
            float freq = 200 - 150 * progress;
            float sweep = (float) Math.sin(2 * Math.PI * freq * t) * 0.3f;
            // High chord (appears after initial blast)
            float chord = 0;
            if (progress > 0.3f) {
                float chordEnv = Math.min(1, (progress - 0.3f) * 3) * (1 - progress);
                chord = ((float) Math.sin(2 * Math.PI * 440 * t) +
                        (float) Math.sin(2 * Math.PI * 550 * t) +
                        (float) Math.sin(2 * Math.PI * 660 * t)) / 3 * chordEnv * 0.3f;
            }
            float val = noise * 0.4f + sweep + chord;
            samples[i] = (short) (Math.max(-1, Math.min(1, val)) * Short.MAX_VALUE);
        }
        return toWav(samples);
    }

    /** Ascending arpeggio (quick notes) */
    private byte[] generateArpeggio(int[] freqs, float totalDuration) {
        int numSamples = (int) (SAMPLE_RATE * totalDuration);
        short[] samples = new short[numSamples];
        int samplesPerNote = numSamples / freqs.length;

        for (int n = 0; n < freqs.length; n++) {
            int start = n * samplesPerNote;
            for (int i = 0; i < samplesPerNote && (start + i) < numSamples; i++) {
                float t = (float) i / SAMPLE_RATE;
                float envelope = 1.0f - (float) i / samplesPerNote;
                float val = (float) Math.sin(2 * Math.PI * freqs[n] * t) * envelope * 0.4f;
                samples[start + i] = (short) (val * Short.MAX_VALUE);
            }
        }
        return toWav(samples);
    }

    /** Musical chord (multiple simultaneous sines) */
    private byte[] generateChord(int[] freqs, float durationSec) {
        int numSamples = (int) (SAMPLE_RATE * durationSec);
        short[] samples = new short[numSamples];

        for (int i = 0; i < numSamples; i++) {
            float t = (float) i / SAMPLE_RATE;
            float envelope = 1.0f - (float) i / numSamples;
            float val = 0;
            for (int freq : freqs) {
                val += (float) Math.sin(2 * Math.PI * freq * t);
            }
            val = val / freqs.length * envelope * 0.35f;
            samples[i] = (short) (val * Short.MAX_VALUE);
        }
        return toWav(samples);
    }

    // --- WAV encoding ---

    /** Convert PCM16 samples to WAV byte array */
    private byte[] toWav(short[] samples) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        int dataSize = samples.length * 2;
        int fileSize = 36 + dataSize;

        try {
            // RIFF header
            dos.writeBytes("RIFF");
            dos.writeInt(Integer.reverseBytes(fileSize));
            dos.writeBytes("WAVE");

            // fmt chunk
            dos.writeBytes("fmt ");
            dos.writeInt(Integer.reverseBytes(16)); // chunk size
            dos.writeShort(Short.reverseBytes((short) 1)); // PCM
            dos.writeShort(Short.reverseBytes((short) 1)); // mono
            dos.writeInt(Integer.reverseBytes(SAMPLE_RATE));
            dos.writeInt(Integer.reverseBytes(SAMPLE_RATE * 2)); // byte rate
            dos.writeShort(Short.reverseBytes((short) 2)); // block align
            dos.writeShort(Short.reverseBytes((short) 16)); // bits per sample

            // data chunk
            dos.writeBytes("data");
            dos.writeInt(Integer.reverseBytes(dataSize));
            for (short s : samples) {
                dos.writeShort(Short.reverseBytes(s));
            }

            dos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bos.toByteArray();
    }

    /** Load a WAV byte array into SoundPool, returns sound ID */
    private int loadGenerated(SoundPool soundPool, byte[] wavData) {
        try {
            // Write to temp file and load (SoundPool needs a file descriptor)
            java.io.File tempFile = java.io.File.createTempFile("sfx_", ".wav");
            tempFile.deleteOnExit();
            java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
            fos.write(wavData);
            fos.close();
            int id = soundPool.load(tempFile.getAbsolutePath(), 1);
            return id;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }
}
