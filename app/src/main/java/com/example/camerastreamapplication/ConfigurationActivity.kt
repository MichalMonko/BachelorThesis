package com.example.camerastreamapplication

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.CheckBox
import android.widget.SeekBar
import com.example.camerastreamapplication.config.*
import com.example.camerastreamapplication.fragments.HelpFragment
import kotlinx.android.synthetic.main.content_configuration.*

private const val HELP_DIALOG_TAG = "HelpDialog"

class ConfigurationActivity : AppCompatActivity(), SeekBar.OnSeekBarChangeListener,
        View.OnClickListener
{

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.configuration_activity)

        detection_threshold_slider.setOnSeekBarChangeListener(this)
        iou_threshold_slider.setOnSeekBarChangeListener(this)
        notifications_slider.setOnSeekBarChangeListener(this)

        flashlight_checkbox.setOnClickListener(this)
        visual_mode_checkbox.setOnClickListener(this)

        detection_threshold_help.setOnClickListener(this)
        iou_threshold_help.setOnClickListener(this)
        notifications_help.setOnClickListener(this)
    }

    override fun onResume()
    {
        super.onResume()

        detection_threshold_slider.progress = (DETECTION_THRESHOLD * 100.0f).toInt()
        iou_threshold_slider.progress = (IoU_THRESHOLD * 100.0f).toInt()
        notifications_slider.progress = MAX_OBJECT_NOTIFICATIONS
        flashlight_checkbox.isChecked = FLASHLIGHT_ENABLED
        visual_mode_checkbox.isChecked = VISUAL_MODE_ENABLED
    }

    override fun onPause()
    {
        super.onPause()
        val sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        editor.putFloat(DETECTION_KEY, DETECTION_THRESHOLD)
        editor.putFloat(IOU_THRESHOLD_KEY, IoU_THRESHOLD)
        editor.putInt(MAX_NOTIFICATIONS_KEY, MAX_OBJECT_NOTIFICATIONS)
        editor.putBoolean(FLASHLIGHT_ENABLED_KEY, FLASHLIGHT_ENABLED)
        editor.putBoolean(VISUAL_MODE_ENABLED_KEY, VISUAL_MODE_ENABLED)

        editor.apply()
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean)
    {
        when (seekBar)
        {
            detection_threshold_slider   ->
            {
                DETECTION_THRESHOLD = intToFloatPercentage(progress)
                detection_threshold_inidicator.text = progress.toString()
            }
            iou_threshold_slider   ->
            {
                IoU_THRESHOLD = intToFloatPercentage(progress)
                iou_threshold_indicator.text = progress.toString()
            }
            notifications_slider   ->
            {
                MAX_OBJECT_NOTIFICATIONS = progress
                notifications_indicator.text = progress.toString()
            }
        }
    }

    override fun onClick(view: View?)
    {
        if (view == null)
        {
            return
        }

        if(view is CheckBox)
        {
            when (view)
            {
                flashlight_checkbox -> FLASHLIGHT_ENABLED = view.isChecked
                visual_mode_checkbox -> VISUAL_MODE_ENABLED = view.isChecked
            }
            return
        }

        val messagesResources = when (view)
        {
            detection_threshold_help   -> Pair(R.string.detection_threshold_help_title, R.string.detection_threshold_help_message)
            iou_threshold_help   -> Pair(R.string.iou_threshold_help_title, R.string.iou_threshold_help_message)
            notifications_help   -> Pair(R.string.notifications_help_title, R.string.notifications_help_message)
            else                 -> throw IllegalStateException("No handler for view: $view")
        }

        val transaction = supportFragmentManager.beginTransaction()
        val previousFragment = supportFragmentManager.findFragmentByTag(HELP_DIALOG_TAG)

        if (previousFragment != null)
        {
            transaction.remove(previousFragment)
        }

        transaction.addToBackStack(null)

        val messages = Pair(getString(messagesResources.first), getString(messagesResources.second))
        val helpFragment = HelpFragment.newInstance(messages.first, messages.second)


        helpFragment.show(transaction, HELP_DIALOG_TAG)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?)
    {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?)
    {
    }

    private fun intToFloatPercentage(integerValue: Int): Float
    {
        return integerValue.toFloat() / 100.0f
    }
}
