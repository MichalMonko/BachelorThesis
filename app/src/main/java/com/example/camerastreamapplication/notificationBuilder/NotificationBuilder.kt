package com.example.camerastreamapplication.notificationBuilder

import android.graphics.RectF
import com.example.camerastreamapplication.predictions.Box
import com.example.camerastreamapplication.utils.area
import com.example.camerastreamapplication.utils.intersectingRectangle
import com.example.camerastreamapplication.utils.iou

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
    private val areas = NonNullMap(mapOf(
            Pair(Location.LEFT, RectF(0.0f, 0.0f, 0.33f, 1.0f)),
            Pair(Location.FRONT, RectF(0.33f, 0.0f, 0.66f, 1.0f)),
            Pair(Location.RIGHT, RectF(0.66f, 0.0f, 1.0f, 1.0f)))
    )

    private val priorities = NonNullMap(mapOf(
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

    fun buildNotifications(predictions: Collection<Pair<String, Box>>): Collection<Notification>
    {
        val notifications = ArrayList<Notification?>()

        for (prediction in predictions)
        {
            notifications.add(buildNotification(prediction))
        }

        return notifications.filterNotNull()
    }

    private fun buildNotification(prediction: Pair<String, Box>): Notification?
    {
        val locationIoU = mapOf(
                Pair(Location.LEFT, iou(areas[Location.LEFT], prediction.second.normalizedRect)),
                Pair(Location.FRONT, iou(areas[Location.FRONT], prediction.second.normalizedRect)),
                Pair(Location.RIGHT, iou(areas[Location.RIGHT], prediction.second.normalizedRect))
        )

        val maxLocation = locationIoU.maxBy { it.value }

        if (maxLocation != null)
        {
            val intersection = intersectingRectangle(prediction.second.normalizedRect, areas[maxLocation.key])
            val normalizedArea = intersection.area()
            val location = maxLocation.key
            val distance = determineDistance(normalizedArea)
            val priority = priorities[Pair(distance, maxLocation.key)]

            return Notification(prediction.first, location, distance, priority)
        }

        return null
    }

    private fun determineDistance(normalizedArea: Float): Distance
    {
        return when
        {
            normalizedArea >= 0.8f                            -> Distance.VERY_CLOSE
            (0.6f <= normalizedArea && normalizedArea < 0.8f) -> Distance.CLOSE
            (0.4f <= normalizedArea && normalizedArea < 0.6f) -> Distance.AVERAGE
            else                                              -> Distance.FAR
        }
    }
}