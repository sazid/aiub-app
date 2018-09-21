package com.mohammedsazid.android.aiub

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.view.View

import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity(), View.OnClickListener {

    private var prefs: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        saveBtn.setOnClickListener(this)
    }

    @SuppressLint("ApplySharedPref")
    override fun onClick(v: View) {
        val usernameStr = username.text.toString()
        val passwordStr = password.text.toString()

        prefs?.edit()
                ?.putString(getString(R.string.pref_username_key), usernameStr)
                ?.putString(getString(R.string.pref_password_key), passwordStr)
                ?.commit()

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
