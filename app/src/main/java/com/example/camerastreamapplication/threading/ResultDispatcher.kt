package com.example.camerastreamapplication.threading

import com.example.camerastreamapplication.tfLiteWrapper.TfLiteUtils

enum class Result
{
    TENSOR_FLOW_INITIALIZED,
}

enum class Status
{
    SUCCESS, FAILURE
}

interface TensorFlowInitializationListener
{
    fun onInitializationSuccess()
    fun onInitializationFailed()
}

object ResultDispatcher
{
    private var tensorFlowInitializationListener: TensorFlowInitializationListener = TfLiteUtils

    fun handleResult(result: Result, status: Status, data: Any? = null)
    {
        when (result)
        {
            Result.TENSOR_FLOW_INITIALIZED -> handleTensorFlowInitialized(status)
            else                           -> return
        }

    }

    private fun handleTensorFlowInitialized(status: Status)
    {
        when (status)
        {
            Status.SUCCESS -> tensorFlowInitializationListener.onInitializationSuccess()
            Status.FAILURE -> tensorFlowInitializationListener.onInitializationFailed()
        }
    }
}

