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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.suisei.healthtopwatch.model.NotifyType
import com.suisei.healthtopwatch.model.PipContent
import com.suisei.healthtopwatch.ui.theme.HealthWatchTheme
import com.suisei.healthtopwatch.ui.theme.cancelColor
import com.suisei.healthtopwatch.ui.theme.mainColor
import com.suisei.healthtopwatch.ui.theme.poppinsMediumFont
import com.suisei.healthtopwatch.viewmodel.StopwatchViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun HealthWatchScreen() {
    val stopwatchViewModel: StopwatchViewModel = viewModel()
    val crtRatio by stopwatchViewModel.crtInterval.collectAsState()
    val isInPipMode by stopwatchViewModel.isInPipMode.collectAsState()
    val time by stopwatchViewModel.crtTime.collectAsState()
    val lastNotifyTime by stopwatchViewModel.lastNotifyTime.collectAsState()
    val pipContent by stopwatchViewModel.pipContent.collectAsState()

    val progress =
        (time / 100 - (lastNotifyTime * 10)) / ((if (crtRatio.toSeconds() != 0) crtRatio.toSeconds() else 1).toFloat() * 10)

    val context = LocalContext.current
    val lifecycleScope = rememberCoroutineScope()

    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    val focusRequest =
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            ).setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener { }
            .build()

    var routedDeviceType by remember { mutableIntStateOf(0) }

    val remainSets by stopwatchViewModel.remainSets.collectAsState()

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

                routedDeviceType = audioTrack.routedDevice.type

                if (routedDeviceType == 7 || routedDeviceType == 8) {
                    delay(100)
                } else {
                    delay(500)
                }
                audioManager.requestAudioFocus(focusRequest)
            }
        }
    }

    LaunchedEffect(Unit) {
        stopwatchViewModel.practiceNotification.collect {
            lifecycleScope.launch {
                if (routedDeviceType == 7 || routedDeviceType == 8) delay(600)
                else delay(1000)
                if (stopwatchViewModel.notifySetting.value.sound) {

                    val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                    if(remainSets != 1) {
                        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                    } else {
                        //toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 400)
                        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                        delay(100)
                        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                    }
                    audioManager.abandonAudioFocusRequest(focusRequest)
                }

                if (routedDeviceType == 7 || routedDeviceType == 8) delay(400)
                if (stopwatchViewModel.notifySetting.value.vibration) {
                    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val vibratorManager =
                            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                        vibratorManager.defaultVibrator
                    } else {
                        @Suppress("DEPRECATION")
                        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    }

                    if(remainSets != 1) {
                        vibrator.vibrate(
                            VibrationEffect.createOneShot(
                                200,
                                VibrationEffect.DEFAULT_AMPLITUDE
                            )
                        )
                    } else {
                        val timings = longArrayOf(0, 150, 100, 150)
                        //val amplitudes = intArrayOf(VibrationEffect.DEFAULT_AMPLITUDE, VibrationEffect.DEFAULT_AMPLITUDE)
                        vibrator.vibrate(
                            VibrationEffect.createWaveform(
                                timings,
                                -1
                            )
                        )
                    }
                }
            }
        }
    }

    if (isInPipMode) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.height(72.dp), contentAlignment = Alignment.BottomCenter) {
                when (pipContent) {
                    PipContent.RunTime -> StopwatchContent(stopwatchViewModel)
                    PipContent.Interval -> NotifyRatioSetting(stopwatchViewModel, Modifier)
                }
            }

            LinearRatioIndicator(stopwatchViewModel, progress)
        }
    } else {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        StopwatchContent(stopwatchViewModel)
                        ControlButtons(stopwatchViewModel)
                    }

                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxWidth(0.9f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularRatioIndicator(stopwatchViewModel, progress)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        NotifyPreferences(stopwatchViewModel, innerPadding)
                    }
                }
            }
        }
    }
}

@Composable
fun CrtRatio(stopwatchViewModel: StopwatchViewModel) {
    val crtRatio by stopwatchViewModel.crtInterval.collectAsState()
    Text(
        "${crtRatio.minutes}:${String.format(Locale.getDefault(), "%02d", crtRatio.seconds)}",
        fontSize = 18.sp
    )
}

@Composable
fun ControlButtons(stopwatchViewModel: StopwatchViewModel) {
    val isRunning by stopwatchViewModel.isRunning.collectAsState()

    Row(modifier = Modifier.fillMaxWidth(0.7f), horizontalArrangement = Arrangement.SpaceBetween) {
        when (isRunning) {
            false -> {
                StopwatchControlBtn(
                    onClick = { stopwatchViewModel.resetStopwatch() },
                    "Reset",
                    ButtonDefaults.outlinedButtonColors(),
                    BorderStroke(2.dp, Color.LightGray)
                )
                StopwatchControlBtn(
                    onClick = { stopwatchViewModel.startStopwatch() },
                    "Start",
                    ButtonDefaults.buttonColors(
                        containerColor = mainColor, contentColor = Color.White
                    ),
                    icon = Icons.Filled.PlayArrow,
                    contentColor = Color.White
                )
            }

            true -> {
                Spacer(modifier = Modifier.width(0.dp))
                StopwatchControlBtn(
                    onClick = { stopwatchViewModel.stopStopwatch() },
                    "Stop",
                    ButtonDefaults.buttonColors(
                        containerColor = cancelColor, contentColor = Color.White
                    ),
                    icon = Icons.Filled.Stop,
                    contentColor = Color.White
                )
            }
        }

    }
}

@Composable
fun StopwatchControlBtn(
    onClick: () -> Unit,
    info: String,
    buttonColors: ButtonColors,
    borderStroke: BorderStroke? = null,
    icon: ImageVector? = null,
    contentColor: Color = Color.Black
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.size(112.dp, 56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = buttonColors,
        border = borderStroke,
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, modifier = Modifier.size(28.dp), tint = contentColor)
                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(info, fontSize = 20.sp, color = contentColor, fontFamily = poppinsMediumFont)
        }
    }
}

@Composable
fun NotifyRatioSetting(stopwatchViewModel: StopwatchViewModel, modifier: Modifier) {
    var isOpen by remember { mutableStateOf(false) }

    val time by stopwatchViewModel.crtTime.collectAsState()
    val lastNotifyTime by stopwatchViewModel.lastNotifyTime.collectAsState()
    val isInPipMode by stopwatchViewModel.isInPipMode.collectAsState()

    val during = time / 1000 - lastNotifyTime

    TextButton(onClick = { isOpen = true }, modifier = modifier) {
        Text(
            "${during / 60}:${String.format(Locale.getDefault(), "%02d", during % 60)}",
            fontSize = if (isInPipMode) 44.sp else 60.sp,
            textAlign = TextAlign.Center,
            fontFamily = poppinsMediumFont
        )
    }

    when {
        isOpen -> {
            IntervalSettingDialog(stopwatchViewModel, closeDialog = { isOpen = false })
        }
    }
}

@Composable
fun NotifyPreferences(stopwatchViewModel: StopwatchViewModel, innerPadding: PaddingValues) {
    val remainSets by stopwatchViewModel.remainSets.collectAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row {
            //var remainSetsText by remember { mutableStateOf(remainSets.toString()) } // 입력 텍스트
            //val keyboardController = LocalSoftwareKeyboardController.current

            NumberPicker(value = remainSets, onValueChange = { newVal -> stopwatchViewModel.setRemainSets(newVal) }, btnChangeValue = 1, 20)
        }
        Row(
            horizontalArrangement = Arrangement.End
        ) {
            val notifySetting by stopwatchViewModel.notifySetting.collectAsState()

            NotifyMethodButton(icon = Icons.AutoMirrored.Outlined.VolumeUp,
                notify = notifySetting.sound,
                toggleNotify = { stopwatchViewModel.toggleNotify(NotifyType.SOUND) })
            NotifyMethodButton(icon = Icons.Outlined.Vibration,
                notify = notifySetting.vibration,
                toggleNotify = { stopwatchViewModel.toggleNotify(NotifyType.VIBRATION) })
        }
    }

}

@Composable
fun NotifyMethodButton(icon: ImageVector, notify: Boolean, toggleNotify: () -> Unit) {
    IconButton(
        onClick = toggleNotify, modifier = Modifier
            .padding(8.dp)
            .border(
                border = BorderStroke(2.dp, Color.DarkGray),
                shape = CircleShape
            )
    ) {
        Icon(icon, null, modifier = Modifier.size(48.dp))
        if (!notify) Icon(Icons.Outlined.Close, null, modifier = Modifier.size(48.dp))
    }

}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    HealthWatchTheme {
        HealthWatchScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun NotifyRatioSettingPreview() {
    HealthWatchTheme {
        val stopwatchViewModel: StopwatchViewModel = viewModel()
        IntervalSettingDialog(stopwatchViewModel) { }
    }
}

@Preview(showBackground = true)
@Composable
fun PipPreview() {
    HealthWatchTheme {
        val stopwatchViewModel: StopwatchViewModel = viewModel()
        val progress = 0.5f
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NotifyRatioSetting(stopwatchViewModel, Modifier)
            LinearRatioIndicator(stopwatchViewModel, progress)
        }
    }
}

