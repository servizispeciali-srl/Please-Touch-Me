package com.ss.app.artcontact.network;


import com.ss.app.artcontact.services.AppService;
import com.ss.app.artcontact.utils.Log;
import com.ss.app.artcontact.utils.Utils;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;

import java.net.URI;
import java.util.HashMap;

public class ServerManager extends AppService implements Runnable {

    public WebSocketClient wsc;
    String webHost;
    SERVICE service;
    HashMap<String, String> headers;
    int nLoop;

    public ServerManager(String webHost, HashMap<String, String> headers, SERVICE service) {
        super(service);
        this.webHost =webHost;
        this.headers = headers;
        this.service = service;
    }

    public void changeWebHost(String webHost) {
        this.webHost =webHost;
    }
    @Override
    public void run() {

        nLoop=0;

        while(!stopRun) {
            nLoop++;
            if (paused) {
                nLoop=0;
                Utils.sleep(100);
            } else {
                manageConnection();
                Utils.sleep(1000);
            }
        }

    }

    @Override
    public void pause() {
        paused=true;
        if (wsc!=null) wsc.getConnection().close();
        wsc=null;
    }

    @Override
    public void stop() {
        if (wsc==null) return;
        wsc.getConnection().close();
        wsc=null;
        stopRun=true;
    }

    @Override
    public boolean canProvide() {
        Log.l(1,"is Running "+isRunning());
        if (!isRunning()) return false;
        Log.l(1,"wsc==null "+(wsc==null));
        if (wsc==null) return false;
        Log.l(1,service.name()+" connection.open="+wsc.getConnection().isOpen());
        if (!wsc.getConnection().isOpen()) return false;
        return true;
    }

    private void manageConnection() {
        boolean debug = false;
        if (wsc != null && wsc.getConnection().isOpen()) {
            Log.l(debug, service.name() + " wsc connection ok nLoop=" + nLoop);
            nLoop = 0;
        } else if (wsc != null && !wsc.getConnection().isOpen()) {
            if (nLoop >= 10) {
                WebSocketClient wsclient = wsc;
                wsclient.getConnection().close();
                wsc = null;
                nLoop = 0;
            }
        } else if (wsc == null) {
            try {
                if (headers.get("id") != null && headers.get("id").equals("download")) {
                    Log.l(debug, "create websocket download client nLoop=" + nLoop + " " + webHost);
                    wsc = new DownloadClient(new URI(webHost), new Draft_6455(), headers);
                    wsc.connect();
                    Utils.sleep(3000);
                } else if (headers.get("id") != null && headers.get("id").equals("sensors")) {
                    Log.l(debug, "create websocket sensors client nLoop=" + nLoop + " " + webHost);
                    wsc = new SensorsClient(new URI(webHost), new Draft_6455(), headers);
                    wsc.connect();
                    Utils.sleep(3000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
