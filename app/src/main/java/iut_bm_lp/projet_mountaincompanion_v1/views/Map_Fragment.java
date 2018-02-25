package iut_bm_lp.projet_mountaincompanion_v1.views;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.graphics.Matrix;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

import iut_bm_lp.projet_mountaincompanion_v1.R;
import iut_bm_lp.projet_mountaincompanion_v1.controllers.MountainDataSource;
import iut_bm_lp.projet_mountaincompanion_v1.models.Mountain;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link Map_Fragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link Map_Fragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Map_Fragment extends Fragment implements LocationListener, OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, SensorEventListener, GoogleMap.OnMarkerClickListener{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private static int REQUEST_LOCATION = 1;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private GoogleApiClient mGoogleApiClient = null;
    private LocationRequest mLocationRequest;

    private Location mLastLocation;

    private GoogleMap mGoogleMap;

    private LatLng mLatLngMaposition;

    private int mRadius;

    private OnFragmentInteractionListener mListener;

    private boolean mLocationEnabled;

    private MountainDataSource mDataSource;

    private ArrayList<Mountain> mMountains = new ArrayList<>();



    private SensorManager mSensorManager;
    Sensor accelerometer;
    Sensor magnetometer;

    Marker compassMarker = null;

    private boolean mHasAccurateGravity = false;
    private boolean mHasAccurateAccelerometer = false;

    float mDeclination = 0;

    float[] mGravity;
    float[] mGeomagnetic;




    public Map_Fragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment Map_fragment.
     */
    // TODO: Rename and change types and number of parameters
    public static Map_Fragment newInstance(String param1, String param2) {
        Map_Fragment fragment = new Map_Fragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        if(mGoogleApiClient == null) {

            mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API).build();
        }

                    /****   CAPTEURS    ****/
        mSensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);

        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
                            /****/
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_map_, container, false);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        return root;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onStart() {

        super.onStart();

        setLocationParameters();
        mGoogleApiClient.connect();
    }

    @Override
    public void onPause() {

        super.onPause();
        stopLocationUpdates();

        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onStop() {

        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onResume() {

        super.onResume();
        setLocationParameters();

                    /****   CAPTEURS    ****/
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
                            /*****/
        if(mGoogleApiClient.isConnected()) {

            if(mLocationEnabled) {

                startLocationUpdates();
            }
        }

    }

    /***************    Méthodes de l'interface LocationListener        *************************/

    @Override
    public void onLocationChanged(Location location) {
        mountainUpdateUi(location);
    }

    /**************/

    /***************    Méthode de l'interface OnMapReadyCallback        *************************/
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mGoogleMap = googleMap;

        mGoogleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {

                Uri uriUrl = Uri.parse((String) marker.getTag());
                Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
                startActivity(launchBrowser);
            }
        });
        mGoogleMap.setOnMarkerClickListener(this);
        mountainUpdateUi(null);

    }

    /**********/


    /***************    Méthodes de l'interface GoogleApiClient.ConnectionCallbacks        *************************/
    @Override
    public void onConnected(@Nullable Bundle bundle) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int styleCarte = Integer.parseInt(sharedPref.getString(getResources().getString(R.string.key_choix_style_carte), "0"));

                //Log.wtf("style", "" + styleCarte);
        if(styleCarte == 1){
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }else if (styleCarte == 2) {
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        }else if(styleCarte == 3) {
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        }else if(styleCarte == 4) {
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        }

        if(ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestLocationPermission();
        } else {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLastLocation != null) {
                mountainUpdateUi(mLastLocation);
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mLatLngMaposition, 10.0f));
            }
            if (mLocationEnabled) {
                startLocationUpdates();
                mGoogleMap.setMyLocationEnabled(true); //Active le calque Ma position sur la carte !
                LatLng myPos = new LatLng(47.648362500000005, 6.8465981);
            } else {
                stopLocationUpdates();
            }
        }


    }

    private void drawCircle(LatLng location) {

        CircleOptions options = new CircleOptions();
        options.center(location);
        //Radius
        options.radius(mRadius*1000);
       // options.fillColor(getResources().getColor(R.color.colorPrimary));
        options.strokeColor(getResources().getColor(R.color.colorPrimaryDark));
        options.strokeWidth(3);
        mGoogleMap.addCircle(options);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }
    /******/

    /***************    Méthode de l'interface GoogleApiClient.OnConnectionFailedListener       *************************/
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
    /******/



    private void requestLocationPermission() {

        ActivityCompat.requestPermissions(
                getActivity(),
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_LOCATION);
    }

    private void setLocationParameters() {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mLocationEnabled = sharedPref.getBoolean(getResources().getString(R.string.key_location_switch), false);

        if(mLocationEnabled) {
            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(1000);
        }
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
        } else {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    protected void stopLocationUpdates() {

        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }


    private void mountainUpdateUi(Location location) {

        if (location != null) {

            mGoogleMap.clear();

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            mRadius = Integer.parseInt(sharedPref.getString(getResources().getString(R.string.key_search_radius), "0"));

            //LatLngBounds.Builder b = new LatLngBounds.Builder();

            mLatLngMaposition = new LatLng(location.getLatitude(), location.getLongitude());
            mGoogleMap.addMarker(new MarkerOptions().position(mLatLngMaposition).title("Ma position"));

            drawCircle(mLatLngMaposition);

            mDataSource = MountainDataSource.getInstance(getContext());
            mDataSource.open();
                 //ArrayList<Mountain> mountains = mDataSource.getAllMountains();
            mMountains = mDataSource.getAllMountains();
            mDataSource.close();

            for (Mountain m: mMountains) {

                Location latLngMountain = new Location("");
                latLngMountain.setLatitude(m.getLatitude());
                latLngMountain.setLongitude(m.getLongitude());

                if (((location.distanceTo(latLngMountain)) / 1000) <= mRadius) { // On divise par 1000 pour avoir des kms

                    LatLng latLngTest2 = new LatLng(m.getLatitude(), m.getLongitude());
                    Marker marker = mGoogleMap.addMarker(new MarkerOptions().position(latLngTest2).title(m.getNom()).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_montagne)).snippet("Altitude: " + m.getAltitude() + "\nDistance: "+ Math.round((location.distanceTo(latLngMountain))/1000)+" kms" ));
                    marker.setTag(m.getWiki());

                    mGoogleMap.setInfoWindowAdapter(new MyInfoWindow(getContext()));
                    //  b.include(latLngTest2);
                }
            }

                            /**** FLECHE ORIENTATION    ****/
            LatLng pt = new LatLng(location.getLatitude(), location.getLongitude());

            compassMarker = mGoogleMap.addMarker(new MarkerOptions().position(pt)
                            .anchor(0.5f,0.5f)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.bluearrow)));
                                        /****/
        }

    }

    /**********************         Méthode de l'interface SensorEventListener      ******************/
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if(sensorEvent.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE){
            if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER && mHasAccurateAccelerometer) return;
            if(sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD && mHasAccurateGravity) return;
        }else{

            if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) mHasAccurateAccelerometer = true;
            if(sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) mHasAccurateGravity = true;
        }

        if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) mGravity = sensorEvent.values;
        if(sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) mGeomagnetic = sensorEvent.values;

        if(mGravity != null && mGeomagnetic != null){

            float[] rotationMatrixA = new float[9];
            if(SensorManager.getRotationMatrix(rotationMatrixA, null, mGravity, mGeomagnetic)) {

                android.graphics.Matrix tmpA = new Matrix();
                tmpA.setValues(rotationMatrixA);
                tmpA.postRotate( -mDeclination);
                tmpA.getValues(rotationMatrixA);

                float[] dv = new float[3];
                SensorManager.getOrientation(rotationMatrixA, dv);
                if(compassMarker != null){
                        //Log.e("orientation:", "" + (float) Math.toDegrees(dv[0]));
                    compassMarker.setRotation( (float) Math.toDegrees(dv[0]));
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    /**********/

    /***
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
