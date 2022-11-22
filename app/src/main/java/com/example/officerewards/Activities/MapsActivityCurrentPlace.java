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

package com.example.officerewards.Activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.officerewards.BuildConfig;
import com.example.officerewards.R;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * An activity that displays a map showing the place at the device's current location.
 */
public class MapsActivityCurrentPlace extends AppCompatActivity
        implements OnMapReadyCallback {

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
    private final LatLng officeLocation = new LatLng(51.898378, -8.465669);
    private static final int DEFAULT_ZOOM = 15;
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

    //firebase auth object to get the logged in users ID, and firebase firestore object to access the database
    FirebaseAuth auth;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    //Declare strings
    String username;
    String userid;
    String currentDate;

    // [START maps_current_place_on_create]
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

        // [START_EXCLUDE silent]
        // Construct a PlacesClient
        Places.initialize(getApplicationContext(), BuildConfig.MAPS_API_KEY);
        placesClient = Places.createClient(this);

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Build the map.
        // [START maps_current_place_map_fragment]
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        // [END maps_current_place_map_fragment]
        // [END_EXCLUDE]


        //Get the ID of the current user with firebase auth, this allows me to match it to a user in the Firestore
        //https://firebase.google.com/docs/auth/android/manage-users
        auth = FirebaseAuth.getInstance();
        userid = auth.getCurrentUser().getUid();

        //Retrieving the user from cloud firestore now that we have the ID
        //https://firebase.google.com/docs/firestore/query-data/get-data
        DocumentReference documentReference = db.collection("Users").document(userid);
        documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        username = String.valueOf(document.getData().get("Name"));
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
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
     * @param item The menu item to handle.
     * @return Boolean.
     */
    // [START maps_current_place_on_options_item_selected]
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.option_get_place) {
            showPoints();
        }
        if (item.getItemId() == R.id.option_check_distance) {
            checkIn();
        }
        if (item.getItemId() == R.id.checkout) {
            checkOut();
        }
        if (item.getItemId() == R.id.logout) {
            userLogout();
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
                @SuppressLint("MissingPermission") Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(lastKnownLocation.getLatitude(),
                                                lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
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
        } catch (SecurityException e)  {
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
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
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
    // [END maps_current_place_on_request_permissions_result]

    /**
     * Prompts the user to select the current place from a list of likely places, and shows the
     * current place on the map - provided the user has granted location permission.
     */

    private void showPoints() {
        //Retrieving the points for the current user
        //https://firebase.google.com/docs/firestore/query-data/get-data
        DocumentReference documentReference = db.collection("Points").document(userid);
        documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        String points = String.valueOf(document.getData().get("points"));
                        Toast.makeText(MapsActivityCurrentPlace.this, "Current Balance: " + points, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "points: " + points);
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }


    //checks if the user is close enough to the office
    //if they are, then we get the current date, and insert it along with the UserID and a timestamp of the current time into firebase
    //code for calculating distance adapted from https://stackoverflow.com/q/18310126
    private void checkIn() {
        double distance = 0;
        Location locationB = new Location("B");
        locationB.setLatitude(officeLocation.latitude);
        locationB.setLongitude(officeLocation.longitude);
        distance = lastKnownLocation.distanceTo(locationB);
        //distance is measured in metres
        if (distance > 2000) {
            Log.d(TAG,"Too far away, not checked in");
        }
        else {
            Log.d(TAG,"Checked In!");

            //get the current date, adapted from https://stackoverflow.com/a/15698784
            currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

            Map<String, Object> data = new HashMap<>();
            data.put("userid", userid);
            //Using a timestamp allows for verifying what time the user checks in
            //Adapted from https://medium.com/firebase-developers/the-secrets-of-firestore-fieldvalue-servertimestamp-revealed-29dd7a38a82b
            data.put("timestampin", FieldValue.serverTimestamp());
            data.put("date", currentDate);

            //Check if there is a corresponding check in on the same day, if there is, we dont allow them to check in a 2nd time
            //Getting data from firestore adapted from https://firebase.google.com/docs/firestore/query-data/get-data
            DocumentReference documentReference = db.collection("Checks").document(userid + currentDate);
            documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Log.d(TAG, "DocumentSnapshot data1: " + document.getData());
                            Toast.makeText(MapsActivityCurrentPlace.this, "You already checked in today.", Toast.LENGTH_SHORT).show();
                        } else {
                            //this means that no record exists and we allow them to check in
                            //Inserting the Check-in into firebase
                            //Inserting into firebase adapted from https://firebase.google.com/docs/firestore/manage-data/add-data
                            DocumentReference docRef = db.collection("Checks").document(userid + currentDate);
                            docRef.set(data).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            Toast.makeText(MapsActivityCurrentPlace.this, "Checked In!", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.w(TAG, "Error adding document", e);
                                        }
                                    });
                        }
                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                        Log.d("TAG", "No Checkin");
                    }
                }
            });
        }
    }

    //similar to check in, gets the distance from the office
    //if close enough, the check out is allowed
    public void checkOut() {
        double distance = 0;
        Location locationB = new Location("B");
        locationB.setLatitude(officeLocation.latitude);
        locationB.setLongitude(officeLocation.longitude);
        distance = lastKnownLocation.distanceTo(locationB);
        if (distance > 2000) {
            Log.d(TAG,"Too far away, not checked in");
        }
        else {
            currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

            //Check if there is a corresponding check in on the same day, if not, the user cannot check out
            DocumentReference documentReference = db.collection("Checks").document(userid + currentDate);
            documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Log.d(TAG, "DocumentSnapshot data1: " + document.getData());
                            if (document.getData().get("timestampout") == null) {
                                //Because we have found a check in on the same date, and no checkout exists then we complete the checkout
                                completeCheckout();
                            }
                            else {
                                //because timestampout is not null, that means the user has already checked out today
                                Toast.makeText(MapsActivityCurrentPlace.this, "You have already checked out today.", Toast.LENGTH_SHORT).show();
                            }

                        } else {
                            //because the document does not exist in firestore, that means the user has not checked in yet
                            Log.d(TAG, "No such document");
                            Toast.makeText(MapsActivityCurrentPlace.this, "You haven't checked in today.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d(TAG, "Error ", task.getException());
                    }
                }
            });
        }
    }

    //Insert the checkout time into the database
    public void completeCheckout() {
        Map<String, Object> data = new HashMap<>();
        //Using a timestamp allows for verifying what time the user checks out
        //Adapted from https://medium.com/firebase-developers/the-secrets-of-firestore-fieldvalue-servertimestamp-revealed-29dd7a38a82b
        data.put("timestampout", FieldValue.serverTimestamp());

        DocumentReference documentReference = db.collection("Checks").document(userid + currentDate);
        documentReference.set(data, SetOptions.merge()).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //Move onto the final part of checking out, calculating the points the user receives
                        calculatePoints();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });
    }

    //Calculates the points the user gets for the difference in time between their check in and check out
    public void calculatePoints() {
        //Gets the record where they checked in and checked out for the current date
        DocumentReference documentReference = db.collection("Checks").document(userid + currentDate);
        documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        //Take the two timestamps, then get the difference between them in milliseconds
                        Timestamp in = (Timestamp) document.getData().get("timestampin");
                        Timestamp out = (Timestamp) document.getData().get("timestampout");
                        //calculating difference between timestamps adapted from https://www.edureka.co/community/7139/calculating-date-time-difference-in-java
                        final Date endTimeDate = out.toDate();
                        final Date startTimeDate = in.toDate();
                        //this variable (diff) is in milliseconds, the minutes variable has converted those to minutes
                        //in future it will be easy to edit this to reward employees with a custom amount of points per hour, depending on what the employer sets
                        long diff = endTimeDate.getTime() - startTimeDate.getTime();
                        long minutes = (diff/1000)/60;


                        DocumentReference recordref = db.collection("Points").document(userid);
                        //Increment the points total for the user by the amount of minutes (for now, this will change), adapted from https://stackoverflow.com/a/71860537
                        recordref.update("points", FieldValue.increment(minutes)).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Log.d("Incremented", "YES");
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d("Not Incremented", String.valueOf(e));
                            }
                        });
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }

    //Signs out the user and brings them back to the starting activity, adapted from https://firebase.google.com/docs/auth/android/firebaseui
    public void userLogout() {
        AuthUI.getInstance()
                .signOut(MapsActivityCurrentPlace.this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull @NotNull Task<Void> task) {
                        Intent intent = new Intent(MapsActivityCurrentPlace.this,RegisterActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull @NotNull Exception e) {
                        Toast.makeText(MapsActivityCurrentPlace.this, "Sign Out Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    // [START maps_current_place_update_location_ui]
    @SuppressLint("MissingPermission")
    private void updateLocationUI() {
        if (map == null) {
            return;
        }
        try {
            if (locationPermissionGranted) {
                map.setMyLocationEnabled(true);
                map.getUiSettings().setMyLocationButtonEnabled(true);
                map.addMarker(new MarkerOptions()
                        .title("Deloitte Cork Office")
                        .position(officeLocation)
                        .snippet("Office Location"));
            } else {
                map.setMyLocationEnabled(false);
                map.getUiSettings().setMyLocationButtonEnabled(false);
                lastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }
    // [END maps_current_place_update_location_ui]
}
