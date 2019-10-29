package com.example.camerastreamapplication.predictions

import android.content.Context
import android.graphics.Rect
import android.graphics.RectF
import android.os.Environment
import android.os.Handler
import android.util.Log
import com.example.camerastreamapplication.config.*
import com.example.camerastreamapplication.utils.Tensor4D
import com.example.camerastreamapplication.utils.iou
import com.example.camerastreamapplication.utils.sigmoid
import com.example.camerastreamapplication.utils.softmax
import java.io.FileOutputStream
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.system.measureTimeMillis

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
//                .filter { it.name in URBAN_NAMES }

        predictionListener.onPredictionsMade(labeledPredictions)
    }

    fun makePredictions(data: Tensor4D)
    {
        val executionTime = measureTimeMillis {
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

                        for (i in 0 until NUM_OF_CLASSES)
                        {
                            classesScores[i] = sigmoid(output[classDataStart + i])
                        }
                        softmax(classesScores)

                        val maxClass = classesScores.withIndex().maxBy { it.value } ?: IndexedValue(0, Float.MIN_VALUE)
                        val maxClassIndex = maxClass.index
                        val maxClassConfidence = maxClass.value * boxObjectConfidence

                        if (maxClassConfidence * boxObjectConfidence > DETECTION_THRESHOLD)
                        {

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
            Log.d(TAG, "Neural network processing finished")
            uiThreadHandler.post { notifyListener(predictions) }
        }

        val outStream = FileOutputStream("${Environment.getExternalStorageDirectory().absolutePath}/output_parsing_time.txt", true)
        outStream.write("${executionTime}\n".toByteArray())
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
            if (iou(topScoredPrediction, sortedPredictions[i]) <= IoU_THRESHOLD)
            {
                suppressed.add(predictions[i])
            }
        }

        return suppressed
    }


}



















