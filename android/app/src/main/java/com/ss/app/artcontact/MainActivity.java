package com.ss.app.artcontact;

import android.Manifest;
import android.app.DownloadManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.ss.app.artcontact.advertising.AdvertisingPacket;
import com.ss.app.artcontact.beacon.Beacon;
import com.ss.app.artcontact.beacon.BeaconCollector;
import com.ss.app.artcontact.network.ServerManager;
import com.ss.app.artcontact.player.Player;
import com.ss.app.artcontact.player.UniversalVideoView;
import com.ss.app.artcontact.services.AppService;
import com.ss.app.artcontact.storage.DataHolder;
import com.ss.app.artcontact.utils.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends AppCompatActivity  {

    ListView listView;
    LinearLayout videoLayout;
    UniversalVideoView videoView;
    LinearLayout beaconLayout;
    public static MainActivity me;
    Player player;
    public Handler uiHandler;
    private int ntap=0;
    private long lastTapTS=0;
    private Menu menu;

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setIcon(R.drawable.macicon);
        Log.l(1, "onCreate");
        me = this;
        DataHolder.init(this.getPreferences(Context.MODE_PRIVATE));
        checkAndRequestPermissions();
        videoLayout = findViewById(R.id.videoLayout);
        videoView = findViewById(R.id.videoView);
        beaconLayout = findViewById(R.id.beaconLayout);
        listView = findViewById(R.id.beaconInfoList);
        ToggleButton lang = findViewById(R.id.lang);
        setLang(lang);
        lang.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.l(1, "current Lang=" + DataHolder.getLang());
                String storedLang = DataHolder.getLang();
                if (storedLang.equals("it")) DataHolder.setLang("en");
                else DataHolder.setLang("it");
                setLang(lang);
                Log.l(1, "after Lang=" + DataHolder.getLang());
            }
        });
        createDownloadServiceManager(false);
        createSensorsServiceManager(false);
        player = getPlayer();
        createBeaconCollector(false);

        Button scanBtn = findViewById(R.id.scanBtn);
        scanBtn.setOnClickListener(v -> {
            // we need to create the object
            // of IntentIntegrator class
            // which is the class of QR library
            IntentIntegrator intentIntegrator = new IntentIntegrator(this);
            intentIntegrator.setPrompt("Scan a barcode or QR Code");
            intentIntegrator.setOrientationLocked(true);
            intentIntegrator.initiateScan();
            // Initiate the scan, which opens the camera.
            // Once the scan is completed or cancelled, onActivityResult will be called.
        });

        ImageView toclogo = findViewById(R.id.appLogo);
        toclogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.l(1,"toc logo click ntap="+ntap);
                long now = System.currentTimeMillis();
                lastTapTS=now;
                if (now-lastTapTS > 1500) {
                    Log.l(1,"reset ntap");
                    ntap=0;
                    return;
                } else {
                    Log.l(1,"increase ntap");
                    ntap++;
                }
                if (ntap>=10) {
                    Log.l(1,"switch settings");
                    MenuItem settingsItem = menu.getItem(0);
                    Log.l(1,"switch settings "+settingsItem.getTitle());
                    if (settingsItem.isVisible()) settingsItem.setVisible(false);
                    else settingsItem.setVisible(true);
                    ntap=0;
                }
            }
        });

        uiHandler = new Handler(Looper.getMainLooper()) {
            public void handleMessage(Message msg) {
                Log.l(1, "uiCallback  " + this.getClass().getSimpleName());
                if (msg.what == 0) {
                    new AlertDialog.Builder(me)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle("NO BLUETOOTH ADAPTER")
                            .setMessage("Device doesn't support bluetooth, exit?")
                            .setPositiveButton("Yes", (dialog, which) -> {
                                finish();
                            })
                            .setNegativeButton("No", null)
                            .show();
                }
                if (msg.what == 1) {
                    Log.l(1, "Alert BLUETOOTH DISABLED");
                    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (ActivityCompat.checkSelfPermission(me, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        checkAndRequestPermissions();
                        return;
                    }
                    Log.l(1, "BLUETOOTH ENABLE");
                    mBluetoothAdapter.enable();
                }
                if (msg.what == 2) {
                    Log.l(1, "activateGPS");
                    activateGPS();
                }
            }
        };

    }

    public boolean checkAndRequestPermissions() {

        List<String> listPermissionsNeeded = new ArrayList<>();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_NETWORK_STATE);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CHANGE_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CHANGE_NETWORK_STATE);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.INTERNET);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_WIFI_STATE);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CHANGE_WIFI_STATE);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.BLUETOOTH);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.BLUETOOTH_ADMIN);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray
                    (new String[0]), 1);
            return false;
        }
        return true;
    }

    public boolean gpsCheck() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public void activateGPS() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void setLang(ToggleButton lang) {
        if (player!=null && player.isPlaying()) player.stop();
        String storedLang = DataHolder.getLang();
        TextView contentsLabel = findViewById(R.id.contentsLabel);
        if (storedLang.equals("it")) {
            lang.setBackgroundResource(R.drawable.it_flag);
            contentsLabel.setText("Contenuti Intorno a Te");
        } else {
            lang.setBackgroundResource(R.drawable.en_flag);
            contentsLabel.setText("Content around You");
        }
    }

    private BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            Log.l(1,"download completed id="+id);
        }
    };
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        }
        if (id == R.id.action_info) {
            startActivity(new Intent(MainActivity.this, InfoActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public Player getPlayer() {
        if (player!=null) return player;
        else return new Player(videoLayout, videoView);
    }

    public void createDownloadServiceManager(boolean paused) {
        Log.l(1,"#0 createDownloadServiceManager");
        if (AppService.canRecoverService(AppService.SERVICE.DOWNLOAD_SERVICE, paused)) return;
        HashMap<String, String> headers = new HashMap<>();
        headers.put("id", "download");
        ServerManager downloadServiceManager = new ServerManager(DataHolder.getDownloadServiceURI(), headers, AppService.SERVICE.DOWNLOAD_SERVICE);
        downloadServiceManager.start(paused);
    }

    public void createSensorsServiceManager(boolean paused) {
        Log.l(1,"#0 createSensorServiceManager");
        if (AppService.canRecoverService(AppService.SERVICE.SENSORS_SEVICE, paused)) return;
        HashMap<String, String> headers = new HashMap<>();
        headers.put("id", "sensors");
        ServerManager sensorServiceManager = new ServerManager(DataHolder.getSensorServiceURI(), headers, AppService.SERVICE.SENSORS_SEVICE);
        sensorServiceManager.start(paused);
    }

    private void createBeaconCollector(boolean paused) {
            BeaconCollector beaconCollector = new BeaconCollector(this, beaconLayout, videoLayout, videoView, listView, player);
            beaconCollector.start(paused);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        AppService.stopService(AppService.SERVICE.SCAN_BLE);
        BeaconCollector beaconCollector = (BeaconCollector) AppService.getService(AppService.SERVICE.SCAN_BLE);
        beaconCollector.stopPlayer();
        AppService.stopService(AppService.SERVICE.DOWNLOAD_SERVICE);
        AppService.stopService(AppService.SERVICE.SENSORS_SEVICE);
        Log.l(1,"@@@@@@@@@@@@@@@@@@@  finish");
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        // if the intentResult is null then
        // toast a message as "cancelled"
        if (intentResult != null) {
            if (intentResult.getContents() == null) {
                Toast.makeText(getBaseContext(), "Cancelled", Toast.LENGTH_SHORT).show();
            } else {
                // if the intentResult is not null we'll set
                // the content and format of scan message
                BeaconCollector beaconCollector = (BeaconCollector) AppService.getService(AppService.SERVICE.SCAN_BLE);
                String beaconName = intentResult.getContents();
                Log.l(1,"Found QR Code <"+beaconName+">");
                beaconCollector.stopPlayer();
                AdvertisingPacket advertisingPacket = new AdvertisingPacket(beaconName, -70 , System.currentTimeMillis());
                beaconCollector.pushAdvertisingPacket(advertisingPacket);
                Beacon beacon = beaconCollector.getBeacon(beaconName);
                if (beacon==null) return;
                beacon.reset();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}