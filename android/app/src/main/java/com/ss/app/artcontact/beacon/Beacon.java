package com.ss.app.artcontact.beacon;

import com.ss.app.artcontact.advertising.AdvertisingPacket;
import com.ss.app.artcontact.advertising.AdvertisingPacketUtil;
import com.ss.app.artcontact.filter.KalmanFilter;
import com.ss.app.artcontact.filter.RssiFilter;
import com.ss.app.artcontact.filter.WindowFilter;
import com.ss.app.artcontact.utils.Log;

import java.util.ArrayList;
import java.util.List;

public class Beacon implements Comparable<Beacon> {

    public boolean proximity = false;
    public final String initSensorsStatus="00000";

    public String name;
    public int rssi;
    public int enterRssi; // in dBm
    public int exitRssi; // in dBm
    public int expireTime=12000;
    private String sensorsStatus=initSensorsStatus;
    private String previousSensorsStatus=initSensorsStatus;
    double course;
    ArrayList<AdvertisingPacket> advertisingPackets = new ArrayList<>();

    public Beacon (AdvertisingPacket advertisingPacket) {
        this.name = advertisingPacket.name;
        advertisingPackets.add(advertisingPacket);
    }

    public boolean emptyFlags() {
        String status = sensorsStatus;
        if (status.length() > 5) status = status.substring(0,5);
        return status.equals(initSensorsStatus);
    }

    public void reset() {
        sensorsStatus=initSensorsStatus;
        previousSensorsStatus=initSensorsStatus;
    }

    public String getSensorsStatus() {
        return this.sensorsStatus;
    }

    public void setSensorsStatus(String sensorsStatus) {
        this.previousSensorsStatus=this.sensorsStatus;
        this.sensorsStatus=sensorsStatus;
    }
    public boolean hasAnyAdvertisingPacket() {
        return !advertisingPackets.isEmpty();
    }

    public AdvertisingPacket getLatestAdvertisingPacket() {
        synchronized (advertisingPackets) {
            if (!hasAnyAdvertisingPacket()) {
                return null;
            }
            return advertisingPackets.get(advertisingPackets.size() - 1);
        }
    }

    public void addAdvertisingPacket(AdvertisingPacket advertisingPacket) {
        synchronized (advertisingPackets) {
            rssi = advertisingPacket.getRssi();
            AdvertisingPacket latestAdvertisingPacket = getLatestAdvertisingPacket();

            if (latestAdvertisingPacket != null && latestAdvertisingPacket.getTimestamp() > advertisingPacket.getTimestamp()) {
                return;
            }

            advertisingPackets.add(advertisingPacket);
        }
    }

    public void trimAdvertisingPackets() {
        synchronized (advertisingPackets) {
            if (!hasAnyAdvertisingPacket()) {
                advertisingPackets.clear();
                return;
            }
            List<AdvertisingPacket> removableAdvertisingPackets = new ArrayList<>();
            long minimumPacketTimestamp = System.currentTimeMillis() - expireTime;
            for (AdvertisingPacket advertisingPacket : advertisingPackets) {
                if (advertisingPacket.getTimestamp() < minimumPacketTimestamp) {
                    // mark old packets as removable
                    removableAdvertisingPackets.add(advertisingPacket);
                }
            }
            advertisingPackets.removeAll(removableAdvertisingPackets);
        }
    }

    public float getRssi(RssiFilter filter) {
        return filter.filter(this);
    }

    public float getFilteredRssi() {
        return getRssi(createSuggestedWindowFilter());
    }

    public long getLatestTimestamp() {
        if (advertisingPackets.size()==0) return 0;
        return getLatestAdvertisingPacket().getTimestamp();
    }

    public WindowFilter createSuggestedWindowFilter() {
        return new KalmanFilter(getLatestTimestamp());
    }

    public ArrayList<AdvertisingPacket> getAdvertisingPacketsBetween(long startTimestamp, long endTimestamp) {
        return AdvertisingPacketUtil.getAdvertisingPacketsBetween(new ArrayList<>(advertisingPackets), startTimestamp, endTimestamp);
    }

    @Override
    public String toString() {
        return "Beacon{" +
                ", rssi=" + rssi +
                ", calibratedRssi=" + enterRssi +
                ", advertisingPackets=" + advertisingPackets +
                '}';
    }

    public int getEnterRssi() {
        return enterRssi;
    }
    public void setEnterRssi(int calibratedRssi) {
        this.enterRssi = calibratedRssi;
    }

    public int getExitRssi() {
        return exitRssi;
    }
    public void setExitRssi(int rssi) {
        this.exitRssi = rssi;
    }

    public int getExpireTime() {
        return expireTime;
    }
    public void setExpireTime(int expireTime) {
        this.expireTime = expireTime;
    }

    public boolean expired() {
        return System.currentTimeMillis()-getLatestTimestamp() > expireTime;
    }
    public double getCourse() {
        return course;
    }
    public void setCourse(double course) {
        this.course = course;
    }

    @Override
    public int compareTo(Beacon beacon) {
        return -Float.compare(this.getFilteredRssi(), beacon.getFilteredRssi());
    }

}
