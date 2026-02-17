package com.ss.app.artcontact.storage;

import android.Manifest;
import android.content.SharedPreferences;

import com.ss.app.artcontact.MainActivity;
import com.ss.app.artcontact.utils.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Locale;


public class DataHolder {
    public static String databasesDir = "data/data/com.ss.app.artcontact/databases";
    public static String dataLoggerIp = "192.168.1.100";
    public static String lang="it";
    public static SharedPreferences sharedPref;
    public static SharedPreferences.Editor editor;
    public static JSONObject jbeaconRefTable;
    public static JSONObject jsensorsTable = new JSONObject();

    public static final String[] BLE_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
    };

    public static final String[] ANDROID_12_BLE_PERMISSIONS = new String[]{
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
    };

    public static void init(SharedPreferences sharedPrefp) {

        sharedPref = sharedPrefp;
        editor = sharedPref.edit();

        Locale current = MainActivity.me.getResources().getConfiguration().getLocales().get(0);
        String localeLang = current.getDisplayLanguage();
        if (localeLang.equals("Italian")) setLang("it");
        else if (localeLang.equals("English")) setLang(lang="en");
        else setLang("it");
        File databasesDir = new File(DataHolder.databasesDir);
        if (!databasesDir.exists()) databasesDir.mkdir();
        Log.l(1, DataHolder.databasesDir +" exists="+databasesDir.exists());
        dataLoggerIp = sharedPref.getString("dataLoggerIp", dataLoggerIp);
    }

    public static void setJbeaconRefTable(JSONObject table) {
        jbeaconRefTable = table;
    }

    public static void setJsensorsTable(JSONObject table) {
        jsensorsTable = table;
    }

    public static String getLang() {
        return sharedPref.getString("lang","it");
    }

    public static void setLang(String lang) {
        sharedPref.edit().putString("lang",lang).commit();
    }

    public static String getContentTitle(String beaconName) {
        String title=null;
        try {
            JSONObject jbeacon = DataHolder.jbeaconRefTable.getJSONObject(beaconName);
            if (jbeacon==null) jbeacon = new JSONObject();
            JSONObject jcontent = jbeacon.getJSONObject("content");
            if (jcontent==null) jcontent = new JSONObject();
            JSONObject jcontent_lang = jcontent.getJSONObject(getLang());
            if (jcontent_lang==null) jcontent_lang = new JSONObject();
            title = jcontent_lang.getString("title");
            if (title==null) title="No Title";
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return title;
    }

    public static String getContentResource(String beaconName, int resourceId) {
        Log.l(0,"getContentResource "+beaconName+" "+resourceId);
        String resource="NO RESOURCE";
        String resourceKey="nokey";
        boolean debug=resourceId==1;
        if (resourceId==0) resourceKey="resource";
        else resourceKey="sensor"+resourceId;
        try {
            if (!DataHolder.jbeaconRefTable.has(beaconName)) return resource;
            JSONObject jbeacon = DataHolder.jbeaconRefTable.getJSONObject(beaconName);
            if (!jbeacon.has("content")) return resource;
            JSONObject jcontent = jbeacon.getJSONObject("content");
            if (!jcontent.has(getLang())) return resource;
            JSONObject jcontent_lang = jcontent.getJSONObject(getLang());
            if (!jcontent_lang.has(resourceKey)) return resource;
            resource = jcontent_lang.getString(resourceKey);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return resource;
    }

    public static String getDownloadServiceURI() {
        return "ws://"+dataLoggerIp+":8789";
    }
    public static void setDataloggerIp(String ip) {
        dataLoggerIp=ip;
        editor.putString("dataLoggerIp", ip);
        editor.commit();
    };

    public static String getSensorServiceURI() {
        return "ws://"+dataLoggerIp+":8790";
    }

}
