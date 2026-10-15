package com.playauction.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.ToneGenerator
import android.media.AudioManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sin

@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var toneGen: ToneGenerator? = null

    init {
        try {
            toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        } catch (_: Exception) {}
    }

    /**
     * Synthesizes the fast slide tone for a placed bid (600Hz -> 1200Hz).
     * Zero-asset WebAudio API synthesizer port.
     */
    fun playBidSound() {
        scope.launch {
            try {
                val sampleRate = 44100
                val durationMs = 120
                val numSamples = (durationMs * sampleRate) / 1000
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val progress = i.toDouble() / numSamples
                    val freq = 600.0 + progress * 600.0
                    val sample = sin(2.0 * Math.PI * freq * (i.toDouble() / sampleRate))
                    val envelope = if (progress < 0.2) progress / 0.2 else (1.0 - progress) / 0.8
                    buffer[i] = (sample * envelope * 20000).toInt().toShort()
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
            } catch (e: Exception) {
                toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
            }
        }
    }

    /**
     * Warning beep for last 3 seconds of timer.
     */
    fun playWarningBeep() {
        scope.launch {
            try {
                toneGen?.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 120)
            } catch (_: Exception) {}
        }
    }

    /**
     * Gavel hammer impact synthesizer.
     */
    fun playGavelSlam(type: String = "SOLD") {
        scope.launch {
            try {
                val sampleRate = 44100
                val durationMs = 250
                val numSamples = (durationMs * sampleRate) / 1000
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val progress = i.toDouble() / numSamples
                    val freq = if (type == "SOLD") 150.0 * (1.0 - progress) else 90.0 * (1.0 - progress)
                    val sample = sin(2.0 * Math.PI * freq * (i.toDouble() / sampleRate))
                    val noise = (Math.random() * 2.0 - 1.0) * 0.3
                    val envelope = (1.0 - progress) * (1.0 - progress)
                    buffer[i] = ((sample + noise) * envelope * 26000).toInt().coerceIn(-32768, 32767).toShort()
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
            } catch (e: Exception) {
                toneGen?.startTone(ToneGenerator.TONE_PROP_PROMPT, 200)
            }
        }
    }
}
