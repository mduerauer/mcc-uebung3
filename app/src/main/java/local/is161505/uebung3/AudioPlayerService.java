package local.is161505.uebung3;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.util.Random;

public class AudioPlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener  {

    private static final String LOG_TAG = AudioPlayerService.class.getSimpleName();

    private final String mFilePath = "file:///sdcard/Music/test.mp3";

    private final IBinder mBinder = new LocalBinder();

    private final Random mGenerator = new Random();

    private MediaPlayer mMediaPlayer;

    private AudioPlayerPlayState mPlayState = AudioPlayerPlayState.STOPPED;

    public AudioPlayerPlayState getPlayState() {
        return mPlayState;
    }

    @Override
    public void onPrepared(MediaPlayer player) {
        if(mPlayState == AudioPlayerPlayState.PLAYING) {
            player.start();
        }
    }

    @Override
    public boolean onError(MediaPlayer player, int what, int extra) {
        Log.e(LOG_TAG, "Error preparing player: " + what);
        return false;
    }

    public class LocalBinder extends Binder {
        AudioPlayerService getService() {
            Log.v(LOG_TAG, "LocalBinder.getService() called.");
            return AudioPlayerService.this;
        }
    }

    public AudioPlayerService() {
        Log.v(LOG_TAG, "AudioPlayerService default constructor called.");
    }

    @Override
    public void onCreate() {
        Log.v(LOG_TAG, "onCreate() called.");
        super.onCreate();

        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setDataSource(getApplicationContext(), Uri.parse(mFilePath));
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnErrorListener(this);
        } catch(IOException ex) {
            Log.e(LOG_TAG, "Can't access file: " + mFilePath);
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(LOG_TAG, "onBind() called.");
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(LOG_TAG, "onStartCommand() called.");



        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.v(LOG_TAG, "onDestroy() called.");
        super.onDestroy();
    }

    public AudioPlayerPlayState playMusic() {
        Log.v(LOG_TAG, "playMusic() called.");

        if(mPlayState == AudioPlayerPlayState.STOPPED) {
            mMediaPlayer.prepareAsync();
            mPlayState = AudioPlayerPlayState.PLAYING;
        } else if(mPlayState == AudioPlayerPlayState.PAUSED) {
            mPlayState = AudioPlayerPlayState.PLAYING;
            mMediaPlayer.start();
        } else {
            Log.i(LOG_TAG, "Already playing.");
        }

        return mPlayState;
    }

    public AudioPlayerPlayState stopMusic() {
        Log.v(LOG_TAG, "stopMusic() called.");

        if(mPlayState == AudioPlayerPlayState.PLAYING || mPlayState == AudioPlayerPlayState.PAUSED) {
            mMediaPlayer.stop();
            mPlayState = AudioPlayerPlayState.STOPPED;
        }

        return mPlayState;
    }

    public AudioPlayerPlayState pauseMusic() {
        Log.v(LOG_TAG, "stopMusic() called.");

        if(mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            mPlayState = AudioPlayerPlayState.PAUSED;
        }

        return mPlayState;
    }

    public AudioPlayerPosition getPosition() {
        Log.v(LOG_TAG, "getPosition() called.");

        int pos = 0, dur = 0;

        if(mPlayState != AudioPlayerPlayState.STOPPED) {
            dur = mMediaPlayer.getDuration();
            pos = mMediaPlayer.getCurrentPosition();
        }

        return new AudioPlayerPosition(pos, dur);
    }

    public void seekTo(int pos) {
        mMediaPlayer.seekTo(pos);
    }
}
