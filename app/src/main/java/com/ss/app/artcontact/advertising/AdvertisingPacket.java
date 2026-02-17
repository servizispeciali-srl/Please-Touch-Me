package com.ss.app.artcontact.advertising;

public class AdvertisingPacket {

    public String name;
    public int rssi;
    public long ts;

    public AdvertisingPacket(String name, int rssi, long ts) {
        this.name = name;
        this.rssi = rssi;
        this.ts = ts;
    }

    public AdvertisingPacket(String name, int rssi) {
        this.name = name;
        this.rssi = rssi;
        ts = System.currentTimeMillis();
    }

    public int getRssi() {
        return rssi;
    }

    public long getTimestamp() {
        return ts;
    }

}

