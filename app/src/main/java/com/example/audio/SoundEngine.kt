package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class SoundEngine {
    private val sampleRate = 22050
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var currentVolume: Float = 0.75f

    private val whistleSamples by lazy { generateWhistle() }
    private val kickSamples by lazy { generateKick() }
    private val passSamples by lazy { generatePass() }
    private val postSamples by lazy { generatePostClang() }
    private val goalSamples by lazy { generateGoalHornAndCheer() }
    private val tackleSamples by lazy { generateTackle() }
    private val tapSamples by lazy { generateUiTap() }

    fun updateVolume(effectiveVolume: Float) {
        currentVolume = effectiveVolume.coerceIn(0f, 1f)
    }

    fun playWhistle() = playClip(whistleSamples, 0.85f)
    fun playKick(power: Float = 1f) = playClip(kickSamples, (0.6f + power * 0.4f).coerceIn(0.4f, 1f))
    fun playPass() = playClip(passSamples, 0.7f)
    fun playPostClang() = playClip(postSamples, 0.9f)
    fun playGoalHorn() = playClip(goalSamples, 1.0f)
    fun playTackle() = playClip(tackleSamples, 0.75f)
    fun playUiTap() = playClip(tapSamples, 0.5f)

    private fun playClip(samples: ShortArray, clipScale: Float) {
        val gain = (currentVolume * clipScale).coerceIn(0f, 1f)
        if (gain <= 0.01f) return

        scope.launch {
            try {
                val byteCount = samples.size * 2
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                val format = AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
                val track = AudioTrack.Builder()
                    .setAudioAttributes(attrs)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(byteCount)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(samples, 0, samples.size)
                track.setVolume(gain)
                track.setNotificationMarkerPosition(samples.size)
                track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                    override fun onMarkerReached(t: AudioTrack?) {
                        try {
                            t?.stop()
                            t?.release()
                        } catch (_: Throwable) {
                        }
                    }

                    override fun onPeriodicNotification(t: AudioTrack?) {}
                })
                track.play()
            } catch (_: Throwable) {
                // Ignore audio hardware unavailability in headless/test environments
            }
        }
    }

    private fun generateWhistle(): ShortArray {
        val duration = 0.34f
        val count = (sampleRate * duration).toInt()
        return ShortArray(count) { i ->
            val t = i.toFloat() / sampleRate
            val env = when {
                t < 0.02f -> t / 0.02f
                t > duration - 0.05f -> (duration - t) / 0.05f
                else -> 1f
            }.coerceIn(0f, 1f)
            val trill = sin(2.0 * PI * 38.0 * t)
            val freq = 2750.0 + trill * 160.0
            val s = sin(2.0 * PI * freq * t) * 0.7 + sin(2.0 * PI * (freq * 1.03) * t) * 0.3
            (s * env * 0.55 * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun generateKick(): ShortArray {
        val duration = 0.16f
        val count = (sampleRate * duration).toInt()
        val rng = Random(42)
        return ShortArray(count) { i ->
            val t = i.toFloat() / sampleRate
            val freq = 165.0 * exp(-t * 28.0) + 48.0
            val thump = sin(2.0 * PI * freq * t) * exp(-t * 22.0)
            val snap = (rng.nextFloat() * 2f - 1f) * exp(-t * 65.0) * 0.35
            ((thump + snap) * 0.85 * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private fun generatePass(): ShortArray {
        val duration = 0.09f
        val count = (sampleRate * duration).toInt()
        return ShortArray(count) { i ->
            val t = i.toFloat() / sampleRate
            val freq = 220.0 * exp(-t * 35.0) + 80.0
            val thump = sin(2.0 * PI * freq * t) * exp(-t * 34.0)
            (thump * 0.65 * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun generatePostClang(): ShortArray {
        val duration = 0.42f
        val count = (sampleRate * duration).toInt()
        return ShortArray(count) { i ->
            val t = i.toFloat() / sampleRate
            val env = exp(-t * 8.5)
            val ring = sin(2.0 * PI * 680.0 * t) * 0.5 +
                sin(2.0 * PI * 1365.0 * t) * 0.32 +
                sin(2.0 * PI * 2040.0 * t) * 0.18
            (ring * env * 0.75 * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun generateGoalHornAndCheer(): ShortArray {
        val duration = 1.05f
        val count = (sampleRate * duration).toInt()
        val rng = Random(99)
        var lastNoise = 0f
        return ShortArray(count) { i ->
            val t = i.toFloat() / sampleRate
            val env = when {
                t < 0.05f -> t / 0.05f
                t > duration - 0.25f -> (duration - t) / 0.25f
                else -> 1f
            }.coerceIn(0f, 1f)
            val horn = (sin(2.0 * PI * 220.0 * t) +
                0.65 * sin(2.0 * PI * 277.18 * t) +
                0.55 * sin(2.0 * PI * 329.63 * t)) * 0.35
            // Low-pass filtered crowd roar noise
            val raw = rng.nextFloat() * 2f - 1f
            lastNoise = lastNoise * 0.84f + raw * 0.16f
            val mix = (horn + lastNoise * 0.55) * env
            (mix * 0.75 * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private fun generateTackle(): ShortArray {
        val duration = 0.14f
        val count = (sampleRate * duration).toInt()
        val rng = Random(7)
        var lp = 0f
        return ShortArray(count) { i ->
            val t = i.toFloat() / sampleRate
            val env = exp(-t * 18.0)
            val raw = rng.nextFloat() * 2f - 1f
            lp = lp * 0.72f + raw * 0.28f
            (lp * env * 0.7 * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun generateUiTap(): ShortArray {
        val duration = 0.045f
        val count = (sampleRate * duration).toInt()
        return ShortArray(count) { i ->
            val t = i.toFloat() / sampleRate
            val env = exp(-t * 60.0)
            val tone = sin(2.0 * PI * 880.0 * t) * env
            (tone * 0.45 * Short.MAX_VALUE).toInt().toShort()
        }
    }

    fun release() {
        scope.cancel()
    }
}
