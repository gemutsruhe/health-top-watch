package com.suisei.healthtopwatch.viewmodel

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suisei.healthtopwatch.model.NotifySettingState
import com.suisei.healthtopwatch.model.NotifyType
import com.suisei.healthtopwatch.model.PipContent
import com.suisei.healthtopwatch.model.PracticeInterval
import com.suisei.healthtopwatch.repository.StopwatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StopwatchViewModel @Inject constructor(
    private val repository: StopwatchRepository
) : ViewModel() {

    val crtTime = repository.crtTime
    val isRunning = repository.isRunning
    val crtInterval = repository.crtInterval
    val interval = repository.interval
    val lastNotifyTime = repository.lastNotifyTime
    val requestSetRunning = repository.requestSetRunning

    private val crtSecond = repository.crtSecond

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
    val startNotification = MutableSharedFlow<Unit>()

    fun toggleStopwatch() {
        when (isRunning.value) {
            true -> stopStopwatch()
            false -> startStopwatch()
        }
    }

    fun startStopwatch() {
        if (!isRunning.value) {
            repository.setIsRunning(true)

            if (crtTime.value == 0L) {
                repository.setCrtInterval(interval.value.copy())
                repository.setCrtTime(-interval.value.prepareTime.toLong())
                repository.setLastNotifyTime(-crtInterval.value.toSeconds())
            }

            if(baseTime == 0L) baseTime = SystemClock.elapsedRealtime() + 10 * 1000// + if(crtTime.value > 0L) crtTime.value else 0
            else baseTime = SystemClock.elapsedRealtime() - crtTime.value

            chronometerJob = viewModelScope.launch {
                while (true) {
                    repository.setCrtTime(SystemClock.elapsedRealtime() - baseTime)
                    delay(10)

                    if (crtTime.value / 1000 != crtSecond.value) {

                        //crtSecond = crtTime.value / 1000
                        repository.setCrtSecond(crtTime.value / 1000)
                        val notifyTime = lastNotifyTime.value + crtInterval.value.toSeconds()
                        //Log.e("TEST", notifyTime.toString())
                        if (crtSecond.value.toInt() == notifyTime - crtInterval.value.prepareTime - 1) {
                            requestAudioFocus()
                            notifyPrepare()
                        }
                        if (crtSecond.value.toInt() == notifyTime - 1) {
                            requestAudioFocus()
                            notifyStart()
                        }
                        if (crtSecond.value.toInt() == notifyTime) {
                            repository.setLastNotifyTime(notifyTime)
                            repository.setCrtInterval(interval.value.copy())
                            if (remainSets.value > 0) _remainSets.value -= 1
                        }
                    }
                }
            }
        }
    }

    fun stopStopwatch() {
        if (isRunning.value) {
            Log.e("TEST", "${SystemClock.elapsedRealtime()}, $baseTime")
            repository.setCrtTime(SystemClock.elapsedRealtime() - baseTime)
            repository.setIsRunning(false)
            chronometerJob?.cancel()
        }
    }

    fun resetStopwatch() {
        baseTime = 0L
        repository.setCrtTime(0)
        repository.setLastNotifyTime(0)
        repository.setIsRunning(false)
        repository.setCrtInterval(PracticeInterval())
        chronometerJob?.cancel()
    }

    fun setInterval(newInterval: PracticeInterval, immediatelyApply: Boolean) {
        if (immediatelyApply) repository.setCrtInterval(newInterval.copy())
        repository.setInterval(newInterval.copy())
    }

    private fun requestAudioFocus() {
        viewModelScope.launch {
            if (notifySetting.value.sound) requestAudioFocus.emit(Unit)
        }
    }

    private fun notifyPrepare() {
        viewModelScope.launch {
            practiceNotification.emit(Unit)
        }
    }

    private fun notifyStart() {
        viewModelScope.launch {
            startNotification.emit(Unit)
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
        when (pipContent.value) {
            PipContent.RunTime -> _pipContent.value = PipContent.Interval
            PipContent.Interval -> _pipContent.value = PipContent.RunTime
        }
    }

    fun setIsOnStop(state: Boolean) {
        repository.setIsOnStop(state)
    }
}