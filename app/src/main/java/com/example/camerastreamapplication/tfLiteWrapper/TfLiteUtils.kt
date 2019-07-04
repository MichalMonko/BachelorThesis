package com.example.camerastreamapplication.tfLiteWrapper

import android.app.Activity
import android.util.Log
import com.example.camerastreamapplication.imageProcessing.DoubleBuffer
import com.example.camerastreamapplication.imageProcessing.ResultHandler
import com.example.camerastreamapplication.threading.*
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder


private const val TAG = "TF_UTILS"

private const val IMAGE_WIDTH = 416
private const val IMAGE_HEIGHT = 416
private const val BATCH_SIZE = 1
private const val PIXEL_SIZE = 3
private const val BYTES_PER_CHANNEL = 4

object TfLiteUtils : TensorFlowInitializationListener
{
    var interpreter: Interpreter? = null
        private set
    var initialized: Boolean = false

    // Output tensor from yolo network
    private val output: Array<Array<Array<FloatArray>>> = Array(1) { Array(13) { Array(13) { FloatArray(425) } } }

    val doubleBuffer: DoubleBuffer

    init
    {
        val imageSize = BYTES_PER_CHANNEL *
                IMAGE_WIDTH *
                IMAGE_HEIGHT *
                BATCH_SIZE *
                PIXEL_SIZE

        doubleBuffer = DoubleBuffer(imageSize)
    }

    private fun loadModelFile(activity: Activity, filename: String): ByteBuffer
    {
        val inputStream = activity.assets.open(filename)
        val byteBuffer = ByteBuffer.allocateDirect(inputStream.available())
        byteBuffer.order(ByteOrder.nativeOrder())
        byteBuffer.put(inputStream.readBytes())
        inputStream.close()
        return byteBuffer
    }

    fun initializeTensorFlow(activity: Activity, filename: String)
    {
        val runnable = Runnable {
            try
            {
                interpreter = Interpreter(loadModelFile(activity, filename))
                ResultDispatcher.handleResult(Result.TENSOR_FLOW_INITIALIZED, Status.SUCCESS)
            } catch (ex: Exception)
            {
                Log.e(TAG, "Exception during tensorflow initialization")
                ex.printStackTrace()
                ResultDispatcher.handleResult(Result.TENSOR_FLOW_INITIALIZED, Status.FAILURE)
            }
        }
        ThreadExecutor.execute(runnable)
    }

    fun process()
    {
        val processingRunnable = Runnable {
            val buffer = doubleBuffer.getBufferForReading()
            if (buffer == null)
            {
                Log.d(TAG, "No free buffer available")
                ResultHandler.onProcessingFailed()
            }

            buffer?.use {
                interpreter?.run(buffer.buffer, output)
                ResultHandler.onProcessingSuccess(output)
            }
        }

        ThreadExecutor.execute(processingRunnable)
    }

    override fun onInitializationSuccess()
    {
        Log.d(TAG, "Initialized tensorflow lite interpreter")
        initialized = true
    }

    override fun onInitializationFailed()
    {
        Log.e(TAG, "Error initializing tensorflow lite interpreter")
    }

    fun isReady(): Boolean
    {
        return initialized && interpreter != null
    }
}