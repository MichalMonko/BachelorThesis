package com.example.camerastreamapplication

import android.Manifest.permission.CAMERA
import android.graphics.*
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.SurfaceHolder
import android.view.TextureView
import com.example.camerastreamapplication.cameraAbstractionLayer.*
import com.example.camerastreamapplication.predictions.Box
import com.example.camerastreamapplication.predictions.PredictionListener
import com.example.camerastreamapplication.predictions.Predictor
import com.example.camerastreamapplication.tfLiteWrapper.TfLiteUtils
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.random.Random

private const val CAMERA_PERMISSION_CODE = 101
private const val TAG = "MAIN_ACTIVITY"
private const val MODEL_FILE = "yolov2tiny.tflite"
private const val CLASSES_LABELS_FILE = "coco.labels"

fun randomColor(): Int
{
    return Color.rgb(
            Random.nextInt(0, 255),
            Random.nextInt(0, 255),
            Random.nextInt(0, 255)
    )
}

class MainActivity :
        AppCompatActivity(),
        TextureView.SurfaceTextureListener,
        CameraReadyListener,
        PredictionListener
{

    private var cameraHandler: CameraAbstractionLayer? = null
    private val captureListener: CameraCaptureSession.CaptureCallback
    private val paint: Paint = Paint()
    private lateinit var canvas: Canvas
    private lateinit var surfaceHolder: SurfaceHolder
    private lateinit var predictor: Predictor

    init
    {
        captureListener = object : CameraCaptureSession.CaptureCallback()
        {
            override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult)
            {
                val bitmap = textureView.bitmap

                if (TfLiteUtils.isReady())
                {
                    predictor.frameWidth = bitmap.width
                    predictor.frameHeight = bitmap.height
                    TfLiteUtils.process(bitmap)
                    Log.d(TAG, "Tensor Flow is ready, starting processing")
                }
                session.capture(request,this,cameraHandler?.backgroundHandler)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        Log.d(TAG, "onCreate(): begin")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        predictor = Predictor(this.applicationContext, CLASSES_LABELS_FILE, this)
        TfLiteUtils.initializeTensorFlow(this, MODEL_FILE, predictor)
        ActivityCompat.requestPermissions(this, arrayOf(CAMERA), CAMERA_PERMISSION_CODE)


        surfaceHolder = surfaceOverlay.holder
        surfaceOverlay.setZOrderOnTop(true)
        surfaceHolder.setFormat(PixelFormat.TRANSLUCENT)
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
        }
    }

    private fun prepareCamera()
    {
        cameraHandler = CameraSessionBuilder()
                .withActivity(this)
                .withCallbackListener(this.captureListener)
                .withReadyListener(this)
                .addTarget(SurfaceTextureWithSize(
                        textureView.surfaceTexture, textureView.width, textureView.height))
                .build()
    }

    override fun onCameraReady()
    {
        cameraHandler?.start()
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

    private fun drawPrediction(label: String, rect: Rect, canvas: Canvas)
    {
        paint.strokeWidth = 10.0f
        paint.style = Paint.Style.STROKE
        paint.color = randomColor()

        canvas.drawRect(rect, paint)

        paint.textSize = 20.0f
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 1.0f
        canvas.drawText(label, rect.left.toFloat(), (rect.top - 20).toFloat(), paint)


    }

    override fun onPredictionsMade(labeledPredictions: List<Pair<String?, Box>>)
    {
        Log.d(TAG, "onPredictionsMade() called")

        val canvas = surfaceHolder.lockCanvas()
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)

        for (prediction in labeledPredictions)
        {
            drawPrediction(prediction.first ?: "None", prediction.second.toRect(), canvas)
        }
        surfaceHolder.unlockCanvasAndPost(canvas)

        TfLiteUtils.ready = true
    }
}
