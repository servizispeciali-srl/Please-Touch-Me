package com.ss.app.artcontact.scanner;

public class BeaconStatistics {
    String name;
    int detect;
    long startDetectTS;
    long lastDetectTS;

    public BeaconStatistics(String name) {
        this.name = name;
        detect=1;
        startDetectTS=System.currentTimeMillis();
        lastDetectTS=System.currentTimeMillis();
    }

    public void addDetect() {
        detect++;
        lastDetectTS=System.currentTimeMillis();
    }

    public String getStatistics() {
        return name+" avg messages per sec = "+1f*detect/((lastDetectTS-startDetectTS)/1000f);
    }
}
