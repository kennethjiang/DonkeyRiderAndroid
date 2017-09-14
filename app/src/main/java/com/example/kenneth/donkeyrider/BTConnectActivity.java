package com.example.kenneth.donkeyrider;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BTConnectActivity extends ListActivity {
    private BluetoothAdapter btAdapter;
    private List<BluetoothDevice> pairedDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.btAdapter = BluetoothAdapter.getDefaultAdapter();

        ArrayAdapter<String> pairedDevicesArrayAdapter =
                new ArrayAdapter<String>(this, R.layout.device_name);
        setListAdapter(pairedDevicesArrayAdapter);
        this.getListView().setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        try {
                            BTConnectActivity.this.btAdapter.cancelDiscovery();
                            final BluetoothDevice device = BTConnectActivity.this.pairedDevices.get(position);

                            Intent intent = new Intent(BTConnectActivity.this, DriveActivity.class);
                            intent.putExtra(DriveActivity.EXTRA_BT_REMOTE_DEIVCE_ADDRESS, device.getAddress());
                            startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(BTConnectActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        // Get a set of currently paired devices
        this.pairedDevices = new ArrayList(this.btAdapter.getBondedDevices());

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    }
}