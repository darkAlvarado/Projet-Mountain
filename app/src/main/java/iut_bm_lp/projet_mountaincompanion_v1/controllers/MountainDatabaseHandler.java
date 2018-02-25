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

    public static String DB_NAME = "montagnees.db";

    public static final int DATABASE_VERSION = 1;

    public MountainDatabaseHandler(Context context) {

        super(context, DB_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

}