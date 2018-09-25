package com.hm.hmlocationawareapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class HMLocationAwareFragment extends Fragment {

    private static final String TAG = HMLocationAwareFragment.class.getSimpleName();

    private static final int REQUEST_LOCATION_PERMISSION = 987;

    private static final int REQUEST_CHECK_SETTINGS = 654;

    private boolean locationServicesEnabled = false;

    private Location deviceLocation;

    private FusedLocationProviderClient mFusedLocationClient;

    private LocationCallback mLocationCallback;

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 600 * 1000;

    /**
     * The fastest rate for active location updates. Updates will never be more frequent
     * than this value.
     */
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private LocationRequest mLocationRequest;

    public HMLocationAwareFragment() {

        //Google mountain view location
        deviceLocation = new Location("default_location");
        deviceLocation.setLatitude(37.428828);
        deviceLocation.setLongitude(-122.084344);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                checkLocationChange(locationResult.getLastLocation());
            }

            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {

                Log.v(TAG, locationAvailability.toString());
            }
        };
    }

    private void checkLocationChange(Location location) {

        if(deviceLocation == null) {

            deviceLocation = location;

            onNewLocation(deviceLocation);
        }

        else {

            float[] distance = new float[1]; //distance in meters
            Location.distanceBetween(location.getLatitude(), location.getLongitude(), deviceLocation.getLatitude(), deviceLocation.getLongitude(), distance);

            //use the distance variable if you want to limit the location updates based on distance between last location and new location

            deviceLocation = location;
            onNewLocation(deviceLocation);
        }
    }

    //Override this method in your HMLocationAwareFragment subclass
    protected void onNewLocation(Location location) {


    }

    private boolean checkLocationPermissions() {

        if(ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            return true;
        }

        else {

            return false;
        }
    }

    private void checkLocationSettings() {

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        SettingsClient client = LocationServices.getSettingsClient(getContext());
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(getActivity(), new OnSuccessListener<LocationSettingsResponse>() {

            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {

                locationServicesEnabled = true;
                getLastLocation();
            }
        });

        task.addOnFailureListener(getActivity(), new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                locationServicesEnabled = false;

                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(getActivity(),
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {

        if(locationServicesEnabled) {
            try {
                mFusedLocationClient.getLastLocation()
                        .addOnCompleteListener(new OnCompleteListener<Location>() {
                            @Override
                            public void onComplete(@NonNull Task<Location> task) {
                                if (task.isSuccessful() && task.getResult() != null) {

                                    checkLocationChange(task.getResult());

                                } else {
                                    Log.v(TAG, "Failed to get location.");

                                    startLocationUpdates();
                                }
                            }
                        });
            } catch (SecurityException unlikely) {

                unlikely.printStackTrace();
            }
        }
    }


    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {

        if(locationServicesEnabled) {
            Log.v(TAG, "Requesting location updates");

            try {
                mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                        mLocationCallback, Looper.myLooper());
            } catch (SecurityException unlikely) {

                unlikely.printStackTrace();
            }
        }
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onStart() {
        super.onStart();

        if(mLocationRequest == null)
            createLocationRequest();

        if(checkLocationPermissions()) {

            checkLocationSettings();
        }

        else {

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        startLocationUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {

        if(requestCode == REQUEST_LOCATION_PERMISSION) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                checkLocationSettings();
            }
            else {

                //Lets use the default location
                checkLocationChange(deviceLocation);
            }
        }
    }
}
