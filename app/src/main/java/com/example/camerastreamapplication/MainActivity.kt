package com.example.camerastreamapplication

import android.app.Activity
import android.graphics.*
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.SurfaceHolder
import android.view.TextureView
import com.example.camerastreamapplication.audio.ALERT_CODES
import com.example.camerastreamapplication.audio.AudioNotificator
import com.example.camerastreamapplication.audio.STATE
import com.example.camerastreamapplication.cameraAbstractionLayer.*
import com.example.camerastreamapplication.config.DETECTION_THRESHOLD
import com.example.camerastreamapplication.config.IoU_THRESHOLD
import com.example.camerastreamapplication.config.VISUAL_MODE_ENABLED
import com.example.camerastreamapplication.predictions.LabeledPrediction
import com.example.camerastreamapplication.predictions.PredictionListener
import com.example.camerastreamapplication.predictions.Predictor
import com.example.camerastreamapplication.tfLiteWrapper.TfLiteUtils
import com.example.camerastreamapplication.threading.ThreadExecutor
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileOutputStream
import java.util.*
import kotlin.random.Random


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
        Activity(),
        TextureView.SurfaceTextureListener,
        CameraStateListener,
        PredictionListener
{

    private lateinit var audioNotificator: AudioNotificator
    private var cameraHandler: CameraAbstractionLayer? = null
    private val captureListener: CameraCaptureSession.CaptureCallback
    private val paint: Paint = Paint()
    private lateinit var surfaceHolder: SurfaceHolder
    private lateinit var predictor: Predictor
    private lateinit var bitmap: Bitmap

    init
    {
        captureListener = object : CameraCaptureSession.CaptureCallback()
        {
            override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult)
            {
                if (cameraHandler != null)
                {

                    bitmap = textureView.bitmap
                    if (TfLiteUtils.isReady() && audioNotificator.isReady())
                    {
                        TfLiteUtils.process(applicationContext, bitmap)
                        Log.d(TAG, "Tensor Flow is ready, starting processing")
                    }
                }
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

        audioNotificator = AudioNotificator(this)
        audioNotificator.prepare()

        surfaceHolder = surfaceOverlay.holder
        surfaceOverlay.setZOrderOnTop(true)
        surfaceHolder.setFormat(PixelFormat.TRANSLUCENT)

        if (!VISUAL_MODE_ENABLED)
        {
            textureView.alpha = 0.0f
        }
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
        cameraHandler = null
        super.onPause()
    }

    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int)
    {
        Log.d(TAG, "onTextureAvailable(): begin")
        prepareCamera()
        applyRotation()
    }

    private fun prepareCamera()
    {
        if (cameraHandler == null && textureView.isAvailable)
        {
            cameraHandler = CameraSessionBuilder()
                    .withActivity(this)
                    .withCallbackListener(this.captureListener)
                    .withReadyListener(this)
                    .addTarget(SurfaceTextureWithSize(
                            textureView.surfaceTexture, textureView.width, textureView.height))
                    .build()
        }
    }

    override fun onCameraReady()
    {
        cameraHandler?.start()
    }

    override fun onCameraFailure()
    {
        cameraHandler = null
        if (audioNotificator.state != STATE.PREPARING)
        {
            audioNotificator.playAlert(ALERT_CODES.CAMERA_ALERT)
        }

        val runnable = Runnable {
            Thread.sleep(5000)
            prepareCamera()
        }

        ThreadExecutor.execute(runnable)
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

    fun toJson(labeledPredictions: List<LabeledPrediction>): String?
    {
        val jsonArray = JSONArray()

        val jsonObject = JSONObject()

        for (prediction in labeledPredictions)
        {
            val pixelRect = prediction.location.toPixelRect(textureView.bitmap.width, textureView.bitmap.height)

            val predictionObject = JSONObject()
            predictionObject.put("name", prediction.name)
            predictionObject.put("confidence", prediction.confidence)

            val locationArray = JSONArray()
            locationArray.put(pixelRect.left)
            locationArray.put(pixelRect.top)
            locationArray.put(pixelRect.right)
            locationArray.put(pixelRect.bottom)

            predictionObject.put("location", locationArray)

            jsonArray.put(predictionObject)
        }
        jsonObject.put("predictions", jsonArray)

        return jsonObject.toString(4)
    }

    override fun onPredictionsMade(labeledPredictions: List<LabeledPrediction>)
    {
        Log.d(TAG, "onPredictionsMade() called")

        if (VISUAL_MODE_ENABLED)
        {
            val canvas = surfaceHolder.lockCanvas()
            if (canvas != null)
            {
                canvas.drawColor(0, PorterDuff.Mode.CLEAR)

                for (prediction in labeledPredictions)
                {
                    drawPrediction("${prediction.name} : ${prediction.confidence}",
                            prediction.location.toPixelRect(bitmap.width, bitmap.height),
                            canvas)
                }
                canvas.setBitmap(textureView.bitmap)
                surfaceHolder.unlockCanvasAndPost(canvas)
            }
        }

        textureView.bitmap?.let {

            val uuid = UUID.randomUUID().toString()
            val outStream = FileOutputStream(
                    "${Environment.getExternalStorageDirectory().absolutePath}/images/${DETECTION_THRESHOLD}_${IoU_THRESHOLD}_${uuid}.jpg")
            it.compress(Bitmap.CompressFormat.JPEG, 30, outStream)

            val outStreamAnnotation = FileOutputStream(
                    "${Environment.getExternalStorageDirectory().absolutePath}/images/${DETECTION_THRESHOLD}_${IoU_THRESHOLD}_${uuid}.json")
            outStreamAnnotation.write(toJson(labeledPredictions)?.toByteArray())
//
//            audioNotificator.notify(labeledPredictions)

            TfLiteUtils.ready = true
        }
    }

}

