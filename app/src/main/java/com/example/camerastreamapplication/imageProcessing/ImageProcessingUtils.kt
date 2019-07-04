package com.example.camerastreamapplication.imageProcessing

import android.graphics.Bitmap
import android.util.Log
import java.nio.ByteBuffer

private const val TAG = "ImgUtils"

object ImageProcessingUtils
{

    private const val IMAGE_MEAN = 128
    private const val IMAGE_STD = 128.0f

    fun storeInBuffer(bitmap: Bitmap, buffer: ByteBuffer)
    {
        Log.d(TAG, "storeInBuffer called()")

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 416, 416, true)
        with(buffer)
        {
            rewind()

            for (i in 0 until 416)
            {
                for (j in 0 until 416)
                {
                    val pixelValue = resizedBitmap.getPixel(i, j)
                    putFloat(((pixelValue shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    putFloat(((pixelValue shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    putFloat(((pixelValue and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                }
            }
        }
    }

}