package com.example.weathervane;

import androidx.preference.PreferenceManager;
import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;


import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    SharedPreferences preferences;

    //URL strings
    private final String stationURLStringFinal = "https://api.synopticdata.com/v2/stations/metadata?&token=&limit=5&radius=&vars=wind_speed&output=json&status=active";
    private final String windSpeedURLStringFinal = "https://api.synopticdata.com/v2/stations/latest?&token=&within=60&output=json&units=speed|kts&stid=&vars=wind_speed,wind_direction,wind_gust&obtimezone=local&fields=sensor_variables";
    private String stationURLString = "https://api.synopticdata.com/v2/stations/metadata?&token=&limit=5&radius=44.155,-73.294,20&vars=wind_speed&output=json&status=active";
    private String windSpeedURLString = "https://api.synopticdata.com/v2/stations/latest?&token=&within=60&output=json&units=speed|kts&stid=UVM03&vars=wind_speed,wind_direction,wind_gust&obtimezone=local&fields=sensor_variables";

    //Variables for Location
    private static final int REQUEST_ACCESS_FINE_LOCATION = 42;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    //Data from the API
    private List<LocationResponse> locations;
    private List<WindResponse> windConditions = new ArrayList<WindResponse>();
    private LocationResponse selectedLocation;

    //Data for the compass animation
    private ImageView image;
    private float currentAzimuth;
    private float windDirection;
    private Compass compass;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Asks for GPS location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ACCESS_FINE_LOCATION);
        }

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        //recovers last location
        String stationName = preferences.getString("Location_Name", "Diamond Island");
        String stationID = preferences.getString("Location_Id", "UVM03");

        addStationToURL(stationID);
        Log.i("Test: ", stationID);
        new MakeApiRequest("weather").execute(windSpeedURLString);

        TextView locationName = (TextView) findViewById(R.id.stationName);
        locationName.setText(stationName);

        windConditions.add(new WindResponse(0, 0, 0));
        image = (ImageView) findViewById(R.id.compassImage);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = new LocationRequest();

        //Settings for a location request
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); //Sets the priority of the requests. This effects accuracy and power consumption.
        locationRequest.setInterval(5000); //Sets the rate at which your app receives location updates
        locationRequest.setFastestInterval(5000);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                //This runs every time there is a new location result
                currentLocation = locationResult.getLastLocation();
                addLocationToURL(currentLocation);
                new MakeApiRequest("location").execute(stationURLString);
                fusedLocationClient.removeLocationUpdates(locationCallback);
            }
        };

        setupCompass();
    }


    //When the app resumes it starts listening to the sensors
    @Override
    protected void onResume(){
        super.onResume();
        compass.start();
    }
    //Stops listening the the sensors when the app is not active
    @Override
    protected void onPause() {
        super.onPause();

        compass.stop();

        //Saves last location
        if (selectedLocation != null) {
            Log.i("Test: " , "inside onPause");
            preferences.edit().putString("Location_Name", selectedLocation.getName()).apply();
            preferences.edit().putString("Location_Id", selectedLocation.getStId()).apply();

        }

    }
    //Adds the Items to the activity bar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }
    //Reacts to a menu item being selected
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_location:
                //getLocation();
                Log.i("Location: ", "Before call " + stationURLString);
                //new MakeApiRequest("location").execute(stationURLString);
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
                return true;
            case R.id.action_refresh:
                if (selectedLocation != null)
                    addStationToURL(selectedLocation.getStId());
                new MakeApiRequest("weather").execute(windSpeedURLString);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupCompass() {
        compass = new Compass(this);
        Compass.CompassListener cl = getCompassListener();
        compass.setListener(cl);
    }

    private void adjustArrow(float azimuth) {
        azimuth += windDirection;

        Animation an = new RotateAnimation(-currentAzimuth, -azimuth,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        currentAzimuth = azimuth;

        an.setDuration(500);
        an.setRepeatCount(0);
        an.setFillAfter(true);

        image.startAnimation(an);
    }

    private Compass.CompassListener getCompassListener() {
        return new Compass.CompassListener() {
            @Override
            public void onNewAzimuth(final float azimuth) {
                // UI updates only in UI thread
                // https://stackoverflow.com/q/11140285/444966
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adjustArrow(azimuth);
                    }
                });
            }
        };
    }


    //Takes the input stream from the HTTP GET and parses the json into objects. After, it creates a dialog box so you can choose which location you want
    public void handleLocationResponse(InputStream in) {
        JSONHandler handler = new JSONHandler();
        try {
            locations = handler.parseLocations(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (locations != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogTheme);
            builder.setTitle(R.string.station_list)
                    .setItems(locationArray(), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            selectedLocation = locations.get(i);

                            TextView locationName = (TextView) findViewById(R.id.stationName);
                            locationName.setText(selectedLocation.getName());
                            addStationToURL(selectedLocation.getStId());
                            new MakeApiRequest("weather").execute(windSpeedURLString);
                            dialogInterface.dismiss();
                        }
                    });
            builder.create();
            builder.show();
        } else {
            Toast toast = Toast.makeText(this, "Failed to find stations", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    /**
     * Takes the locations list and adds the name of each element to a array
     * @return an array with names of all the locations
     */
    public CharSequence[] locationArray() {
        CharSequence[] locationStrings = new CharSequence[locations.size()];
        for (int i = 0; i < locations.size(); i++){
            locationStrings[i] = locations.get(i).getName();
        }
        return locationStrings;
    }

    /**
     * Adds the station id to the URL for the weather GET request
     * @param stId station Id that you want to get data from. Never null.
     */
    public void addStationToURL(String stId) {
        int locationPosition = windSpeedURLStringFinal.indexOf("stid=") + 5;
        windSpeedURLString = windSpeedURLStringFinal.substring(0, locationPosition) + stId + windSpeedURLStringFinal.substring(locationPosition);
    }

    /**
     * Takes the input stream from a get request and parses it into weatherObjects
     * @param in inputStream containing wind data in JSON. Never null.
     */
    public void handleWindResponse(InputStream in) {
        JSONHandler handler = new JSONHandler();
        try {
            windConditions = handler.parseWind(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (windConditions != null) {
            TextView windSpeed = (TextView) findViewById(R.id.windSpeed);
            windSpeed.setText(Double.toString(windConditions.get(0).getWindSpeed()));
            TextView gustSpeed = (TextView) findViewById(R.id.windGust);
            gustSpeed.setText(Double.toString(windConditions.get(0).getWindGust()));
            windDirection = (float) windConditions.get(0).getWindDir();
        } else {
            Toast toast = Toast.makeText(this, "Failed to update weather", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    /**
     * Gets last location. Broken DO NOT USE
     */
    public void getLocation(){
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {

                        if (location != null){
                            currentLocation = location;
                            addLocationToURL(location);
                            Log.i("Location: ", "is  not null");
                            Log.i("Location: ", location.toString());
                            //new MakeApiRequest("location").execute(stationURLString);
                        } else {
                            Log.i("Location: ", "is null");
                        }

                    }
                });

    }

    /**
     * Adds the latittude and Longitude to the location GET request URL
     * @param loc Location object containing the need location data
     */
    public void addLocationToURL(Location loc){
        String coordinates = loc.getLatitude() + "," + loc.getLongitude() + "," + 20;
        int locationPosition = stationURLStringFinal.indexOf("radius=") + 6;
        stationURLString = stationURLStringFinal.substring(0, locationPosition + 1) + coordinates + stationURLStringFinal.substring(locationPosition + 1);
        Log.i("Location:", "In addLocation " + stationURLString);
    }
    //This handles the HTTP requests
    private class MakeApiRequest extends AsyncTask<String, Integer, InputStream> {
        private String type;
        public MakeApiRequest(String type){
            this.type = type;
        }
        protected InputStream doInBackground(String... strings) {
            try {
                Log.i("Test:", "In Task" + strings[0]);
                //Making the HTTP GET request
                URL stationsURL = new URL(strings[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) stationsURL.openConnection();

                String responseMessage = urlConnection.getResponseMessage();
                Log.i("Response: ", responseMessage);
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());

                urlConnection.disconnect();

                return in;

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(InputStream in) {
            if (type.equals("location")){
                handleLocationResponse(in);
            } else if (type.equals("weather")){
                handleWindResponse(in);
            }
        }
    }


}
