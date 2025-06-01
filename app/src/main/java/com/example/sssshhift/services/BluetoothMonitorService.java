package com.example.sssshhift.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import java.util.Set;

public class BluetoothMonitorService extends Service {

    private static final String TAG = "BluetoothMonitorService";
    private static final String CHANNEL_ID = "BluetoothMonitorChannel";
    private static final int NOTIFICATION_ID = 2;

    private BluetoothAdapter bluetoothAdapter;
    private WifiManager wifiManager;
    private BluetoothReceiver bluetoothReceiver;
    private WifiStateReceiver wifiStateReceiver;

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();

        // Initialize Bluetooth
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Initialize WiFi
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Register receivers
        registerReceivers();

        Log.d(TAG, "BluetoothMonitorService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification());

        Log.d(TAG, "BluetoothMonitorService started");

        // Monitor current states
        monitorBluetoothState();
        monitorWifiState();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unregister receivers
        if (bluetoothReceiver != null) {
            unregisterReceiver(bluetoothReceiver);
        }
        if (wifiStateReceiver != null) {
            unregisterReceiver(wifiStateReceiver);
        }

        Log.d(TAG, "BluetoothMonitorService destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Bluetooth & WiFi Monitor",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Monitors Bluetooth and WiFi state changes");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Monitoring Connectivity")
                .setContentText("Bluetooth and WiFi monitoring active")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private void registerReceivers() {
        // Bluetooth receiver
        bluetoothReceiver = new BluetoothReceiver();
        IntentFilter bluetoothFilter = new IntentFilter();
        bluetoothFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        bluetoothFilter.addAction(BluetoothDevice.ACTION_FOUND);
        bluetoothFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        bluetoothFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        bluetoothFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        bluetoothFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(bluetoothReceiver, bluetoothFilter);

        // WiFi receiver
        wifiStateReceiver = new WifiStateReceiver();
        IntentFilter wifiFilter = new IntentFilter();
        wifiFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        wifiFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(wifiStateReceiver, wifiFilter);
    }

    private void monitorBluetoothState() {
        if (bluetoothAdapter != null) {
            int state = bluetoothAdapter.getState();
            handleBluetoothStateChange(state);
        }
    }

    private void monitorWifiState() {
        if (wifiManager != null) {
            int state = wifiManager.getWifiState();
            handleWifiStateChange(state);
        }
    }

    private void handleBluetoothStateChange(int state) {
        String stateString;
        switch (state) {
            case BluetoothAdapter.STATE_OFF:
                stateString = "OFF";
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                stateString = "TURNING ON";
                break;
            case BluetoothAdapter.STATE_ON:
                stateString = "ON";
                // When Bluetooth is on, check for paired devices
                checkPairedDevices();
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                stateString = "TURNING OFF";
                break;
            default:
                stateString = "UNKNOWN";
                break;
        }

        Log.d(TAG, "Bluetooth state changed to: " + stateString);

        // Broadcast the state change to other components
        Intent intent = new Intent("com.example.sssshhift.BLUETOOTH_STATE_CHANGED");
        intent.putExtra("state", state);
        intent.putExtra("stateString", stateString);
        sendBroadcast(intent);
    }

    private void handleWifiStateChange(int state) {
        String stateString;
        switch (state) {
            case WifiManager.WIFI_STATE_DISABLED:
                stateString = "DISABLED";
                break;
            case WifiManager.WIFI_STATE_DISABLING:
                stateString = "DISABLING";
                break;
            case WifiManager.WIFI_STATE_ENABLED:
                stateString = "ENABLED";
                break;
            case WifiManager.WIFI_STATE_ENABLING:
                stateString = "ENABLING";
                break;
            case WifiManager.WIFI_STATE_UNKNOWN:
                stateString = "UNKNOWN";
                break;
            default:
                stateString = "UNKNOWN";
                break;
        }

        Log.d(TAG, "WiFi state changed to: " + stateString);

        // Broadcast the state change to other components
        Intent intent = new Intent("com.example.sssshhift.WIFI_STATE_CHANGED");
        intent.putExtra("state", state);
        intent.putExtra("stateString", stateString);
        sendBroadcast(intent);
    }

    private void checkPairedDevices() {
        if (bluetoothAdapter != null && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {

            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

            if (pairedDevices.size() > 0) {
                Log.d(TAG, "Found " + pairedDevices.size() + " paired devices:");
                for (BluetoothDevice device : pairedDevices) {
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress();
                    Log.d(TAG, "Device: " + deviceName + " [" + deviceHardwareAddress + "]");
                }
            }
        }
    }

    // Bluetooth Broadcast Receiver
    private class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                handleBluetoothStateChange(state);

            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && ActivityCompat.checkSelfPermission(context,
                        Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress();
                    Log.d(TAG, "Device found: " + deviceName + " [" + deviceHardwareAddress + "]");
                }

            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && ActivityCompat.checkSelfPermission(context,
                        Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Device connected: " + device.getName());
                }

            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && ActivityCompat.checkSelfPermission(context,
                        Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Device disconnected: " + device.getName());
                }
            }
        }
    }

    // WiFi State Receiver
    private class WifiStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                        WifiManager.WIFI_STATE_UNKNOWN);
                handleWifiStateChange(state);
            }
        }
    }
}