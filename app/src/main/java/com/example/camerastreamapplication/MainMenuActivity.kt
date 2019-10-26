package com.example.camerastreamapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.example.camerastreamapplication.audio.AudioNotificator
import com.example.camerastreamapplication.config.*
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
        configureButton.setOnClickListener(this)
        helpButton.setOnClickListener(this)

        val sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        DETECTION_THRESHOLD = sharedPreferences.getFloat(DETECTION_KEY, DETECTION_THRESHOLD)
        IoU_THRESHOLD = sharedPreferences.getFloat(IOU_THRESHOLD_KEY, IoU_THRESHOLD)
        MAX_OBJECT_NOTIFICATIONS = sharedPreferences.getInt(MAX_NOTIFICATIONS_KEY, MAX_OBJECT_NOTIFICATIONS)

    }

    private fun playInstruction()
    {
        AudioNotificator.playRawAudio(applicationContext, R.raw.instrukcja)
    }

    override fun onClick(view: View?)
    {
        when (view)
        {
            startButton     -> startActivity(Intent(this, MainActivity::class.java))
            configureButton -> startActivity(Intent(this, ConfigurationActivity::class.java))
            helpButton      -> playInstruction()
            else            -> throw IllegalArgumentException("Invalid view passed to on click listener")
        }
    }
}
