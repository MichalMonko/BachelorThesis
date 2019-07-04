package com.example.camerastreamapplication.imageProcessing

import android.util.Log
import com.example.camerastreamapplication.threading.TensorFlowProcessingListener


private const val TAG = "ResultHandler"

object ResultHandler : TensorFlowProcessingListener
{
    override fun onProcessingSuccess(data: Any?)
    {
        data?.let {
            val tensor = data as Array<Array<Array<FloatArray>>>
            val output = tensor.flatten()


        }

        Log.d(TAG, "Neural network processing finished")
    }

    override fun onProcessingFailed()
    {
        Log.e(TAG, "Error processing image in tensor flow neural network")
    }
}