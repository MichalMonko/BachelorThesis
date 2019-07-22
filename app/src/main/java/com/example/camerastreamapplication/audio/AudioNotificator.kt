package com.example.camerastreamapplication.audio

import android.app.Activity
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
import android.media.SoundPool
import android.net.Uri
import com.example.camerastreamapplication.notificationBuilder.Distance
import com.example.camerastreamapplication.notificationBuilder.Notification
import com.example.camerastreamapplication.notificationBuilder.NotificationBuilder
import com.example.camerastreamapplication.predictions.Box
import com.example.camerastreamapplication.threading.ThreadExecutor

enum class STATE
{
    PREPARING, READY, PLAYING, IDLE, CLOSING
}

data class SoundMetadata(val soundId: Int, val duration: Double)

class AudioNotificator(private val activity: Activity)
{
    var state = STATE.IDLE
        private set
    private val rawPathBase = "android.resource://${activity.packageName}/"
    private lateinit var soundPool: SoundPool

    private val soundBoard = mutableMapOf<Any, SoundMetadata>()


    fun prepare()
    {
        if (state != STATE.IDLE)
        {
            throw IllegalStateException("prepare function can only be called when in IDLE state")
        }

        state = STATE.PREPARING

        val runnable = Runnable {

            val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()

            soundPool = SoundPool.Builder()
                    .setMaxStreams(1)
                    .setAudioAttributes(audioAttributes)
                    .build()

            val metadataRetriever = MediaMetadataRetriever()

            for (entry in AUDIO_FILES.entries)
            {
                val path = rawPathBase + entry.value
                val soundID = soundPool.load(path, 1)
                metadataRetriever.setDataSource(activity.baseContext, Uri.parse(path))
                val duration = metadataRetriever.extractMetadata(METADATA_KEY_DURATION).toDouble()
                metadataRetriever.release()
                soundBoard[entry.key] = SoundMetadata(soundID, duration)
            }

            state = STATE.READY
        }

        ThreadExecutor.execute(runnable)
    }

    fun notify(predictions: Collection<Pair<String, Box>>)
    {
        if (state != STATE.READY)
        {
            throw IllegalStateException("notify can be called only from READY state")
        }

        state = STATE.PLAYING

        val runnable = Runnable {
            val notifications = NotificationBuilder.buildNotifications(predictions)
                    .sortedByDescending { it.priority }
                    .subList(0, MAX_OBJECT_NOTIFICATIONS)

            val soundsToPlay = buildSoundAlerts(notifications)

            playSounds(soundsToPlay)
            state = STATE.READY
        }

        ThreadExecutor.execute(runnable)
    }

    private fun playSounds(soundsToPlay: List<SoundMetadata>)
    {
        for (sound in soundsToPlay)
        {
            soundPool.play(sound.soundId, 1.0f, 1.0f, 1, 0, 1.0f)
            Thread.sleep(sound.duration.toLong())
        }
    }

    private fun buildSoundAlerts(notifications: List<Notification>): List<SoundMetadata>
    {
        return listOfNotNull(
                soundBoard["cup"],
                soundBoard[Distance.FAR]
        )
    }

    fun isReady(): Boolean
    {
        return this.state == STATE.READY
    }
}










