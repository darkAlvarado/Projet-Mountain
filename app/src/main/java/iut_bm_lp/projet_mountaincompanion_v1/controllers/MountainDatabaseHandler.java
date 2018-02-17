package iut_bm_lp.projet_mountaincompanion_v1.controllers;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;


/**
 * Created by Zekri on 26/11/2017.
 */

public class MountainDatabaseHandler extends SQLiteAssetHelper {

    public static String DB_NAME = "montagnes.db";

//    public static final String MOUNTAIN_KEY = "id";
//    public static final String MOUNTAIN_LONGITUDE = "longitude";
//    public static final String MOUNTAIN_LATITUDE = "latitude";
//    public static final String MOUNTAIN_NOM = "nom";
//    public static final String MOUNTAIN_ALTITUDE = "altitude";

    public static final int DATABASE_VERSION = 1;

//    public static final String MOUNTAIN_TABLE_NAME = "sommets";


    public MountainDatabaseHandler(Context context) {

        super(context, DB_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

}