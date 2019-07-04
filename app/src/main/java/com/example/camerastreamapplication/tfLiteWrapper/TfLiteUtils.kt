package com.example.camerastreamapplication.tfLiteWrapper

import android.app.Activity
import android.graphics.Bitmap
import android.util.Log
import com.example.camerastreamapplication.imageProcessing.ImageProcessingUtils
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

    private var ready = false

    // Output tensor from yolo network
    private val output: Array<Array<Array<FloatArray>>> = Array(1) { Array(13) { Array(13) { FloatArray(425) } } }

    private val buffer: ByteBuffer

    init
    {
        val imageSize = BYTES_PER_CHANNEL *
                IMAGE_WIDTH *
                IMAGE_HEIGHT *
                BATCH_SIZE *
                PIXEL_SIZE

        buffer = ByteBuffer.allocateDirect(imageSize)
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

    fun process(bitmap: Bitmap)
    {
        if (!ready)
        {
            ResultHandler.onProcessingFailed("Last processing request hasn't finished yet")
            return
        }

        ready = false
        val processingRunnable = Runnable {

            ImageProcessingUtils.storeInBuffer(bitmap, buffer)
            interpreter?.run(buffer, output)
            ResultHandler.onProcessingSuccess(output)
            this.ready = true
        }

        ThreadExecutor.execute(processingRunnable)
    }

    override fun onInitializationSuccess()
    {
        ready = true
        Log.d(TAG, "Initialized tensorflow lite interpreter")
        initialized = true
    }

    override fun onInitializationFailed()
    {
        ready = true
        Log.e(TAG, "Error initializing tensorflow lite interpreter")
    }

    fun isReady(): Boolean
    {
        return initialized && interpreter != null && ready
    }
}