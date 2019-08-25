package com.example.camerastreamapplication.audio

import android.app.Activity
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
import android.media.SoundPool
import com.example.camerastreamapplication.config.MAX_OBJECT_NOTIFICATIONS
import com.example.camerastreamapplication.notificationBuilder.Notification
import com.example.camerastreamapplication.notificationBuilder.NotificationBuilder
import com.example.camerastreamapplication.predictions.LabeledPrediction
import com.example.camerastreamapplication.threading.ThreadExecutor

enum class STATE
{
    PREPARING, READY, PLAYING, IDLE
}

data class SoundMetadata(val soundId: Int, val duration: Long)

class AudioNotificator(private val activity: Activity)
{
    var state = STATE.IDLE
        private set
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
                val assetDescriptor = activity.assets.openFd(entry.value)
                metadataRetriever.setDataSource(assetDescriptor.fileDescriptor, assetDescriptor.startOffset, assetDescriptor.length)
                val duration = metadataRetriever.extractMetadata(METADATA_KEY_DURATION).toLong()

                val soundID = soundPool.load(assetDescriptor, 1)

                soundBoard[entry.key] = SoundMetadata(soundID, duration)
            }
            metadataRetriever.release()

            state = STATE.READY
        }

        ThreadExecutor.execute(runnable)
    }

    fun notify(predictions: Collection<LabeledPrediction>)
    {
        if (state != STATE.READY)
        {
            return
        }

        state = STATE.PLAYING

        val runnable = Runnable {


            val notifications = NotificationBuilder.buildNotifications(predictions)
                    .sortedByDescending { it.priority }

            val sliced = when
            {
                notifications.size >= MAX_OBJECT_NOTIFICATIONS -> notifications.subList(0, MAX_OBJECT_NOTIFICATIONS)
                else                                                                                      -> notifications.subList(0, notifications.size)
            }

            val soundsToPlay = buildSoundAlerts(sliced)

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
            Thread.sleep(sound.duration)
        }
    }

    private fun buildSoundAlerts(notifications: List<Notification>): List<SoundMetadata>
    {
        val soundsToPlay = ArrayList<SoundMetadata?>()

        notifications.forEach {
            val nameSound = when (soundBoard[it.objectName])
            {
                null -> soundBoard["unknown"]
                else -> soundBoard[it.objectName]
            }

            soundsToPlay.add(nameSound)
            soundsToPlay.add(soundBoard[it.location])
            soundsToPlay.add(soundBoard[it.distance])
        }

        return soundsToPlay.filterNotNull()
    }

    fun isReady(): Boolean
    {
        return this.state == STATE.READY
    }
}










