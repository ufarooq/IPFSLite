package threads.server;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import androidx.annotation.NonNull;
import androidx.core.util.Preconditions;

import static android.content.Context.AUDIO_SERVICE;

public class RTCSoundPool {

    private boolean playing = false;
    private boolean loaded = false;
    private boolean playingCalled = false;
    private float volume = 0f;
    private SoundPool soundPool;
    private int ringingSoundId;
    private int ringingStreamId;

    private RTCSoundPool(@NonNull Context context, int resId) {
        Preconditions.checkNotNull(context);


        // AudioManager audio settings for adjusting the volume
        AudioManager audioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);
        float actualVolume = 0f;
        float maxVolume = 0f;
        if (audioManager != null) {
            actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            volume = actualVolume / maxVolume;
        }


        // Load the sounds
        int maxStreams = 1;

        soundPool = new SoundPool.Builder().setMaxStreams(maxStreams).build();

        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                loaded = true;
                if (playingCalled) {
                    play();
                    playingCalled = false;
                }
            }

        });
        ringingSoundId = soundPool.load(context, resId, 1);
    }

    public static RTCSoundPool create(Context context, int resId) {
        return new RTCSoundPool(context, resId);
    }

    public void play() {
        if (loaded && !playing) {
            ringingStreamId = soundPool.play(ringingSoundId, volume, volume, 1, -1, 1f);
            playing = true;
        } else {
            playingCalled = true;
        }
    }

    public void stop() {
        if (playing) {
            soundPool.stop(ringingStreamId);
            playing = false;
        }
    }

    public void release() {
        if (soundPool != null) {
            soundPool.unload(ringingSoundId);
            soundPool.release();
        }
    }

}
