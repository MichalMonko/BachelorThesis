package com.example.camerastreamapplication.tfLiteWrapper

import android.app.Activity
import android.graphics.Bitmap
import android.util.Log
import com.example.camerastreamapplication.imageProcessing.ImageProcessingUtils
import com.example.camerastreamapplication.predictions.Predictor
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

    var ready = false

    // Output tensor from yolo network
    private val output: Array<Array<Array<FloatArray>>> = Array(1) { Array(13) { Array(13) { FloatArray(425) } } }

    private val buffer: ByteBuffer


    private var predictor : Predictor? = null

    init
    {
        val imageSize = BYTES_PER_CHANNEL *
                IMAGE_WIDTH *
                IMAGE_HEIGHT *
                BATCH_SIZE *
                PIXEL_SIZE

        buffer = ByteBuffer.allocateDirect(imageSize)
        buffer.order(ByteOrder.nativeOrder())
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

    fun initializeTensorFlow(activity: Activity, filename: String, predictor : Predictor)
    {
        val runnable = Runnable {
            try
            {
                this.predictor = predictor
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

    fun process(bitmap: Bitmap) : Boolean
    {
        Log.d(TAG,"processing started")
        if (!ready)
        {
            return false
        }

        ready = false
        val processingRunnable = Runnable {

            ImageProcessingUtils.storeInBuffer(bitmap, buffer)

            Log.d(TAG,"Running interpreter")
            interpreter?.run(buffer, output)
            Log.d(TAG,"Interpreter run finished")

            predictor?.makePredictions(output)
        }

        ThreadExecutor.execute(processingRunnable)

        Log.d(TAG,"processing request send to thread")
        return true
    }

    override fun onInitializationSuccess()
    {
        Log.d(TAG, "Initialized tensorflow lite interpreter")

        ready = true
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