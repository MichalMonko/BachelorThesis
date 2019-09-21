package com.example.camerastreamapplication.audio

import com.example.camerastreamapplication.notificationBuilder.Distance
import com.example.camerastreamapplication.notificationBuilder.Location

enum class ALERT_CODES(name: String)
{
    CAMERA_ALERT("CAMERA_ALERT")
}

val AUDIO_FILES = mapOf(
        Pair("cup", "kubek.mp3"),
        Pair("keyboard", "klawiatura.mp3"),
        Pair("tv", "telewizor.mp3"),
        Pair("unknown", "nieznany.mp3"),
        Pair(Location.RIGHT, "prawo.mp3"),
        Pair(Location.LEFT, "lewo.mp3"),
        Pair(Location.FRONT, "prosto.mp3"),
        Pair(Distance.VERY_CLOSE, "bardzo_blisko.mp3"),
        Pair(Distance.CLOSE, "blisko.mp3"),
        Pair(Distance.FAR, "daleko.mp3"),
        Pair(ALERT_CODES.CAMERA_ALERT, "blad_kamery.mp3")
)


