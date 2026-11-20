package ir.jabheh.sharghi;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;

import java.util.Random;

/** Short synthesized paper-rustle sound, so no sound file is needed. */
final class FlipSound {
    private static final int SR = 22050;
    private final short[] data;
    private final Handler handler = new Handler(Looper.getMainLooper());

    FlipSound() {
        int n = SR * 16 / 100;
        data = new short[n];
        Random r = new Random(11);
        float lp = 0f;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            float env = (float) (Math.pow(1f - t, 2.2) * Math.min(1f, t * 30f));
            float x = r.nextFloat() * 2f - 1f;
            lp += 0.30f * (x - lp);
            float v = (x - lp) * env;
            data[i] = (short) (v * 11000f);
        }
    }

    void play() {
        try {
            final AudioTrack t = new AudioTrack(AudioManager.STREAM_MUSIC, SR,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    data.length * 2, AudioTrack.MODE_STATIC);
            t.write(data, 0, data.length);
            t.play();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        t.release();
                    } catch (Throwable ignored) {
                    }
                }
            }, 500);
        } catch (Throwable ignored) {
        }
    }
}
