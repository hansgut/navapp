package com.mapbox.services.android.navigation.testapp;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.testapp.activity.navigationui.NavigationViewActivity;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class mRoute {

    public double start_latitude;
    public double start_longitude;
    public double finish_latitude;
    public double finish_longitude;
    public String start;
    public String finish;
    public String distance;
    public String duration;
    public int distance_value;
    public int duration_value;
    public String date;

    public mRoute()
    {

    }

    public mRoute(Point start, Point finish, String start_s, String finish_s, String distance, String duration, int distance_value, int duration_value){
        this.start_latitude = start.latitude();
        this.start_longitude = start.longitude();
        this.finish_latitude = finish.latitude();
        this.finish_longitude = finish.longitude();
        this.start = start_s;
        this.finish = finish_s;
        this.distance = distance;
        this.duration = duration;
        this.distance_value = distance_value;
        this.duration_value = duration_value;
        this.date = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date());
    }




    public double getStart_latitude() {
        return start_latitude;
    }

    public double getStart_longitude() {
        return start_longitude;
    }

    public double getFinish_latitude() {
        return finish_latitude;
    }

    public double getFinish_longitude() {
        return finish_longitude;
    }

    public String getStart(){ return start; }
    public String getFinish() { return finish; }

    public String getDistance() { return distance; }

    public String getDuration() { return duration; }

    public int getDistance_value() { return distance_value; }

    public int getDuration_value() { return duration_value; }

    public String getDate() { return  date; };
}
