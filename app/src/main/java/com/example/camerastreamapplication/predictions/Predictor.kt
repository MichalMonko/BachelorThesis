package com.example.camerastreamapplication.predictions

import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.util.Log
import com.example.camerastreamapplication.tfLiteWrapper.*
import com.example.camerastreamapplication.utils.Tensor4D
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

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
    private val left = max(((xc - (widthF / 2.0f)) * frameWidth).toInt(), 0)
    private val top = max(((yc - (heightF / 2.0f)) * frameHeight).toInt(), 0)
    private val right = min(((xc + (widthF / 2.0f)) * frameWidth).toInt(), frameWidth - 1)
    private val bottom = min(((yc + (heightF / 2.0f)) * frameHeight).toInt(), frameHeight - 1)

    fun toRect(): Rect
    {
        return Rect(left, top, right, bottom)
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

        val tensor3d = data[0]

        var predictions = ArrayList<BoundingBoxPrediction>()

        for (y in 0 until GRID_HEIGHT)
        {
            for (x in 0 until GRID_WIDTH)
            {
                val output = tensor3d[x][y]
                for (box in 0 until BOXES_PER_SEGMENT)
                {
                    val boxOffset = box * OFFSET_PER_BOX
                    val classDataStart = boxOffset + 5

                    val boxObjectConfidence = sigmoid(output[boxOffset + 4])

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

                        if (maxClassConfidence > CLASS_CONFIDENCE_THRESHOLD)
                        {

                            val centerX = (x + sigmoid(output[boxOffset])) * CELL_WIDTH / INPUT_WIDTH
                            val centerY = (y + sigmoid(output[boxOffset + 1])) * CELL_HEIGHT / INPUT_HEIGHT
                            val width = exp(output[boxOffset + 2]) * ANCHORS[2 * box] * CELL_WIDTH / INPUT_WIDTH
                            val height = exp(output[boxOffset + 3]) * ANCHORS[2 * box + 1] * CELL_WIDTH / INPUT_WIDTH

                            val boundingBox = Box(
                                    centerX, centerY, width, height, frameWidth, frameHeight
                            )

                            predictions.add(
                                    BoundingBoxPrediction(maxClassIndex, maxClassConfidence, boundingBox)
                            )
                        }
                    }
                }
            }
        }

        predictions = getNonMaxSuppressed(predictions)
        Log.d(TAG, "Neural network processing finished")
        uiThreadHandler.post { notifyListener(predictions) }
    }

    private fun getNonMaxSuppressed(predictions: ArrayList<BoundingBoxPrediction>): ArrayList<BoundingBoxPrediction>
    {
        val groupedByClass = predictions.groupBy { prediction -> prediction.classIndex }
        val newPredictions = ArrayList<BoundingBoxPrediction>()

        for (classPredictions in groupedByClass.values)
        {
            val suppressed = nonMaxSuppression(classPredictions)
            newPredictions.addAll(suppressed)
        }

        return newPredictions
    }

    private fun nonMaxSuppression(predictions: List<BoundingBoxPrediction>): List<BoundingBoxPrediction>
    {
        if (predictions.size < 2)
        {
            return predictions
        }
        val sortedPredictions = predictions.sortedByDescending { it.confidence }
        val topScoredPrediction = sortedPredictions[0]

        val suppressed = ArrayList<BoundingBoxPrediction>()

        for (i in 1 until sortedPredictions.size)
        {
            if (IoU(topScoredPrediction, sortedPredictions[i]) <= IoU_THRESHOLD)
            {
                suppressed.add(predictions[i])
            }
        }

        return suppressed
    }

    private fun IoU(lhs: BoundingBoxPrediction, rhs: BoundingBoxPrediction): Double
    {
        val lhsRect = lhs.location.toRect()
        val rhsRect = rhs.location.toRect()

        val biggerLeft = max(lhsRect.left, rhsRect.left)
        val smallerRight = min(lhsRect.right, rhsRect.right)

        val biggerTop = max(lhsRect.top, rhsRect.top)
        val smallerBottom = min(lhsRect.bottom, rhsRect.bottom)

        val intersectionX = (smallerRight - biggerLeft).toDouble()
        val intersectionY = (smallerBottom - biggerTop).toDouble()

        if (intersectionX < 0 || intersectionY < 0)
            return 0.0

        val intersection = intersectionX * intersectionY

        val lhsArea = ((lhsRect.right - lhsRect.left) * (lhsRect.bottom - lhsRect.top)).toDouble()
        val rhsArea = ((rhsRect.right - rhsRect.left) * (rhsRect.bottom - rhsRect.top)).toDouble()

        return intersection / (lhsArea + rhsArea - intersection)
    }

}



















