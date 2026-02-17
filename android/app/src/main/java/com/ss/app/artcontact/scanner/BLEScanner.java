package com.ss.app.artcontact.scanner;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.ss.app.artcontact.MainActivity;
import com.ss.app.artcontact.advertising.AdvertisingPacket;
import com.ss.app.artcontact.advertising.AdvertisingPacketQueue;
import com.ss.app.artcontact.beacon.BeaconCollector;
import com.ss.app.artcontact.services.AppService;
import com.ss.app.artcontact.storage.DataHolder;
import com.ss.app.artcontact.utils.Log;
import com.ss.app.artcontact.utils.WiFiUtils;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BLEScanner {

    private BluetoothLeScanner bluetoothLeScanner;
    Context context;
    boolean debugScan = true;
    boolean isScanning = false;
    AdvertisingPacketQueue advertisingPackets;
    String uuid = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public BLEScanner(Context context, AdvertisingPacketQueue advertisingPackets) {
        this.advertisingPackets = advertisingPackets;
        this.context = context;
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Log.l(1, "No Support for bluetooth");
            MainActivity.me.uiHandler.sendEmptyMessage(0);
            return;
        } else if (!mBluetoothAdapter.isEnabled()) {
            // Bluetooth is not enabled :)
            Log.l(1, "Bluetooth is not enabled");
            MainActivity.me.uiHandler.sendEmptyMessage(1);
            MainActivity.me.uiHandler.sendEmptyMessage(2);
        } else {
            MainActivity.me.uiHandler.sendEmptyMessage(2);
            this.bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        }

        Log.l(1,"connect WiFi");
        WiFiUtils.init();
    }

    public boolean isScanning() {
        return isScanning;
    }

    public void startScanning() {
        if (isScanning) {
            return;
        }
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter==null) return;
        this.bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        if (bluetoothLeScanner==null) return;
        if (!bluetoothAdapter.isEnabled()) return;
        if (!MainActivity.me.gpsCheck()) return;

        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build();
        List<ScanFilter> scanFilters = new ArrayList<>();

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.l(1,"BLUETOOTH_SCAN not granted request permission");
            MainActivity.me.checkAndRequestPermissions();
        }

        bluetoothLeScanner.startScan(scanFilters, scanSettings, leScanCallback);
        isScanning = true;
    }

    public void stopScanning() {
        if (!isScanning) {
            return;
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothLeScanner.stopScan(leScanCallback);
        isScanning=false;
    }

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            String names = result.getScanRecord().getDeviceName();
            Log.l(0,"? BLE name="+names);
            if (names == null) return;
            //StringBuilder sb = new StringBuilder();
            //sb.append(names);
            //sb.append("->");
            String name = names;
            Log.l(0,"BLE name="+name);
            if (DataHolder.jbeaconRefTable == null) {
                Log.l(1, "jbeaconRefTable is null");
                return;
            }
            if (!DataHolder.jbeaconRefTable.has(name)) {
                Log.l(0, "jbeaconRefTable hasn't " + name);
                return;
            }
            Log.l(1, "scanner found beacon " + name + " rssi=" + result.getRssi());
            advertisingPackets.push(new AdvertisingPacket(name,result.getRssi()));
        }
    };

}
