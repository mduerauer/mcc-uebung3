package local.is161505.uebung3;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.SyncStateContract;
import android.util.Log;

import java.io.IOException;

public class AudioPlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener  {

    private static final String LOG_TAG = AudioPlayerService.class.getSimpleName();

    public static final int ONGOING_NOTIFICATION_ID = 254;

    private final String mFilePath = "file:///sdcard/Music/test.mp3";

    private final IBinder mBinder = new LocalBinder();

    private MediaPlayer mMediaPlayer;

    private AudioPlayerPlayState mPlayState = AudioPlayerPlayState.STOPPED;

    public AudioPlayerPlayState getPlayState() {
        return mPlayState;
    }

    private Notification mForegroundNotification;

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

    @Override
    public void onCompletion(MediaPlayer mp) {
        mMediaPlayer.stop();
        mPlayState = AudioPlayerPlayState.STOPPED;
    }

    public class LocalBinder extends Binder {
        AudioPlayerService getService() {
            Log.v(LOG_TAG, "LocalBinder.getService() called.");
            return AudioPlayerService.this;
        }
    }

    @Override
    public void onCreate() {
        Log.v(LOG_TAG, "onCreate() called.");
        super.onCreate();

        if(Build.VERSION.SDK_INT > 15) {
            mForegroundNotification = new Notification.Builder(this)
                    .setContentTitle(getString(R.string.notification_title))
                    .setContentText(getString(R.string.notification_text))
                    .setSmallIcon(R.drawable.ic_headset_black_24dp)
                    .setOngoing(true)
                    .build();
        } else {
            mForegroundNotification = new Notification.Builder(this)
                    .setContentTitle(getString(R.string.notification_title))
                    .setContentText(getString(R.string.notification_text))
                    .setSmallIcon(R.drawable.ic_headset_black_24dp)
                    .setOngoing(true)
                    .getNotification();
        }

        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setDataSource(getApplicationContext(), Uri.parse(mFilePath));
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setOnCompletionListener(this);
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

        startForeground(ONGOING_NOTIFICATION_ID, mForegroundNotification);

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

        stopForeground(true);

        if(mPlayState == AudioPlayerPlayState.PLAYING || mPlayState == AudioPlayerPlayState.PAUSED) {
            mMediaPlayer.stop();
            mPlayState = AudioPlayerPlayState.STOPPED;
        }

        return mPlayState;
    }

    public AudioPlayerPlayState pauseMusic() {
        Log.v(LOG_TAG, "pauseMusic() called.");

        if(mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            mPlayState = AudioPlayerPlayState.PAUSED;
        }

        return mPlayState;
    }

    public AudioPlayerPosition getPosition() {
        int pos = 0, dur = 0;

        if(mMediaPlayer.isPlaying()) {
            dur = mMediaPlayer.getDuration();
            pos = mMediaPlayer.getCurrentPosition();
        }

        return new AudioPlayerPosition(pos, dur);
    }

    public void seekTo(int pos) {
        Log.v(LOG_TAG, "seekTo() called.");
        mMediaPlayer.seekTo(pos);
    }
}
