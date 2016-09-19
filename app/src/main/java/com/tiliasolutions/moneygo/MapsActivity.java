package com.tiliasolutions.moneygo;

import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.media.MediaPlayer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.tiliasolutions.moneygo.db.DatabaseHelper;

import java.util.HashMap;
import java.util.Random;

public class MapsActivity extends FragmentActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, GoogleMap.OnCameraIdleListener{

    private GoogleMap mMap;
    private final float MAX_ZOOM_LEVEL = 14.6f;

    private final int mCoinFive = 5;
    private final int mCoinTwo = 2;
    private final int mCoinOne = 1;


    private boolean mGameMode = false;

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;


    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private String mUser;

    LatLngBounds mBounds;
    private HashMap<LatLng, Integer> mAllGPSData = new HashMap<>();
    private HashMap<Marker, Integer> mAllMarkers = new HashMap<>();

    private MediaPlayer mMoneyCollectedSound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mDbHelper = DatabaseHelper.getInstance(this);
        mDb = mDbHelper.getWritableDatabase();

        //get username from previous screen
        Intent i = getIntent();
        mUser = i.getExtras().getString("USERNAME");

        //this could be probably removed, and just use method from dbHelper to check does user
        // exists
        mGameMode = mDbHelper.checkDoesUserExistsInGPSTable(mDb, mUser);
        mMoneyCollectedSound = MediaPlayer.create(getApplicationContext(), R.raw.cha_ching);

        buildGoogleApiClient();
        setupLocationRequest();
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "YOU NEED TO START GPS", Toast.LENGTH_LONG)
                    .show();
        } else {
            mMap.setMyLocationEnabled(true);
        }

        if (mDbHelper.checkDoesUserExistsInGPSTable(mDb, mUser)){
            //if user exists in gps table, then just populate map with game data
            removeButtonsFromMaps();
            mMap.resetMinMaxZoomPreference();
            loadAllGPSCoordinatesForThisUser();
            populateMarkers();
            //we should set camera on one of points from game
        }else{
            //if user does not exists then he needs to select area for play. Camera Listener is
            // needed to get LatLng data
            mMap.setOnCameraIdleListener(this);
            mMap.setMaxZoomPreference(MAX_ZOOM_LEVEL);
        }

    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
        else {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        double currentLat = (location.getLatitude());
        double currentLng = (location.getLongitude());
        for (Marker m : mAllMarkers.keySet()) {
            double markerLat = m.getPosition().latitude;
            double markerLng = m.getPosition().longitude;

            float[] res = new float[3];
            Location.distanceBetween(currentLat, currentLng, markerLat, markerLng, res);

            if (res[0] < 50 && mAllMarkers.get(m) < 8) {
                switch (mAllMarkers.get(m)) {
                    case 1:
                        m.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.marker10_40));
                        mDbHelper.updateCollectedCoinByTen(mDb, mUser, new LatLng(markerLat, markerLng),
                                mAllMarkers.get(m));
                        break;
                    case 2:
                        m.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.marker20_40));
                        mDbHelper.updateCollectedCoinByTen(mDb, mUser, new LatLng(markerLat, markerLng),
                                mAllMarkers.get(m));
                        break;
                    case 5:
                        m.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.marker50_40));
                        mDbHelper.updateCollectedCoinByTen(mDb, mUser, new LatLng(markerLat, markerLng),
                                mAllMarkers.get(m));
                        break;
                }
                if (mMoneyCollectedSound.isPlaying()) {
                    mMoneyCollectedSound.stop();
                }
                mMoneyCollectedSound.start();
            }
        }

    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, 9000);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "PROBLEM WITH CONNECTION", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    public void onCameraIdle() {
        //as camera moves, get screen bounds
        if (!mGameMode){
            mBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        }
    }

    //loading game data
    private void loadAllGPSCoordinatesForThisUser() {
        mAllGPSData = mDbHelper.getAllGPSData(mDb, mUser);
    }

    //populationg game data
    private void populateMarkers() {
        for (LatLng ll : mAllGPSData.keySet()) {
            Marker m;
            switch (mAllGPSData.get(ll)) {
                case 1:
                    m = mMap.addMarker(new MarkerOptions().
                            position(ll).
                            icon(BitmapDescriptorFactory.fromResource(R.drawable.marker1_40)));
                    mAllMarkers.put(m, mCoinOne);
                    break;
                case 2:
                    m = mMap.addMarker(new MarkerOptions().position(ll).icon
                            (BitmapDescriptorFactory.fromResource(R.drawable.marker2_40)));
                    mAllMarkers.put(m, mCoinTwo);
                    break;
                case 5:
                    m = mMap.addMarker(new MarkerOptions().position(ll).icon
                            (BitmapDescriptorFactory.fromResource(R.drawable.marker5_40)));
                    mAllMarkers.put(m, mCoinFive);
                    break;
                case 10:
                    m = mMap.addMarker(new MarkerOptions().position(ll).icon
                            (BitmapDescriptorFactory.fromResource(R.drawable.marker10_40)));
                    mAllMarkers.put(m, mCoinOne*10);
                    break;
                case 20:
                    m = mMap.addMarker(new MarkerOptions().position(ll).icon
                            (BitmapDescriptorFactory.fromResource(R.drawable.marker20_40)));
                    mAllMarkers.put(m, mCoinTwo*10);
                    break;
                case 50:
                    m = mMap.addMarker(new MarkerOptions().position(ll).icon
                            (BitmapDescriptorFactory.fromResource(R.drawable.marker50_40)));
                    mAllMarkers.put(m, mCoinFive*10);
                    break;
            }
        }
    }


    private void setupLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(3000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }


    //creating game data if user is just starting to play
    private void createRandomGPSCoordinates(){
        HashMap<String, String> coords = new HashMap<>();

        double lat1 = mBounds.northeast.latitude;
        double lat2 = mBounds.southwest.latitude;
        double lng1 = mBounds.southwest.longitude;
        double lng2 = mBounds.northeast.longitude;

        Random rand = new Random();

        //generate random LatLng points in bounds of screen
        for (int x = 0; x<65; x++) {
            double randomLat = rand.nextDouble();
            double randomLng = rand.nextDouble();
            double lat = lat2 + (randomLat * (lat1 - lat2));
            double lng = lng1 + (randomLng * (lng2 - lng1));
            coords.put(String.valueOf(lat), String.valueOf(lng));
        }

        //insert data into table
        //5 time 5 coins
        //15 times 2 coins
        //45 times 1 coin
        //100 coins in total
        int insertCoin=0;
        for (String lat : coords.keySet()){
            if (insertCoin < 5){
                mDbHelper.insertGPSDataInGPSTable(mDb,
                        mUser,
                        String.valueOf(lat),
                        String.valueOf(coords.get(lat)),
                        mCoinFive);
                insertCoin++;
            }

            else if (insertCoin < 20){
                mDbHelper.insertGPSDataInGPSTable(mDb,
                        mUser,
                        String.valueOf(lat),
                        String.valueOf(coords.get(lat)),
                        mCoinTwo);
                insertCoin++;
            }

            else if (insertCoin < 65) {
                mDbHelper.insertGPSDataInGPSTable(mDb,
                        mUser,
                        String.valueOf(lat),
                        String.valueOf(coords.get(lat)),
                        mCoinOne);
                insertCoin++;
            }
        }

    }

    //call this if user is alredy ready to play. this is needed only on first start of game for
    // particular user
    public void removeButtonsFromMaps(){
        ImageButton okButton = (ImageButton) findViewById(R.id.btn_ok);
        okButton.setVisibility(View.GONE);

        TextView textOnMap = (TextView) findViewById(R.id.text_on_map);
        textOnMap.setVisibility(View.GONE);
    }

    //strat play screen
    public void createGame(View v){
        createRandomGPSCoordinates();
        Toast.makeText(this, "COORDINATES CREATED. START PLAYING.", Toast.LENGTH_SHORT).show();
        removeButtonsFromMaps();

        mMap.setOnCameraIdleListener(null);
        mMap.resetMinMaxZoomPreference();
        loadAllGPSCoordinatesForThisUser();
        populateMarkers();

    }

    //this should be called to set camera on position of user.
    public void setCameraPosition(LatLng latLng){
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
    }


}

