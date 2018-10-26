package com.mapbox.services.android.navigation.testapp;

import android.content.Intent;
import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.JsonObject;
import com.mapbox.services.android.navigation.testapp.activity.navigationui.NavigationViewActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

public class ReportActivity extends AppCompatActivity {

    private TextView Start;
    private TextView Finish;
    private TextView km;
    private TextView time;
    private TextView date;
    private Button show;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        Start = (TextView) findViewById(R.id.Start);
        Finish = (TextView) findViewById(R.id.Finish);
        km = (TextView) findViewById(R.id.km_value);
        time = (TextView) findViewById(R.id.time_value);
        show = (Button) findViewById(R.id.show);
        date = (TextView) findViewById(R.id.date_value);
        date.setText(getIntent().getStringExtra("date"));
        Start.setText(getIntent().getStringExtra("start"));
        Finish.setText(getIntent().getStringExtra("finish"));
        km.setText(getIntent().getStringExtra("distance"));
        time.setText(getIntent().getStringExtra("duration"));

        show.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), NavigationViewActivity.class);
                intent.putExtra("start_latitude", getIntent().getDoubleExtra("start_latitude",0.0));
                intent.putExtra("start_longitude",getIntent().getDoubleExtra("start_longitude", 0.0));
                intent.putExtra("finish_latitude",  getIntent().getDoubleExtra("finish_latitude",0.0));
                intent.putExtra("finish_longitude", getIntent().getDoubleExtra("finish_longitude", 0.0));
                startActivity(intent);
            }
        });
    }





}
