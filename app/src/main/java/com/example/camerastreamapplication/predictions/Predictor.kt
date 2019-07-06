package com.example.camerastreamapplication.predictions

import android.content.Context
import android.util.Log
import com.example.camerastreamapplication.tfLiteWrapper.*
import com.example.camerastreamapplication.utils.Tensor4D

private const val TAG = "PREDICTOR"

fun sigmoid(x: Float): Float
{
    val exponent = kotlin.math.exp(x)
    return exponent / (exponent + 1)
}

fun softmax(data: FloatArray)
{
    val exponentialSum = data.sumByDouble { kotlin.math.exp(it.toDouble()) }.toFloat()
    data.map { it / exponentialSum }
}

interface PredictionListener
{
    fun onPredictionsMade(classes: List<Pair<String, Float>>)
}

class Predictor(context: Context, classesFile: String, private val predictionListener: PredictionListener)
{
    private val classMapping = mutableMapOf<Int, String>()
    private val classesScores = FloatArray(NUM_OF_CLASSES)

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

    private fun notifyListener(predictions: List<Pair<Int, Float>>)
    {
        val classes = mutableListOf<Pair<String, Float>>()
        predictions.forEach { classes.add(Pair(classMapping[it.first] ?: "", it.second)) }

        Log.d(TAG, "Predicted classes ${classes.joinToString(", ")}")
        predictionListener.onPredictionsMade(classes)
    }

    fun makePredictions(data: Tensor4D)
    {
        Log.d(TAG, "Started output processing")

        val output = mutableListOf<Float>()
        val tensor2d = data[0].flatten()
        tensor2d.forEach { output.addAll(it.asIterable()) }

        val predictions = mutableListOf<Pair<Int, Float>>()

        for (y in 0 until GRID_HEIGHT)
        {
            for (x in 0 until GRID_WIDTH)
            {
                for (box in 0 until BOXES_PER_SEGMENT)
                {
                    val segmentOffset = y * x * OFFSET_PER_SEGMENT + x * OFFSET_PER_SEGMENT
                    val boxOffset = box * OFFSET_PER_BOX
                    val confidenceIndex = segmentOffset + boxOffset + CONFIDENCE_OFFSET
                    val classDataStart = confidenceIndex + 1

                    val boxObjectConfidence = sigmoid(output[confidenceIndex])

                    println(boxObjectConfidence)

                    for (i in 0 until NUM_OF_CLASSES)
                    {
                        classesScores[i] = sigmoid(output[classDataStart + i])
                    }
                    softmax(classesScores)

                    val maxClass = classesScores.withIndex().maxBy { it.value } ?: IndexedValue(0, Float.MIN_VALUE)
                    val maxClassIndex = maxClass.index
                    val maxClassConfidence = maxClass.value

                    if (boxObjectConfidence * maxClassConfidence > DETECTION_THRESHOLD)
                    {
                        predictions.add(Pair(maxClassIndex, maxClassConfidence))
                    }
                }
            }
        }

        Log.d(TAG, "Neural network processing finished")

        notifyListener(predictions)
    }

}
