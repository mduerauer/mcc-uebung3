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
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

public class AudioPlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener  {

    private static final String LOG_TAG = AudioPlayerService.class.getSimpleName();

    public static final int ONGOING_NOTIFICATION_ID = 254;

    private final String mFilePath = "file:///sdcard/Music/test.mp3";

    private final IBinder mBinder = new LocalBinder();

    private MediaPlayer mMediaPlayer;

    private boolean mDataSourceSet = false;

    private boolean mPrepared = false;

    private AudioPlayerPlayState mPlayState = AudioPlayerPlayState.STOPPED;

    public AudioPlayerPlayState getPlayState() {
        return mPlayState;
    }

    private Notification mForegroundNotification;

    @Override
    public void onPrepared(MediaPlayer player) {
        mPrepared = true;
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
        mPrepared = false;

        stopForeground(true);
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


        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);


        if(Build.VERSION.SDK_INT > 15) {
            mForegroundNotification = new Notification.Builder(this)
                    .setContentTitle(getString(R.string.notification_title))
                    .setContentText(getString(R.string.notification_text))
                    .setSmallIcon(R.drawable.ic_headset_black_24dp)
                    .setOngoing(true)
                    .setContentIntent(pendingIntent)
                    .build();
        } else {
            mForegroundNotification = new Notification.Builder(this)
                    .setContentTitle(getString(R.string.notification_title))
                    .setContentText(getString(R.string.notification_text))
                    .setSmallIcon(R.drawable.ic_headset_black_24dp)
                    .setOngoing(true)
                    .setContentIntent(pendingIntent)
                    .getNotification();
        }

            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setOnCompletionListener(this);

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

            if(!mDataSourceSet) {
                try {
                    mMediaPlayer.setDataSource(getApplicationContext(), Uri.parse(mFilePath));
                    mDataSourceSet = true;
                } catch (IOException ex) {
                    Toast.makeText(this, "Can't access file: " + mFilePath, Toast.LENGTH_LONG).show();
                    return AudioPlayerPlayState.STOPPED;
                }
            }

            mPlayState = AudioPlayerPlayState.PLAYING;
            if(!mPrepared) {
                mMediaPlayer.prepareAsync();
            }

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
            mPrepared = false;
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
