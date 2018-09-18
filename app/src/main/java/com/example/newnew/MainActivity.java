package com.example.newnew.locationtest;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class MainActivity extends AppCompatActivity {

    private FusedLocationProviderClient mFusedLocationClient;
    private boolean mLocationPermissionGranted;

    private Location preLocation;
    private Location thisLocation;

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    //ลูบทุกๆวิ (ตรงนี้เราต้องไป set เวลาเองว่าเราจะเอากี่นาทีเพื่อหาระยะห่างและเวลาซึ่งในที่นี้เราอาจจะจับที่ 5 นาที)
    private static final int INTERVAL = 1 * 1000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        //เปิด service เพื่อขอ current location
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        //ขอ permission โทรศัพท์
        getLocationPermission();
        //ขอ latitide/longtitude ครั้งแรก
        getDeviceLocation("initial");
        //ทำทุกๆ interval 1 วิ (1*1000)
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
            //หลักจาก get location ขอ current แต่ละ location เรื่อยๆ
                getDeviceLocation("service");
                handler.postDelayed(this, INTERVAL);

            }
        }, INTERVAL);
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
    }

    private void getDeviceLocation(final String type) {
        Log.d("Debugging", "in getDevicePermission");
        try {
            if(mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationClient.getLastLocation();
                locationResult.addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        Log.d("Debugging", "in onSuccess");
                        if(location != null) {
                            if (type == "initial")
                                preLocation = location;
                            else {
                                thisLocation = location;
                            }

                            calculateVelocity();
                        } else {
                            Log.d("Debugging", "location is null");
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void calculateVelocity() {
        Log.d("Debugging", "in calculate velo");
        if (preLocation != null && thisLocation != null) {
            (new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String url = "https://maps.googleapis.com/maps/api/directions/json?";
                        url += "origin=" + preLocation.getLatitude() + "," + preLocation.getLongitude();
                        url += "&destination=" + thisLocation.getLatitude() + "," + thisLocation.getLongitude();
                        url += "&key=" + "AIzaSyDjEK_vRWBhbFL4S_3CsXWO-TG_7bBkXwk";
                        //ปริ้นดูค่าใน logcat จะขึ้น http คลิกตามลิ้ง
                        //Log.d("Debugging", url);
                        URLConnection connection = new URL(url).openConnection();
                        InputStream response = connection.getInputStream();
                        JSONParser parser = new JSONParser();
                        JSONObject result = (JSONObject) parser.parse(
                                new InputStreamReader(response, "UTF-8"));
                        JSONObject routes = (JSONObject) ((JSONArray)result.get("routes")).get(0);
                        JSONObject legs = (JSONObject) ((JSONArray) routes.get("legs")).get(0);
                        Long distance = (Long)((JSONObject) legs.get("distance")).get("value");
                        Long duration = (Long)((JSONObject) legs.get("duration")).get("value");

                        // calculate เพือหา v ในทุกๆ 5 นาที ทำอันนี้******** (ซึ่งตอนนี้เป็น1วิ)
                        Log.d("Debugging", distance + "");
                        Log.d("Debugging", duration + "");

                        // reset location
                        //ห้ามลบบรรทัดนี้*****
                        preLocation = thisLocation;
                        thisLocation = null;

                    } catch (MalformedURLException ex) {
                        ex.printStackTrace();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    } catch (ParseException ex) {
                        ex.printStackTrace();
                    }
                }
            })).start();
        }
    }
}
//ค่าที่ได้มาอาจจะเพี้ยน เพราะ current location มีหลายที่ อย่างเช้นใน ม.ปักหมุด current เราไม่เหมือนกัร
//ตอนนี้ค่าที่ได้เป็น value อยู่ดูได้ใน http