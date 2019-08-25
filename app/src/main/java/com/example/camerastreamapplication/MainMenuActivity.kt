package com.example.camerastreamapplication

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_main_menu.*

private const val CAMERA_PERMISSION_CODE = 101
private const val TAG = "MAIN_MENU"

class MainMenuActivity : AppCompatActivity(), View.OnClickListener
{

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        startButton.setOnClickListener(this)
    }

    override fun onClick(view: View?)
    {
        when (view)
        {
            startButton     -> startActivity(Intent(this, MainActivity::class.java))
            configureButton -> Log.d(TAG, "Configure Clicked")
            helpButton      -> Log.d(TAG, "Help clicked")
            else            -> throw IllegalArgumentException("Invalid view passed to on click listener")
        }
    }
}
