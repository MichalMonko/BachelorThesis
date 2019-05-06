package com.example.camerastreamapplication

import android.Manifest.permission.CAMERA
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.CameraCharacteristics.LENS_FACING
import android.hardware.camera2.CameraMetadata.LENS_FACING_BACK
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import kotlinx.android.synthetic.main.activity_main.*

private const val CAMERA_PERMISSION_CODE = 101
private const val TAG = "MAIN_ACTIVITY"

class MainActivity : AppCompatActivity(), TextureView.SurfaceTextureListener {

    private val cameraManager: CameraManager by lazy { getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    private var cameraId = ""
    private var previewSize = Size(0, 0)
    private var backgroundHandler: Handler? = null
    private var handlerThread: HandlerThread? = null
    private var cameraDevice: CameraDevice? = null
    private var cameraSession: CameraCaptureSession? = null
    private var captureRequestBuilder: CaptureRequest.Builder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate(): begin")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ActivityCompat.requestPermissions(this, arrayOf(CAMERA), CAMERA_PERMISSION_CODE)
    }

    override fun onResume() {
        super.onResume()
        startBackgroundHandler()
        if (textureView.isAvailable) {
            setUpCamera()
            openCamera()
        } else {
            textureView.surfaceTextureListener = this
        }
    }

    override fun onStop() {
        super.onStop()
        closeCamera()
        stopBackgroundThread()
    }

    private fun stopBackgroundThread() {
        if (backgroundHandler != null) {
            handlerThread?.quitSafely()
        }
        backgroundHandler = null
        handlerThread = null
    }

    private fun closeCamera() {
        cameraSession?.close()
        cameraSession = null
        cameraDevice?.close()
        cameraDevice = null
    }

    private fun setUpCamera() {
        Log.d(TAG, "setUpCamera(): begin")
        try {
            cameraId = cameraManager.cameraIdList.filter {
                cameraManager.getCameraCharacteristics(it).get(LENS_FACING) == LENS_FACING_BACK
            }[0]

            val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
            val streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            previewSize = streamConfigurationMap.getOutputSizes(SurfaceTexture::class.java)[0]

        } catch (ex: CameraAccessException) {
            ex.printStackTrace()
        }
    }

    private fun openCamera() {
        Log.d(TAG, "openCamera(): begin")
        val stateCallback = object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) {
                Log.d(TAG, "onOpened(): begin")
                cameraDevice = device
                startPreview()
            }

            override fun onDisconnected(p0: CameraDevice) {
                Log.d(TAG, "onDisconnected(): begin")
                cameraDevice?.close()
                cameraDevice = null
            }

            override fun onError(p0: CameraDevice, p1: Int) {
                Log.d(TAG, "onError(): begin")
                cameraDevice?.close()
                cameraDevice = null
            }
        }

        try {
            cameraManager.openCamera(cameraId, stateCallback, backgroundHandler)
        } catch (ex: SecurityException) {
            ex.printStackTrace()
        }
    }

    private fun startPreview() {
        Log.d(TAG, "startPreview(): begin")
        try {
            val texture = textureView.surfaceTexture
            texture.setDefaultBufferSize(previewSize.width, previewSize.height)
            val previewSurface = Surface(texture)
            captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder?.addTarget(previewSurface)

            cameraDevice?.createCaptureSession(listOf(previewSurface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigureFailed(p0: CameraCaptureSession) {
                    Log.d(TAG, "onConfigureFailed(): begin")
                }

                override fun onConfigured(captureSession: CameraCaptureSession) {
                    Log.d(TAG, "onConfigured(): begin")
                    if (cameraDevice == null) {
                        return
                    }
                    try {
                        val captureRequest = captureRequestBuilder?.build()
                        cameraSession = captureSession
                        cameraSession?.setRepeatingRequest(captureRequest, null, backgroundHandler)
                    } catch (ex: CameraAccessException) {
                        ex.printStackTrace()
                    }
                }
            }, backgroundHandler)
        } catch (ex: CameraAccessException) {
            ex.printStackTrace()
        }
    }

    private fun startBackgroundHandler() {
        Log.d(TAG, "startBackgroundHandler(): begin")
        handlerThread = HandlerThread("Camera handler thread")
        handlerThread?.start()
        backgroundHandler = Handler(handlerThread?.looper)
    }

    override fun onSurfaceTextureAvailable(p0: SurfaceTexture?, p1: Int, p2: Int) {
        Log.d(TAG, "onTextureAvailable(): begin")
        setUpCamera()
        openCamera()
    }

    override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture?, p1: Int, p2: Int) {
    }

    override fun onSurfaceTextureUpdated(p0: SurfaceTexture?) {
    }

    override fun onSurfaceTextureDestroyed(p0: SurfaceTexture?): Boolean {
        return false
    }
}
