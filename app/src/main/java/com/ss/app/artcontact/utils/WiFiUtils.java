package com.ss.app.artcontact.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.wifi.WifiNetworkSuggestion;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.ss.app.artcontact.MainActivity;

import java.util.ArrayList;
import java.util.List;

public class WiFiUtils {

    static ConnectivityManager mConnectivityManager;


    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static void init() {

        final PasspointConfiguration passpointConfig = new PasspointConfiguration();

        final WifiNetworkSuggestion suggestion1 =
                new WifiNetworkSuggestion.Builder()
                        .setSsid("MAC TouchMe")
                        .setWpa2Passphrase("mac2024@")
                        .setIsAppInteractionRequired(false) // Optional (Needs location permission)
                        .build();
        final List<WifiNetworkSuggestion> suggestionsList =
                new ArrayList<WifiNetworkSuggestion> (){{
            add(suggestion1);
        }};

        ConnectivityManager connectivityManager = (ConnectivityManager) MainActivity.me.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiConnection = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        // on below line displaying toast message when wi-fi is connected when wi-fi is disconnected
        if (wifiConnection.isConnected()) return;

        WifiManager wifiManager = (WifiManager)MainActivity.me.getSystemService(MainActivity.me.WIFI_SERVICE);
        Intent settingsIntent = new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY);
        MainActivity.me.startActivityForResult(settingsIntent, 1);

        /*

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {// if build version is less than Q try the old traditional method
            if (!wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(true);
            } else {
                wifiManager.setWifiEnabled(false);
            }
        } else {// if it is Android Q and above go for the newer way    NOTE: You can also use this code for less than android Q also
            Intent panelIntent = new Intent(Settings.Panel.ACTION_WIFI);
            MainActivity.me.startActivityForResult(panelIntent, 1);
        }
*/
        final int status = wifiManager.addNetworkSuggestions(suggestionsList);
        if (status != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
            Log.l(true, "status != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS");
        }
        final IntentFilter intentFilter =
                new IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION);

        final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!intent.getAction().equals(
                        WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)) {
                    return;
                }
                // do post connect processing here...
            }
        };
        MainActivity.me.registerReceiver(broadcastReceiver, intentFilter);

    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static void connect(String ssid, String password) {
        NetworkSpecifier networkSpecifier  = new WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .setIsHiddenSsid(true) //specify if the network does not broadcast itself and OS must perform a forced scan in order to connect
                .build();
        NetworkRequest networkRequest  = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(networkSpecifier)
                .build();
        Log.l(1,"requestNetwork");
        mConnectivityManager.requestNetwork(networkRequest, mNetworkCallback);
    }

    public static void disconnectFromNetwork(){
        //Unregistering network callback instance supplied to requestNetwork call disconnects phone from the connected network
        mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
    }

    private static ConnectivityManager.NetworkCallback mNetworkCallback = new ConnectivityManager.NetworkCallback(){
        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            Log.l(1,"requestNetwork");
            //phone is connected to wifi network
        }

        @Override
        public void onLosing(@NonNull Network network, int maxMsToLive) {
            super.onLosing(network, maxMsToLive);
            //phone is about to lose connection to network
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
            //phone lost connection to network
        }

        @Override
        public void onUnavailable() {
            super.onUnavailable();
            //user cancelled wifi connection
        }
    };
}
