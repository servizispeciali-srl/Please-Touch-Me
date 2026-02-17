package com.ss.app.artcontact.network;

import com.ss.app.artcontact.services.AppService;
import com.ss.app.artcontact.storage.DataHolder;
import com.ss.app.artcontact.utils.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

public class ContentDownloader {

    static JSONObject jresourceTable = new JSONObject();
    public static JSONObject jrequestTable = new JSONObject();
    static int nextId = 0;

    public static int getNextId() {
        nextId++;
        if (nextId > 500) nextId = 1;
        return nextId;
    }

    public static String getFilename(String resource) {
        String destinationDirectory = DataHolder.databasesDir;
        return destinationDirectory + "/" + resource;
    }

    public static boolean hasDownload(String resource) {
        File file = new File(getFilename(resource));
        if (file.exists()) return true;
        return false;
    }

    public static boolean hasDownloadRequest(String resource) {
        return jrequestTable.has(resource);
    }

    public static int getRequestId(String resource) {
        Iterator<String> iterator = jrequestTable.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            if (!key.equals(resource)) continue;
            try {
                int id = jrequestTable.getInt("key");
                return id;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    public static JSONObject getJRequest(int requestId) {
        Iterator<String> iterator = jrequestTable.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            try {
                JSONObject jrequest = jrequestTable.getJSONObject(key);
                int id = 0;
                id = jrequest.getInt("id");
                if (id == requestId) return jrequest;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static void saveResourceInfo(JSONObject resourceInfo) {
        try {
            jresourceTable.put(resourceInfo.getString("resource"), resourceInfo);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static JSONObject getResourceInfo(String resource) {
            try {
                if (!jresourceTable.has(resource)) return null;
                else return jresourceTable.getJSONObject(resource);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
    }

    public static void download(String resource) {
        try {
            if (resource==null) return;
            if (resource.equals("NO RESOURCE")) return;

            if (hasDownloadRequest(resource)) {
                Log.l(0,"Download risorsa "+resource+" già richiesto");
                return;
            }

            if (hasDownload(resource)) {
                    Log.l(0,"Risorsa "+resource+" già disponibile");
                    /*
                    if (!jresourceTable.has(resource)) {
                        JSONObject jrequest = new JSONObject();
                        jrequest.put("messageType", "requestInfo");
                        jrequest.put("service", "download");
                        jrequest.put("resource", resource);
                        sendRequest(jrequest, resource);
                        return;
                    }
                    JSONObject jresourceInfo = getResourceInfo(resource);
                    int size = jresourceInfo.getInt(resource);
                    File file = new File(ContentDownloader.getFilename(resource));
                    if (file.exists() && file.length()!=size) file.delete();

                     */
                return;
            }
            Log.l(1, "Risorsa non esistente o non ancora richiesta - avvio download: " + resource);
            JSONObject jrequest = new JSONObject();
            jrequest.put("messageType", "request");
            jrequest.put("service", "download");
            jrequest.put("resource", resource);
            jrequest.put("id", getNextId());
            if (sendRequest(jrequest, resource)) jrequestTable.put(resource, jrequest);
        } catch (JSONException e) {
                e.printStackTrace();
        }
    }

    public static boolean sendRequest(JSONObject jrequest, String resource) {
        ServerManager downloadServiceManager = (ServerManager) AppService.getService(AppService.SERVICE.DOWNLOAD_SERVICE);
        if (downloadServiceManager.canProvide()) {
            Log.l(1, "Downloader Provider disponibile: " + resource);
            downloadServiceManager.wsc.send(jrequest.toString());
            return true;
        } else {
            Log.l(1, "Downloader Provider non disponibile: " + resource);
            return false;
        }
    }

    public static void checkDownloadStarted(int requestId){
        JSONObject jrequest = getJRequest(requestId);
        if (jrequest.has("fos")) return;
        try {
            String resource=jrequest.getString("resource");
            jrequest.put("fos", new FileOutputStream(getFilename(resource)));
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void appendData(byte[] data, FileOutputStream fos) {
        try {
            fos.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void finish(int requestId) {

        JSONObject jrequest = getJRequest(requestId);
        if (jrequest!=null) {
            try {
                String resource = jrequest.getString("resource");
                Log.l(1,"Download completed for "+resource);
                jrequestTable.remove(resource);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}