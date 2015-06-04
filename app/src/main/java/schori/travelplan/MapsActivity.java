package schori.travelplan;

import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.model.PolylineOptions;

public class MapsActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    public static final String TAG = MapsActivity.class.getSimpleName();
    public static final String TAG2 = "TheDebug";

    /*
     * Define a request code to send to Google Play services
     * This code is returned in Activity.onActivityResult
     */
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    ArrayList<LatLng> markerPoints;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();

        markerPoints = new ArrayList<LatLng>();

        //SupportMapFragment fm =
        //        (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);

        //map = fm.getMap();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link com.google.android.gms.maps.SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {

                mMap.setMyLocationEnabled(true);
                mMap.setOnMapClickListener(new OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng pt) {

                        /*// Already two locations
                        if(markerPoints.size()>1){
                            markerPoints.clear();
                            mMap.clear();
                        }
                        */
                        // Adding new item to the ArrayList
                        markerPoints.add(pt);
                        // Creating MarkerOptions
                        MarkerOptions options = new MarkerOptions();
                        // Setting the position of the marker
                        options.position(pt);

                        /**
                         * For the start location, the color of marker is GREEN and
                         * for the end location, the color of marker is RED.
                         */
                        if(markerPoints.size()==1){
                            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                        }else if(markerPoints.size()==2){
                            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                        }

                        // Add new marker to the Google Map Android API V2
                        mMap.addMarker(options);

                        // Checks, whether start and end locations are captured
                        if(markerPoints.size() >= 2) {
                            LatLng origin = markerPoints.get(0);
                            LatLng dest = markerPoints.get(1);

                            // Getting URL to the Google Directions API
                            String url = getDirectionsUrl(origin, dest);

                            DownloadTask downloadTask = new DownloadTask();

                            // Start downloading json data from Google Directions API
                            downloadTask.execute(url);
                        }
                    }
                });
                setUpMap();
            }
        }
    }

    private String getDirectionsUrl(LatLng origin,LatLng dest){

        // Origin of route
        String str_origin = "origin="+origin.latitude+","+origin.longitude;

        // Destination of route
        String str_dest = "destination="+dest.latitude+","+dest.longitude;


        // Sensor enabled
        String sensor = "sensor=false";

        //Set into walking mode so as to get a simpler route instead of a driving itinerary
        String mode = "mode=walking";
        // Building the parameters to the web service
        String parameters = str_origin+"&"+str_dest+"&"+sensor+"&"+mode;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;


        return url;
    }

    /** A method to download json data from url */
    private String downloadUrl(String strUrl) throws IOException{
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb  = new StringBuffer();

            String line = "";
            while( ( line = br.readLine())  != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }catch(Exception e){
            Log.d("Exception while dl url", e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    // Fetches data from url passed
    private class DownloadTask extends AsyncTask<String, Void, String>{

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try{
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);

        }
    }

    /** A class to parse the Google Places in JSON format */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>> >{

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try{
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            }catch(Exception e){
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        //@Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();

            // Traversing through all the routes
            for(int i=0;i<result.size();i++){
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for(int j=0;j<path.size();j++){
                    HashMap<String,String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(14);
                lineOptions.color(Color.DKGRAY);

            }

            // Drawing polyline in the Google Map for the i-th route
            mMap.addPolyline(lineOptions);
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera.
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener()
        {
            @Override
            public boolean onMarkerClick(com.google.android.gms.maps.model.Marker marker)
            {
                marker.showInfoWindow();
                return true;
            }
        });

        LatLng hotel = new LatLng(48.88035000000001,2.3173060000000305);
        LatLng place1 = new LatLng(48.85837, 2.294481000000019);
        LatLng place2 = new LatLng(48.864342, 2.297820999999999);
        LatLng place3 = new LatLng(48.873792, 2.295028000000002);
        LatLng place4 = new LatLng(48.852968, 2.349901999999929);
        LatLng place5 = new LatLng(48.860611, 2.3376439999999548);
        LatLng place6 = new LatLng(48.804865, 2.1203550000000178);
        LatLng resto1 =  new LatLng(48.867198, 2.289799000000017);
        LatLng resto2 =  new LatLng(48.860805, 2.3005600000000186);
        LatLng resto3 =  new LatLng(48.85955500000001, 2.353964000000019);


        LatLng[] places = {place1, place2, place3, place4, place5, place6};
        LatLng[] restos = {resto1, resto2, resto3};

        // creating the markers
        Marker[] markerPlaces = new Marker[places.length];
        Marker[] markerRestos = new Marker[restos.length];
        for (int i = 0; i < places.length; i++){
             markerPlaces[i] = mMap.addMarker(new MarkerOptions().position(places[i])
                    .title("Place")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.museums)));
        }
        for (int i = 0; i < restos.length;  i++){
            markerRestos[i] = mMap.addMarker(new MarkerOptions().position(restos[i])
                    .title("Restaurant")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.restaurants)));
        }

        mMap.addMarker(new MarkerOptions().position(hotel).title("Hotel")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.hotels))
                .draggable(false));


        CameraPosition cameraPos = new CameraPosition.Builder().target(
                hotel).zoom(12).tilt(70).build();

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPos));
        mMap.getUiSettings().setZoomControlsEnabled(true);

        makeItinerary(places,restos,hotel);



        }
    //The vrp algorithm
    private void makeItinerary(LatLng[] places, LatLng[] restos, LatLng hotel){
        //TODO: Apply algorithm mentioned in paper (already implemented in Python) or include the Python script

        //After the algorithm returns paths, simply draw them by calling the function. Here is an example.

        //Example of a path
        LatLng[] path1 = {hotel, places[0], places[1], restos[0], places[2]};
        LatLng[] path2 = {hotel, places[3], restos[1], places[4], restos[2]};

        LatLng[] path3 = {hotel, places[5]};

        for(int i = 0; i < path3.length; i++){
            if (i < path3.length - 1) {
                // Getting URL to the Google Directions API
                String url = getDirectionsUrl(path3[i], path3[i + 1]);
                DownloadTask downloadTask = new DownloadTask();
                // Start downloading json data from Google Directions API
                downloadTask.execute(url);
            } else { //round-trip
                String url = getDirectionsUrl(path3[i], path3[0]);
                DownloadTask downloadTask = new DownloadTask();
                downloadTask.execute(url);
            }
        }
/*
        String url = getDirectionsUrl(hotel, restos[1]);

        DownloadTask downloadTask = new DownloadTask();


        downloadTask.execute(url);*/
    }

    public float distance (float lat_a, float lng_a, float lat_b, float lng_b )
    {
        double earthRadius = 3958.75;
        double latDiff = Math.toRadians(lat_b-lat_a);
        double lngDiff = Math.toRadians(lng_b-lng_a);
        double a = Math.sin(latDiff /2) * Math.sin(latDiff /2) +
                Math.cos(Math.toRadians(lat_a)) * Math.cos(Math.toRadians(lat_b)) *
                        Math.sin(lngDiff /2) * Math.sin(lngDiff /2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double distance = earthRadius * c;

        int meterConversion = 1609;

        return new Float(distance * meterConversion).floatValue();
    }

    private void handleNewLocation(Location location) {
        Log.d(TAG, location.toString());

        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();

        LatLng latLng = new LatLng(currentLatitude, currentLongitude);

        //mMap.addMarker(new MarkerOptions().position(new LatLng(currentLatitude, currentLongitude)).title("Current Location"));
        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title("I am here!");
        mMap.addMarker(options);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

    }

    @Override
    public void onConnected(Bundle bundle) {
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location == null) {
            Log.d(TAG2, "In the IF");
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            Log.d(TAG2, "location?"+location);
        }
        else {
            Log.d(TAG2, "in the else...");
            handleNewLocation(location);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }
}
