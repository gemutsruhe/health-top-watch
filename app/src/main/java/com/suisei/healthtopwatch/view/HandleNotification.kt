package com.suisei.healthtopwatch.view

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.suisei.healthtopwatch.viewmodel.StopwatchViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Composable
fun HandleRequestAudioFocus(
    stopwatchViewModel: StopwatchViewModel,
    lifecycleScope: CoroutineScope,
    audioManager: AudioManager,
    focusRequest: AudioFocusRequest,
    routedDeviceType: MutableStateFlow<Int>
) {
    LaunchedEffect(Unit) {
        stopwatchViewModel.requestAudioFocus.collect {
            lifecycleScope.launch {
                val sampleRate = 44100
                val bufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack.play()

                routedDeviceType.value = audioTrack.routedDevice.type

                if (routedDeviceType.value == 7 || routedDeviceType.value == 8) {
                    delay(100)
                } else {
                    delay(500)
                }
                audioManager.requestAudioFocus(focusRequest)
            }
        }
    }
}

@Composable
fun HandlePracticeNotification(
    stopwatchViewModel: StopwatchViewModel,
    lifecycleScope: CoroutineScope,
    context: Context,
    audioManager: AudioManager,
    focusRequest: AudioFocusRequest,
    routedDeviceType: StateFlow<Int>,
) {
    val notifySetting by stopwatchViewModel.notifySetting.collectAsState()
    val remainSets by stopwatchViewModel.remainSets.collectAsState()
    LaunchedEffect(Unit) {
        stopwatchViewModel.practiceNotification.collect {
            lifecycleScope.launch {
                if (routedDeviceType.value == 7 || routedDeviceType.value == 8) delay(600)
                else delay(1000)
                if (notifySetting.sound) {

                    val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                    if (remainSets != 1) {
                        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                    } else {
                        //toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 400)
                        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                        delay(100)
                        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                    }
                    audioManager.abandonAudioFocusRequest(focusRequest)
                }

                if (routedDeviceType.value == 7 || routedDeviceType.value == 8) delay(400)
                if (notifySetting.vibration) {
                    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val vibratorManager =
                            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                        vibratorManager.defaultVibrator
                    } else {
                        @Suppress("DEPRECATION")
                        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    }

                    if (remainSets != 1) {
                        vibrator.vibrate(
                            VibrationEffect.createOneShot(
                                200,
                                VibrationEffect.DEFAULT_AMPLITUDE
                            )
                        )
                    } else {
                        val timings = longArrayOf(150, 100, 150)
                        val amplitudes = intArrayOf(
                            VibrationEffect.DEFAULT_AMPLITUDE,
                            0,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                        vibrator.vibrate(
                            VibrationEffect.createWaveform(
                                timings,
                                amplitudes,
                                -1
                            )
                        )
                    }
                }
            }
        }
    }
}