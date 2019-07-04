package com.example.camerastreamapplication.imageProcessing

import android.util.Log
import com.example.camerastreamapplication.threading.TensorFlowProcessingListener


private const val TAG = "ResultHandler"

object ResultHandler : TensorFlowProcessingListener
{
    override fun onProcessingSuccess(data: Any?)
    {
        data?.let {
            if(data is Array<*>)
            {
                val tensor = data as Array<Array<Array<FloatArray>>>
                val output = tensor.flatten()
            }


        }

        Log.d(TAG, "Neural network processing finished")
    }

    override fun onProcessingFailed(reason: String?)
    {
        Log.e(TAG, "Error during neural network processing : $reason")
    }
}