package com.example.camerastreamapplication.cameraAbstractionLayer

import android.graphics.Matrix
import android.graphics.RectF
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import kotlin.math.max


object CameraUtils
{
    private val ORIENTATIONS = SparseIntArray()

    init
    {
        ORIENTATIONS.append(Surface.ROTATION_0, 90)
        ORIENTATIONS.append(Surface.ROTATION_90, 0)
        ORIENTATIONS.append(Surface.ROTATION_180, 270)
        ORIENTATIONS.append(Surface.ROTATION_270, 180)
    }

    fun getRotation(sensorOrientation: Int, deviceOrientation: Int): Int
    {
        val mappedDeviceOrientation = CameraUtils.ORIENTATIONS.get(deviceOrientation)
        return (sensorOrientation + mappedDeviceOrientation + 360) % 360
    }

    fun shouldSwapDimensions(sensorOrientation: Int,
                             deviceOrientation: Int): Boolean
    {
        val totalRotation = getRotation(sensorOrientation, deviceOrientation)
        return totalRotation == 90 || totalRotation == 270
    }

    fun transformImage(textureView: TextureView, previewSize: Size, rotation: Int)
    {
        val width = textureView.width.toFloat()
        val height = textureView.height.toFloat()

        val previewWidth = previewSize.width.toFloat()
        val previewHeight = previewSize.height.toFloat()

        val textureRectF = RectF(0.0f, 0.0f, width, height)

        //YES it should be height and then width
        val previewRectF = RectF(0.0f, 0.0f, previewHeight, previewWidth)

        val centerX = textureRectF.centerX()
        val centerY = textureRectF.centerY()

        val matrix = Matrix()

        if (rotation in listOf(Surface.ROTATION_90, Surface.ROTATION_270))
        {
            previewRectF.offset(centerX - previewRectF.centerX(),
                    centerY - previewRectF.centerY())
            val scale = max(width / previewWidth, height / previewHeight)
            matrix.setRectToRect(textureRectF, previewRectF, Matrix.ScaleToFit.FILL)
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate(90.0f * (rotation - 2), centerX, centerY)

            textureView.setTransform(matrix)
        }
    }
}
