package com.ss.app.artcontact.services;

import com.ss.app.artcontact.utils.Log;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

public class AppService implements Runnable {

    public enum SERVICE { SCAN_BLE, DOWNLOAD_SERVICE, SENSORS_SEVICE, SCAN_SIMULATOR }
    protected SERVICE service;
    static Hashtable<SERVICE,AppService> services = new Hashtable<>();
    protected boolean started=false, stopRun=false, paused=false;

    public AppService(SERVICE service) {
        this.service = service;
        services.put(service, this);
    }

    public void start(boolean paused) {
        this.paused = paused;
        Thread thread = new Thread(this);
        thread.start();
        started=true;
    }

    public void pause() {
        paused=true;
    }

    public void resume() {
        paused=false;
    }

    public void stop() {
        stopRun=true;
    }

    public boolean isRunning() {
        return started && !stopRun;
    }

    public boolean isPaused() {
        return paused && !stopRun;
    }

    public boolean canProvide() {
        return true;
    }

    public static boolean hasService(SERVICE service) {
        return services.containsKey(service);
    }

    public static AppService getService(SERVICE service) {
        return services.get(service);
    }

    public static List<AppService> getServices() {
        List<AppService> serviceList = new ArrayList<>();
        for(Enumeration<AppService> e=AppService.services.elements(); e.hasMoreElements();) serviceList.add(e.nextElement());
        return serviceList;
    }

    public String getServiceName() {
        return service.name();
    }

    public static boolean canRecoverService(SERVICE service, boolean paused) {
        if (AppService.hasService(service)) {
            AppService nmservice = AppService.getService(service);
            if (nmservice.isRunning() && nmservice.isPaused()==paused) return true;
            if (nmservice.isRunning() && nmservice.isPaused()!=paused) {
                if (paused) nmservice.pause();
                else nmservice.resume();
                return true;
            }
        }
        return false;
    }

    public static void pauseService(SERVICE service) {
        AppService nmservice = services.get(service);
        if (nmservice!=null) nmservice.pause();
    }

    public static void resumeService(SERVICE service) {
        AppService nmservice = services.get(service);
        if (nmservice!=null) nmservice.resume();
    }

    public static void stopService(SERVICE service) {
        AppService nmservice = services.get(service);
        if (nmservice!=null) nmservice.stop();
    }

    public static boolean canProvideService(SERVICE service) {
        AppService nmservice = services.get(service);
        if ((nmservice==null)) {
            for(Enumeration<AppService> e=services.elements(); e.hasMoreElements();) {
                AppService nms = e.nextElement();
                Log.l(1,nms.service.name()+" "+nms.paused+" "+nms.stopRun);
            }
        }
        if (nmservice!=null) return nmservice.canProvide();
        return false;
    }

    public static void setupRequestedServices() {

        AppService.getService(SERVICE.DOWNLOAD_SERVICE).pause();
    }

    @Override
    public void run() {

    }

}
