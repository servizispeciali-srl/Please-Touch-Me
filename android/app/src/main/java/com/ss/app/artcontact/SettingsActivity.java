package com.ss.app.artcontact;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.ss.app.artcontact.network.ServerManager;
import com.ss.app.artcontact.services.AppService;
import com.ss.app.artcontact.storage.DataHolder;

import java.util.Objects;

public class SettingsActivity extends PreferenceActivity {

    AppCompatDelegate mDelegate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        getDelegate().setSupportActionBar(toolbar);
        getDelegate().getSupportActionBar().setIcon(R.drawable.macicon);

        Objects.requireNonNull(getDelegate().getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        addPreferencesFromResource(R.xml.mac_prefs);

        // Network
        final EditTextPreference dataLoggerURI = (EditTextPreference) findPreference("key_dl_ip");
        String dataLoggerIp = DataHolder.dataLoggerIp;
        dataLoggerURI.setText(dataLoggerIp);
        dataLoggerURI.setSummary(dataLoggerIp);
        dataLoggerURI.setOnPreferenceChangeListener((preference, newValue) -> {
            preference.setSummary((CharSequence) newValue);
            DataHolder.setDataloggerIp((String) newValue);
            AppService sensorService  =  AppService.getService(AppService.SERVICE.SENSORS_SEVICE);
            AppService downloadService  =  AppService.getService(AppService.SERVICE.DOWNLOAD_SERVICE);
            boolean sensorServicePaused= sensorService.isPaused();
            boolean downloadServicePaused= downloadService.isPaused();
            sensorService.stop();
            downloadService.stop();
            ((ServerManager) sensorService).changeWebHost(DataHolder.getSensorServiceURI());
            ((ServerManager) downloadService).changeWebHost(DataHolder.getDownloadServiceURI());
            MainActivity.me.createSensorsServiceManager(sensorServicePaused);
            MainActivity.me.createDownloadServiceManager(downloadServicePaused);
            downloadService.start(false);
            return true;
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getDelegate().onPostResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        getDelegate().onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getDelegate().onDestroy();
    }

    private AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        return mDelegate;
    }
}
