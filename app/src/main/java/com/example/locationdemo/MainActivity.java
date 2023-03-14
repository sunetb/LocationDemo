package com.example.locationdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;

import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

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

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    FusedLocationProviderClient fusedLocationClient;
    LocationCallback locationCallback; //for continuous updates

    TextView text;
    Button active, stop;
    RadioButton once, continuous;
    int counter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Setting the views
        text = findViewById(R.id.statustext);
        active = findViewById(R.id.button);
        stop = findViewById(R.id.button2);
        stop.setOnClickListener(this);
        active.setOnClickListener(this);
        once = findViewById(R.id.once);
        continuous = findViewById(R.id.continuous);

        //startMaps();
    }

    @Override
    public void onClick(View view) {
        if (view == stop){
            if (locationCallback != null) {
                fusedLocationClient.removeLocationUpdates(locationCallback);
            }
            return;
        }

        //Check app permissions
        boolean fineLocationAlreadyAccepted = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseLocationAlreadyAccepted = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (!(fineLocationAlreadyAccepted && coarseLocationAlreadyAccepted)) {
            //Dialogue to ask the user
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1234);
            text.setText("Press again");
            return;

        }

        //We ask if Location is enabled in settings
        askToEnableLocation();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        text.setText("");

        //Check the radiobuttons
        if (once.isChecked()) askForLocationOnce();
        else askForLocationContinuously();

    }

    private void askForLocationContinuously() {

        // Instantiate the callback-object
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                counter++;
                if (locationResult == null) {
                    text.setText("Error: no location detected");
                    return;
                }
                Location location = locationResult.getLastLocation();
                System.out.println(location);
                String result = "Long: " + location.getLongitude() + " | Lat: " + location.getLatitude();
                text.setText(counter + " location " + result);
            }
        };

        //Create a request object
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        //Required extra check before calling requestLocationUpdates()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        //Ask for updates
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());

    }
    private void askForLocationOnce() {
        //Required extra check before calling getCurrentLocation()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, new CancellationToken() {
            @NonNull
            @Override
            public CancellationToken onCanceledRequested(@NonNull OnTokenCanceledListener onTokenCanceledListener) {return null;}
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
                    text.setText("Error: No location");
                    askToEnableLocation();

                }
            }
        });
    }


    //Helper method
    //If the user disabled GPS location on their phone, we ask them to turn it on
    void askToEnableLocation() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // User has Location enabled
            }
        });
        //If GPS is diabled, we show a dialog
        task.addOnFailureListener(this, new OnFailureListener() {
            int REQUEST_CHECK_SETTINGS = 0x1;
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });

    }

//Just for fun

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
