package com.example.camerastreamapplication.cameraAbstractionLayer

import android.Manifest.permission.CAMERA
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.CameraCharacteristics.LENS_FACING
import android.hardware.camera2.CameraMetadata.LENS_FACING_BACK
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.content.ContextCompat
import android.util.Log
import android.util.Size
import android.view.Surface

private const val TAG = "CAMERA"

data class SurfaceTextureWithSize(val surfaceTexture: SurfaceTexture,
                                  val width: Int,
                                  val height: Int)

interface CameraReadyListener
{
    fun onCameraReady()
}

class CameraSessionBuilder
{
    private lateinit var callbackListener: CameraCaptureSession.CaptureCallback
    private lateinit var activity: Activity
    private lateinit var readyListener: CameraReadyListener
    private val targets = arrayListOf<SurfaceTextureWithSize>()


    fun withActivity(activity: Activity): CameraSessionBuilder
    {
        this.activity = activity
        return this
    }

    fun withCallbackListener(listener: CameraCaptureSession.CaptureCallback): CameraSessionBuilder
    {
        this.callbackListener = listener
        return this
    }

    fun withReadyListener(readyListener: CameraReadyListener): CameraSessionBuilder
    {
        this.readyListener = readyListener
        return this
    }

    fun addTarget(target: SurfaceTextureWithSize): CameraSessionBuilder
    {
        this.targets.add(target)
        return this
    }

    fun build(): CameraAbstractionLayer
    {
        return CameraAbstractionLayer(activity, callbackListener, readyListener, this.targets)
    }
}

class CameraAbstractionLayer(private val activity: Activity, private val listener: CameraCaptureSession.CaptureCallback,
                             private val readyListener: CameraReadyListener,
                             private val targets: List<SurfaceTextureWithSize>) : CameraDevice.StateCallback()
{

    private var cameraCaptureSession: CameraCaptureSession? = null
    private var camera: CameraDevice? = null
    private val cameraId: String
    val previewSize: Size
    private var sensorOrientation = 0

    var backgroundHandler: Handler? = null
        private set
    private var handlerThread: HandlerThread? = null

    private lateinit var requestBuilder: CaptureRequest.Builder
    private val cameraManager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    var isReady = false
        private set

    var isClosed = true
        private set


    init
    {
        Log.d(TAG, "init() started")
        startBackgroundHandler()
        cameraId = cameraManager.cameraIdList.filter {
            cameraManager.getCameraCharacteristics(it).get(LENS_FACING) == LENS_FACING_BACK
        }[0]

        val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
        val streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        previewSize = streamConfigurationMap.getOutputSizes(SurfaceTexture::class.java)[0]

        if (ContextCompat.checkSelfPermission(activity, CAMERA) == PackageManager.PERMISSION_GRANTED)
        {
            cameraManager.openCamera(cameraId, this, backgroundHandler)
        }
        else
        {
            throw SecurityException("Camera permission is not granted")
        }
        Log.d(TAG, "init() ended")
    }

    private val captureSessionStateListener = object : CameraCaptureSession.StateCallback()
    {
        override fun onConfigureFailed(captureSession: CameraCaptureSession)
        {
            Log.d(TAG, "onConfigureFailed() started")
            cameraCaptureSession = captureSession
            Log.e(TAG, "Capture Session configuration failed")
            teardown()
        }

        override fun onConfigured(captureSession: CameraCaptureSession)
        {
            Log.d(TAG, "onConfigured() started")
            try
            {
                cameraCaptureSession = captureSession
                isReady = true
                captureSession.capture(requestBuilder.build(), listener, backgroundHandler)
                readyListener.onCameraReady()
            } catch (ex: CameraAccessException)
            {
                Log.e(TAG, "Error setting capture session request, reason: ${ex.reason}")
                ex.printStackTrace()
            }
        }
    }

    fun start()
    {
        Log.d(TAG, "start() called")
        try
        {
            cameraCaptureSession?.capture(requestBuilder.build(), listener, backgroundHandler)
        } catch (ex: CameraAccessException)
        {
            Log.e(TAG, "Error setting capture session request, reason: ${ex.reason}")
            ex.printStackTrace()
        }
    }


    //CameraDevice.StateCallback methods
    override fun onOpened(camera: CameraDevice)
    {
        Log.d(TAG, "onOpened() started")
        this.camera = camera

        val characteristics = cameraManager.getCameraCharacteristics(cameraId)

        val displaySize = Point()
        activity.windowManager.defaultDisplay.getSize(displaySize)

        val displayRotation = activity.windowManager.defaultDisplay.rotation
        sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)

        val shouldSwapDimensions = CameraUtils.shouldSwapDimensions(sensorOrientation, displayRotation)


        requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)

        val surfaces = configureSurfaces(targets, shouldSwapDimensions)

        for (surface in surfaces)
        {
            requestBuilder.addTarget(surface)
        }
        camera.createCaptureSession(surfaces, captureSessionStateListener, backgroundHandler)

        isClosed = false
        Log.d(TAG, "onOpened() ending")
    }

    private fun configureSurfaces(targets: List<SurfaceTextureWithSize>, swapDimensions: Boolean): List<Surface>
    {
        val surfaceList = arrayListOf<Surface>()
        Log.d(TAG, "configureSurfaces() started")
        targets.forEach {
            if (swapDimensions)
            {
                it.surfaceTexture.setDefaultBufferSize(previewSize.height, previewSize.width)
            }
            else
            {
                it.surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
            }
            surfaceList.add(Surface(it.surfaceTexture))
        }
        Log.d(TAG, "configureSurfaces() ending")

        return surfaceList
    }

    override fun onDisconnected(p0: CameraDevice)
    {
        Log.d(TAG, "cameraSession.onDisconnected() called")
        teardown()
    }

    override fun onError(p0: CameraDevice, p1: Int)
    {
        Log.e(TAG, "cameraSessionError()")
        teardown()
    }

    override fun onClosed(camera: CameraDevice)
    {
        Log.d(TAG, "cameraSession.onClosed() called")
        isClosed = true
    }

    //End of CameraDevice.StateCallback methods
    private fun startBackgroundHandler()
    {
        handlerThread = HandlerThread("CameraThread")
        handlerThread?.start()
        backgroundHandler = Handler(handlerThread?.looper)
    }

    private fun stopBackgroundThread()
    {
        Log.d(TAG, "stopBackgroundThread() called")
        handlerThread?.quitSafely()
        handlerThread = null
        backgroundHandler = null
    }

    fun teardown()
    {
        Log.d(TAG, "teardown() called")
        camera?.close()
        camera = null
        stopBackgroundThread()
    }

}