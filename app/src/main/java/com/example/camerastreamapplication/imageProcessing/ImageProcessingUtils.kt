package com.example.camerastreamapplication.imageProcessing

import android.graphics.Bitmap
import android.util.Log
import java.nio.ByteBuffer

private const val TAG = "ImgUtils"
private const val RGB_MAX_VALUE = 255.0f

object ImageProcessingUtils
{

    private val intPixelsValues = IntArray(416 * 416)

    fun storeInBuffer(bitmap: Bitmap, buffer: ByteBuffer)
    {
        Log.d(TAG, "storeInBuffer() started")

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 416, 416, true)
        resizedBitmap.getPixels(intPixelsValues, 0, resizedBitmap.width, 0, 0, resizedBitmap.width, resizedBitmap.height)

        buffer.rewind()

        for (pixel in 0 until 416 * 416)
        {
            val pixelValue = intPixelsValues[pixel]
            buffer.putFloat(((pixelValue shr 16 and 0xFF) / RGB_MAX_VALUE))
            buffer.putFloat(((pixelValue shr 8 and 0xFF) / RGB_MAX_VALUE))
            buffer.putFloat(((pixelValue and 0xFF) / RGB_MAX_VALUE))
        }

        Log.d(TAG, "storeInBuffer() ended")
    }

}