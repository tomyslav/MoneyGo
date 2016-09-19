package com.tiliasolutions.moneygo;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.tiliasolutions.moneygo.db.DatabaseHelper;


public class StartScreenActivity extends AppCompatActivity implements View.OnClickListener{

    DatabaseHelper mDbHelper;
    SQLiteDatabase mDb;

    LinearLayout mLayoutForUserButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_screen);

        mDbHelper = DatabaseHelper.getInstance(this);
        mDb = mDbHelper.getWritableDatabase();

        //get layotu where buttons will be generated
        mLayoutForUserButtons = (LinearLayout) findViewById(R.id.layout_for_buttons);

        //get button id
        Button mAddNewUserButton = (Button) findViewById(R.id.btn_add_new_user);
        mAddNewUserButton.setOnClickListener(this);

        populateUserButtons();
    }

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        return super.onCreateView(parent, name, context, attrs);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_add_new_user:
                //start alert dialog to create new user
                askForUsername();
                break;
        }
    }


    //method to create new username in username table via AlertDialog
    private void askForUsername(){
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View usernameInputView = layoutInflater.inflate(R.layout.username_dialog, null);
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(usernameInputView);

        final EditText usernameInput = (EditText) usernameInputView.findViewById(R.id
                .username_input);

        Button btnOK = (Button) usernameInputView.findViewById(R.id.buttonOK);
        Button btnNOK = (Button) usernameInputView.findViewById(R.id.buttonNOK);

        alertDialogBuilder.setCancelable(true);
        final AlertDialog usernameDialog = alertDialogBuilder.create();
        usernameDialog.show();

        btnOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(usernameInput.getText().toString().toUpperCase().trim().length()>0){
                    //check does user exists in database
                    if(mDbHelper.checkDoesUserExists(mDb, usernameInput.getText().toString()
                            .toUpperCase().trim())){    //true - user exists
                        //toast thats say that user alredy exists
                        Toast.makeText(getApplicationContext(), "USERNAME " +
                                usernameInput.getText().toString().toUpperCase().trim() +
                                "ALREADY EXISTS. PLEASE SELECT ANOTHER ONE",
                                Toast.LENGTH_SHORT).show();
                    }else{   //false - user does not exists
                        //create new user
                        if (mDbHelper.insertUserInUsersTable(mDb, usernameInput.getText().toString()
                                .toUpperCase().trim())){
                            //if creation in table is successful, then make toast
                            Toast.makeText(getApplicationContext(), "USERNAME " +
                                    usernameInput.getText().toString().toUpperCase().trim() +
                                    " IS CREATED",
                                    Toast.LENGTH_SHORT).show();

                            //remove dialog and populate buttons
                            usernameDialog.dismiss();
                            populateUserButtons();
                        }
                    }
                }else {
                    //show this if username is not entered.
                    Toast.makeText(getApplicationContext(), "PLEASE ENETER AN USERNAME",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        btnNOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                usernameDialog.dismiss();
            }
        });
    }


    //populate all buttons in layout with usernames
    private void populateUserButtons(){
        mLayoutForUserButtons.removeAllViews();
        for (final String user : mDbHelper.getUsersFromUsersTable(mDb)){
            Button bt = new Button(this);
            bt.setText(user);
            bt.setBackgroundResource(R.drawable.rect_button);
            bt.setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0,0,0,10);
            bt.setLayoutParams(params);
            bt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(getApplicationContext(), UserScreen.class);
                    i.putExtra("USERNAME", user);
                    startActivity(i);
                }
            });

            //long clcik is to delete user from users table and all GPS points for that user from
            // gps points table
            bt.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (mDbHelper.deleteUserFromTable(mDb, user)){
                        mDbHelper.deleteAllGPSCoordinatesFromTableForUser(mDb, user);
                        Toast.makeText(getApplicationContext(), "USERNAME " +
                                user +  " IS DELETED", Toast.LENGTH_SHORT).show();
                        view.setVisibility(View.GONE);
                    }else {
                        Toast.makeText(getApplicationContext(), "PROBLEM WITH USER DELETION",
                                Toast.LENGTH_SHORT).show()
                        ;
                    }
                    return true;
                }
            });
            mLayoutForUserButtons.addView(bt);
        }
    }
}
