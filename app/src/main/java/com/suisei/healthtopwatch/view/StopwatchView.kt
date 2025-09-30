package com.suisei.healthtopwatch.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suisei.healthtopwatch.ui.theme.mainColor
import com.suisei.healthtopwatch.viewmodel.StopwatchViewModel
import kotlin.math.abs

fun formatElapsed(ms: Long, isInPipMode: Boolean): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds / 60) % 60
    val seconds = totalSeconds % 60
    val millis = ms % 1000
    return if (ms < 0) "-%02d.%03d".format(abs(seconds), abs(millis))
    else if (isInPipMode) "%02d:%02d:%02d".format(hours, minutes, seconds, millis)
    else "%02d:%02d:%02d.%03d".format(hours, minutes, seconds, millis)
}

@Composable
fun StopwatchContent(viewModel: StopwatchViewModel) {
    val elapsed by viewModel.crtTime.collectAsState()
    val isInPipMode by viewModel.isInPipMode.collectAsState()
    val timeString = remember(elapsed) { formatElapsed(elapsed, isInPipMode) }

    Text(
        text = timeString,
        fontSize = if (!isInPipMode) 36.sp else 26.sp,
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}


@Composable
fun LinearRatioIndicator(stopwatchViewModel: StopwatchViewModel, progress: Float) {
    val remainSets by stopwatchViewModel.remainSets.collectAsState()
    Column(modifier = Modifier.fillMaxWidth(), Arrangement.Center, Alignment.CenterHorizontally) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(8.dp),
            color = mainColor,
            trackColor = Color.LightGray
        )
        Box(modifier = Modifier.fillMaxWidth(0.8f), contentAlignment = Alignment.BottomCenter) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                //Sets
                Row {
                    Text(
                        "$remainSets",
                        modifier = Modifier.padding(0.dp, 0.dp, 6.dp, 0.dp),
                        fontSize = 18.sp
                    )
                    Text("Set", fontSize = 18.sp)
                }

                CrtRatio(stopwatchViewModel)
            }

        }
    }
}