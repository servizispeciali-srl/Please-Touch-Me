package com.ss.app.artcontact.network;

import com.ss.app.artcontact.storage.DataHolder;
import com.ss.app.artcontact.utils.ByteQueue;
import com.ss.app.artcontact.utils.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;

public class DownloadClient extends WebSocketClient {

    public DownloadClient(URI serverUri, Draft draft, Map<String, String> headers) {
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
                    DataHolder.jbeaconRefTable=jresponse.getJSONObject("jbeaconRefTable");
                    return;
                }
                String messageType = jresponse.getString("messageType");
                String service = jresponse.getString("service");
                /*
                if (messageType.equals("resourceInfo") && service.equals("download")) {
                    String resource = jresponse.getString("resource");
                    int size=jresponse.getInt("size");
                    JSONObject jresourceInfo = new JSONObject();
                    jresourceInfo.put(resource,size);
                    ContentDownloader.saveResourceInfo(jresourceInfo);
                }
                 */
                if (messageType.equals("requestStatus") && service.equals("download")) {
                    String resource = jresponse.getString("resource");
                    int id = jresponse.getInt("id");
                    int size=jresponse.getInt("size");
                    String status = jresponse.getString("status");
                    if (status.equals("DOWNLOADING")) {
                        File file = new File(ContentDownloader.getFilename(resource));
                        if (file.exists() && file.length()!=size) file.delete();
                        ContentDownloader.checkDownloadStarted(id);
                    } else if (status.equals("completed")) {
                        ContentDownloader.finish(id);
                    }
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
                ContentDownloader.checkDownloadStarted(requestId);
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
