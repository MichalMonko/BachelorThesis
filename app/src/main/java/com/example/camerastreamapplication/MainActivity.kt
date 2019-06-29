package com.example.camerastreamapplication

import android.Manifest.permission.CAMERA
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.TextureView
import com.example.camerastreamapplication.CameraAbstractionLayer.CameraAbstractionLayer
import com.example.camerastreamapplication.CameraAbstractionLayer.CameraSessionBuilder
import com.example.camerastreamapplication.CameraAbstractionLayer.CameraUtils
import com.example.camerastreamapplication.CameraAbstractionLayer.SurfaceTextureWithSize
import kotlinx.android.synthetic.main.activity_main.*

private const val CAMERA_PERMISSION_CODE = 101
private const val TAG = "MAIN_ACTIVITY"

class MainActivity : AppCompatActivity(), TextureView.SurfaceTextureListener
{

    private var cameraHandler: CameraAbstractionLayer? = null
    private val captureListener: CameraCaptureSession.CaptureCallback

    init
    {
        captureListener = object : CameraCaptureSession.CaptureCallback()
        {
            override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult)
            {
                Log.d(TAG, "onCaptureCompleted()")
                super.onCaptureCompleted(session, request, result)
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        Log.d(TAG, "onCreate(): begin")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ActivityCompat.requestPermissions(this, arrayOf(CAMERA), CAMERA_PERMISSION_CODE)
    }

    override fun onResume()
    {
        Log.d(TAG, "onResume() called")
        super.onResume()
        if (textureView.isAvailable)
        {
            Log.d(TAG, "onResume() Texture View is valid")
            prepareCamera()
            applyRotation()
            startCameraSession()
        }
        else
        {
            textureView.surfaceTextureListener = this
        }
    }

    override fun onPause()
    {
        cameraHandler?.teardown()
        super.onPause()
    }

    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int)
    {
        Log.d(TAG, "onTextureAvailable(): begin")
        if (cameraHandler == null)
        {
            prepareCamera()
            applyRotation()
            startCameraSession()
        }
    }

    private fun prepareCamera()
    {
        cameraHandler = CameraSessionBuilder()
                .withActivity(this)
                .withListener(this.captureListener)
                .addTarget(SurfaceTextureWithSize(
                        textureView.surfaceTexture, textureView.width, textureView.height))
                .build()
    }

    private fun startCameraSession(): Boolean
    {
        cameraHandler?.let {
            if (it.isReady)
            {
                it.start()
                return true
            }
            return false
        }
        throw NullPointerException("cameraHandler is not initialized")
    }

    private fun applyRotation()
    {
        cameraHandler?.let {
            val rotation = windowManager.defaultDisplay.rotation
            CameraUtils.transformImage(textureView, it.previewSize, rotation)
            return
        }
        throw NullPointerException("cameraHandler is not initialized")
    }

    override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture?, p1: Int, p2: Int)
    {
    }

    override fun onSurfaceTextureUpdated(p0: SurfaceTexture?)
    {
    }

    override fun onSurfaceTextureDestroyed(p0: SurfaceTexture?): Boolean
    {
        Log.d(TAG, "onSurfaceTextureDestroyed() begin")
        return true
    }
}
