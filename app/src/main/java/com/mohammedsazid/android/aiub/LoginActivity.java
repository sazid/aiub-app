package com.mohammedsazid.android.aiub;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.view.View;
import android.widget.Button;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private AppCompatEditText username;
    private AppCompatEditText password;
    private Button saveBtn;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        bindViews();

        saveBtn.setOnClickListener(this);
    }

    private void bindViews() {
        username = (AppCompatEditText) findViewById(R.id.username);
        password = (AppCompatEditText) findViewById(R.id.password);
        saveBtn = (Button) findViewById(R.id.save);
    }

    @Override
    public void onClick(View v) {
        String usernameStr = username.getText().toString();
        String passwordStr = password.getText().toString();

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(getString(R.string.pref_username_key), usernameStr);
        editor.putString(getString(R.string.pref_password_key), passwordStr);
        editor.apply();

        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
