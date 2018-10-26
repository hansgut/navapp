package com.mapbox.services.android.navigation.testapp.activity.navigationui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.firebase.ui.database.FirebaseListAdapter;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.exceptions.InvalidLatLngBoundsException;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;
import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.testapp.RoutesActivity;
import com.mapbox.services.android.navigation.testapp.mRoute;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.ui.v5.route.OnRouteSelectionChangeListener;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;
import com.mapbox.services.android.navigation.v5.utils.LocaleUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.Route;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static com.mapbox.android.core.location.LocationEnginePriority.HIGH_ACCURACY;

public class NavigationViewActivity extends AppCompatActivity implements OnMapReadyCallback,
  MapboxMap.OnMapLongClickListener, LocationEngineListener, Callback<DirectionsResponse>,
  OnRouteSelectionChangeListener {

  private static final int CAMERA_ANIMATION_DURATION = 1000;
  private static final int DEFAULT_CAMERA_ZOOM = 16;

  private LocationLayerPlugin locationLayer;
  private LocationEngine locationEngine;
  private NavigationMapRoute mapRoute;
  private MapboxMap mapboxMap;

  private PlaceAutocompleteFragment autocompleteFragment;

  private FirebaseAuth mAuth;
  private DatabaseReference mRef;

  FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

  @BindView(R.id.mapView)
  MapView mapView;
  @BindView(R.id.launch_route_btn)
  Button launchRouteBtn;
  @BindView(R.id.loading)
  ProgressBar loading;
  @BindView(R.id.launch_btn_frame)
  FrameLayout launchBtnFrame;

  private Marker currentMarker;
  private Point currentLocation;
  private Point destination;
  private DirectionsRoute route;

  private boolean locationFound;

  private String distance;
  private String duration;
  private int distance_value;
  private int duration_value;

  private Geocoder geocoder;
  private List<Address> addresses_start;
  private List<Address> addresses_finish;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_navigation_view);

    autocompleteFragment = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
    mRef = FirebaseDatabase.getInstance().getReference();
    ButterKnife.bind(this);
    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);

    autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
      @Override
      public void onPlaceSelected(Place place) {
        destination = Point.fromLngLat(place.getLatLng().longitude, place.getLatLng().latitude);
        LatLng point = new LatLng(destination.latitude(), destination.longitude());
        setCurrentMarkerPosition(point);
        if (currentLocation != null) {
          fetchRoute();
        }
      }

      @Override
      public void onError(Status status) {
        // TODO: Handle the error.
        Snackbar.make(mapView, "Произошла ошибка.", Snackbar.LENGTH_SHORT).show();
      }
    });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.navigation_view_activity_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.settings:
        showSettings();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void showSettings() {
    startActivity(new Intent(this, NavigationViewSettingsActivity.class));
  }

  @SuppressWarnings( {"MissingPermission"})
  @Override
  protected void onStart() {
    super.onStart();
    mapView.onStart();
    if (locationLayer != null) {
      locationLayer.onStart();
    }
  }

  @SuppressWarnings( {"MissingPermission"})
  @Override
  public void onResume() {
    super.onResume();
    mapView.onResume();
    if (locationEngine != null) {
      locationEngine.addLocationEngineListener(this);
      if (!locationEngine.isConnected()) {
        locationEngine.activate();
      }
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    mapView.onPause();
    if (locationEngine != null) {
      locationEngine.removeLocationEngineListener(this);
    }
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  @Override
  protected void onStop() {
    super.onStop();
    mapView.onStop();
    if (locationLayer != null) {
      locationLayer.onStop();
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mapView.onDestroy();
    if (locationEngine != null) {
      locationEngine.removeLocationUpdates();
      locationEngine.deactivate();
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }

  @OnClick(R.id.launch_route_btn)
  public void onRouteLaunchClick() {
    launchNavigationWithRoute();
  }

  @Override
  public void onMapReady(MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
    this.mapboxMap.setOnMapLongClickListener(this);
    this.mapboxMap.getUiSettings().setAttributionEnabled(false);
    this.mapboxMap.getUiSettings().setLogoEnabled(false);
    initLocationEngine();
    initLocationLayer();
    initMapRoute();

    double Default = 0.0;

    if (getIntent().getDoubleExtra("start_latitude", Default) != 0.0)
    {
      Point start = Point.fromLngLat(getIntent().getDoubleExtra("start_longitude", Default), getIntent().getDoubleExtra("start_latitude", Default));
      Point finish = Point.fromLngLat(getIntent().getDoubleExtra("finish_longitude", Default), getIntent().getDoubleExtra("finish_latitude", Default));
      NavigationRoute.Builder builder = NavigationRoute.builder()
              .accessToken(Mapbox.getAccessToken())
              .origin(start)
              .profile(getRouteProfile())
              .destination(finish)
              .alternatives(true);
      setFieldsFromSharedPreferences(builder);
      builder.build()
              .getRoute(this);
      loading.setVisibility(View.VISIBLE);
      boundCameraToRoute();
    }

  }

  @Override
  public void onMapLongClick(@NonNull LatLng point) {
    destination = Point.fromLngLat(point.getLongitude(), point.getLatitude());
    launchRouteBtn.setEnabled(false);
    loading.setVisibility(View.VISIBLE);
    setCurrentMarkerPosition(point);
    if (currentLocation != null) {
      fetchRoute();
    }
  }

  @SuppressWarnings( {"MissingPermission"})
  @Override
  public void onConnected() {
    locationEngine.requestLocationUpdates();
  }

  @Override
  public void onLocationChanged(Location location) {
    currentLocation = Point.fromLngLat(location.getLongitude(), location.getLatitude());
    onLocationFound(location);
  }

  @Override
  public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
    if (validRouteResponse(response)) {
      hideLoading();
      route = response.body().routes().get(0);
      if (route.distance() > 25d) {
        launchRouteBtn.setEnabled(true);
        mapRoute.addRoutes(response.body().routes());
        boundCameraToRoute();
      } else {
        Snackbar.make(mapView, R.string.error_select_longer_route, Snackbar.LENGTH_SHORT).show();
      }
    }
  }

  @Override
  public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
    Timber.e(throwable.getMessage());
  }

  @Override
  public void onNewPrimaryRouteSelected(DirectionsRoute directionsRoute) {
    route = directionsRoute;
  }

  @SuppressWarnings( {"MissingPermission"})
  private void initLocationEngine() {
    locationEngine = new LocationEngineProvider(this).obtainBestLocationEngineAvailable();
    locationEngine.setPriority(HIGH_ACCURACY);
    locationEngine.setInterval(0);
    locationEngine.setFastestInterval(1000);
    locationEngine.addLocationEngineListener(this);
    locationEngine.activate();

    if (locationEngine.getLastLocation() != null) {
      Location lastLocation = locationEngine.getLastLocation();
      onLocationChanged(lastLocation);
      currentLocation = Point.fromLngLat(lastLocation.getLongitude(), lastLocation.getLatitude());
    }
  }

  @SuppressWarnings( {"MissingPermission"})
  private void initLocationLayer() {
    locationLayer = new LocationLayerPlugin(mapView, mapboxMap, locationEngine);
    locationLayer.setRenderMode(RenderMode.COMPASS);
  }

  private void initMapRoute() {

    mapRoute = new NavigationMapRoute(mapView, mapboxMap);
    mapRoute.setOnRouteSelectionChangeListener(this);
  }

  private void fetchRoute() {
    NavigationRoute.Builder builder = NavigationRoute.builder()
      .accessToken(Mapbox.getAccessToken())
      .origin(currentLocation)
      .profile(getRouteProfile())
      .destination(destination)
      .alternatives(true);
    setFieldsFromSharedPreferences(builder);
    builder.build()
      .getRoute(this);
    loading.setVisibility(View.VISIBLE);



    String url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=" + currentLocation.latitude() +
            "," + currentLocation.longitude() + "&destinations=" + destination.latitude() + "," + destination.longitude() + "&mode=walking&language=ru-Ru";

    RequestQueue queue = Volley.newRequestQueue(NavigationViewActivity.this);
    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
            (Request.Method.GET, url, null, new com.android.volley.Response.Listener<JSONObject>() {

              @Override
              public void onResponse(JSONObject response) {
                try{
                  JSONArray array = response.getJSONArray("rows");
                  JSONObject object = array.getJSONObject(0).getJSONArray("elements").getJSONObject(0);
                  JSONObject distance_j = object.getJSONObject("distance");
                  JSONObject duration_j = object.getJSONObject("duration");
                  distance = distance_j.getString("text");
                  duration = duration_j.getString("text");
                  distance_value = distance_j.getInt("value");
                  duration_value = duration_j.getInt("value");
                }catch (JSONException e){
                  e.printStackTrace();
                }
              }
            }, new com.android.volley.Response.ErrorListener() {

              @Override
              public void onErrorResponse(VolleyError error) {
                // TODO: Handle error

              }
            });
    queue.add(jsonObjectRequest);
    queue.start();
    Handler handler = new Handler();
    handler.postDelayed(new Runnable() {
      @Override
      public void run() {
        geocoder = new Geocoder(NavigationViewActivity.this, Locale.getDefault());
        try {
          addresses_start = geocoder.getFromLocation(currentLocation.latitude(), currentLocation.longitude(), 1);
          addresses_finish = geocoder.getFromLocation(destination.latitude(), destination.longitude(), 1);
          mRef.child(user.getUid()).child("Routes").push().setValue(new mRoute(currentLocation, destination,
                  addresses_start.get(0).getAddressLine(0),
                  addresses_finish.get(0).getAddressLine(0),
                  distance, duration, distance_value, duration_value
          ));
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }, 2000);




  }

  private void setFieldsFromSharedPreferences(NavigationRoute.Builder builder) {
    Locale locale = getLocale();
    builder
      .language(locale)
      .voiceUnits(NavigationUnitType.getDirectionsCriteriaUnitType(getUnitType(), locale));
  }

  private Locale getLocale() {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    String defaultString = getString(R.string.language_default_value_device_locale);
    String localeString = sharedPreferences.getString(getString(R.string.language_key), defaultString);
    return localeString.equals(defaultString) ? LocaleUtils.getDeviceLocale(this) : new Locale(localeString);
  }

  @NavigationUnitType.UnitType
  private int getUnitType() {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    return Integer.parseInt(sharedPreferences.getString(getString(R.string.unit_type_key),
      Integer.toString(NavigationUnitType.NONE_SPECIFIED)));
  }

  private boolean getShouldSimulateRoute() {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    return sharedPreferences.getBoolean(getString(R.string.simulate_route_key), false);
  }

  private String getRouteProfile() {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    return sharedPreferences.getString(
      getString(R.string.route_profile_key), DirectionsCriteria.PROFILE_DRIVING
    );
  }

  private void launchNavigationWithRoute() {
    if (route == null) {
      Snackbar.make(mapView, R.string.error_route_not_available, Snackbar.LENGTH_SHORT).show();
      return;
    }

    NavigationView navigationView;

    Locale locale = getLocale();
    NavigationLauncherOptions.Builder optionsBuilder = NavigationLauncherOptions.builder()
      .shouldSimulateRoute(getShouldSimulateRoute())
      .locale(locale)
      .unitType(getUnitType())
      .directionsProfile(getRouteProfile());

    if (sameLocaleAndUnitType(route.routeOptions())) {
      optionsBuilder.directionsRoute(route);
    } else {
      optionsBuilder
        .origin(currentLocation)
        .destination(destination);
    }

    NavigationLauncher.startNavigation(this, optionsBuilder.build());
  }

  private boolean sameLocaleAndUnitType(RouteOptions routeOptions) {
    return routeOptions.language().equals(getLocale().getLanguage())
      && routeOptions.voiceUnits().equals(NavigationUnitType.getDirectionsCriteriaUnitType(getUnitType(), getLocale()));
  }

  private boolean validRouteResponse(Response<DirectionsResponse> response) {
    return response.body() != null && !response.body().routes().isEmpty();
  }

  private void hideLoading() {
    if (loading.getVisibility() == View.VISIBLE) {
      loading.setVisibility(View.INVISIBLE);
    }
  }

  private void onLocationFound(Location location) {
    if (!locationFound) {
      animateCamera(new LatLng(location.getLatitude(), location.getLongitude()));
      Snackbar.make(mapView, R.string.explanation_long_press_waypoint, Snackbar.LENGTH_LONG).show();
      locationFound = true;
      hideLoading();
    }
  }

  public void boundCameraToRoute() {
    if (route != null) {
      List<Point> routeCoords = LineString.fromPolyline(route.geometry(),
        Constants.PRECISION_6).coordinates();
      List<LatLng> bboxPoints = new ArrayList<>();
      for (Point point : routeCoords) {
        bboxPoints.add(new LatLng(point.latitude(), point.longitude()));
      }
      if (bboxPoints.size() > 1) {
        try {
          LatLngBounds bounds = new LatLngBounds.Builder().includes(bboxPoints).build();
          // left, top, right, bottom
          int topPadding = launchBtnFrame.getHeight() * 2;
          animateCameraBbox(bounds, CAMERA_ANIMATION_DURATION, new int[] {50, topPadding, 50, 100});
        } catch (InvalidLatLngBoundsException exception) {
          Toast.makeText(this, R.string.error_valid_route_not_found, Toast.LENGTH_SHORT).show();
        }
      }
    }
  }

  private void animateCameraBbox(LatLngBounds bounds, int animationTime, int[] padding) {
    CameraPosition position = mapboxMap.getCameraForLatLngBounds(bounds, padding);
    mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), animationTime);
  }

  private void animateCamera(LatLng point) {
    mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point, DEFAULT_CAMERA_ZOOM), CAMERA_ANIMATION_DURATION);
  }

  private void setCurrentMarkerPosition(LatLng position) {
    if (position != null) {
      if (currentMarker == null) {
        MarkerViewOptions markerViewOptions = new MarkerViewOptions()
          .position(position);
        currentMarker = mapboxMap.addMarker(markerViewOptions);
      } else {
        currentMarker.setPosition(position);
      }
    }
  }
}
