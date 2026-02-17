package com.ss.app.artcontact.beacon;

import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.ss.app.artcontact.MainActivity;
import com.ss.app.artcontact.R;
import com.ss.app.artcontact.player.Player;
import com.ss.app.artcontact.storage.DataHolder;
import com.ss.app.artcontact.utils.Log;

import java.util.List;

public class BeaconListAdapter extends ArrayAdapter<String> {
    Activity context;
    List<Beacon> beaconList;
    Player player;
    public BeaconListAdapter(@NonNull Activity context, String[] listS,
                             List<Beacon> beaconList, Player player) {
        super(context, R.layout.beacon_row2, listS);
        this.context = context;
        this.beaconList = beaconList;
        this.player = player;
    }

    @Override
    public int getCount() {
        return beaconList.size();
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        Log.l(0,"getView position="+position);
        LayoutInflater inflater = context.getLayoutInflater();
        Beacon beacon = beaconList.get(position);
        float filteredRssi = Math.round(beacon.getFilteredRssi()*10)/10f;
        View rowView2 = inflater.inflate(R.layout.beacon_row2, null, true);
        TextView beaconInfo = rowView2.findViewById(R.id.beaconInfo);
        String title= DataHolder.getContentTitle(beacon.name);
        beaconInfo.setText((position+1)+") "+title+"    (RSSI: "+filteredRssi+")");
        if (player==null) player = MainActivity.me.getPlayer();
        Beacon playedBeacon = player.getPlayingBeacon();
        if (playedBeacon!=null  && playedBeacon.equals(beacon)) beaconInfo.setBackgroundColor(Color.parseColor("#d07537"));
        else beaconInfo.setBackgroundColor(Color.BLUE);
        return rowView2;
    }

}
