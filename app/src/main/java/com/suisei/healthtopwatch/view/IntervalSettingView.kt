package com.suisei.healthtopwatch.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.suisei.healthtopwatch.model.PracticeInterval
import com.suisei.healthtopwatch.ui.theme.HealthWatchTheme
import com.suisei.healthtopwatch.ui.theme.mainColor
import com.suisei.healthtopwatch.ui.theme.poppinsMediumFont
import com.suisei.healthtopwatch.viewmodel.StopwatchViewModel
import kotlinx.coroutines.launch

@Composable
fun IntervalSettingDialog(stopwatchViewModel: StopwatchViewModel, closeDialog: () -> Unit) {
    val interval by stopwatchViewModel.interval.collectAsState()

    var tempMinutes by remember { mutableIntStateOf(interval.minutes) }
    var tempSeconds by remember { mutableIntStateOf(interval.seconds) }
    var tempPrepareTime by remember { mutableIntStateOf(interval.prepareTime) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var immediatelyApply by remember { mutableStateOf(false) }

    AlertDialog(onDismissRequest = { closeDialog() },
        confirmButton = {
            TextButton(onClick = {
                if (tempMinutes >= 0 && tempSeconds >= 0 && tempPrepareTime >= 0) {
                    stopwatchViewModel.setInterval(
                        PracticeInterval(
                            tempMinutes,
                            tempSeconds,
                            tempPrepareTime
                        ), immediatelyApply
                    )
                    closeDialog()
                } else {
                    scope.launch {
                        val result = snackbarHostState
                            .showSnackbar(
                                message = "Snackbar",
                                actionLabel = "Action",
                                duration = SnackbarDuration.Short
                            )
                    }
                }

            }, colors = ButtonDefaults.buttonColors().copy(containerColor = mainColor)) {
                Text(
                    "설정",
                    color = Color.White
                )
            }
        },
        dismissButton = {
            //Row(modifier = Modifier.fillMaxWidth()) {


            Row(modifier = Modifier, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.padding(0.dp).padding(0.dp, 0.dp, 20.dp, 0.dp), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(immediatelyApply, { immediatelyApply = !immediatelyApply }, modifier = Modifier.width(28.dp).padding(0.dp, 0.dp, 8.dp, 0.dp))
                    Text("즉시 적용")
                }

                TextButton(onClick = {
                    closeDialog()
                }, border = BorderStroke(2.dp, Color.LightGray)) { Text("취소") }
            }

            //}

        },
        title = { Text("주기 설정") },
        text = {
            Column(verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("주기")
                        Spacer(Modifier.width(4.dp))
                        IntervalSettingTooltip("운동 세트 간격")
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        NumberPicker(
                            value = tempMinutes,
                            onValueChange = { newVal ->
                                tempMinutes = newVal
                            },
                            btnChangeValue = 1,
                            10
                        )
                        NumberPicker(
                            value = tempSeconds,
                            onValueChange = { newVal ->
                                if (newVal >= 0) {
                                    tempMinutes += newVal / 60
                                    tempSeconds = newVal % 60
                                } else {
                                    if (tempMinutes > 0) {
                                        tempMinutes -= 1
                                        tempSeconds = newVal + 60
                                    }
                                }
                            },
                            btnChangeValue = 15,
                            60
                        )
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier.padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("알림")
                        Spacer(Modifier.width(4.dp))
                        IntervalSettingTooltip("운동 시작 n초전 알림")
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    NumberPicker(
                        value = tempPrepareTime,
                        onValueChange = { newVal ->
                            if (newVal < 0) tempPrepareTime = 0
                            else tempPrepareTime = newVal
                        },
                        btnChangeValue = 5,
                        30
                    )
                }
            }
        }
    )
}

@Composable
fun NumberPicker(value: Int, onValueChange: (Int) -> Unit, btnChangeValue: Int, maxVal: Int) {
    val keyboardController = LocalSoftwareKeyboardController.current

    var numberString by remember { mutableStateOf(value.toString()) }

    LaunchedEffect(value) {
        numberString = value.toString()
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = {
            val newValue = (value - btnChangeValue).coerceAtLeast(-60)
            onValueChange(newValue)
        }) { Icon(Icons.Outlined.RemoveCircleOutline, null, tint = Color.Gray) }
        BasicTextField(
            value = numberString,
            onValueChange = {
                if (it == "") {
                    numberString = it
                } else if (it.last() in '0'..'9') {
                    numberString = it
                }

            },
            modifier = Modifier.width(60.dp),
            textStyle = LocalTextStyle.current.copy(
                color = Color.Black,
                fontSize = 24.sp,
                fontFamily = poppinsMediumFont,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = {
                if (numberString != "") onValueChange(numberString.toInt())
                else onValueChange(0)
                keyboardController?.hide()
            }),
            singleLine = true
        )

        IconButton(onClick = {
            val newValue = (value + btnChangeValue).coerceAtMost(maxVal)
            onValueChange(newValue)
        }) { Icon(Icons.Default.AddCircleOutline, null, tint = Color.Gray) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntervalSettingTooltip(tooltip: String) {
    val tooltipState = rememberTooltipState()
    val coroutineScope = rememberCoroutineScope()

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = tooltipState,
        //modifier = Modifier.align(Alignment.CenterVertically)
    ) {
        Icon(
            Icons.Outlined.Info,
            null,
            modifier = Modifier
                .size(14.dp)
                .clickable {
                    coroutineScope.launch {
                        if (tooltipState.isVisible) tooltipState.dismiss()
                        else tooltipState.show()
                    }
                })
    }
}

@Preview
@Composable
fun IntervalSettingDialogPreview() {
    val stopwatchViewModel: StopwatchViewModel = viewModel()
    HealthWatchTheme {
        IntervalSettingDialog(stopwatchViewModel) {

        }
    }
}