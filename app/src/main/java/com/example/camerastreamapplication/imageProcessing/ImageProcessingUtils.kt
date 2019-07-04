package com.example.camerastreamapplication.imageProcessing

import android.graphics.Bitmap
import android.util.Log
import com.example.camerastreamapplication.tfLiteWrapper.TfLiteUtils

private const val TAG = "ImgUtils"

object ImageProcessingUtils
{

    private const val IMAGE_MEAN = 128
    private const val IMAGE_STD = 128.0f

    fun storeInBuffer(bitmap: Bitmap): Boolean
    {
        Log.d(TAG, "storeInBuffer called()")
        val buffer = TfLiteUtils.doubleBuffer.getBufferForWriting() ?: return false

        buffer.use {
            it.buffer.rewind()
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 416, 416, true)

            for (i in 0 until 416)
            {
                for (j in 0 until 416)
                {
                    val pixelValue = resizedBitmap.getPixel(i, j)
                    it.buffer.putFloat(((pixelValue shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    it.buffer.putFloat(((pixelValue shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    it.buffer.putFloat(((pixelValue and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                }
            }
        }

        return true
    }

}