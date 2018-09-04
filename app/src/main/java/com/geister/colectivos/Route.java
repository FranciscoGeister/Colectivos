package com.geister.colectivos;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

/**
 * Created by Francisco on 03-07-2018.
 */

public class Route {
    private ArrayList<LatLng> latLngRoute;
    private ArrayList<LatLng> latLngStops;
    private String name;

    public Route(ArrayList<LatLng> latLngRoute, ArrayList<LatLng> latLngStops, String name){
        this.latLngRoute = latLngRoute;
        this.latLngStops = latLngStops;
        this.name = name;
    }

    public ArrayList<LatLng> getLatLngRoute(){
        return this.latLngRoute;
    }

    public ArrayList<LatLng> getLatLngStops(){
        return this.latLngStops;
    }
}
