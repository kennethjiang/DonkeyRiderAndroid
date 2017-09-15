package com.example.kenneth.donkeyrider;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.BoolRes;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

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

    private float getThrottle() {
        ToggleButton btn = (ToggleButton) findViewById(R.id.driveButton);
        if (!btn.isChecked()) {
            return 0;
        } else {
            Spinner spinner = (Spinner) findViewById(R.id.throttleSpinner);
            return Float.valueOf(spinner.getSelectedItem().toString().replaceAll("%", "")) / 100.0f;
        }
    }

    private float getAngle() {
        return this.angle;
    }

    private String getMode() {
        RadioButton userRadio = (RadioButton) findViewById(R.id.userRadioBtn);
        if (userRadio.isChecked()) {
            return "user";
        } else {
            return "local_angle"; // for self driving mode
        }
    }

    private Boolean getRecording() {
        ToggleButton recordingBtn = (ToggleButton) findViewById(R.id.recordingToggle);
        return recordingBtn.isChecked();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drive);
        this.provider = new OrientationProvider(this);
        this.btAdapter = BluetoothAdapter.getDefaultAdapter();

        this.initControls();
    }

    @Override
    protected void onResume() {
        super.onResume();

        this.angle = 0;

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

        Spinner spinner = (Spinner) findViewById(R.id.steeringSpinner);
        float factor = Float.valueOf(spinner.getSelectedItem().toString().replaceAll("x", ""));
        this.angle = Math.min(1.0f, this.angle * factor);

        TextView textView = (TextView) findViewById(R.id.angleText);
        textView.setText(String.format("%.3f", this.angle));
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
                String msg = String.format("%f,%f,%s,%b\n",
                        DriveActivity.this.getAngle(),
                        DriveActivity.this.getThrottle(),
                        DriveActivity.this.getMode(),
                        DriveActivity.this.getRecording());

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

    private void initControls() {
        // Throttle spinner
        Spinner spinner = (Spinner) findViewById(R.id.throttleSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.throttle_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0);

        // Steering spinner
        Spinner spinner1 = (Spinner) findViewById(R.id.steeringSpinner);
        ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(this,
                R.array.steering_array, android.R.layout.simple_spinner_item);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner1.setAdapter(adapter1);
        spinner1.setSelection(3);

        RadioButton userRadio = (RadioButton) findViewById(R.id.userRadioBtn);
        userRadio.setSelected(true);
    }
}
