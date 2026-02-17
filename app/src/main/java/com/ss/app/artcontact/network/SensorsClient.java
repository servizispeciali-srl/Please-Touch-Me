package com.ss.app.artcontact.network;

import com.ss.app.artcontact.beacon.BeaconCollector;
import com.ss.app.artcontact.services.AppService;
import com.ss.app.artcontact.storage.DataHolder;
import com.ss.app.artcontact.utils.ByteQueue;
import com.ss.app.artcontact.utils.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;

public class SensorsClient extends WebSocketClient {

    public SensorsClient(URI serverUri, Draft draft, Map<String, String> headers) {
        super(serverUri, draft, headers, 10000);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.l(1, "onOpen: ");

    }

    @Override
    public void onMessage(String message) {
        Log.l(1, "onMessage: "+message);
        if (message.startsWith("{")) {
            try {
                JSONObject jresponse = new JSONObject(message);
                if (jresponse.has("jbeaconRefTable")) {
                    Log.l(1,"Received jbeaconRefTable");
                    JSONObject jbeaconRefTable=jresponse.getJSONObject("jbeaconRefTable");
                    if (jbeaconRefTable.toString().equals(DataHolder.jsensorsTable.toString())) return;
                    DataHolder.setJbeaconRefTable(jbeaconRefTable);
                }
                if (jresponse.has("jsensorsTable")) {
                    JSONObject jsensorsTable=jresponse.getJSONObject("jsensorsTable");
                    DataHolder.setJsensorsTable(jsensorsTable);
                    BeaconCollector beaconCollector = (BeaconCollector) AppService.getService(AppService.SERVICE.SCAN_BLE);
                    Iterator<String> iterator = jsensorsTable.keys();
                    while (iterator.hasNext()) {
                        String beaconName = iterator.next();
                        String sensorStatus = jsensorsTable.getString(beaconName);
                        beaconCollector.manageSensors(beaconName, sensorStatus);
                    }
                    return;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onMessage(ByteBuffer message) {
        Log.l(1, "received data buffer size="+message.capacity());
        ByteQueue messageBuffer = new ByteQueue(message.array());
        int requestId = messageBuffer.popU2B();
        JSONObject jrequest = ContentDownloader.getJRequest(requestId);
        if (jrequest!=null) {
            Log.l(1, "found jrequest "+jrequest.toString()+" buffer size="+messageBuffer.size());
            try {
                ContentDownloader.appendData(messageBuffer.popAll(), (FileOutputStream) jrequest.get("fos"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Log.l(1, "requestId="+requestId+" not found");
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.l(1,"onClose:"+code+"|"+reason+"|"+remote);
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }

    public boolean sendAsString(String message) {

        Log.l(1, "Local sendAsString "+message);
        try {
            if (getConnection().isOpen()) {
                send(message);
                return true;
            } else return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
