// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlemap.mapmusicapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Bundle;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An activity that displays a map showing the place at the device's current location.
 */
public class MapsActivityCurrentPlace extends AppCompatActivity
        implements OnMapReadyCallback, SensorEventListener {

    private static final String TAG = MapsActivityCurrentPlace.class.getSimpleName();
    private GoogleMap map;
    private CameraPosition cameraPosition;

    // The entry point to the Places API.
    private PlacesClient placesClient;

    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient fusedLocationProviderClient;

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng defaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final int DEFAULT_ZOOM = 16;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean locationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location lastKnownLocation;

    // Keys for storing activity state.
    // [START maps_current_place_state_keys]
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";
    // [END maps_current_place_state_keys]

    // Used for selecting the current place.
    private static final int M_MAX_ENTRIES = 5;
    private String[] likelyPlaceNames;
    private String[] likelyPlaceAddresses;
    private List[] likelyPlaceAttributions;
    private LatLng[] likelyPlaceLatLngs;
    private boolean isLoop = false;
    private Polyline mutablePolyline;
    private PolylineOptions polylineOptions = new PolylineOptions();
    private List<LatLng> latLngList = new ArrayList<>();
    private MediaPlayer mMediaPlayer;
    private boolean isFilePre = false;
    private String playStatus = "";

    private ImageView iv_image;
    private int step;
    private SensorManager sensorManager;
    private Sensor sensor;
    private double original_value;
    private double last_value;
    private double current_value;
    private boolean motionState = true; //是否处于运动状态
    private boolean processState = false;  //是否已经开始计步
    private SimpleDateFormat simpleDateFormat;

    private TextView tv_text;

    private Thread mThread;
    private Chronometer timer;
    public static int total_step = 0;
    private int total_step1 = 0;
    private float pep_weight = 60;
    private float per_step = 0.5f;
    private float last_time = 0;
    private float kaluli = 0;
    private float x = 0;
    private float st_x = 0;
    private float st_y = 0;
    private String inputName = "";
    private SensorManager sm1;
    private SensorManager sm2;

    public static float[] da_st = new float[10000];
    float[] accelerometerValues = new float[3];
    float[] magneticFieldValues = new float[3];

    private Intent intentService;

    private Chronometer walkTimer;
    private Chronometer runTimer;
    private List<Float> walkSpeedList = new ArrayList<>();
    private List<Float> runSpeedList = new ArrayList<>();

    private String walkDis;
    private String walkDur;
    private String walkSpe;
    private String runSpe;
    private String runDis;
    private String runDur;
    private String totalDis;
    private String totalSteps;
    private Animation mRotate;
    private Button btn_down;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 2) {
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
                mapFragment.getMapAsync(MapsActivityCurrentPlace.this);
                //然后过5秒后发送一次消息
                mHandler.sendEmptyMessageDelayed(2, 2000);
            } else if (msg.what == 0x00) {
                countStep();
                last_time = last_time / 1000 / 3600;
                kaluli = pep_weight * last_time * 30 / (last_time / per_step / total_step * 400 * 60);
                float[] values = new float[3];
                float[] R = new float[9];
                SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldValues);
                SensorManager.getOrientation(R, values);
                da_st[total_step] = values[0];
                float speed = total_step * per_step / (last_time * 3600);
                if (speed >= 1) {
                    walkTimer.stop();
                    runTimer.start();
                    runSpeedList.add(speed);
                } else {
                    walkTimer.start();
                    runTimer.stop();
                    walkSpeedList.add(speed);
                }

            }

        }
    };


    private void countStep() {
        if (StepDetector.CURRENT_SETP % 2 == 0) {
            total_step = StepDetector.CURRENT_SETP;
        } else {
            total_step = StepDetector.CURRENT_SETP + 1;
        }
        Log.i("total_step", StepDetector.CURRENT_SETP + "");
        total_step = StepDetector.CURRENT_SETP;
    }

    private void play() {
        try {
            mMediaPlayer.reset();
            AssetFileDescriptor descriptor = getAssets().openFd("chengdu.mp3");
            mMediaPlayer.setDataSource(descriptor.getFileDescriptor());
            mMediaPlayer.setOnPreparedListener(mp -> {
                mMediaPlayer.start();
                playStatus = "start";
            });
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    // [START maps_current_place_on_create]
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // [START_EXCLUDE silent]
        // [START maps_current_place_on_create_save_instance_state]
        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }
        // [END maps_current_place_on_create_save_instance_state]
        // [END_EXCLUDE]

        // Retrieve the content view that renders the map.
        setContentView(R.layout.activity_maps);
        iv_image = findViewById(R.id.iv_image);
        tv_text = findViewById(R.id.tv_text);
        iv_image.setVisibility(View.GONE);
        mMediaPlayer = new MediaPlayer();
        mRotate = new RotateAnimation(
                0f,
                360f,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);
        // 初始话插值器并设置线性插值器，变化率，并不是运行速度。一个插补动画，可以将动画效果设置为加速、减速、反复、反弹等
        //linearInterpolator为匀速
        mRotate.setInterpolator(new LinearInterpolator());
        // 设置动画从fromDegrees转动到toDegrees花费的时间，毫秒。可以用来计算速度
        mRotate.setDuration(2000);
        // 设置重复的次数（旋转圈数），Animation.INFINITE = -1，无限循环
        mRotate.setRepeatCount(Animation.INFINITE);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        sm1 = (SensorManager) getSystemService(SENSOR_SERVICE);
        sm2 = (SensorManager) getSystemService(SENSOR_SERVICE);
        timer = new Chronometer(this);
        walkTimer = findViewById(R.id.walk_timer);
        runTimer = findViewById(R.id.run_timer);
        btn_down = findViewById(R.id.btn_down);
        btn_down.setVisibility(View.GONE);
        // [START_EXCLUDE silent]
        // Construct a PlacesClient
        Places.initialize(getApplicationContext(), "AIzaSyCg1X2bewE2BT6JqFVGEbmYShyzLoCvKsY");
        placesClient = Places.createClient(this);

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Build the map.
        // [START maps_current_place_map_fragment]
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        intentService = new Intent(this, StepCounterService.class);
        findViewById(R.id.iv_music).setOnClickListener(view -> {
            iv_image.setVisibility(View.VISIBLE);
            iv_image.clearAnimation();
            iv_image.setAnimation(mRotate);
            iv_image.startAnimation(mRotate);
            getFilePermission();

        });
        findViewById(R.id.iv_pause).setOnClickListener(view -> {
            mMediaPlayer.pause();
            iv_image.clearAnimation();
            playStatus = "pause";
            iv_image.setVisibility(View.GONE);
        });
        findViewById(R.id.iv_stop).setOnClickListener(view -> {
            mMediaPlayer.stop();
            playStatus = "stop";
            iv_image.setVisibility(View.GONE);
        });
        findViewById(R.id.btn_start).setOnClickListener(view -> {
//            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
            processState = true;
            timer.setBase(SystemClock.elapsedRealtime());
            timer.start();
            runTimer.start();
            walkTimer.start();
            startService(intentService);
            if (mThread == null) {
                mThread = new Thread() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        super.run();
                        int temp = 0;
                        while (true) {
                            try {
                                sleep(300);
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            if (StepCounterService.FLAG) {
                                Log.i("istrue", "true");
                                if (temp != StepDetector.CURRENT_SETP) {
                                    temp = StepDetector.CURRENT_SETP;
                                }
                                last_time = SystemClock.elapsedRealtime() - timer.getBase();
                                mHandler.sendEmptyMessage(0x00);
                            }
                        }
                    }
                };
                mThread.start();
            }
        });
        findViewById(R.id.btn_stop).setOnClickListener(view -> {
            iv_image.setVisibility(View.GONE);
            btn_down.setVisibility(View.VISIBLE);
//            sensorManager.unregisterListener(this, sensor);
            runTimer.stop();
            walkTimer.stop();
            stopService(intentService);
            StepDetector.CURRENT_SETP = 0;
            mHandler.removeCallbacks(mThread);
            float totalWalk = 0f;
            for (float walk : walkSpeedList) {
                totalWalk = totalWalk + walk;
            }
            float walkSpeed = totalWalk / walkSpeedList.size();
            float totalRun = 0f;
            for (float run : runSpeedList) {
                totalRun = totalRun + run;
            }
            float runSpeed = totalRun / runSpeedList.size();
            int runDuration = getChronometerSeconds(runTimer);
            int walkDuration = getChronometerSeconds(walkTimer);
            float runDistance = (runDuration * runSpeed);
            float walkDistance = (walkDuration * walkSpeed);

            walkDis = walkDistance + "";
            walkDur = walkDuration + "";
            walkSpe = walkSpeed + "";
            runDis = runDistance + "";
            runDur = runDuration + "";
            runSpe = runSpeed + "";
            totalDis = (runDistance + walkDistance) + "";
            totalSteps = total_step + "";
            tv_text.setText("Walk Distance：" + walkDis + "m\n"
                    + "Walk Duration：" + walkDur + "s\n"
                    + "Walk Speed：" + walkSpe + "m/s\n"
                    + "Run Distance：" + runDis + "m\n"
                    + "Run Duration：" + runDur + "s\n"
                    + "Run Speed：" + runSpe + "m/s\n"
                    + "Total Distance：" + totalDis + "m\n"
                    + "Number of Steps：" + total_step + "\n"
            );
        });

        findViewById(R.id.btn_down).setOnClickListener(view -> {
            ExcelBean excelBean = new ExcelBean();
            excelBean.setWalkDistance(walkDis);
            excelBean.setWalkDuration(walkDur);
            excelBean.setWalkSpeed(walkSpe);
            excelBean.setRunDistance(runDis);
            excelBean.setRunDuration(runDur);
            excelBean.setRunSpeed(runSpe);
            excelBean.setTotalDistance(totalDis);
            excelBean.setSteps(totalSteps);
            List<ExcelBean> list = new ArrayList<>();
            list.add(excelBean);
            try {
                ExcelUtil.writeExcel(this, list, "mapMusic");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static int getChronometerSeconds(Chronometer cmt) {
        int totalss = 0;
        String string = cmt.getText().toString();
        if (string.length() == 7) {

            String[] split = string.split(":");
            String string2 = split[0];
            int hour = Integer.parseInt(string2);
            int Hours = hour * 3600;
            String string3 = split[1];
            int min = Integer.parseInt(string3);
            int Mins = min * 60;
            int SS = Integer.parseInt(split[2]);

            totalss = Hours + Mins + SS;
            return totalss;
        } else if (string.length() == 5) {

            String[] split = string.split(":");
            String string3 = split[0];
            int min = Integer.parseInt(string3);
            int Mins = min * 60;
            int SS = Integer.parseInt(split[1]);

            totalss = Mins + SS;
            return totalss;
        }
        return totalss;
    }

    /**
     * @param cmt Chronometer控件
     * @return 小时+分钟+秒数  的所有秒数
     */
    public static String getChronometer(Chronometer cmt) {
        int totalss = 0;
        String string = cmt.getText().toString();
        if (string.length() == 7) {

            String[] split = string.split(":");
            String string2 = split[0];
            int hour = Integer.parseInt(string2);
            int Hours = hour * 3600;
            String string3 = split[1];
            int min = Integer.parseInt(string3);
            int Mins = min * 60;
            int SS = Integer.parseInt(split[2]);

            totalss = Hours + Mins + SS;
            return Hours + "hour" + " " + Mins + "min " + SS + "s";
        } else if (string.length() == 5) {

            String[] split = string.split(":");
            String string3 = split[0];
            int min = Integer.parseInt(string3);
            int Mins = min * 60;
            int SS = Integer.parseInt(split[1]);

            totalss = Mins + SS;
            return Mins + "min " + SS + "s";
        }
        return String.valueOf(totalss);
    }
    // [END maps_current_place_on_create]

    /**
     * Saves the state of the map when the activity is paused.
     */
    // [START maps_current_place_on_save_instance_state]
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (map != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, map.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, lastKnownLocation);
        }
        super.onSaveInstanceState(outState);
    }
    // [END maps_current_place_on_save_instance_state]

    /**
     * Sets up the options menu.
     *
     * @param menu The options menu.
     * @return Boolean.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.current_place_menu, menu);
        return true;
    }

    /**
     * Handles a click on the menu option to get a place.
     *
     * @param item The menu item to handle.
     * @return Boolean.
     */
    // [START maps_current_place_on_options_item_selected]
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.option_get_place) {
            showCurrentPlace();
        }
        return true;
    }
    // [END maps_current_place_on_options_item_selected]

    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    // [START maps_current_place_on_map_ready]
    @Override
    public void onMapReady(GoogleMap map) {
        this.map = map;
        polylineOptions.color(R.color.purple_500).width(4f);
        mutablePolyline = map.addPolyline(polylineOptions);
        // [START_EXCLUDE]
        // [START map_current_place_set_info_window_adapter]
        // Use a custom info window adapter to handle multiple lines of text in the
        // info window contents.
        this.map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            // Return null here, so that getInfoContents() is called next.
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                // Inflate the layouts for the info window, title and snippet.
                View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_contents,
                        (FrameLayout) findViewById(R.id.map), false);

                TextView title = infoWindow.findViewById(R.id.title);
                title.setText(marker.getTitle());

                TextView snippet = infoWindow.findViewById(R.id.snippet);
                snippet.setText(marker.getSnippet());

                return infoWindow;
            }
        });
        // [END map_current_place_set_info_window_adapter]

        // Prompt the user for permission.
        getLocationPermission();
        // [END_EXCLUDE]

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();
    }
    // [END maps_current_place_on_map_ready]

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    // [START maps_current_place_get_device_location]
    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (locationPermissionGranted) {
                Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
                                if (!isLoop) {
                                    mHandler.sendEmptyMessage(2);
                                    isLoop = true;
                                }
                                map.clear();
                                Log.e("----经纬度", "" + lastKnownLocation.getLatitude() + "," + lastKnownLocation.getLongitude());

                                latLngList.add(new LatLng(lastKnownLocation.getLatitude(),
                                        lastKnownLocation.getLongitude()));
                                PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
                                for (int i = 0; i < latLngList.size(); i++) {
                                    LatLng point = latLngList.get(i);
                                    options.add(point);
                                }
                                mutablePolyline = map.addPolyline(options); //add Polyline
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(lastKnownLocation.getLatitude(),
                                                lastKnownLocation.getLongitude()), DEFAULT_ZOOM));

                                map.addMarker(new MarkerOptions()
                                        .position(new LatLng(lastKnownLocation.getLatitude(),
                                                lastKnownLocation.getLongitude()))
                                        .title("").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                            }
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            map.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                            map.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }
    // [END maps_current_place_get_device_location]

    /**
     * Prompts the user for permission to use the device location.
     */
    // [START maps_current_place_location_permission]
    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    private void getFilePermission() {
        isFilePre = true;
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            if (mMediaPlayer.isPlaying()) {
                return;
            }
            play();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                            , Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.RECORD_AUDIO},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }
    // [END maps_current_place_location_permission]

    /**
     * Handles the result of the request for location permissions.
     */
    // [START maps_current_place_on_request_permissions_result]
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (isFilePre) {
            isFilePre = false;
            if (requestCode
                    == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {// If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true;
                }
            } else {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
            if (mMediaPlayer.isPlaying()) {
                return;
            }
            play();
        } else {
            locationPermissionGranted = false;
            if (requestCode
                    == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {// If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true;
                }
            } else {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
            updateLocationUI();
        }

    }
    // [END maps_current_place_on_request_permissions_result]

    /**
     * Prompts the user to select the current place from a list of likely places, and shows the
     * current place on the map - provided the user has granted location permission.
     */
    // [START maps_current_place_show_current_place]
    private void showCurrentPlace() {
        if (map == null) {
            return;
        }

        if (locationPermissionGranted) {
            // Use fields to define the data types to return.
            List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME, Place.Field.ADDRESS,
                    Place.Field.LAT_LNG);

            // Use the builder to create a FindCurrentPlaceRequest.
            FindCurrentPlaceRequest request =
                    FindCurrentPlaceRequest.newInstance(placeFields);

            // Get the likely places - that is, the businesses and other points of interest that
            // are the best match for the device's current location.
            @SuppressWarnings("MissingPermission") final Task<FindCurrentPlaceResponse> placeResult =
                    placesClient.findCurrentPlace(request);
            placeResult.addOnCompleteListener(new OnCompleteListener<FindCurrentPlaceResponse>() {
                @Override
                public void onComplete(@NonNull Task<FindCurrentPlaceResponse> task) {
                    if (task.isSuccessful() && task.getResult() != null) {
                        FindCurrentPlaceResponse likelyPlaces = task.getResult();

                        // Set the count, handling cases where less than 5 entries are returned.
                        int count;
                        if (likelyPlaces.getPlaceLikelihoods().size() < M_MAX_ENTRIES) {
                            count = likelyPlaces.getPlaceLikelihoods().size();
                        } else {
                            count = M_MAX_ENTRIES;
                        }

                        int i = 0;
                        likelyPlaceNames = new String[count];
                        likelyPlaceAddresses = new String[count];
                        likelyPlaceAttributions = new List[count];
                        likelyPlaceLatLngs = new LatLng[count];

                        for (PlaceLikelihood placeLikelihood : likelyPlaces.getPlaceLikelihoods()) {
                            // Build a list of likely places to show the user.
                            likelyPlaceNames[i] = placeLikelihood.getPlace().getName();
                            likelyPlaceAddresses[i] = placeLikelihood.getPlace().getAddress();
                            likelyPlaceAttributions[i] = placeLikelihood.getPlace()
                                    .getAttributions();
                            likelyPlaceLatLngs[i] = placeLikelihood.getPlace().getLatLng();

                            i++;
                            if (i > (count - 1)) {
                                break;
                            }
                        }

                        // Show a dialog offering the user the list of likely places, and add a
                        // marker at the selected place.
                        MapsActivityCurrentPlace.this.openPlacesDialog();
                    } else {
                        Log.e(TAG, "Exception: %s", task.getException());
                    }
                }
            });
        } else {
            // The user has not granted permission.
            Log.i(TAG, "The user did not grant location permission.");

            // Add a default marker, because the user hasn't selected a place.
            map.addMarker(new MarkerOptions()
                    .title(getString(R.string.default_info_title))
                    .position(defaultLocation)
                    .snippet(getString(R.string.default_info_snippet)));

            // Prompt the user for permission.
            getLocationPermission();
        }
    }
    // [END maps_current_place_show_current_place]

    /**
     * Displays a form allowing the user to select a place from a list of likely places.
     */
    // [START maps_current_place_open_places_dialog]
    private void openPlacesDialog() {
        // Ask the user to choose the place where they are now.
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // The "which" argument contains the position of the selected item.
                LatLng markerLatLng = likelyPlaceLatLngs[which];
                String markerSnippet = likelyPlaceAddresses[which];
                if (likelyPlaceAttributions[which] != null) {
                    markerSnippet = markerSnippet + "\n" + likelyPlaceAttributions[which];
                }

                // Add a marker for the selected place, with an info window
                // showing information about that place.
                map.addMarker(new MarkerOptions()
                        .title(likelyPlaceNames[which])
                        .position(markerLatLng)
                        .snippet(markerSnippet));

                // Position the map's camera at the location of the marker.
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(markerLatLng,
                        DEFAULT_ZOOM));
            }
        };

        // Display the dialog.
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.pick_place)
                .setItems(likelyPlaceNames, listener)
                .show();
    }
    // [END maps_current_place_open_places_dialog]

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    // [START maps_current_place_update_location_ui]
    private void updateLocationUI() {
        if (map == null) {
            return;
        }
        try {
            if (locationPermissionGranted) {
                map.setMyLocationEnabled(true);
                map.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                map.setMyLocationEnabled(false);
                map.getUiSettings().setMyLocationButtonEnabled(false);
                lastKnownLocation = null;
                getLocationPermission();
//                map.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney,16.0f));
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }
    // [END maps_current_place_update_location_ui]


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
        mMediaPlayer.release();
        sensorManager.unregisterListener(this, sensor);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {


        double range = 1; //设置一个精度范围
        float[] value = sensorEvent.values;
        current_value = magnitude(value[0], value[1], value[2]); //计算当前的模

        //向上加速的状态
        if (motionState == true) {
            if (current_value >= last_value)
                last_value = current_value;
            else {
                //检测到一次峰值
                if (Math.abs(current_value - last_value) > range) {
                    original_value = current_value;
                    motionState = false;
                }
            }
        }
        //向下加速的状态
        if (motionState == false) {
            if (current_value <= last_value)
                last_value = current_value;
            else {
                //检测到一次峰值
                if (Math.abs(current_value - last_value) > range) {
                    original_value = current_value;
                    step++; //检测到开始记录，步数加1
                    tv_text.setText("当前步伐计数：" + step);
                    motionState = true;
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private double magnitude(float x, float y, float z) {
        double magnitude = 0;
        magnitude = Math.sqrt(x * x + y * y + z * z);
        return magnitude;
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (sm1 != null) {
            sm1.registerListener(sev1, sm1.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (sm2 != null) {
            sm2.registerListener(sev2, sm2.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private SensorEventListener sev1 = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            accelerometerValues = event.values;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private SensorEventListener sev2 = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            magneticFieldValues = event.values;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

}
