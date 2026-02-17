package com.ss.app.artcontact.scanner;

import com.ss.app.artcontact.advertising.AdvertisingPacket;
import com.ss.app.artcontact.advertising.AdvertisingPacketQueue;
import com.ss.app.artcontact.beacon.BeaconCollector;
import com.ss.app.artcontact.services.AppService;
import com.ss.app.artcontact.storage.DataHolder;
import com.ss.app.artcontact.utils.Log;
import com.ss.app.artcontact.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ScannerSimulator extends AppService implements Runnable {

    List<BeaconSimulator> beaconList = new ArrayList<>();
    AdvertisingPacketQueue advertisingPackets;

    public ScannerSimulator(SERVICE service) {
        super(service);
        Iterator<String> iterator = DataHolder.jbeaconRefTable.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            try {
                JSONObject jBeacon = DataHolder.jbeaconRefTable.getJSONObject(key);
                String name = key;
                int pos = jBeacon.getInt("pos");
                double course = jBeacon.getDouble("course");
                BeaconSimulator beacon = new BeaconSimulator(name, pos, course);
                beaconList.add(beacon);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void setup(AdvertisingPacketQueue advertisingPackets) {
        this.advertisingPackets = advertisingPackets;
    }

    @Override
    public void run() {
        Utils.sleep(1000);
        double step=23;
        int nLoop=0;
        while (!stopRun) {
            for(BeaconSimulator beacon: beaconList) {
                AdvertisingPacket advertisingPacket = beacon.getPacket(step);
                if (advertisingPacket!=null) advertisingPackets.push(advertisingPacket);
            }
            Utils.sleep(1000);
            if (step < 29) step+=0.5;
            nLoop++;
            if (nLoop > 120) {
                step=20;
            }
            /*
            if (nLoop > 240) {
                nLoop=0;
                step=20;
            }

             */
            Log.l(1,"nLoop="+nLoop+" step="+step);
        }
    }
}