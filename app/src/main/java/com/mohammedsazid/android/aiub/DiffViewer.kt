package com.mohammedsazid.android.aiub

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView

class DiffViewer : AppCompatActivity() {

    private var prefTv: TextView? = null
    private var notiTv: TextView? = null
    private var titleTv: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diff_viewer)

        bindViews()

        val title_text = intent?.getStringExtra("TITLE")
        val pref_text = intent?.getStringExtra("PREF_STRING")
        val noti_text = intent?.getStringExtra("NOTI_STRING")

        titleTv?.text = title_text
        prefTv?.text = pref_text
        notiTv?.text = noti_text
    }

    private fun bindViews() {
        prefTv = findViewById(R.id.pref_value_tv)
        notiTv = findViewById(R.id.pref_value_tv)
        titleTv = findViewById(R.id.type_tv)
    }

}
