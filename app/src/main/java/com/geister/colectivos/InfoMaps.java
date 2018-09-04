package com.geister.colectivos;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

public class InfoMaps extends FragmentActivity implements OnMapReadyCallback,
                                                    GoogleMap.OnPolylineClickListener{

    private GoogleMap mMap;
    private Button comunaConcepcion;
    private Button comunaHualqui;
    private final LatLng mDefaultLocation = new LatLng(-36.8030027144374, -73.05426939044139);
    private static final int DEFAULT_ZOOM = 15;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.info_maps);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference myRef = database.getReference();
        final DatabaseReference myRef2 = database.getReference("Geofire");

        comunaConcepcion = (Button) findViewById(R.id.comuna_concepcion);
        comunaHualqui = (Button) findViewById(R.id.comuna_hualqui);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        //para registrar el callback del mapa
        mapFragment.getMapAsync(this);

        comunaConcepcion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                //GeoFire geoFire = new GeoFire(myRef2);
                //geoFire.setLocation("firebase-hq", new GeoLocation(37.7853889, -122.4056973));

                mMap.clear();
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-36.818392673689296, -73.04774365622558)
                        ,13));

                myRef.child("comunas").child("Concepción").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        //String value = dataSnapshot.getValue().toString();
                        Map<String, String> value = (Map<String, String>) dataSnapshot.getValue();
                        JSONObject jsonObject = null;
                        try {
                            jsonObject = new JSONObject(value);
                            JSONArray jsonArray = jsonObject.getJSONArray("features");

                            int count = jsonArray.length();
                            ArrayList<LatLng> polyline_array;

                            JSONObject jsonobject2;
                            JSONObject jsonobject3;
                            JSONArray jsonarray2;
                            JSONArray jsonarray3;

                            for (int i = 0; i < count; i++) {
                                jsonobject2 = jsonArray.getJSONObject(i);
                                jsonobject3 = jsonArray.getJSONObject(i);
                                jsonarray2 = jsonobject2.getJSONObject("geometry").getJSONArray("coordinates");
                                String ruta = jsonobject3.getJSONObject("properties").get("Ruta").toString();
                                int count2 = jsonarray2.length();
                                for (int k = 0; k < count2; k++){
                                    polyline_array = new ArrayList<LatLng>();
                                    jsonarray3 = jsonarray2.getJSONArray(k);
                                    int count3 = jsonarray3.length();
                                    for (int j = 0; j < count3; j++){
                                        polyline_array.add(new LatLng(jsonarray3.getJSONArray(j).getDouble(1),
                                                jsonarray3.getJSONArray(j).getDouble(0)));
                                    }
                                    PolylineOptions options2 = new PolylineOptions();
                                    options2.color(Color.BLUE);
                                    options2.width(10);
                                    options2.clickable(true);
                                    options2.addAll(polyline_array);

                                    mMap.addPolyline(options2).setTag(ruta);

                                    /*
                                    Polyline polyline1 =  mMap.addPolyline(new PolylineOptions()
                                            .clickable(true)
                                            .addAll(polyline_array));
                                    // Store a data object with the polyline, used here to indicate an arbitrary type.
                                    polyline1.setTag(ruta);
                                    */
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        });

        //es hualpen
        comunaHualqui.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mMap.clear();
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-36.86038851267162, -73.11883901321744)
                        ,13));

                myRef.child("rutas").child("San Pedro de la Paz").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        //String value = dataSnapshot.getValue().toString();
                        Map<String, String> value = (Map<String, String>) dataSnapshot.getValue();
                        JSONObject jsonObject = null;
                        try {
                            jsonObject = new JSONObject(value);
                            JSONArray jsonArray = jsonObject.getJSONArray("features");

                            int count = jsonArray.length();
                            ArrayList<LatLng> polyline_array;

                            JSONObject jsonobject2;
                            JSONObject jsonobject3;
                            JSONArray jsonarray2;
                            JSONArray jsonarray3;

                            for (int i = 0; i < count; i++) {
                                jsonobject2 = jsonArray.getJSONObject(i);
                                jsonobject3 = jsonArray.getJSONObject(i);
                                jsonarray2 = jsonobject2.getJSONObject("geometry").getJSONArray("coordinates");
                                String ruta = jsonobject3.getJSONObject("properties").get("Ruta").toString();
                                int count2 = jsonarray2.length();
                                for (int k = 0; k < count2; k++){
                                    polyline_array = new ArrayList<LatLng>();
                                    jsonarray3 = jsonarray2.getJSONArray(k);
                                    int count3 = jsonarray3.length();
                                    for (int j = 0; j < count3; j++){
                                        polyline_array.add(new LatLng(jsonarray3.getJSONArray(j).getDouble(1),
                                                jsonarray3.getJSONArray(j).getDouble(0)));
                                    }
                                    PolylineOptions options2 = new PolylineOptions();
                                    options2.color(Color.BLUE);
                                    options2.width(10);
                                    options2.clickable(true);
                                    options2.addAll(polyline_array);

                                    mMap.addPolyline(options2).setTag(ruta);

                                    /*
                                    Polyline polyline1 =  mMap.addPolyline(new PolylineOptions()
                                            .clickable(true)
                                            .addAll(polyline_array));
                                    // Store a data object with the polyline, used here to indicate an arbitrary type.
                                    polyline1.setTag(ruta);
                                    */
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        });


    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation,DEFAULT_ZOOM));

        mMap.setOnPolylineClickListener(this);

    }

    @Override
    public void onPolylineClick(Polyline polyline) {
        Toast.makeText(this, "Recorrido: " + polyline.getTag().toString(),
                Toast.LENGTH_SHORT).show();
    }
}
