package com.example.camerastreamapplication.threading

import com.example.camerastreamapplication.imageProcessing.ResultHandler
import com.example.camerastreamapplication.tfLiteWrapper.TfLiteUtils

enum class Result
{
    TENSOR_FLOW_INITIALIZED,

    TENSOR_FLOW_PROCESSED
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

interface TensorFlowProcessingListener
{
    fun onProcessingSuccess(data: Any?)
    fun onProcessingFailed()
}

object ResultDispatcher
{
    private var tensorFlowInitializationListener: TensorFlowInitializationListener = TfLiteUtils
    private var tensorFlowProcessingListener: TensorFlowProcessingListener = ResultHandler

    fun handleResult(result: Result, status: Status, data: Any? = null)
    {
        when (result)
        {
            Result.TENSOR_FLOW_INITIALIZED -> handleTensorFlowInitialized(status)
            Result.TENSOR_FLOW_PROCESSED   -> handleTensorFlowResult(status, data)
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

    private fun handleTensorFlowResult(status: Status, data: Any?)
    {
        when (status)
        {
            Status.SUCCESS -> tensorFlowProcessingListener.onProcessingSuccess(data)
            Status.FAILURE -> tensorFlowProcessingListener.onProcessingFailed()
        }
    }
}

