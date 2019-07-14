package com.example.camerastreamapplication.predictions

import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.util.Log
import com.example.camerastreamapplication.tfLiteWrapper.*
import com.example.camerastreamapplication.utils.Tensor4D
import kotlin.math.exp

private const val TAG = "PREDICTOR"

fun sigmoid(x: Float): Float
{
    return 1.0f / (1.0f + exp(-x))
}

fun softmax(data: FloatArray)
{
    val exponentialSum = data.sumByDouble { exp(it.toDouble()) }.toFloat()
    data.map { it / exponentialSum }
}

interface PredictionListener
{
    fun onPredictionsMade(labeledPredictions: List<Pair<String?, Box>>)
}

class Box(xc: Float, yc: Float, widthF: Float, heightF: Float, frameWidth: Int, frameHeight: Int)
{
    private val left = (xc - (widthF / 2.0f)) * frameWidth
    private val top = (yc - (heightF / 2.0f)) * frameHeight
    private val right = (xc + (widthF / 2.0f)) * frameWidth
    private val bottom = (yc + (heightF / 2.0f)) * frameHeight

    fun toRect(): Rect
    {
        return Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
    }
}

data class BoundingBoxPrediction(val classIndex: Int, val confidence: Float, val location: Box)

class Predictor(context: Context, classesFile: String, private val predictionListener: PredictionListener)
{
    private val classMapping = mutableMapOf<Int, String>()
    private val classesScores = FloatArray(NUM_OF_CLASSES)
    private val uiThreadHandler: Handler = Handler(context.mainLooper)
    var frameWidth: Int = INPUT_WIDTH
    var frameHeight: Int = INPUT_HEIGHT

    init
    {
        val inputStream = context.assets.open(classesFile)

        val byteArray = ByteArray(inputStream.available())

        inputStream.use {
            it.read(byteArray)
        }

        val classNames = String(byteArray).split('\n')

        for (i in 0 until classNames.size)
        {
            classMapping[i] = classNames[i]
        }
    }

    private fun notifyListener(predictions: ArrayList<BoundingBoxPrediction>)
    {

        val labeledPredictions = predictions.map { prediction ->
            Pair("${classMapping[prediction.classIndex]} : ${prediction.confidence}",
                    prediction.location)
        }

        predictionListener.onPredictionsMade(labeledPredictions)
    }

    fun makePredictions(data: Tensor4D)
    {
        Log.d(TAG, "Started output processing")

        val output = mutableListOf<Float>()
        val tensor2d = data[0].flatten()
        tensor2d.forEach { output.addAll(it.asIterable()) }

        val predictions = ArrayList<BoundingBoxPrediction>()

        for (y in 0 until GRID_HEIGHT)
        {
            for (x in 0 until GRID_WIDTH)
            {
                for (box in 0 until BOXES_PER_SEGMENT)
                {
                    val segmentOffset = y * x * OFFSET_PER_SEGMENT + x * OFFSET_PER_SEGMENT
                    val boxOffset = box * OFFSET_PER_BOX
                    val boxDataStart = segmentOffset + boxOffset
                    val confidenceIndex = segmentOffset + boxOffset + CONFIDENCE_OFFSET
                    val classDataStart = confidenceIndex + 1

                    val boxObjectConfidence = sigmoid(output[confidenceIndex])

                    if (boxObjectConfidence > BOX_DETECTION_THRESHOLD)
                    {
                        for (i in 0 until NUM_OF_CLASSES)
                        {
                            classesScores[i] = sigmoid(output[classDataStart + i])
                        }
                        softmax(classesScores)

                        val maxClass = classesScores.withIndex().maxBy { it.value } ?: IndexedValue(0, Float.MIN_VALUE)
                        val maxClassIndex = maxClass.index
                        val maxClassConfidence = maxClass.value * boxObjectConfidence

                        val centerX = (x + sigmoid(output[boxDataStart])) * CELL_WIDTH / INPUT_WIDTH
                        val centerY = (y + sigmoid(output[boxDataStart + 1])) * CELL_HEIGHT / INPUT_HEIGHT
                        val width = exp(output[boxDataStart + 2]) * ANCHORS[2 * box] * CELL_WIDTH / INPUT_WIDTH
                        val height = exp(output[boxDataStart + 3]) * ANCHORS[2 * box + 1] * CELL_WIDTH / INPUT_WIDTH

                        val boundingBox = Box(
                                centerX, centerY, width, height, frameWidth,frameHeight
                        )

                        predictions.add(
                                BoundingBoxPrediction(maxClassIndex, maxClassConfidence, boundingBox)
                        )
                    }
                }
            }
        }

        Log.d(TAG, "Neural network processing finished")
        uiThreadHandler.post { notifyListener(predictions) }
    }

}
