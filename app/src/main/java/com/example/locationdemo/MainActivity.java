package com.example.locationdemo;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.location.LocationManagerCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;

import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest; //Do not use android.location.LocationRequest
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnTokenCanceledListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    FusedLocationProviderClient fusedLocationClient;
    //boolean justAccepted = false;

    TextView text;
    int counter = 0;

    int LOCATION_TYPE = 1;  //1: Ask once
    //2: Ask until cancelled

    LocationCallback locationCallback; //for continuous updates

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text = findViewById(R.id.statustext);


        //Did user already accept the needed permissions?
        boolean fineLocationAlreadyAccepted = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseLocationAlreadyAccepted = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        //If we dont have the permissions, launch a dialogue that asks the user to accept
        askToEnable();
        if (!(fineLocationAlreadyAccepted && coarseLocationAlreadyAccepted)) {
            checkPermissions();

        }
        else if (LOCATION_TYPE == 1) askForLocationOnce();
        else askForLocationContinuously();

        //startMaps();
    }

    private void askForLocationContinuously() {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                counter++;
                if (locationResult == null) {
                    text.setText("Null location or error");
                    askToEnable();
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    text.setText(counter + " location " + location);
                }
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
               return;
        }
        fusedLocationClient.requestLocationUpdates(newRequest(),
                locationCallback,
                Looper.getMainLooper());

    }
    private void askForLocationOnce() {
        System.out.println("Start measuring");

        //// Now that we have the right permissions and GPS is enabled, we can ask for locations

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            System.out.println("permission check failed");
            return;
        }
        fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, new CancellationToken() {
            @NonNull
            @Override
            public CancellationToken onCanceledRequested(@NonNull OnTokenCanceledListener onTokenCanceledListener) {
                return null;
            }

            @Override
            public boolean isCancellationRequested() {
                return false;
            }
        }).addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
               if (location != null) {
                    String status = counter + " location: " + location.toString();
                    text.setText(status);
                } else {
                    text.setText("Null location or error");
                    askToEnable();

                }
            }
        });




    }


    //Helper method
    LocationRequest newRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    //If the user disabled GPS location on their phone, we ask them to turn it on
    void askToEnable() {
        LocationRequest l = newRequest();
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(l);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. The client can initialize
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            int REQUEST_CHECK_SETTINGS = 0x1;

            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;

                        resolvable.startResolutionForResult(MainActivity.this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });

    }

    //Check app permissions on runtime if not accepted earlier or if revoked
    private void checkPermissions() {
        // 1: Build the Permission request object with callbacks
        ActivityResultLauncher<String[]> locationPermissionRequest =
                registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                            System.out.println("Result recieved");
                    Boolean fineLocationGranted = null;
                    Boolean coarseLocationGranted = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                        coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                    }
                    else{
                        LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
                        fineLocationGranted = LocationManagerCompat.isLocationEnabled(locationManager);
                        coarseLocationGranted = LocationManagerCompat.isLocationEnabled(locationManager);
                    }

                            if (fineLocationGranted != null && fineLocationGranted) {
                                System.out.println("Fine accepted");
                                // Spaghetti solution. Could be handled better
                                if (LOCATION_TYPE == 1) askForLocationOnce();
                                else askForLocationContinuously();
                            } else if (coarseLocationGranted != null && coarseLocationGranted) {
                                System.out.println("coarse accepted");
                            } else {
                                System.out.println("None accepted");
                            }
                        }
                );
        // 2: Launch the permission request dialogue
        locationPermissionRequest.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });

    }


    private void startMaps() {

// Create a Uri from an intent string. Use the result to create an Intent.
        Uri gmmIntentUri = Uri.parse("google.streetview:cbll=46.414382,10.013988");

// Create an Intent from gmmIntentUri. Set the action to ACTION_VIEW
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
// Make the Intent explicit by setting the Google Maps package
        mapIntent.setPackage("com.google.android.apps.maps");

// Attempt to start an activity that can handle the Intent
        startActivity(mapIntent);

    }

}
