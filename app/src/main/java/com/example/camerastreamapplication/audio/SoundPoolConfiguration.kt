package com.example.camerastreamapplication.audio

import com.example.camerastreamapplication.notificationBuilder.Distance
import com.example.camerastreamapplication.notificationBuilder.Location

enum class ALERT_CODES(name: String)
{
    CAMERA_ALERT("CAMERA_ALERT")
}

val AUDIO_FILES = mapOf(
        Pair("person", "osoba.mp3"),
        Pair("bicycle", "rower.mp3"),
        Pair("car", "samochod.mp3"),
        Pair("motorcycle", "motor.mp3"),
        Pair("bus", "autobus.mp3"),
        Pair("train", "pociag.mp3"),
        Pair("truck", "ciezarowka.mp3"),
        Pair("traffic light", "swiatla.mp3"),
        Pair("fire hydrant", "hydrant.mp3"),
        Pair("parking meter", "parkometr.mp3"),
        Pair("bench", "lawka.mp3"),
        Pair("unknown", "nieznanyobiekt.mp3"),
        Pair(Location.RIGHT, "prawo.mp3"),
        Pair(Location.LEFT, "lewo.mp3"),
        Pair(Location.FRONT, "prosto.mp3"),
        Pair(Distance.VERY_CLOSE, "bardzoblisko.mp3"),
        Pair(Distance.CLOSE, "blisko.mp3"),
        Pair(Distance.FAR, "daleko.mp3"),
        Pair(ALERT_CODES.CAMERA_ALERT, "bladkamery.mp3")
)


