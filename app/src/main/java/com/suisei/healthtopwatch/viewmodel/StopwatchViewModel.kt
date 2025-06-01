package com.suisei.healthtopwatch.viewmodel

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suisei.healthtopwatch.model.NotifySettingState
import com.suisei.healthtopwatch.model.NotifyType
import com.suisei.healthtopwatch.model.PipContent
import com.suisei.healthtopwatch.model.PracticeInterval
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StopwatchViewModel : ViewModel() {

    private val _crtTime = MutableStateFlow(0L) // 경과 시간
    val crtTime: StateFlow<Long> = _crtTime.asStateFlow()

    private val _isRunning = MutableStateFlow(false) // 실행 상태
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _crtInterval = MutableStateFlow(PracticeInterval(0, 0, 0))
    val crtInterval = _crtInterval.asStateFlow()

    private val _interval = MutableStateFlow(PracticeInterval(2, 30, 15))
    val interval = _interval.asStateFlow()

    private val _lastNotifyTime = MutableStateFlow(0)
    val lastNotifyTime = _lastNotifyTime.asStateFlow()

    private var crtSecond = 0L

    private val _pipMode = MutableStateFlow(false)
    val pipMode = _pipMode.asStateFlow()

    private val _isInPipMode = MutableStateFlow(false)
    val isInPipMode = _isInPipMode.asStateFlow()

    private val _notifySetting = MutableStateFlow(NotifySettingState())
    val notifySetting = _notifySetting.asStateFlow()

    private val _remainSets = MutableStateFlow(0)
    val remainSets = _remainSets.asStateFlow()

    private var baseTime = 0L
    private var chronometerJob: Job? = null

    private val _pipContent = MutableStateFlow(PipContent.RunTime)
    val pipContent = _pipContent.asStateFlow()

    val requestAudioFocus = MutableSharedFlow<Unit>()
    val practiceNotification = MutableSharedFlow<Unit>()

    fun toggleStopwatch() {
        when(isRunning.value) {
            true -> stopStopwatch()
            false -> startStopwatch()
        }
    }

    fun startStopwatch() {
        if (!_isRunning.value) {
            _isRunning.value = true
            baseTime = SystemClock.elapsedRealtime() - _crtTime.value
            if (crtTime.value == 0L) _crtInterval.value = interval.value.copy()

            chronometerJob = viewModelScope.launch {
                while (true) {
                    _crtTime.value = SystemClock.elapsedRealtime() - baseTime
                    delay(10)

                    if (crtTime.value / 1000 != crtSecond) {

                        crtSecond = crtTime.value / 1000
                        val notifyTime = lastNotifyTime.value + crtInterval.value.toSeconds()
                        Log.e("TEST", "crtTime: ${crtTime.value}, notifyTime: $notifyTime")
                        if(crtSecond.toInt() == notifyTime - crtInterval.value.prepareTime - 1) {
                            requestAudioFocus()
                            notifyPrepare()
                            Log.e("TEST", "notifyPrepare")
                        }
                        if (crtSecond.toInt() == notifyTime) {
                            Log.e("TEST", "newInterval")
                            _lastNotifyTime.value = notifyTime
                            _crtInterval.value = interval.value.copy()
                            if(remainSets.value > 0) _remainSets.value -= 1
                        }
                    }
                }
            }
        }
    }

    fun stopStopwatch() {
        if (_isRunning.value) {
            _crtTime.value = SystemClock.elapsedRealtime() - baseTime
            _isRunning.value = false
            chronometerJob?.cancel()
        }
    }

    fun resetStopwatch() {
        _crtTime.value = 0
        _lastNotifyTime.value = 0
        _isRunning.value = false
        _crtInterval.value = PracticeInterval()
        chronometerJob?.cancel()
    }

    fun setInterval(newInterval: PracticeInterval, immediatelyApply: Boolean) {
        if(immediatelyApply) _crtInterval.value = newInterval.copy()
        _interval.value = newInterval.copy()
    }

    private fun requestAudioFocus() {
        viewModelScope.launch {
            if(notifySetting.value.sound) requestAudioFocus.emit(Unit)
        }
    }

    private fun notifyPrepare() {
        viewModelScope.launch {
            practiceNotification.emit(Unit)
        }
    }

    fun setPip() {
        _pipMode.value = true
        _pipMode.value = false
    }

    fun setPipMode(isInPipMode: Boolean) {
        _isInPipMode.value = isInPipMode
    }

    fun toggleNotify(type: NotifyType) {
        _notifySetting.update { current ->
            when (type) {
                NotifyType.SOUND -> current.copy(sound = !current.sound)
                NotifyType.VIBRATION -> current.copy(vibration = !current.vibration)
            }
        }
    }

    fun setRemainSets(newSets: Int) {
        _remainSets.value = newSets
    }

    fun changePipContent() {
        when(pipContent.value) {
            PipContent.RunTime -> _pipContent.value = PipContent.Interval
            PipContent.Interval -> _pipContent.value = PipContent.RunTime
        }
    }
}