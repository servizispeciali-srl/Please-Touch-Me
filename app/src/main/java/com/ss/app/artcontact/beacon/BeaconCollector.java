package com.ss.app.artcontact.beacon;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.annotation.RequiresApi;

import com.ss.app.artcontact.MainActivity;
import com.ss.app.artcontact.advertising.AdvertisingPacket;
import com.ss.app.artcontact.advertising.AdvertisingPacketQueue;
import com.ss.app.artcontact.network.ContentDownloader;
import com.ss.app.artcontact.player.Player;
import com.ss.app.artcontact.player.UniversalVideoView;
import com.ss.app.artcontact.scanner.BLEScanner;
import com.ss.app.artcontact.scanner.ScannerSimulator;
import com.ss.app.artcontact.services.AppService;
import com.ss.app.artcontact.storage.DataHolder;
import com.ss.app.artcontact.utils.Log;
import com.ss.app.artcontact.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

public class BeaconCollector extends AppService implements Runnable {

    enum WorkingMode {COLLECT, SIMULATOR}

    WorkingMode workingMode = WorkingMode.COLLECT;

    LinearLayout beaconLayout;
    LinearLayout videoLayout;
    UniversalVideoView videoView;
    ListView listView;
    Player player;
    BeaconListAdapter beaconListAdapter;
    Handler uiHandler;

    Context context;
    AdvertisingPacketQueue advertisingPackets = new AdvertisingPacketQueue();
    Hashtable<String, Beacon> beaconTable = new Hashtable<>();

    public BeaconCollector(Context context, LinearLayout beaconLayout, LinearLayout videoLayout, UniversalVideoView videoView, ListView listView, Player player) {
        super(SERVICE.SCAN_BLE);
        this.context = context;
        this.beaconLayout = beaconLayout;
        this.videoLayout = videoLayout;
        this.videoView = videoView;
        this.listView = listView;
        this.player = player;
        uiHandler = new Handler(Looper.getMainLooper()) {
            public void handleMessage(Message msg) {
                Log.l(0, "uiCallback  " + this.getClass().getSimpleName());
                if (msg.what == 1) {
                    if (listView != null) listView.setAdapter(beaconListAdapter);
                }
            }
        };
    }

    public boolean isPlaying() {
        if (player==null) return false;
        return player.isPlaying();
    }

    public void stopPlayer() {
        if (player!=null && player.isPlaying()) {
            Log.l(1,"Stop Player");
            player.stop();
            player=null;
        }
    }

    public boolean completed() {
        if (player==null) return false;
        return player.completed();
    }

    public boolean hasErrorState() {
        if (player==null) return false;
        return player.hasErrorState();
    }

    @Override
    public void stop() {
        super.stop();

    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void run() {

        BLEScanner scanner=null;

        if (workingMode== WorkingMode.SIMULATOR) {
            while (DataHolder.jbeaconRefTable==null) Utils.sleep(1000);
            ScannerSimulator scannerSimulator = new ScannerSimulator(SERVICE.SCAN_SIMULATOR);
            scannerSimulator.setup(advertisingPackets);
            scannerSimulator.start(false);
        } else {
            scanner = new BLEScanner(context, advertisingPackets);
            scanner.startScanning();
        }
        while (!stopRun) {
            if (workingMode==WorkingMode.COLLECT) Log.l(0,"sleep... "+scanner.isScanning());
            Utils.sleep(500);
            if (workingMode==WorkingMode.COLLECT && !scanner.isScanning()) scanner.startScanning();
            processAdvertisingPackets();
            if (beaconTable.size()==0) continue;
            List<Beacon> beaconList = new ArrayList<>();
            for(Enumeration<Beacon> e = beaconTable.elements(); e.hasMoreElements();) {
                Beacon beacon = e.nextElement();
                beacon.trimAdvertisingPackets();
                if (beacon.hasAnyAdvertisingPacket() && !beacon.expired()) {
                    beaconList.add(beacon);
                    for(int i=0; i < 6; i++) ContentDownloader.download(DataHolder.getContentResource(beacon.name,i));
                }
            }
            if (beaconList.size()==0) {
                String[] listS = new String[beaconList.size()];
                beaconListAdapter = new BeaconListAdapter(MainActivity.me, listS, beaconList, player);
                uiHandler.sendEmptyMessage(1);
                continue;
            }
            Collections.sort(beaconList);
            Beacon beacon = beaconList.get(0);
            Log.l(0,"beacon "+beacon.name+" "+beacon.expired()+" "+beacon.getLatestTimestamp()+" "+(System.currentTimeMillis()-beacon.getLatestTimestamp())+" "+beacon.expireTime);
            float filteredRssi = beacon.getFilteredRssi();
            if (player!=null && filteredRssi >= beacon.getEnterRssi()) {
                Log.l(canDebug(beacon),"enterRssi "+beacon.name);
                if (player.isPlaying() && player.getPlayingBeacon().equals(beacon) ) {
                    Log.l(canDebug(beacon),"is Playing do Nothing");
                    // do nothing
                } else if (player.isPlaying()) {
                    // another player running
                    Log.l(canDebug(beacon),"another player running");
                    player.stop();
                    beacon.proximity=true;
                    player.start(beacon);
                } else if (!beacon.proximity){
                    Log.l(canDebug(beacon),"start player");
                    player.stop();
                    player.start(beacon);
                }
            } else if (player!=null && filteredRssi <= beacon.getExitRssi()){
                Log.l(canDebug(beacon) && beacon.proximity,"exitRssi "+beacon.name);
                beacon.proximity=false;
                player.resource="no resource";
                player.previousResource="no previous resource";
                player.stop();
            } else {
                // rssi between enter and exit,  do nothing
            }
            String[] listS = new String[beaconList.size()];
            for (int i = 0; i < beaconList.size(); i++) {
                listS[i] = i + ")";
                if (i > 0) beaconList.get(i).proximity=false;
            }
            beaconListAdapter = new BeaconListAdapter(MainActivity.me, listS, beaconList, player);
            uiHandler.sendEmptyMessage(1);
            continue;
        }
        Log.l(1,"exit BeaconCollector");
        if (scanner!=null) scanner.stopScanning();
    }

    public void pushAdvertisingPacket(AdvertisingPacket advertisingPacket) {
        advertisingPackets.push(advertisingPacket);
    }

    private void processAdvertisingPackets () {
        for(AdvertisingPacket advertisingPacket: advertisingPackets.getAll()) {
            String name = advertisingPacket.name;
            Beacon beacon = beaconTable.get(name);
            if (beacon == null) {
                try {
                    JSONObject jbeacon = DataHolder.jbeaconRefTable.getJSONObject(name);
                    beacon = new Beacon(advertisingPacket);
                    beacon.setEnterRssi(jbeacon.getInt("enter"));
                    beacon.setExitRssi(jbeacon.getInt("exit"));
                    beacon.setExpireTime(jbeacon.getInt("expireTime"));
                    beacon.setCourse(jbeacon.getDouble("course"));
                    beaconTable.put(name,beacon);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else beacon.addAdvertisingPacket(advertisingPacket);
        }
    };

    public Beacon getBeacon(String beaconName) {
        return beaconTable.get(beaconName);
    }

    public void manageSensors(String beaconName, String sensorsStatus) {
       Log.l(0,"manageSensors "+beaconName+" "+sensorsStatus);
       processAdvertisingPackets();
       Beacon beacon = beaconTable.get(beaconName);
       if (beacon==null) return;
       Log.l(0,"manageSensors "+sensorsStatus+" old status="+beacon.getSensorsStatus());
       if (beacon.getSensorsStatus()==null) {
           beacon.reset();
       }
       if (beacon.getSensorsStatus().equals(sensorsStatus)) return;
       Log.l(1,"check handover");
       if (handOverChanged(beacon, sensorsStatus)) {
           Log.l(1,"handover changed");
           beacon.setSensorsStatus(sensorsStatus);
           if (hasHandOver(beacon)) {
               Log.l(1,"has handover");
               selectAndPlay(beacon,4);
           } else {
               Log.l(1,"no handover clean player");
               cleanPlayer(beacon);
           }
       } else {
           Log.l(1,"no handover change");
           beacon.setSensorsStatus(sensorsStatus);
           if (!hasHandOver(beacon)) return;
           Log.l(1,"has handover selectAndPlay");
           selectAndPlay(beacon);
       }
   }

    public boolean selectAndPlay(Beacon beacon, int i) {
        if (beacon.getSensorsStatus().equals("00001T") && player.isPlaying()) return true;
        if (beacon.getSensorsStatus().charAt(i)=='0') return false;
        Log.l(1,"Check if player has same resource "+player.resource+" "+DataHolder.getContentResource(beacon.name, i+1));
        if (player.resource!=null && player.resource.equals(DataHolder.getContentResource(beacon.name, i+1))) return true;
        Log.l(1,"New resource for player, stop player if it is playing");
        if (player.isPlaying()) player.stop();
        Log.l(1,"@@@@@@ after stop");
        Utils.sleep(500);
        Log.l(1,"start player "+beacon.name+" "+beacon.getSensorsStatus());
        player.start(beacon);
        return true;
    }

    public void selectAndPlay(Beacon beacon) {
        Log.l(1,"#0 selectAndPlay");
       if (beacon.getFilteredRssi() < beacon.enterRssi-1) return;
       Log.l(1,"#1 selectAndPlay");
       for(int i=0; i < beacon.getSensorsStatus().length()-1; i++) {
           if (!selectAndPlay(beacon, i)) continue;
           break;
       }
   }

   public void cleanPlayer(Beacon beacon) {
       if (player.isPlaying()) player.stop();
       else player.resource="NO RESOURCE";
       player.previousResource="NO RESOURCE";
       Utils.sleep(500);
   }

   public boolean handOverChanged(Beacon beacon, String newSensorStatus) {
        if (newSensorStatus.charAt(5)=='M') return false;
        return beacon.getSensorsStatus().charAt(4)!=newSensorStatus.charAt(4);
   }

   public boolean hasHandOver(Beacon beacon) {
       return beacon.getSensorsStatus().charAt(4)=='1';
   }

   private boolean canDebug(Beacon beacon) {
     if (beacon.name.equals("RTNode1")) return true;
     return false;
   }
}
