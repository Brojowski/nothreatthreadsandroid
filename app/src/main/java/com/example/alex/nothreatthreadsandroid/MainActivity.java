package com.example.alex.nothreatthreadsandroid;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity
{
    boolean isSafe = true;
    private TextView _safetyStatus = null;
    private TextView _gpsOut = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissions(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.SEND_SMS
        }, 0);

        _safetyStatus = findViewById(R.id.safetyStatus);
        _gpsOut = findViewById(R.id.gpsOut);

        Button safeBtn = findViewById(R.id.safe);
        safeBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                isSafe = true;
                if (_safetyStatus != null) {
                    _safetyStatus.setText("Safe");
                }
            }
        });

        Button alertBtn = findViewById(R.id.notSafe);
        alertBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                TriggerLocationUpdates();
            }
        });

        try{
            Socket hardwareTrigger = IO.socket("http://174.104.106.130:8080");

            hardwareTrigger.on(Socket.EVENT_CONNECT, new Emitter.Listener()
            {
                @Override
                public void call(Object... args)
                {
                    Log.v("Socket", "Connection success");
                }
            }).on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener()
            {
                @Override
                public void call(Object... args)
                {
                    Log.e("Socket", "Connection error");
                }
            }).on(Socket.EVENT_ERROR, new Emitter.Listener()
            {
                @Override
                public void call(Object... args)
                {
                    Log.e("Socket", "Error");
                }
            });

            hardwareTrigger.on("alert", new Emitter.Listener()
            {
                @Override
                public void call(Object... args)
                {
                    Handler runOnMain = new Handler(Looper.getMainLooper());

                    runOnMain.post(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            TriggerLocationUpdates();
                        }
                    });
                }
            });

            hardwareTrigger.connect();

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void TriggerLocationUpdates() {
        isSafe = false;
        if (_safetyStatus != null) {
            _safetyStatus.setText("Not Safe");
        }
        LocationManager lm  = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 15000, 50, new LocationListener()
            {
                @Override
                public void onLocationChanged(Location loc)
                {
                    String locStr = "GPS: " + loc.getLatitude() + " " + loc.getLongitude() + "\n";
                    if (!isSafe && _gpsOut != null) {
                        _gpsOut.append(locStr);
                        SmsManager.getDefault().sendTextMessage(
                                "9375573171",
                                null,
                                locStr,
                                null,
                                null
                        );
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras)
                {

                }

                @Override
                public void onProviderEnabled(String provider)
                {

                }

                @Override
                public void onProviderDisabled(String provider)
                {

                }
            });
        }
    }


}
