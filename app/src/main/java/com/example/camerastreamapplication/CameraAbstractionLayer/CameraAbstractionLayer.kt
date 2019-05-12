package com.example.camerastreamapplication.CameraAbstractionLayer

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.CameraCharacteristics.LENS_FACING
import android.hardware.camera2.CameraMetadata.LENS_FACING_BACK
import android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT
import android.hardware.camera2.params.SessionConfiguration
import android.os.Handler
import android.os.HandlerThread
import android.se.omapi.Session
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.Size
import android.view.Surface

enum class CAMERA_TYPE(val num: Int) {
    BACK(0), FRONT(1)
}

private const val TAG = "CAL_TAG"

class CameraAbstractionLayer(val activity: AppCompatActivity, private val listener: CameraCaptureSession.CaptureCallback, val cameraType: CAMERA_TYPE,
                             private val surface_textures: List<SurfaceTexture>) : CameraDevice.StateCallback() {

    private val cameraManager: CameraManager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val cameraId: String
    private val previewSize: Size

    private var backgroundHandler: Handler? = null
    private var handlerThread: HandlerThread? = null

    private lateinit var requestBuilder: CaptureRequest.Builder

    init {
        val lensPositionEnum = when (cameraType) {
            CAMERA_TYPE.BACK -> LENS_FACING_BACK
            CAMERA_TYPE.FRONT -> LENS_FACING_FRONT
        }

        startBackgroundHandler()

        cameraId = cameraManager.cameraIdList.filter {
            cameraManager.getCameraCharacteristics(it).get(LENS_FACING) == lensPositionEnum
        }[0]

        val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
        val streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        previewSize = streamConfigurationMap.getOutputSizes(SurfaceTexture::class.java)[0]
    }

    //CameraDevice.StateCallback methods
    override fun onOpened(camera: CameraDevice) {
        val surfaces = configureSurfaces()
        requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        for (surface in surfaces) {
            requestBuilder.addTarget(surface)
        }

        val captureSessionStateListener = object : CameraCaptureSession.StateCallback() {

            override fun onConfigureFailed(p0: CameraCaptureSession) {
                Log.e(TAG, "Capture Session configuration failed")
                stopBackgroundThread()
            }

            override fun onConfigured(captureSession: CameraCaptureSession) {
                try {
                    captureSession.setRepeatingRequest(requestBuilder.build(), listener, backgroundHandler)
                } catch (ex: CameraAccessException) {
                    Log.e(TAG, "Error setting capture session request, reason: ${ex.reason}")
                    ex.printStackTrace()
                }
            }
        }
        camera.createCaptureSession(surfaces, captureSessionStateListener, backgroundHandler)
    }

    override fun onDisconnected(p0: CameraDevice) {
        stopBackgroundThread()
    }

    override fun onError(p0: CameraDevice, p1: Int) {
        stopBackgroundThread()
    }
    //End of CameraDevice.StateCallback methods

    private fun configureSurfaces(): List<Surface> {
        surface_textures.forEach {
            it.setDefaultBufferSize(previewSize.width, previewSize.height)
        }
        return surface_textures.map { Surface(it) }
    }

    private fun startBackgroundHandler() {
        handlerThread = HandlerThread("CameraThread")
        handlerThread?.start()
        backgroundHandler = Handler(handlerThread?.looper)
    }

    fun stopBackgroundThread() {
        handlerThread?.quitSafely()
        handlerThread = null
        backgroundHandler = null
    }

}