package iut_bm_lp.projet_mountaincompanion_v1.views.Camera;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import android.support.v4.app.ActivityCompat;

import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;

import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

import iut_bm_lp.projet_mountaincompanion_v1.R;
import iut_bm_lp.projet_mountaincompanion_v1.controllers.MountainDataSource;
import iut_bm_lp.projet_mountaincompanion_v1.models.Mountain;
import iut_bm_lp.projet_mountaincompanion_v1.views.MountainCompanionActivity;

public class CameraActivity extends AppCompatActivity implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, MountainCompanionActivity,
        SensorEventListener {

    public static final int ALPHA_LABEL_MAX = 255;
    //    public static final int ALPHA_LINE_MAX = 205;
    public static final int ALPHA_DECREMENT = 10;
    public static final int ALPHA_STROKE_MIN = 200;
    public static final int ALPHA_LABEL_MIN = 180;
    public static final int ALPHA_LINE_MIN = 50;
    private static final float TEXT_SIZE_DECREMENT = 1;
    private static final float TEXT_SIZE_MIN = 7;
    private static int REQUEST_LOCATION = 1;
    private static int REQUEST_CAMERA = 1;
    public float hfov = (float) 50.2;
    public float vfov = (float) 20.0;
    public int scrwidth = 650;
    public int scrheight = 1020;
    public int scrdpi = 10;
    public DrawOnTop mDraw;
    Sensor accelerometer;
    Sensor magnetometer;
    float[] mGravity;
    float[] mGeomagnetic;
    float mRotationMatrixA[] = new float[9];
    float mRottaionMatrixB[] = new float[9];
    float mDeclination = 0;
    boolean showdir = false;
    boolean showdist = false;
    boolean typeunits = false;
    boolean showheight = false;
    Float textsize = 30f;
    private Camera mCamera = null;
    //    boolean showhelp = true;
    private CameraView mCameraView = null;
    private SensorManager mSensorManager;
    private int compassSmoothingWindow = 50;
    private float compassAdjustment = 0;
    private ArrayList<HillMarker> mMarkers = new ArrayList<>();
    private boolean mHasAccurateGravity = false;
    private boolean mHasAccurateAccelerometer = false;
    //    public static CameraPreviewSurface cv;
    private MountainDataSource database;
    private filteredDirection fd = new filteredDirection();
    private filteredElevation fe = new filteredElevation();
    private int mRadius;
    private int mMaxDistance;
    private GoogleApiClient mGoogleApiClient = null;
    private boolean mLocationEnabled;
    private LocationRequest mLocationRequest;
    private Location mLastLocation;

    private static final String TAG = "CameraActivity";

    public int getRotation() {

        Display display = getWindowManager().getDefaultDisplay();
        return display.getRotation();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        database = MountainDataSource.getInstance(this);

        if (mGoogleApiClient == null) {

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API).build();
        }

        setLocationParameters();
        mGoogleApiClient.connect();


        /****   CAPTEURS    ****/
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
        /****/

        mDraw = new DrawOnTop(this);
        addContentView(mDraw, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));


        /****   CAMERA  ****/
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
        }

        try {
            mCamera = Camera.open(0);
        } catch (Exception e) {

            Log.d("ERROR", "Failed to get camera: " + e.getMessage());
        }

        if (mCamera != null) {

            mCameraView = new CameraView(this, mCamera);
            FrameLayout camera_view = (FrameLayout) findViewById(R.id.camera_view);
            camera_view.addView(mCameraView);

        }

        /****/

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mMaxDistance = Integer.parseInt(sharedPref.getString(getResources().getString(R.string.key_max_distance), "0"));

        UpdateMarkers(mLastLocation);
    }

    private void requestCameraPermission() {

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                finish();
                startActivity(getIntent());
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    protected void onPause() {
        super.onPause();

        mSensorManager.unregisterListener(this);
        database.close();
    }

    protected void onResume() {
        super.onResume();

        fd = new filteredDirection();
        fe = new filteredElevation();

        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);

        if (mGoogleApiClient.isConnected()) {

            if (mLocationEnabled) {

                startLocationUpdates();
            }
        }

        UpdateMarkers(mLastLocation);
    }

    protected void onStop() {

        super.onStop();

        mSensorManager.unregisterListener(this);
        database.close();
        mGoogleApiClient.disconnect();
    }

    /*****                  LOCATION                *******/

    private void setLocationParameters() {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mLocationEnabled = sharedPref.getBoolean(getResources().getString(R.string.key_location_switch), false);

        if (mLocationEnabled) {

//            Toast.makeText(this, "Localisation ON", Toast.LENGTH_SHORT).show();

            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(1000);
        }
    }

    protected void startLocationUpdates() {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
        } else {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    private void requestLocationPermission() {

        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_LOCATION);
    }

    protected void stopLocationUpdates() {

        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
        } else {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLastLocation != null) {
                UpdateMarkers(mLastLocation);
            }
            if (mLocationEnabled) {
                startLocationUpdates();

            } else {
                stopLocationUpdates();
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {

        UpdateMarkers(location);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    /***************    Méthode de l'interface GoogleApiClient.OnConnectionFailedListener       *************************/
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    /******             FIN LOCATION             ******/

    @Override
    public void UpdateMarkers(Location location) {

        if (location != null) {

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            mMaxDistance = Integer.parseInt(sharedPref.getString(getResources().getString(R.string.key_search_radius), "0"));

            Log.d(TAG, "search radius: " + mMaxDistance);

            database = MountainDataSource.getInstance(this);
            database.open();
            database.setDirections(location, 10, 30, 5, mMaxDistance);
            database.close();
        }
    }

    /******/

    private String distanceAsImperialOrMetric(double distance) {

        if (typeunits) return (int) distance + "m";
        else return (int) (distance * 3.2808399) + "ft";
    }

    /****       FIN DESSIN      ****/

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if (sensorEvent.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {

            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER && mHasAccurateAccelerometer)
                return;
            if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD && mHasAccurateGravity)
                return;
        } else {

            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                mHasAccurateAccelerometer = true;
            if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                mHasAccurateGravity = true;
        }

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = sensorEvent.values;
        if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = sensorEvent.values;

        if (mGravity != null && mGeomagnetic != null) {

            float[] rotationMatrixA = mRotationMatrixA;
            if (SensorManager.getRotationMatrix(rotationMatrixA, null, mGravity, mGeomagnetic)) {

                Matrix tmpA = new Matrix();
                tmpA.setValues(rotationMatrixA);
                tmpA.postRotate(-mDeclination);
                tmpA.getValues(rotationMatrixA);

                float[] rotationMatrixB = mRottaionMatrixB;

                switch (getRotation()) {

                    //portrait - normal
                    case Surface.ROTATION_0:
                        SensorManager.remapCoordinateSystem(rotationMatrixA, SensorManager.AXIS_X, SensorManager.AXIS_Z, rotationMatrixB);
                        break;

                    //rotation left (landscape)
                    case Surface.ROTATION_90:
                        SensorManager.remapCoordinateSystem(rotationMatrixA, SensorManager.AXIS_X, SensorManager.AXIS_Z, rotationMatrixB);
                        break;

                    //upside down
                    case Surface.ROTATION_180:
                        SensorManager.remapCoordinateSystem(rotationMatrixA, SensorManager.AXIS_X, SensorManager.AXIS_Z, rotationMatrixB);
                        break;

                    //rotated right (landscape)
                    case Surface.ROTATION_270:
                        SensorManager.remapCoordinateSystem(rotationMatrixA, SensorManager.AXIS_MINUS_Z, SensorManager.AXIS_X, rotationMatrixB);
                        break;

                    default:
                        break;
                }

                float[] dv = new float[3];
                SensorManager.getOrientation(rotationMatrixB, dv);

                fd.addLatest(dv[0]);
                fe.addLatest((double) dv[1]);

            }
            mDraw.invalidate();
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public class HillMarker {
        public Rect location;
        public int hillid;

        public HillMarker(int id, Rect loc) {
            location = loc;
            hillid = id;
        }
    }

    class filteredDirection {

        double dir;
        double sinValues[] = new double[compassSmoothingWindow];
        double cosValues[] = new double[compassSmoothingWindow];
        int index = 0;

        void addLatest(double d) {

            sinValues[index] = Math.sin(d);
            cosValues[index] = Math.cos(d);
            index++;
            if (index > compassSmoothingWindow - 1) index = 0;
            double sumc = 0;
            double sums = 0;
            for (int a = 0; a < compassSmoothingWindow; a++) {

                sumc += cosValues[a];
                sums += sinValues[a];
            }
            dir = Math.atan2(sums / compassSmoothingWindow, sumc / compassSmoothingWindow);

        }

        double getDirection() {

            return (Math.toDegrees(dir) + compassAdjustment + 720) % 360;
        }
    }

    class filteredElevation {

        int AVERAGINGWINDOW = 10;
        double dir;
        double sinValues[] = new double[AVERAGINGWINDOW];
        double cosValues[] = new double[AVERAGINGWINDOW];
        int index = 0;

        void addLatest(double d) {

            sinValues[index] = Math.sin(d);
            cosValues[index] = Math.cos(d);
            index++;
            if (index > AVERAGINGWINDOW - 1) index = 0;
            double sumc = 0;
            double sums = 0;
            for (int a = 0; a < AVERAGINGWINDOW; a++) {

                sumc += cosValues[a];
                sums += sinValues[a];
            }

            dir = Math.atan2(sums / AVERAGINGWINDOW, sumc / AVERAGINGWINDOW);
        }

        double getDirection() {
            return dir;
        }
    }

    class tmpMountain {

        Mountain m;
        double ratio;
        int toppt;
    }

    /****       DESSIN        ****/
    class DrawOnTop extends View {

        int subwidth;
        int subheight;
        int gap;
        int txtgap;
        int vtxtgap;
        RectF fovrect;
        ArrayList<tmpMountain> mountainsToPlot;
        private Paint strokePaint = new Paint();
        private Paint textPain = new Paint();
        private Paint paint = new Paint();
        private Paint transpRedPaint = new Paint();

        public DrawOnTop(Context context) {

            super(context);
            textPain.setTextAlign(Paint.Align.CENTER);
            textPain.setTypeface(Typeface.DEFAULT_BOLD);

            strokePaint.setTextAlign(Paint.Align.CENTER);
            strokePaint.setTypeface(Typeface.DEFAULT_BOLD);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(2);

            paint.setARGB(255, 255, 255, 255);
            transpRedPaint.setARGB(100, 255, 0, 0);

            subwidth = (int) (scrwidth * 0.7);
            subheight = (int) (scrheight * 0.7);
            gap = (scrwidth - subwidth) / 2;
            txtgap = gap + (subwidth / 30);
            vtxtgap = (subheight / 10);

            mountainsToPlot = new ArrayList<>();
            fovrect = new RectF(gap, vtxtgap, scrwidth - gap, vtxtgap * 11);
        }

        @Override
        protected void onDraw(Canvas canvas) {

            database = MountainDataSource.getInstance(getContext());
            database.open();

            database.setDirections(mLastLocation, 10, 30, 5, mMaxDistance);
            ArrayList<Mountain> mountains = database.getMountains();

            database.close();

            int topPt = calculateHillsCanFitOnCanvas((int) (scrheight / 1.6), mountains);

            drawHillLabelLines(canvas, topPt);
            drawHillLabelText(canvas, topPt);

            super.onDraw(canvas);
        }

        private int calculateHillsCanFitOnCanvas(int topPt, ArrayList<Mountain> mountains) {

            Float drawtextsize = textsize;
            mountainsToPlot.clear();
            mMarkers.clear();
            for (int h = 0; h < mountains.size() && topPt > 0; h++) {

                Mountain m1 = mountains.get(h);

                // this is the angle of the peak from our ligne of sight
                double offset = fd.getDirection() - m1.getDirection();
                double offset2 = fd.getDirection() - (360 + m1.getDirection());
                double offset3 = 360 + fd.getDirection() - (m1.getDirection());
                double ratio = 0;

                // is it in our line of sight
                boolean inLineOfSight = false;
                if (Math.abs(offset) * 2 < hfov) {
                    ratio = offset / hfov * -1;
                    inLineOfSight = true;
                }
                if (Math.abs(offset2) * 2 < hfov) {
                    ratio = offset2 / hfov * -1;
                    inLineOfSight = true;
                }
                if (Math.abs(offset3) * 2 < hfov) {
                    ratio = offset3 / hfov * -1;
                    inLineOfSight = true;
                }
                if (inLineOfSight) {

                    tmpMountain tm = new tmpMountain();

                    tm.m = m1;
                    tm.ratio = ratio;
                    tm.toppt = topPt;
                    mountainsToPlot.add(tm);

                    topPt -= (showdir || showdist || showheight && tm.m.getAltitude() > 0) ? (1 + drawtextsize * 2) : drawtextsize;
                }
            }

            topPt -= Math.max(0, 13 - drawtextsize);
            return topPt;
        }


        private void drawHillLabelLines(Canvas canvas, int toppt) {

            int alpha = ALPHA_LABEL_MAX;

            //draw lines first
            for (int i = 0; i < mountainsToPlot.size(); i++) {

                textPain.setARGB(alpha, 255, 255, 255);
                strokePaint.setARGB(alpha, 0, 0, 0);
                tmpMountain tm = mountainsToPlot.get(i);
                double vratio = Math.toDegrees(tm.m.getVisualElevation() - fe.getDirection());
                int yloc = (int) ((scrheight * vratio / vfov) + (scrheight / 2));
                int xloc = (int) ((scrwidth * tm.ratio) + (scrwidth / 2));
                canvas.drawLine(xloc, yloc, xloc, tm.toppt - toppt, strokePaint);
                canvas.drawLine(xloc, yloc, xloc, tm.toppt - toppt, textPain);

                canvas.drawLine(xloc - 20, tm.toppt - toppt, xloc + 20, tm.toppt - toppt, strokePaint);
                canvas.drawLine(xloc - 20, tm.toppt - toppt, xloc + 20, tm.toppt - toppt, textPain);

                if (alpha - ALPHA_DECREMENT >= ALPHA_LINE_MIN) {

                    alpha -= ALPHA_DECREMENT;
                }
            }
        }

        private void drawHillLabelText(Canvas canvas, int toppt) {
            boolean moreinfo;
            Float drawtextsize = textsize;
            int alpha = ALPHA_LABEL_MAX;
            // draw text over top
            for (int i = 0; i < mountainsToPlot.size(); i++) {
                textPain.setARGB(alpha, 255, 255, 255);
                strokePaint.setARGB(Math.min(alpha, ALPHA_STROKE_MIN), 0, 0, 0);

                textPain.setTextSize(drawtextsize);
                strokePaint.setTextSize(drawtextsize);

                tmpMountain tm = mountainsToPlot.get(i);
                moreinfo = (showdir || showdist || showheight && tm.m.getAltitude() > 0);
                int xloc = ((int) (scrwidth * tm.ratio) + (scrwidth / 2));

                Rect bnds = new Rect();
                strokePaint.getTextBounds(tm.m.getNom(), 0, tm.m.getNom().length(), bnds);
                bnds.left += xloc - (textPain.measureText(tm.m.getNom()) / 2.0);
                bnds.right += xloc - (textPain.measureText(tm.m.getNom()) / 2.0);
                bnds.top += tm.toppt - 5 - toppt;
                if (moreinfo) bnds.top -= drawtextsize;
                bnds.bottom += tm.toppt - 5 - toppt;

                // canvas.drawRect(bnds, strokePaint)

                mMarkers.add(new HillMarker(tm.m.getId(), bnds));
                canvas.drawText(tm.m.getNom(), xloc, tm.toppt - ((moreinfo) ? drawtextsize : 0) - 5 - toppt, strokePaint);
                canvas.drawText(tm.m.getNom(), xloc, tm.toppt - ((moreinfo) ? drawtextsize : 0) - 5 - toppt, textPain);

                if (showdir || showdist || showheight) {

                    boolean hascontents = false;
                    String marker = " (";
                    if (showdir) {
                        hascontents = true;
                        marker += Math.floor(10 * tm.m.getDirection()) / 10 + "\u00B0";
                    }
                    if (showdist) {
                        hascontents = true;
                        double multip = (typeunits) ? 1 : 0.621371;
                        marker += (showdir ? " " : "") + Math.floor(10 * tm.m.getDistance() * multip) / 10;
                        if (typeunits) marker += "km";
                        else marker += "miles";
                    }
                    if (showheight) {
                        if (tm.m.getAltitude() > 0) {
                            hascontents = true;
                            marker += ((showdir || showdist) ? " " : "") + distanceAsImperialOrMetric(tm.m.getAltitude());
                        }
                    }
                    marker += ")";
                    if (hascontents) {
                        canvas.drawText(marker, xloc, tm.toppt - 5 - toppt, strokePaint);
                        canvas.drawText(marker, xloc, tm.toppt - 5 - toppt, textPain);
                    }
                }

                if (alpha - ALPHA_DECREMENT >= ALPHA_LABEL_MIN) {
                    alpha -= ALPHA_DECREMENT;
                }

                if (drawtextsize - TEXT_SIZE_DECREMENT >= TEXT_SIZE_MIN) {
                    drawtextsize -= TEXT_SIZE_DECREMENT;
                }
            }
        }

    }

}