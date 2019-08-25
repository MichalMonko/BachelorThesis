package com.example.camerastreamapplication

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.SeekBar
import com.example.camerastreamapplication.config.*
import kotlinx.android.synthetic.main.configuration_activity.*
import kotlinx.android.synthetic.main.content_configuration.*

class ConfigurationActivity : AppCompatActivity(), SeekBar.OnSeekBarChangeListener
{

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.configuration_activity)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        box_threshold_slider.progress = (BOX_DETECTION_THRESHOLD * 100.0f).toInt()
        class_threshold_slider.progress = (CLASS_CONFIDENCE_THRESHOLD * 100.0f).toInt()
        iou_threshold_slider.progress = (IoU_THRESHOLD * 100.0f).toInt()
        notifications_slider.progress = MAX_OBJECT_NOTIFICATIONS

        box_threshold_slider.setOnSeekBarChangeListener(this)
        class_threshold_slider.setOnSeekBarChangeListener(this)
        iou_threshold_slider.setOnSeekBarChangeListener(this)
        notifications_slider.setOnSeekBarChangeListener(this)
    }

    override fun onPause()
    {
        super.onPause()
        val sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        editor.putFloat(BOX_DETECTION_KEY, BOX_DETECTION_THRESHOLD)
        editor.putFloat(CLASS_DETECTION_KEY, CLASS_CONFIDENCE_THRESHOLD)
        editor.putFloat(IOU_THRESHOLD_KEY, IoU_THRESHOLD)
        editor.putInt(MAX_NOTIFICATIONS_KEY, MAX_OBJECT_NOTIFICATIONS)

        editor.apply()
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean)
    {
        when (seekBar)
        {
            box_threshold_slider   ->
            {
                BOX_DETECTION_THRESHOLD = intToFloatPercentage(progress)
                box_threshold_inidicator.text = progress.toString()
            }
            class_threshold_slider ->
            {
                CLASS_CONFIDENCE_THRESHOLD = intToFloatPercentage(progress)
                class_threshold_indicator.text = progress.toString()
            }
            iou_threshold_slider   ->
            {
                IoU_THRESHOLD = intToFloatPercentage(progress)
                iou_threshold_indicator.text = progress.toString()
            }
            box_threshold_slider   ->
            {
                MAX_OBJECT_NOTIFICATIONS = progress
                notifications_indicator.text = progress.toString()
            }
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?)
    {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?)
    {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun intToFloatPercentage(integerValue: Int): Float
    {
        return integerValue.toFloat() / 100.0f
    }
}
