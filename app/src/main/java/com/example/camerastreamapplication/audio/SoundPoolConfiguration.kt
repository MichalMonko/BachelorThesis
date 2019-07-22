package com.example.camerastreamapplication.audio

import com.example.camerastreamapplication.R
import com.example.camerastreamapplication.notificationBuilder.Distance

val AUDIO_FILES = mapOf(
        Pair("cup", R.raw.cup),
//        Pair("Keyboard", "keyboard.mp3"),
//        Pair(Location.RIGHT, "right.mp3"),
//        Pair(Location.LEFT, "left.mp3"),
//        Pair(Location.FRONT, "front.mp3"),
        Pair(Distance.VERY_CLOSE, R.raw.veryclose)
//        Pair(Distance.CLOSE, "close.mp3"),
//        Pair(Distance.FAR, "far.mp3")
)

const val MAX_OBJECT_NOTIFICATIONS = 3