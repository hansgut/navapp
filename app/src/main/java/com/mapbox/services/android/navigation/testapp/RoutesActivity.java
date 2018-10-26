package com.mapbox.services.android.navigation.testapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.database.FirebaseListOptions;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.services.android.navigation.testapp.activity.navigationui.NavigationViewActivity;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RoutesActivity extends AppCompatActivity {

    private FirebaseListAdapter<mRoute> mAdapter;
    private ListView ListUserRoutes;
    private DatabaseReference mRef;
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private String last_adr;

    private ProgressBar progressBar;

    private PlaceAutocompleteFragment autocompleteFragment;

    private RadioButton r_start;
    private RadioButton r_finish;

    private EditText duration;
    private EditText distance;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routes);

        r_start = (RadioButton) findViewById(R.id.r_start);
        r_finish = (RadioButton) findViewById(R.id.r_finish);

        distance = (EditText) findViewById(R.id.distance);
        duration = (EditText) findViewById(R.id.duration);

        autocompleteFragment = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        progressBar = (ProgressBar) findViewById(R.id.loading);
        ListUserRoutes = (ListView) findViewById(R.id.RoutesList);
        mRef = FirebaseDatabase.getInstance().getReference();
        last_adr = "";




        r_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!last_adr.equals("")){
                    Query query = FirebaseDatabase.getInstance().getReference().child(user.getUid()).child("Routes")
                            .orderByChild("start").equalTo(last_adr);
                    setAdapter(query);
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            updateInfo();
                        }
                    }, 500);
                }
            }
        });

        r_finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!last_adr.equals("")){
                    Query query = FirebaseDatabase.getInstance().getReference().child(user.getUid()).child("Routes")
                            .orderByChild("finish").equalTo(last_adr);
                    setAdapter(query);
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            updateInfo();
                        }
                    }, 500);
                }
            }
        });

        autocompleteFragment.getView().findViewById(R.id.place_autocomplete_clear_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Query query = FirebaseDatabase.getInstance().getReference().child(user.getUid()).child("Routes");
                        setAdapter(query);
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                updateInfo();
                            }
                        }, 500);
                        last_adr = "";
                        autocompleteFragment.setText("");
                        view.setVisibility(View.GONE);
                    }
                });

        Query query = FirebaseDatabase.getInstance().getReference().child(user.getUid()).child("Routes");
        setAdapter(query);


        ListUserRoutes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mRoute item = mAdapter.getItem(position);
                Intent intent = new Intent(getBaseContext(), ReportActivity.class);
                intent.putExtra("start_latitude", item.getStart_latitude());
                intent.putExtra("start_longitude", item.getStart_longitude());
                intent.putExtra("finish_latitude", item.getFinish_latitude());
                intent.putExtra("finish_longitude", item.getFinish_longitude());
                intent.putExtra("start", item.getStart());
                intent.putExtra("finish", item.getFinish());
                intent.putExtra("distance", item.getDistance());
                intent.putExtra("duration", item.getDuration());
                intent.putExtra("date", item.getDate());
                startActivity(intent);
            }
        });

        mAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                progressBar.setVisibility(View.INVISIBLE);
                updateInfo();
            }

            @Override
            public void onInvalidated() {
                super.onInvalidated();
            }
        });



        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                mRef = FirebaseDatabase.getInstance().getReference();
                Geocoder geocoder;
                List<Address> addresses;
                geocoder = new Geocoder(RoutesActivity.this, Locale.getDefault());
                String adr = "";
                try {
                    addresses = geocoder.getFromLocation(place.getLatLng().latitude, place.getLatLng().longitude, 1);
                    adr = addresses.get(0).getAddressLine(0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                last_adr = adr;

                Query query = FirebaseDatabase.getInstance().getReference().child(user.getUid()).child("Routes");

                if(r_start.isChecked()){
                    query = FirebaseDatabase.getInstance().getReference().child(user.getUid()).child("Routes")
                            .orderByChild("start").equalTo(adr);
                }
                if(r_finish.isChecked()){
                    query = FirebaseDatabase.getInstance().getReference().child(user.getUid()).child("Routes")
                            .orderByChild("finish").equalTo(adr);
                }

                setAdapter(query);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        updateInfo();
                    }
                }, 500);

            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.

            }
        });

    }

    private void setAdapter(Query query) {
        FirebaseListOptions<mRoute> options =
                new FirebaseListOptions.Builder<mRoute>()
                        .setQuery(query, mRoute.class)
                        .setLayout(android.R.layout.two_line_list_item)
                        .build();

        mAdapter = new FirebaseListAdapter<mRoute>(options) {
            @Override
            protected void populateView(View v, mRoute model, int position) {
                String s = model.getStart() + " - " + model.getFinish();
                ((TextView) v.findViewById(android.R.id.text1)).setText(s);
            }
        };
        ListUserRoutes.setAdapter(mAdapter);
        mAdapter.startListening();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAdapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAdapter.stopListening();
    }

    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    private void updateInfo()
    {
        double total_distance = 0;
        double total_duration = 0;
        for(int i = 0; i < mAdapter.getCount(); i++){
            mRoute item = mAdapter.getItem(i);
            total_distance += item.getDistance_value();
            total_duration += item.getDuration_value();
        }
        distance.setText(String.valueOf(round(total_distance/1000, 2)) + " км.");
        duration.setText(String.valueOf(round(total_duration/60, 2))+ " мин.");
        //Toast.makeText(RoutesActivity.this, String.valueOf(total), Toast.LENGTH_LONG).show();
    }
}
