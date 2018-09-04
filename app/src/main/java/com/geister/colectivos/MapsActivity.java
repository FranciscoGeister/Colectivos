package com.geister.colectivos;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryDataEventListener;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CustomCap;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static android.provider.Contacts.SettingsColumns.KEY;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
                                                            GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener,
                                                            RoutingListener{

    private GoogleMap mMap;
    //variables para permisos de ubicación
    private boolean mLocationPermissionGranted;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private CameraPosition mCameraPosition;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng mDefaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final int DEFAULT_ZOOM = 15;
    private static final String TAG = MapsActivity.class.getSimpleName();

    //para las polilineas
    private static final int COLOR_BLACK_ARGB = 0xff000000;
    private static final int POLYLINE_STROKE_WIDTH_PX = 12;

    private EditText address;
    private Button ruta;
    private LatLng destination;
    private String comuna;
    private LatLng closestStop;
    private Boolean primera;
    private Boolean firstStop;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseDatabase database;
    private DatabaseReference mRef;
    private DatabaseReference mRefLastRoute;
    private static final String TAG2 = "AnonymousAuth";

    //lat lng of the nearest stop to destination;

    private Button info;
    private Button driver;

    private ArrayList<Route> routes;

    private GeoQuery geoQueryDrivers;
    GeoQuery geoQueryStops;
    GeoQuery geoQueryStopFromRoute;

    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    Location mLastLocation;
    SupportMapFragment mapFragment;
    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.wallet_holo_blue_light};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        database = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        primera = true;
        firstStop = true;
        polylines = new ArrayList<>();
        address = (EditText) findViewById(R.id.editText);
        routes = new ArrayList();
        ruta = (Button) findViewById(R.id.ruta);
        info = (Button) findViewById(R.id.info);
        driver = (Button) findViewById(R.id.driver);

        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), InfoMaps.class);
                startActivity(i);
            }
        });

        driver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), DriverActivity.class);
                startActivity(i);
            }
        });

        ruta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.clear();
                if (!primera){
                    geoQueryStops.removeAllListeners();
                    geoQueryStopFromRoute.removeAllListeners();
                    mRefLastRoute.removeValue();
                }
                if (!firstStop){
                    geoQueryDrivers.removeAllListeners();
                }
                primera = true;
                firstStop = true;
                mRef = database.getReference("stops/"+comuna);
                GeoFire geoFireStops = new GeoFire(mRef);
                geoQueryStops = geoFireStops.queryAtLocation(new GeoLocation(destination.latitude,destination.longitude),0.2);
                geoQueryStops.addGeoQueryEventListener(new GeoQueryEventListener() {
                    @Override
                    public void onKeyEntered(String key, GeoLocation location) {
                        if (primera) {
                            final String[] routeName = key.split("-");
                            address.setText(routeName[0]);
                            mRef = database.getReference("routes/"+comuna+"/"+routeName[0]);
                            mRef.orderByKey().addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();
                                    ArrayList<LatLng> latLngRoute = new ArrayList<>();
                                    DataSnapshot latLng;
                                    while (iterator.hasNext()) {
                                        latLng = iterator.next();
                                        latLngRoute.add(new LatLng(Double.parseDouble(latLng.child("l").child("0").getValue().toString()),Double.parseDouble(latLng.child("l").child("1").getValue().toString())));
                                    }
                                    mMap.addPolyline(new PolylineOptions().addAll(latLngRoute));
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                            //route from stop to destination
                            MakeRoute(new LatLng(location.latitude,location.longitude),new LatLng(destination.latitude,destination.longitude));
                            GeoFire geoFireStopFromRoute = new GeoFire(mRef);
                            geoQueryStopFromRoute = geoFireStopFromRoute.queryAtLocation(new GeoLocation(mLastLocation.getLatitude(),mLastLocation.getLongitude()),0.2);
                            geoQueryStopFromRoute.addGeoQueryEventListener(new GeoQueryEventListener() {
                                @Override
                                public void onKeyEntered(String key, GeoLocation location) {
                                    if (firstStop){
                                        MakeRoute(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()),new LatLng(location.latitude,location.longitude));
                                        final Map<String,Marker> markers = new HashMap<>();
                                        mRef = database.getReference("usersLocation/"+comuna+"/"+routeName[0]+"/drivers");
                                        GeoFire geoFireDrivers = new GeoFire(mRef);
                                        geoQueryDrivers = geoFireDrivers.queryAtLocation(location, 0.5);
                                        geoQueryDrivers.addGeoQueryDataEventListener(new GeoQueryDataEventListener() {
                                            @Override
                                            public void onDataEntered(DataSnapshot dataSnapshot, GeoLocation location) {
                                                Marker newDriver = mMap.addMarker(new MarkerOptions().position(new LatLng(location.latitude,location.longitude)).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_driver)));
                                                markers.put(dataSnapshot.getKey(), newDriver);
                                            }

                                            @Override
                                            public void onDataExited(DataSnapshot dataSnapshot) {
                                                markers.remove(dataSnapshot.getKey());
                                            }

                                            @Override
                                            public void onDataMoved(DataSnapshot dataSnapshot, GeoLocation location) {
                                                markers.get(dataSnapshot.getKey()).setPosition(new LatLng(location.latitude,location.longitude));
                                            }

                                            @Override
                                            public void onDataChanged(DataSnapshot dataSnapshot, GeoLocation location) {

                                            }

                                            @Override
                                            public void onGeoQueryReady() {

                                            }

                                            @Override
                                            public void onGeoQueryError(DatabaseError error) {

                                            }
                                        });
                                        firstStop = false;
                                    }
                                }

                                @Override
                                public void onKeyExited(String key) {

                                }

                                @Override
                                public void onKeyMoved(String key, GeoLocation location) {

                                }

                                @Override
                                public void onGeoQueryReady() {

                                }

                                @Override
                                public void onGeoQueryError(DatabaseError error) {

                                }
                            });
                            if (user == null){
                                signInAnonymously();
                            }
                            mRef = database.getReference("usersLocation/"+comuna+"/"+routeName[0]+"/users");
                            GeoFire geoFireUserLocation = new GeoFire(mRef);
                            geoFireUserLocation.setLocation(user.getUid(), new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), new GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {
                                }
                            });
                            mRefLastRoute = database.getReference("usersLocation/"+comuna+"/"+routeName[0]+"/users");

                            //marker en el destino
                            mMap.addMarker(new MarkerOptions().position(destination));



                            primera = false;
                        }
                    }

                    @Override
                    public void onKeyExited(String key) {

                    }

                    @Override
                    public void onKeyMoved(String key, GeoLocation location) {

                    }

                    @Override
                    public void onGeoQueryReady() {

                    }

                    @Override
                    public void onGeoQueryError(DatabaseError error) {

                    }
                });
            }
        });

        /*
        obtiene el SupportMapFragment (definido en el activity_maps.xml) y es notificado
        cuando el mapa esta listo para usar
        */
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST_CODE);
        }
        else{
            //para registrar el callback del mapa
            mapFragment.getMapAsync(this);
        }

        //autocomplete
        final PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        //restingir el autocomplete a Concepcion
        autocompleteFragment.setBoundsBias(new LatLngBounds(
                new LatLng(-37.02583151932976, -73.212164658635),
                new LatLng(-36.583807508197495, -72.8372562113693)));


        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                destination = place.getLatLng();
                String[] separated = place.getAddress().toString().split(",");
                address.setText(separated[1].trim());
                comuna = separated[1].trim();
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });
    }

    public void MakeRoute(LatLng start, LatLng end){
        Routing routing = new Routing.Builder()
                .travelMode(Routing.TravelMode.WALKING)
                .withListener(this)
                .waypoints(start,end)
                .build();
        routing.execute();
    }


    private void signInAnonymously() {
        // [START signin_anonymously]
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG2, "signInAnonymously:success");
                            user = mAuth.getCurrentUser();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG2, "signInAnonymously:failure", task.getException());
                            Toast.makeText(MapsActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST_CODE);
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }

    protected synchronized void buildGoogleApiClient(){
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST_CODE);
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        if (!primera){
            GeoFire geoFireUserLastLocation = new GeoFire(mRefLastRoute);
            geoFireUserLastLocation.setLocation(user.getUid(), new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                @Override
                public void onComplete(String key, DatabaseError error) {
                }
            });
        }
    }

    final int LOCATION_REQUEST_CODE = 1;
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case LOCATION_REQUEST_CODE:{
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    mapFragment.getMapAsync(this);
                }
                break;
            }
        }
    }

    @Override
    public void onRoutingFailure(RouteException e) {
        if(e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<com.directions.route.Route> route, int shortestRouteIndex) {
        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i <route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(),"distancia a paradero: "+ route.get(i).getDistanceValue()+": duracion: "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingCancelled() {

    }
}
