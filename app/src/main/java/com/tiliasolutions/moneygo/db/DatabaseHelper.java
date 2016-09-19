package com.tiliasolutions.moneygo.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;


import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;


public class DatabaseHelper extends SQLiteOpenHelper {

    private static DatabaseHelper mInstance = null;
    private static Context mCxt;


    // Database Version
    private static final int DATABASE_VERSION = 1;
    // Database Name
    private static final String DATABASE_NAME = "MoneyGoDB";

    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INT";
    private static final String COMMA_SEP = ",";


    private DatabaseHelper(Context context) {       //constructor is private, so that we can use it as singleton
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mCxt=context;
    }

    public static DatabaseHelper getInstance(Context ctx) {     //getting/creating singleton for database helper
        if (mInstance == null) {
            mInstance = new DatabaseHelper(ctx.getApplicationContext());
        }
        return mInstance;
    }


    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        //creating tables if they do not exists
        createTableForUsers(sqLiteDatabase);
        createTableForGPSData(sqLiteDatabase);

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        //this should beadded later on...
    }


    private void createTableForUsers(SQLiteDatabase db){
        String CREATE_USERS_TABLE = "CREATE TABLE " +
                UserEntries.USERS_TABLE_NAME + " (" +
                UserEntries._ID + " INTEGER PRIMARY KEY " + COMMA_SEP +
                UserEntries.USERS_TABLE_USERNAME + TEXT_TYPE +
                " )";
        db.execSQL(CREATE_USERS_TABLE);
    }

    private void createTableForGPSData(SQLiteDatabase db){
        String CREATE_DATA_TABLE ="CREATE TABLE " +
                DataEntries.DATA_TABLE_NAME + " (" +
                DataEntries._ID + " INTEGER PRIMARY KEY " + COMMA_SEP +
                DataEntries.DATA_TABLE_USERNAME + TEXT_TYPE + COMMA_SEP +
                DataEntries.DATA_TABLE_LATITUDE + TEXT_TYPE + COMMA_SEP +
                DataEntries.DATA_TABLE_LONGITUDE + TEXT_TYPE + COMMA_SEP +
                DataEntries.DATA_TABLE_COIN + INT_TYPE + //"1,2,5" - "10,20,50" collectd
                " )";

        db.execSQL(CREATE_DATA_TABLE);
    }


    public boolean insertUserInUsersTable(SQLiteDatabase db, String userName){
        boolean data = false;
        ContentValues cv = new ContentValues();
        cv.put(UserEntries.USERS_TABLE_USERNAME, userName);
        long rowInserted = db.insert(UserEntries.USERS_TABLE_NAME,
                null ,
                cv);

        if(rowInserted != -1){
            data = true;
        }
        return data;
    }

    public boolean deleteUserFromTable(SQLiteDatabase db, String userName){
        return db.delete(UserEntries.USERS_TABLE_NAME,
                UserEntries.USERS_TABLE_USERNAME + "=?",
                new String[] {userName})
                > 0;

    }

    public boolean deleteAllGPSCoordinatesFromTableForUser(SQLiteDatabase db, String userName){
        return db.delete(DataEntries.DATA_TABLE_NAME,
                DataEntries.DATA_TABLE_USERNAME + "=?",
                new String[] {userName})
                > 0;

    }



    public boolean insertGPSDataInGPSTable(SQLiteDatabase db, String userName, String lat,
                                         String lng, int coinValue){

        boolean data = false;
        ContentValues cv = new ContentValues();
        cv.put(DataEntries.DATA_TABLE_USERNAME, userName);
        cv.put(DataEntries.DATA_TABLE_LATITUDE, lat);
        cv.put(DataEntries.DATA_TABLE_LONGITUDE, lng);
        cv.put(DataEntries.DATA_TABLE_COIN, coinValue);

        long rowInserted = db.insert(DataEntries.DATA_TABLE_NAME,
                null,
                cv);

        if(rowInserted != -1){
            data = true;
        }
        return data;
    }





    public ArrayList<String> getUsersFromUsersTable(SQLiteDatabase db){
        ArrayList<String> data = new ArrayList<>();

        Cursor c = db.query(true,
                UserEntries.USERS_TABLE_NAME,   //table name
                new String[] {UserEntries.USERS_TABLE_USERNAME},    //columns
                null,           //selections
                null,           //selection args
                null,           //group by
                null,           //having
                null,           //order by
                null);          //limit

        //read data and copy it to ArrayList
        if (c!=null) {
            c.moveToFirst();
            while (!c.isAfterLast()) {
                data.add(c.getString(0));
                c.moveToNext();
            }
            c.close();
        }
            return data;
    }

    public HashMap<LatLng, Integer> getAllGPSData(SQLiteDatabase db, String userName){
        HashMap<LatLng, Integer> data = new HashMap<>();

        Cursor c = db.query(true,
                DataEntries.DATA_TABLE_NAME,   //table name
                new String[] {DataEntries.DATA_TABLE_USERNAME,
                DataEntries.DATA_TABLE_LATITUDE,
                DataEntries.DATA_TABLE_LONGITUDE,
                DataEntries.DATA_TABLE_COIN},    //columns
                DataEntries.DATA_TABLE_USERNAME + "=?",     //selections (WHERE statement)
                new String []{userName},                    //selection args
                null,           //group by
                null,           //having
                null,           //order by
                null);          //limit

        //read data and copy it to ArrayList
        if (c!=null) {

            c.moveToFirst();
            while (!c.isAfterLast()) {
                data.put(new LatLng(Double.parseDouble(c.getString(1)),
                            Double.parseDouble(c.getString(2))),
                        c.getInt(3));
                c.moveToNext();
            }
            c.close();
        }
        return data;
    }

    public int getCollectedAmount(SQLiteDatabase db, String userName){
        int data = 0;
        Cursor c = db.query(false,
                DataEntries.DATA_TABLE_NAME,   //table name
                new String[] {DataEntries.DATA_TABLE_COIN},    //columns
                DataEntries.DATA_TABLE_USERNAME + "=? AND " +
                        DataEntries.DATA_TABLE_COIN + ">?",   //selections (WHERE statement)
                new String []{userName, String.valueOf(9)},   //selection args
                null,           //group by
                null,           //having
                null,           //order by
                null);          //limit
        if (c!=null) {
            c.moveToFirst();
            while (!c.isAfterLast()) {
                if(c.getInt(0)>8){
                    data = data + c.getInt(0);
                }
                c.moveToNext();
            }
            c.close();
        }
        return data/10;
    }



    public void updateCollectedCoinByTen(SQLiteDatabase db, String userName, LatLng coord , int
            coinValue){
        String lat = String.valueOf(coord.latitude);
        String lon = String.valueOf(coord.longitude);

        ContentValues cv = new ContentValues();
        cv.put(DataEntries.DATA_TABLE_COIN, coinValue*10);

        //I have used LatLng as query to change coin value. If two users have same LatLng, they
        // will both be updated, but this is very lov risk.
        int result = db.update(DataEntries.DATA_TABLE_NAME,
                cv,
                DataEntries.DATA_TABLE_LATITUDE + "= ?" + " AND " + DataEntries
                        .DATA_TABLE_LONGITUDE + " = ?",
                new String[] {lat, lon});
    }

    public boolean checkDoesGPSCoordinateExists(SQLiteDatabase db, LatLng coord){
        boolean data = false;
        String lat = String.valueOf(coord.latitude);
        String lon = String.valueOf(coord.longitude);
        Cursor c = db.query(true,
                DataEntries.DATA_TABLE_NAME,   //table name
                new String[] {DataEntries.DATA_TABLE_NAME,
                        DataEntries.DATA_TABLE_LATITUDE,
                        DataEntries.DATA_TABLE_LONGITUDE,
                        DataEntries.DATA_TABLE_COIN},    //columns
                DataEntries.DATA_TABLE_LATITUDE + " = ?" +
                DataEntries.DATA_TABLE_LONGITUDE + "= ?",     //selections (WHERE statement)
                new String []{lat, lon},                    //selection args
                null,           //group by
                null,           //having
                null,           //order by
                null);          //limit

        if (c!=null) {
            data = true;
            c.close();
        }
        return data;
    }

    public boolean checkDoesUserExists(SQLiteDatabase db, String userName){
        boolean data = false;
        Cursor c = db.query(true,
                UserEntries.USERS_TABLE_NAME,   //table name
                new String[] {UserEntries.USERS_TABLE_USERNAME},    //columns
                UserEntries.USERS_TABLE_USERNAME + "= ? ",     //selections (WHERE statement)
                new String []{userName},                    //selection args
                null,           //group by
                null,           //having
                null,           //order by
                null);          //limit

        if (c.getCount()>0) {  //if user exists, cursor will be different than "null"
            data = true;
            c.close();
        }
        return data;
    }


    public boolean checkDoesUserExistsInGPSTable(SQLiteDatabase db, String userName){
        boolean data = false;
        Cursor c = db.query(true,
                DataEntries.DATA_TABLE_NAME,   //table name
                new String[] {DataEntries.DATA_TABLE_USERNAME},    //columns
                DataEntries.DATA_TABLE_USERNAME + "= ? ",     //selections (WHERE statement)
                new String []{userName},                    //selection args
                null,           //group by
                null,           //having
                null,           //order by
                null);          //limit

        if (c.getCount()>0) {  //if user exists, cursor will be different than "null"
            data = true;
            c.close();
        }
        return data;
    }




    private static abstract class DataEntries implements BaseColumns{
        public static final String DATA_TABLE_NAME = "DATA_TABLE";
        public static final String DATA_TABLE_USERNAME = "DATA_TABLE_USERNAME";
        public static final String DATA_TABLE_LATITUDE = "DATA_TABLE_LATITUDE";
        public static final String DATA_TABLE_LONGITUDE = "DATA_TABLE_LONGITUDE";
        public static final String DATA_TABLE_COIN = "DATA_TABLE_COIN";
    }

    private static abstract class UserEntries implements BaseColumns{
        public static final String USERS_TABLE_NAME = "USERS_TABLE";
        public static final String USERS_TABLE_USERNAME = "USERS_TABLE_USERNAME";
    }

}
