package com.example.camerastreamapplication.utils

import android.graphics.RectF
import com.example.camerastreamapplication.predictions.BoundingBoxPrediction
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

fun RectF.area(): Float
{
    return (right - left) * (bottom - top)
}


fun iou(lhs: BoundingBoxPrediction, rhs: BoundingBoxPrediction): Double
{
    val lhsRect = lhs.location.normalizedRect
    val rhsRect = rhs.location.normalizedRect

    return iou(lhsRect, rhsRect)
}

fun iou(lhsRect: RectF, rhsRect: RectF): Double
{
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

fun intersectingRectangle(lhsRect: RectF, rhsRect: RectF): RectF
{
    val biggerLeft = max(lhsRect.left, rhsRect.left)
    val smallerRight = min(lhsRect.right, rhsRect.right)

    val biggerTop = max(lhsRect.top, rhsRect.top)
    val smallerBottom = min(lhsRect.bottom, rhsRect.bottom)

    return RectF(biggerLeft, biggerTop, smallerRight, smallerBottom)
}

fun sigmoid(x: Float): Float
{
    return 1.0f / (1.0f + exp(-x))
}

fun softmax(data: FloatArray)
{
    val exponentialSum = data.sumByDouble { exp(it.toDouble()) }.toFloat()
    data.map { it / exponentialSum }
}
