package local.is161505.uebung3;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final int PAUSE_FOR_MILLIS = 250;

    private static final int MY_PERMISSIONS_READ_EXTERNAL_STORAGE = 123;

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private AudioPlayerService mAudioPlayerService;

    private Handler mHandler;

    private boolean mBound = false;

    private AudioPlayerPlayState mPlayState = AudioPlayerPlayState.STOPPED;

    private SeekBar mUiPositionSlider = null;

    private boolean mHasPermissions = false;

    private Runnable mSliderUpdater = new Runnable() {
        @Override
        public void run() {
            try {
                updateSliderPosition();
            } finally {
                mHandler.postDelayed(mSliderUpdater, PAUSE_FOR_MILLIS);
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreate() called.");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button buttonPlay = (Button) findViewById(R.id.buttonPlay);
        buttonPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playMusic();
            }
        });

        final Button buttonStop = (Button) findViewById(R.id.buttonStop);
        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopMusic();
            }
        });

        final Button buttonPause = (Button) findViewById(R.id.buttonPause);
        buttonPause.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        pauseMusic();
                    }
                }
        );

        mUiPositionSlider = (SeekBar) findViewById(R.id.seekBarPosition);
        mUiPositionSlider.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if(fromUser) {
                            updatePosition(progress);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) { }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) { }
                }
        );

        mHandler = new Handler();

        checkPermissions();

    }

    private void checkPermissions() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    MY_PERMISSIONS_READ_EXTERNAL_STORAGE);
        } else {
            mHasPermissions = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)  {
                    mHasPermissions = true;
                } else {

                    Log.e(LOG_TAG, "Permissions not granted.");
                    Toast.makeText(this, "Permissions not granted!", Toast.LENGTH_LONG).show();
                    mHasPermissions = false;
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void updatePosition(int position) {
        Log.v(LOG_TAG, "updatePosition() called.");
        mAudioPlayerService.seekTo(position);
    }

    @Override
    protected void onStart() {
        Log.d(LOG_TAG, "onStart() called.");
        super.onStart();

        // Bind to Service
        if(!mBound) {
            bindAudioPlayerService();
        }
    }

    private void bindAudioPlayerService() {
        // Bind to LocalService
        Intent intent = new Intent(this, AudioPlayerService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }


    @Override
    protected void onStop() {
        Log.d(LOG_TAG, "onStop() called.");
        super.onStop();

        // Unbind service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }

    }

    @Override
    protected void onResume() {
        Log.d(LOG_TAG, "onResume() called.");
        super.onResume();

        // Rebind service
        if(!mBound) {
            bindAudioPlayerService();
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "onDestroy() called.");
        super.onDestroy();
        stopSliderUpdater();
    }

    private void playMusic() {
        Log.d(LOG_TAG, "playMusic() called.");

        if(!mHasPermissions) {

            Toast.makeText(this, "Permissions not granted!", Toast.LENGTH_LONG).show();
            return;
        }

        if (mBound) {
            mPlayState = mAudioPlayerService.playMusic();
            Toast.makeText(this, "play state: " +  mPlayState, Toast.LENGTH_SHORT).show();

            if(mPlayState == AudioPlayerPlayState.PLAYING) {
                startSliderUpdater();
            }
        }

    }

    private void pauseMusic() {
        Log.d(LOG_TAG, "pauseMusic() called.");

        if (mBound) {
            mPlayState = mAudioPlayerService.pauseMusic();
            Toast.makeText(this, "play state: " +  mPlayState, Toast.LENGTH_SHORT).show();

            if(mPlayState == AudioPlayerPlayState.PAUSED) {
                stopSliderUpdater();
            }
        }

    }

    private void stopMusic() {
        Log.d(LOG_TAG, "stopMusic() called.");

        if (mBound) {
            mPlayState = mAudioPlayerService.stopMusic();
            Toast.makeText(this, "play state: " + mPlayState, Toast.LENGTH_SHORT).show();

            if(mPlayState == AudioPlayerPlayState.STOPPED) {
                stopSliderUpdater();
                updateSliderPosition();
            }
        }

    }

    private void startSliderUpdater() {
        Log.d(LOG_TAG, "startSliderUpdater() called.");

        mSliderUpdater.run();
    }

    private void stopSliderUpdater() {
        Log.d(LOG_TAG, "stopSliderUpdater() called.");

        mHandler.removeCallbacks(mSliderUpdater);
    }

    private void updateSliderPosition() {
        AudioPlayerPosition position = null;

        if(mBound) {
            position = mAudioPlayerService.getPosition();
            final TextView tvPos = (TextView) findViewById(R.id.textViewPosition);
            tvPos.setText(position.toString());

            if(mUiPositionSlider.getMax() != position.getDuration()) {
                mUiPositionSlider.setMax(position.getDuration());
            }

            mUiPositionSlider.setProgress(position.getPosition());
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.d(LOG_TAG, "onServiceConnected() called.");

            AudioPlayerService.LocalBinder binder = (AudioPlayerService.LocalBinder) service;
            mAudioPlayerService = binder.getService();

            mPlayState = mAudioPlayerService.getPlayState();

            updateSliderPosition();
            if(mPlayState == AudioPlayerPlayState.PLAYING) {
                startSliderUpdater();
            }

            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
}
