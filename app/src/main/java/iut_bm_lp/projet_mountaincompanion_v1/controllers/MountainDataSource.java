package iut_bm_lp.projet_mountaincompanion_v1.controllers;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;


import java.util.ArrayList;

import iut_bm_lp.projet_mountaincompanion_v1.models.Mountain;

/**
 * Created by Zekri on 27/11/2017.
 */

public class MountainDataSource {

    private SQLiteDatabase database;
    private SQLiteOpenHelper openHelper;
    private static MountainDataSource instance;

    public ArrayList<Mountain> mountains = new ArrayList<>();

    public MountainDataSource(Context context) {

        this.openHelper = new MountainDatabaseHandler(context);
    }

    public static MountainDataSource getInstance(Context context) {

        if (instance == null) {

            instance = new MountainDataSource(context);
        }
        return instance;
    }

    public void open() {

        this.database = openHelper.getWritableDatabase();
    }

    public void close() {

        if (database != null) {
            this.database.close();
        }
    }
    public ArrayList<Mountain> getMountains() {
        return mountains;
    }

    public void setMountains(ArrayList<Mountain> mountains) {
        this.mountains = mountains;
    }

    public ArrayList<Mountain> getAllMountains() {

        ArrayList<Mountain> mountainList = new ArrayList<>();

        Cursor cursor = database.rawQuery("SELECT * FROM sommets", null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {

            Mountain mountain = cursorToMountain(cursor);
            mountainList.add(mountain);
            cursor.moveToNext();
        }

        return mountainList;
    }

    private Mountain cursorToMountain(Cursor cursor) {

        Mountain mountain = new Mountain();
        mountain.setId(cursor.getInt(0));
        mountain.setLongitude(cursor.getFloat(1));
        mountain.setLatitude(cursor.getFloat(2));
        mountain.setNom(cursor.getString(3));
        mountain.setAltitude(cursor.getInt(4));
        mountain.setWiki(cursor.getString(5));

        return mountain;
    }


    public void setDirections(Location currentLocation, int minHeight, int maxHeight, int minDistance, int maxDistance) {


        Log.e("MaxDistance: ", "" + maxDistance);
        if (currentLocation == null) {
            return;
        }

        mountains.clear();

        double currentLatitude = currentLocation.getLatitude();
        double currentLongitude = currentLocation.getLongitude();

        double minLat = Math.min(currentLatitude - (maxDistance/111.0f), currentLatitude + (maxDistance/111.0f));
        double maxLat = Math.max(currentLatitude - (maxDistance/111.0f), currentLatitude + (maxDistance/111.0f));
        double minLong = Math.min(currentLongitude - (maxDistance/(111.0 * Math.sin(currentLatitude*Math.PI / 180))),
                (currentLongitude + (maxDistance/(111.0 * Math.sin(currentLatitude * Math.PI / 180)))));
        double maxLong = Math.max(currentLongitude - (maxDistance/(111.0 * Math.sin(currentLatitude * Math.PI / 180))),
                (currentLongitude + (maxDistance/(111.0 * Math.sin(currentLatitude * Math.PI / 180)))));

        Cursor cursor;

        try {
            this.open();
            cursor = database.rawQuery("SELECT * FROM sommets where latitude between " + minLat + " and " + maxLat + " and longitude between " + minLong + " and " + maxLong , null);
        } catch (SQLiteException e) {
            return;
        }

        if (cursor == null) {
            return;
        }

        int tooFar = 0;

        if (cursor.moveToFirst()) {

            double dLat, dLon, lat1, lat2;
            double x, y, brng, a, c, dheight;
            Mountain m;

            do {
                try {
                    m = cursorToMountain(cursor);

                        //    Log.e("CurrentLat: ", ""+ currentLatitude);
                        //    Log.e("CurrentLong: ", ""+ currentLongitude);

                    dLat = Math.toRadians(m.getLatitude() - currentLatitude);
                    dLon = Math.toRadians(m.getLongitude() - currentLongitude);
                    lat1 = Math.toRadians(currentLatitude);
                    lat2 = Math.toRadians(m.getLatitude());


                    //direction calculation
                    y = Math.sin(dLon) * Math.cos(lat2);
                    x = Math.cos(lat1) * Math.sin(lat2) -
                            Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
                    brng = Math.atan2(y, x) * 180 / Math.PI;

                    m.setDirection((brng < 0) ? brng + 360 : brng);

                    //distance calculation
                    a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                            Math.cos(lat2) * Math.cos(lat1) *
                                    Math.sin(dLon / 2) * Math.sin(dLon / 2);
                    c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

                    m.setDistance(Math.floor(10 * 6371 * c) / 10.0); //Distance in kms

                    //vertical angle

                    dheight = m.getAltitude() - currentLocation.getAltitude();

                    m.setVisualElevation(Math.atan2(dheight, m.getDistance() * 1000));
//
                    //Log.e("Nom: ","" + m.getNom());
                    //Log.e("angle: ", "" + m.getVisualElevation());
                    //Log.e("Distance: ", "" + m.getDistance());

                      //Log.e("Direction:", "" + m.getDirection());
                    mountains.add(m);

                } catch (Exception e) {
                Log.e("MountainCompanion", "Bad database read: " + e.getMessage());
            }
            } while (cursor.moveToNext());
        }
        this.close();
        cursor.close();
        Log.e("MountainCompanion", "Added : " + mountains.size() + " markers, SKIPPED " + tooFar + " tooFar.");
    }

}