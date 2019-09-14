package com.example.camerastreamapplication

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.SeekBar
import com.example.camerastreamapplication.config.*
import com.example.camerastreamapplication.fragments.HelpFragment
import kotlinx.android.synthetic.main.configuration_activity.*
import kotlinx.android.synthetic.main.content_configuration.*

private const val HELP_DIALOG_TAG = "HelpDialog"

class ConfigurationActivity : AppCompatActivity(), SeekBar.OnSeekBarChangeListener,
        View.OnClickListener
{

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.configuration_activity)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        box_threshold_slider.setOnSeekBarChangeListener(this)
        class_threshold_slider.setOnSeekBarChangeListener(this)
        iou_threshold_slider.setOnSeekBarChangeListener(this)
        notifications_slider.setOnSeekBarChangeListener(this)

        box_threshold_help.setOnClickListener(this)
        class_threshold_help.setOnClickListener(this)
        iou_threshold_help.setOnClickListener(this)
        notifications_help.setOnClickListener(this)
    }

    override fun onResume()
    {
        super.onResume()

        box_threshold_slider.progress = (BOX_DETECTION_THRESHOLD * 100.0f).toInt()
        class_threshold_slider.progress = (CLASS_CONFIDENCE_THRESHOLD * 100.0f).toInt()
        iou_threshold_slider.progress = (IoU_THRESHOLD * 100.0f).toInt()
        notifications_slider.progress = MAX_OBJECT_NOTIFICATIONS
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

        val messagesResources = when (view)
        {
            box_threshold_help   -> Pair(R.string.box_threshold_help_title, R.string.box_threshold_help_message)
            class_threshold_help -> Pair(R.string.class_threshold_help_title, R.string.class_threshold_help_message)
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
