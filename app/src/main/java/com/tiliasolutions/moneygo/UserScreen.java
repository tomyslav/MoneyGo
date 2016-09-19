package com.tiliasolutions.moneygo;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.tiliasolutions.moneygo.db.DatabaseHelper;

import java.util.HashMap;
import java.util.Random;

public class UserScreen extends AppCompatActivity {


    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private String mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_screen);

        mDbHelper = DatabaseHelper.getInstance(this);
        mDb = mDbHelper.getWritableDatabase();

        TextView mUserName = (TextView) findViewById(R.id.tv_username);
        Button mUserPointsBtn = (Button) findViewById(R.id.btn_points);
        Button mStartGame = (Button) findViewById(R.id.btn_start_game);

        //get username from previous screen
        Intent getI = getIntent();
        mUser = getI.getExtras().getString("USERNAME");

        mUserName.setText(mUser);

        //print number of points that user has
        mUserPointsBtn.setText(String.valueOf(mDbHelper.getCollectedAmount(mDb, mUser)));

        //button to strat game
        mStartGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), MapsActivity.class);
                i.putExtra("USERNAME", mUser);
                startActivity(i);
                finish();
            }
        });
    }
}