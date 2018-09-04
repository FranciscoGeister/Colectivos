package com.geister.colectivos;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Francisco on 19-06-2018.
 */

public class GetGeocodingData extends AsyncTask<Object,String,String>{
    GoogleMap mMap;
    String url;
    //LatLng startLatLng,endLatLng;

    HttpURLConnection httpURLConnection=null;
    String data = "";
    InputStream inputStream = null;
    Context c;

    GetGeocodingData(Context c){
        this.c=c;
    }
    @Override
    protected String doInBackground(Object[] params) {

        mMap = (GoogleMap)params[0];
        url = (String)params[1];
        //startLatLng = (LatLng)params[2];
        //endLatLng = (LatLng)params[3];

        try {
            URL myurl = new URL(url);
            httpURLConnection = (HttpURLConnection)myurl.openConnection();
            httpURLConnection.connect();

            inputStream = httpURLConnection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuffer sb = new StringBuffer();
            String line="";
            while((line= bufferedReader.readLine())!=null){
                sb.append(line);
            }
            data = sb.toString();
            bufferedReader.close();

        }
        catch(MalformedURLException e){
            e.printStackTrace();
        }
        catch(IOException e){
            e.printStackTrace();
        }

        return data;
    }

    @Override
    protected void onPostExecute(String s) {

        try {
            JSONObject jsonObject = new JSONObject(s);

            String lat = ((JSONArray)jsonObject.get("results")).getJSONObject(0).getJSONObject("geometry")
                    .getJSONObject("location").get("lat").toString();
            String lng = ((JSONArray)jsonObject.get("results")).getJSONObject(0).getJSONObject("geometry")
                    .getJSONObject("location").get("lng").toString();

            LatLng address = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
            mMap.addMarker(new MarkerOptions().position(address));


        }
        catch(JSONException e){
            e.printStackTrace();
        }
    }
}