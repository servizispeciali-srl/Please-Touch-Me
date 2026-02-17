package com.ss.app.artcontact.player;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ss.app.artcontact.MainActivity;
import com.ss.app.artcontact.beacon.Beacon;
import com.ss.app.artcontact.network.ContentDownloader;
import com.ss.app.artcontact.storage.DataHolder;
import com.ss.app.artcontact.utils.Log;

import java.io.File;

public class Player {

    Beacon beacon;
    public String resource;
    public String previousResource;
    boolean isPlaying=false;
    boolean completed=false;
    LinearLayout videoLayout;
    UniversalVideoView videoView;
    CustomAudioPlayer customAudioPlayer;
    long startTS;
    Handler uiHandler;

    public Player(LinearLayout videoLayout, UniversalVideoView videoView) {
        this.videoLayout = videoLayout;
        this.videoView = videoView;
        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                switch(extra){
                    case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                        Log.l(1,"MediaPlayer.MEDIA_ERROR_SERVER_DIED");
                        break;
                    case MediaPlayer.MEDIA_ERROR_IO:
                        Log.l(1,"MediaPlayer.MEDIA_ERROR_IO");
                        break;
                    case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                        Log.l(1,"MediaPlayer.MEDIA_ERROR_TIMED_OUT");
                        break;
                    default:
                        Log.l(1,"MediaPlayer default error "+extra);
                        File file = new File(ContentDownloader.getFilename(resource));
                        if (file.exists()) file.delete();
                        break;
                }
                //You must always return true if you want the error listener to work
                return true;
            }
        });
        uiHandler = new Handler(Looper.getMainLooper()) {
            public void handleMessage(Message msg) {
                Log.l(0, "uiCallback  " + this.getClass().getSimpleName());
                if (msg.what == 1) {
                    if (resource==null) return;
                    Log.l(1,"### check if equals previous resource <"+resource+"> <"+previousResource+">");
                    if (resource.equals(previousResource)) {
                        Log.l(1,"do nothing same as previous resource "+resource);
                        return;
                    }
                    previousResource = resource;
                    if (resource.endsWith(".mp3")) {
                        customAudioPlayer = new CustomAudioPlayer();
                        isPlaying=true;
                        completed=false;
                        startTS=System.currentTimeMillis();
                        customAudioPlayer.load(ContentDownloader.getFilename(resource));
                        customAudioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mediaPlayer) {
                                customAudioPlayer.start();
                            }
                        });
                        customAudioPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mediaPlayer) {
                                Log.l(1,"@@@@@@@@@@@ Completed");
                                customAudioPlayer.stop();
                                customAudioPlayer.release();
                                completed=true;
                                if (beacon!=null) beacon.proximity=true;
                            }
                        });
                        return;
                    }
                    if (resource.endsWith(".mp4")) {
                        setVideoParams(400);
                    } else setVideoParams(10);
                    videoLayout.setVisibility(View.VISIBLE);
                    videoView.setVisibility(View.VISIBLE);
                    String filename = ContentDownloader.getFilename(resource);
                    Uri vidUri = Uri.parse(filename);
                    if (ContextCompat.checkSelfPermission(MainActivity.me,
                            Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {
                        ActivityCompat.requestPermissions(MainActivity.me, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
                    }
                    videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mediaPlayer) {
                            Log.l(1,"Video Completed");
                            completed=true;
                            beacon.proximity=true;
                            videoView.setVisibility(View.INVISIBLE);
                            videoLayout.setVisibility(View.INVISIBLE);
                        }
                    });
                    videoView.refreshDrawableState();
                    videoView.setVideoURI(vidUri);
                    videoView.start();
                    isPlaying=true;
                    completed=false;
                    startTS=System.currentTimeMillis();
                }
                if (msg.what == 2) {
                    Log.l(1,"1@@@@@@@@@@@@@@@@@@@@@@ stop "+resource);
                    if (resource.endsWith(".mp3")) {
                        if (isPlaying) customAudioPlayer.stop();
                        customAudioPlayer.release();
                        isPlaying=false;
                        resource=null;
                        beacon=null;
                        return;
                    }
                    isPlaying=false;
                    videoView.setVisibility(View.INVISIBLE);
                    videoLayout.setVisibility(View.INVISIBLE);
                    resource=null;
                    previousResource=null;
                    beacon=null;
                }
            }
        };
    }

    public void start(Beacon beacon) {
        if (isPlaying && !completed) return;
        Log.l(1,"#0 check if content file available "+beacon.name+" "+beacon.getFilteredRssi()+" beacon sensorStatus="+beacon.getSensorsStatus());
        if (beacon.emptyFlags()) {
            resource= DataHolder.getContentResource(beacon.name,0);
        } else {
            for(int i=0; i < 5; i++) {
                if (beacon.getSensorsStatus().charAt(i)=='1') {
                    resource=DataHolder.getContentResource(beacon.name,i+1);
                    Log.l(1,"found resource "+resource);
                    break;
                }
            }
        }
        Log.l(1,"player resource "+resource);
        if (resource==null || resource.equals("NO RESOURCE")) return;
        Log.l(1,"player resource "+resource+" table="+ContentDownloader.jrequestTable.toString());
        if (ContentDownloader.hasDownloadRequest(resource)) return;
        Log.l(0,"#####0 player resource "+resource);
        if (!ContentDownloader.hasDownload(resource)) return;
        Log.l(1,"#1 content file available "+beacon.name+" "+beacon.getFilteredRssi()+" resource="+resource);
        this.beacon = beacon;
        uiHandler.sendEmptyMessage(1);
    }

    public void stop() {
        if (!isPlaying) return;
        Log.l(1,"#1 stop");
        uiHandler.sendEmptyMessage(2);
    }

    public boolean isPlaying() {
        return isPlaying && !completed;
    }
    public boolean completed() {
        return completed;
    }
    public boolean hasErrorState() {
        return videoView.hasErrorState();
    }

    public Beacon getPlayingBeacon() {
        return beacon;
    }

    public int getDuration() {
        Log.l(1,"duration="+Long.valueOf((System.currentTimeMillis()-startTS)/1000).intValue());
        return Long.valueOf((System.currentTimeMillis()-startTS)/1000).intValue();
    }

    public void setVideoParams(int height) {
        ViewGroup.LayoutParams layoutParams = videoLayout.getLayoutParams();
        layoutParams.height = height;
        videoLayout.setLayoutParams(layoutParams);
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) videoView.getLayoutParams();
        params.height = height;
        videoView.setLayoutParams(params);
    }

}
