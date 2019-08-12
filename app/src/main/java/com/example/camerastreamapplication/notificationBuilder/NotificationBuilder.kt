package com.example.camerastreamapplication.notificationBuilder

import android.graphics.RectF
import android.util.Log
import com.example.camerastreamapplication.predictions.LabeledPrediction
import com.example.camerastreamapplication.utils.area
import com.example.camerastreamapplication.utils.intersectingRectangle
import com.example.camerastreamapplication.utils.iou

const val TAG = "AUDIO"

class NonNullMap<K, V>(private val map: Map<K, V>) : Map<K, V> by map
{
    override operator fun get(key: K): V
    {
        return map[key] ?: error("Null key passed to NonNullMap instance")
    }
}

enum class Location
{
    FRONT, LEFT, RIGHT
}

enum class Distance
{
    FAR, AVERAGE, CLOSE, VERY_CLOSE
}

data class Notification(val objectName: String,
                        val location: Location,
                        val distance: Distance,
                        var priority: Int)

object NotificationBuilder
{
    private val AREAS = NonNullMap(mapOf(
            Pair(Location.LEFT, RectF(0.0f, 0.0f, 0.33f, 1.0f)),
            Pair(Location.FRONT, RectF(0.33f, 0.0f, 0.66f, 1.0f)),
            Pair(Location.RIGHT, RectF(0.66f, 0.0f, 1.0f, 1.0f)))
    )

    private val PRIORITIES = NonNullMap(mapOf(
            Pair(Pair(Distance.VERY_CLOSE, Location.FRONT), 6),
            Pair(Pair(Distance.CLOSE, Location.FRONT), 5),
            Pair(Pair(Distance.VERY_CLOSE, Location.RIGHT), 5),
            Pair(Pair(Distance.VERY_CLOSE, Location.LEFT), 5),
            Pair(Pair(Distance.AVERAGE, Location.FRONT), 3),
            Pair(Pair(Distance.CLOSE, Location.LEFT), 4),
            Pair(Pair(Distance.CLOSE, Location.RIGHT), 4),
            Pair(Pair(Distance.AVERAGE, Location.LEFT), 2),
            Pair(Pair(Distance.AVERAGE, Location.RIGHT), 2),
            Pair(Pair(Distance.FAR, Location.FRONT), 1),
            Pair(Pair(Distance.FAR, Location.LEFT), 1),
            Pair(Pair(Distance.FAR, Location.RIGHT), 1)
    ))

    fun buildNotifications(predictions: Collection<LabeledPrediction>): Collection<Notification>
    {
        val notifications = ArrayList<Notification?>()

        for (prediction in predictions)
        {
            notifications.add(buildNotification(prediction))
        }

        return notifications.filterNotNull()
    }

    private fun buildNotification(prediction: LabeledPrediction): Notification?
    {
        val locationIoU = mapOf(
                Pair(Location.LEFT, iou(AREAS[Location.LEFT], prediction.location.normalizedRect)),
                Pair(Location.RIGHT, iou(AREAS[Location.RIGHT], prediction.location.normalizedRect)),
                Pair(Location.FRONT, iou(AREAS[Location.FRONT], prediction.location.normalizedRect))
        )

        val maxLocation = locationIoU.maxBy { it.value }

        if (maxLocation != null)
        {
            val sector = AREAS[maxLocation.key]
            val intersection = intersectingRectangle(prediction.location.normalizedRect, sector)
            val intersectionNormalizeArea = intersection.area()
            val sectorNormalizedArea = sector.area()
            val location = maxLocation.key
            val distance = determineDistance(intersectionNormalizeArea / sectorNormalizedArea)
            Log.d(TAG, "Intersection area is $intersectionNormalizeArea, Sector Area is $sectorNormalizedArea, " +
                    " Ratio is ${intersectionNormalizeArea / sectorNormalizedArea} ")
            val priority = PRIORITIES[Pair(distance, maxLocation.key)]

            return Notification(prediction.name, location, distance, priority)
        }

        return null
    }

    private fun determineDistance(normalizedArea: Float): Distance
    {
        return when
        {
            normalizedArea >= 0.7f                            -> Distance.VERY_CLOSE
            (0.5f <= normalizedArea && normalizedArea < 0.7f) -> Distance.CLOSE
            (0.2f <= normalizedArea && normalizedArea < 0.5f) -> Distance.AVERAGE
            else                                              -> Distance.FAR
        }
    }
}