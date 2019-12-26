package com.example.camerastreamapplication.predictions

import android.content.Context
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.util.Log
import com.example.camerastreamapplication.config.*
import com.example.camerastreamapplication.utils.Tensor4D
import com.example.camerastreamapplication.utils.iou
import com.example.camerastreamapplication.utils.sigmoid
import com.example.camerastreamapplication.utils.softmax
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

private const val TAG = "PREDICTOR"

data class BoundingBoxPrediction(val classIndex: Int, val confidence: Float, val location: Box)
data class LabeledPrediction(val name: String, val confidence: Float, val location: Box)

interface PredictionListener
{
    fun onPredictionsMade(labeledPredictions: List<LabeledPrediction>)
}

class Box(xc: Float, yc: Float, widthF: Float, heightF: Float)
{
    private val left = max(xc - (widthF / 2.0f), 0.0f)
    private val top = max(yc - (heightF / 2.0f), 0.0f)
    private val right = min(xc + (widthF / 2.0f), 1.0f)
    private val bottom = min(yc + (heightF / 2.0f), 1.0f)

    val normalizedRect: RectF = RectF(left, top, right, bottom)

    fun toPixelRect(bitmapWidth: Int, bitmapHeight: Int): Rect
    {
        return Rect((left * bitmapWidth).toInt(),
                (top * bitmapHeight).toInt(),
                (right * bitmapWidth).toInt(),
                (bottom * bitmapHeight).toInt())
    }
}


class Predictor(context: Context, classesFile: String, private val predictionListener: PredictionListener)
{
    private val classMapping = mutableMapOf<Int, String>()
    private val classesScores = FloatArray(NUM_OF_CLASSES)
    private val uiThreadHandler: Handler = Handler(context.mainLooper)

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
            when (val name = classMapping[prediction.classIndex])
            {
                null -> null
                else ->
                {
                    LabeledPrediction(name = name,
                            confidence = prediction.confidence,
                            location = prediction.location)
                }
            }
        }.filterNotNull()
                .filter { it.name in URBAN_NAMES }

        predictionListener.onPredictionsMade(labeledPredictions)
    }

    fun makePredictions(data: Tensor4D)
    {
// val executionTime = measureTimeMillis {
        Log.d(TAG, "Started output processing")

        val tensor3d = data[0]

        var predictions = ArrayList<BoundingBoxPrediction>()

        for (y in 0 until GRID_HEIGHT)
        {
            for (x in 0 until GRID_WIDTH)
            {
                val output = tensor3d[y][x]
                for (box in 0 until BOXES_PER_SEGMENT)
                {
                    val boxOffset = box * OFFSET_PER_BOX
                    val classDataStart = boxOffset + 5

                    val boxObjectConfidence = sigmoid(output[boxOffset + 4])

                    if (boxObjectConfidence > DETECTION_THRESHOLD)
                    {
                        for (i in 0 until NUM_OF_CLASSES)
                        {
                            classesScores[i] = sigmoid(output[classDataStart + i])
                        }
                        softmax(classesScores)

                        val maxClass = classesScores.withIndex().maxBy { it.value }
                                ?: IndexedValue(0, Float.MIN_VALUE)
                        val maxClassIndex = maxClass.index
                        val maxClassConfidence = maxClass.value * boxObjectConfidence

                        val centerX = (x + sigmoid(output[boxOffset])) * CELL_WIDTH / INPUT_WIDTH
                        val centerY = (y + sigmoid(output[boxOffset + 1])) * CELL_HEIGHT / INPUT_HEIGHT
                        val width = exp(output[boxOffset + 2]) * ANCHORS[2 * box] * CELL_WIDTH / INPUT_WIDTH
                        val height = exp(output[boxOffset + 3]) * ANCHORS[2 * box + 1] * CELL_WIDTH / INPUT_WIDTH

                        val boundingBox = Box(
                                centerX, centerY, width, height
                        )

                        predictions.add(
                                BoundingBoxPrediction(maxClassIndex, maxClassConfidence, boundingBox)
                        )
                    }
                }
            }
        }

        predictions = getNonMaxSuppressed(predictions)
        val imageArea = INPUT_HEIGHT * INPUT_WIDTH
        predictions.filter {
            val pixelRect = it.location.toPixelRect(INPUT_WIDTH, INPUT_HEIGHT)
            pixelRect.width() * pixelRect.height() > THRESHOLD_SMALL_REMOVAL * imageArea
        }
        Log.d(TAG, "Neural network processing finished")
        uiThreadHandler.post { notifyListener(predictions) }
//       }
// Can be uncommented to measure execution time
//        val outStream = FileOutputStream("${Environment.getExternalStorageDirectory().absolutePath}/output_parsing_time.txt", true)
//        outStream.write("${executionTime}\n".toByteArray())
    }

    private fun getNonMaxSuppressed(predictions: ArrayList<BoundingBoxPrediction>): ArrayList<BoundingBoxPrediction>
    {
        val groupedByClass = predictions.groupBy { prediction -> prediction.classIndex }
        val newPredictions = ArrayList<BoundingBoxPrediction>()

        for (clazz in groupedByClass.keys)
        {
            for (classPredictions in groupedByClass.values)
            {
                val suppressed = nonMaxSuppression(classPredictions)
                newPredictions.addAll(suppressed)
            }
        }

        return newPredictions
    }

    private fun nonMaxSuppression(predictions: List<BoundingBoxPrediction>): List<BoundingBoxPrediction>
    {
        val afterSuppression = ArrayList<BoundingBoxPrediction>()
        if (predictions.size < 2)
        {
            return predictions
        }

        val mutablePredictions = predictions.toMutableList()
        while (mutablePredictions.size > 0)
        {
            val topScoredPrediction = mutablePredictions.maxBy { it.confidence } ?: break
            mutablePredictions.remove(topScoredPrediction)
            afterSuppression.add(topScoredPrediction)

            val markedForRemoval = ArrayList<BoundingBoxPrediction>()
            for (prediction in mutablePredictions)
            {
                if (iou(topScoredPrediction, prediction) >= IoU_THRESHOLD)
                    markedForRemoval.add(prediction)
            }
            mutablePredictions.removeAll(markedForRemoval)
        }
        return afterSuppression
    }


}



















