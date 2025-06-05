package com.suisei.healthtopwatch.view

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fappslab.tourtip.compose.TourtipLayout
import com.suisei.healthtopwatch.model.NotifyType
import com.suisei.healthtopwatch.model.PipContent
import com.suisei.healthtopwatch.model.isFirstRun
import com.suisei.healthtopwatch.ui.theme.HealthWatchTheme
import com.suisei.healthtopwatch.ui.theme.cancelColor
import com.suisei.healthtopwatch.ui.theme.mainColor
import com.suisei.healthtopwatch.ui.theme.poppinsMediumFont
import com.suisei.healthtopwatch.viewmodel.StopwatchViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale

@Composable
fun HealthWatchScreen() {
    val stopwatchViewModel: StopwatchViewModel = viewModel()
    val crtRatio by stopwatchViewModel.crtInterval.collectAsState()
    val isInPipMode by stopwatchViewModel.isInPipMode.collectAsState()
    val time by stopwatchViewModel.crtTime.collectAsState()
    val lastNotifyTime by stopwatchViewModel.lastNotifyTime.collectAsState()

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

    val routedDeviceType = MutableStateFlow(0)

    HandleRequestAudioFocus(
        stopwatchViewModel,
        lifecycleScope,
        audioManager,
        focusRequest,
        routedDeviceType
    )

    HandlePracticeNotification(
        stopwatchViewModel,
        lifecycleScope,
        context,
        audioManager,
        focusRequest,
        routedDeviceType
    )

    if (isInPipMode) {
        PipView(stopwatchViewModel, progress)
    } else {
        TourtipLayout { controller ->
            LaunchedEffect(Unit) {
                context.isFirstRun.collect {
                    if (it) controller.startTourtip()
                }
            }
            HealthWatchTheme {
                Scaffold(modifier = Modifier) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        StopwatchView(
                            modifier = Modifier.weight(1f),
                            stopwatchViewModel = stopwatchViewModel
                        )

                        ContentBodyView(
                            modifier = Modifier.weight(2f),
                            stopwatchViewModel = stopwatchViewModel,
                            progress = progress
                        )

                        NotifyPreferenceView(
                            modifier = Modifier.weight(1f),
                            stopwatchViewModel = stopwatchViewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PipView(stopwatchViewModel: StopwatchViewModel, progress: Float) {
    val pipContent by stopwatchViewModel.pipContent.collectAsState()
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
}

@Composable
fun StopwatchView(modifier: Modifier, stopwatchViewModel: StopwatchViewModel) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StopwatchContent(stopwatchViewModel)
        ControlButtons(stopwatchViewModel)
    }
}

@Composable
fun ContentBodyView(modifier: Modifier, stopwatchViewModel: StopwatchViewModel, progress: Float) {
    Box(
        modifier.fillMaxWidth(0.9f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CrtRatio(stopwatchViewModel)

            Box(modifier = Modifier, contentAlignment = Alignment.Center) {
                IntervalIndicator(stopwatchViewModel, progress)
                PipButton(Modifier.matchParentSize(), stopwatchViewModel)
            }
        }
    }
}

@Composable
fun NotifyPreferenceView(modifier: Modifier, stopwatchViewModel: StopwatchViewModel) {
    Column(modifier = modifier) {
        NotifyPreferences(stopwatchViewModel)
    }
}

@Composable
fun IntervalIndicator(stopwatchViewModel: StopwatchViewModel, progress: Float) {
    Box(modifier = Modifier) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .align(Alignment.Center)
                .clip(CircleShape),
            color = mainColor,
            strokeWidth = 8.dp,
            trackColor = Color.LightGray
        )
        NotifyRatioSetting(
            stopwatchViewModel, Modifier
                .matchParentSize()
                .align(Alignment.Center)
        )
    }
}

@Composable
fun PipButton(modifier: Modifier, stopwatchViewModel: StopwatchViewModel) {
    Box(modifier = modifier, contentAlignment = Alignment.BottomEnd) {
        TextButton(
            onClick = { stopwatchViewModel.setPip() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .pipGuide(),
            contentPadding = PaddingValues()
        ) {
            Icon(
                Icons.Outlined.PictureInPictureAlt,
                null,
                modifier = Modifier.size(36.dp),
                tint = Color.Black
            )
        }
    }
}

@Composable
fun CrtRatio(stopwatchViewModel: StopwatchViewModel) {
    val crtRatio by stopwatchViewModel.crtInterval.collectAsState()
    Text(
        "${crtRatio.minutes}:${String.format(Locale.getDefault(), "%02d", crtRatio.seconds)}",
        modifier = Modifier.intervalGuide(),
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

    TextButton(onClick = { isOpen = true }, modifier = modifier.intervalDuringGuide()) {
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
fun NotifyPreferences(stopwatchViewModel: StopwatchViewModel) {
    val remainSets by stopwatchViewModel.remainSets.collectAsState()
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .padding(16.dp, 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
            NumberPicker(
                value = remainSets,
                onValueChange = { newVal -> stopwatchViewModel.setRemainSets(newVal) },
                btnChangeValue = 1,
                20
            )
        }
        Row(
            modifier = Modifier.iconGuide(context = context),
            horizontalArrangement = Arrangement.End
        ) {
            val notifySetting by stopwatchViewModel.notifySetting.collectAsState()

            NotifyMethodButton(
                icon = Icons.AutoMirrored.Outlined.VolumeUp,
                notify = notifySetting.sound,
                toggleNotify = { stopwatchViewModel.toggleNotify(NotifyType.SOUND) }
            )
            NotifyMethodButton(icon = Icons.Outlined.Vibration,
                notify = notifySetting.vibration,
                toggleNotify = { stopwatchViewModel.toggleNotify(NotifyType.VIBRATION) })
        }
    }

}

@Composable
fun NotifyMethodButton(
    icon: ImageVector,
    notify: Boolean,
    toggleNotify: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = toggleNotify, modifier = Modifier
            .padding(8.dp)
            .border(
                border = BorderStroke(2.dp, Color.DarkGray),
                shape = CircleShape
            )
    ) {
        Icon(icon, null, modifier = Modifier.size(48.dp))
        if (!notify) Icon(Icons.Outlined.Close, null, modifier = modifier.size(48.dp))
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

