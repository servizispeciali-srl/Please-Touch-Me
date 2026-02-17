package com.ss.app.artcontact.scanner;


import com.ss.app.artcontact.advertising.AdvertisingPacket;
import com.ss.app.artcontact.beacon.Beacon;
import com.ss.app.artcontact.utils.Log;

public class BeaconSimulator {
    String name;
    int pos;
    double course;
    long ts=0;

    public BeaconSimulator(String name, int pos, double course) {
        this.name = name;
        this.pos = pos;
        this.course = course;
    }
    public AdvertisingPacket getPacket(double step) {
        int drssi = Long.valueOf(Math.round(Math.abs(step-course)*5)).intValue();
        Log.l(canDebug(),name+" drssi="+drssi+" step="+step);
        if (System.currentTimeMillis() - ts < 500) return null;
        ts = System.currentTimeMillis();
        int rssi = -70-drssi;
        if (rssi < -100) rssi=-100;
        Log.l(canDebug(),name+" rssi="+rssi);
        AdvertisingPacket advertisingPacket = new AdvertisingPacket(name, rssi , ts);
        return advertisingPacket;
    }

    private boolean canDebug() {
        //if (name.equals("RTNode1")) return true;
        return false;
    }

}
