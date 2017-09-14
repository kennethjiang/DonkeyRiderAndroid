package com.example.kenneth.donkeyrider;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kenneth.donkeyrider.orientation.Orientation;
import com.example.kenneth.donkeyrider.orientation.OrientationListener;
import com.example.kenneth.donkeyrider.orientation.OrientationProvider;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class DriveActivity extends AppCompatActivity implements OrientationListener {
    public static String EXTRA_BT_REMOTE_DEIVCE_ADDRESS = "BT_REMOTE_DEIVCE_ADDRESS";

    private static UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private OrientationProvider provider;
    private BluetoothAdapter btAdapter;
    private BluetoothSocket btSocket;

    Timer timer;
    TimerTask timerTask;

    private float angle;
    private float throttle;
    private String mode;
    private String recording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drive);
        this.provider = new OrientationProvider(this);
        this.btAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    protected void onResume() {
        super.onResume();

        this.angle = 0;
        this.throttle = 0;
        this.mode = "user";
        this.recording = "false";

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (this.provider.isSupported()) {
            this.provider.startListening(this);
        } else {
            Toast.makeText(this, getText(R.string.not_supported), Toast.LENGTH_LONG).show();
        }
        final String address = getIntent().getStringExtra(DriveActivity.EXTRA_BT_REMOTE_DEIVCE_ADDRESS);
        BluetoothDevice remoteDevice = this.btAdapter.getRemoteDevice(address);
        final BluetoothSocket socket;
        try {
            this.btSocket = remoteDevice.createRfcommSocketToServiceRecord(
                    MY_UUID);
            this.btSocket.connect();
        } catch (IOException e) {
            try {
                this.btSocket.close();
            } catch (IOException e2) {
                Log.e("BluetoothService", "unable to close()", e2);
            }
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        this.startTimer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.stoptimer();
        this.provider.stopListening();
        try {
            if (this.btSocket != null) {
                this.btSocket.close();
            }
        } catch (IOException e2) {
            Log.e("BluetoothService", "unable to close()", e2);
        }
    }

    @Override
    public void onOrientationChanged(Orientation orientation, float pitch, float roll, float balance) {
        this.angle = roll/-90.0f;

        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText(String.format("pitch: %f - roll: %f - balance %f", pitch, roll, balance));
    }

    @Override
    public void onCalibrationReset(boolean success) {
        Toast.makeText(this, success ?
                        R.string.calibrate_restored : R.string.calibrate_failed,
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCalibrationSaved(boolean success) {
        Toast.makeText(this, success ?
                        R.string.calibrate_saved : R.string.calibrate_failed,
                Toast.LENGTH_LONG).show();
    }

    private void startTimer() {
        //set a new Timer
        this.timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        this.timer.schedule(timerTask, 500, 50); //
    }

    private void stoptimer() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                String msg = String.format("%f,%f,%s,%s\n",
                        DriveActivity.this.angle,
                        DriveActivity.this.throttle,
                        DriveActivity.this.mode,
                        DriveActivity.this.recording);

                if (DriveActivity.this.btSocket != null && DriveActivity.this.btSocket.isConnected()) {
                    try {
                        DriveActivity.this.btSocket.getOutputStream().write(msg.getBytes());
                    } catch (IOException e) {
                        try {
                            DriveActivity.this.btSocket.close();
                        } catch (IOException e2) {
                            Log.e("BluetoothService", "unable to close()", e2);
                        }
                        Toast.makeText(DriveActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };
    }

}
