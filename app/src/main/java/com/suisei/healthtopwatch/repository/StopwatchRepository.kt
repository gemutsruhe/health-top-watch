package com.suisei.healthtopwatch.repository

import com.suisei.healthtopwatch.model.PracticeInterval
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class StopwatchRepository @Inject constructor() {

    private val _isRunning = MutableStateFlow(false) // 실행 상태
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _crtTime = MutableStateFlow(0L) // 경과 시간
    val crtTime: StateFlow<Long> = _crtTime.asStateFlow()

    private val _crtSecond = MutableStateFlow(0L)
    val crtSecond = _crtSecond.asStateFlow()

    private val _crtInterval = MutableStateFlow(PracticeInterval(0, 0, 0))
    val crtInterval = _crtInterval.asStateFlow()

    private val _interval = MutableStateFlow(PracticeInterval(2, 30, 10))
    val interval = _interval.asStateFlow()

    private val _lastNotifyTime = MutableStateFlow(0)
    val lastNotifyTime = _lastNotifyTime.asStateFlow()

    private val _isOnStop = MutableStateFlow(false)
    val isOnStop = _isOnStop.asStateFlow()

    val requestSetRunning = MutableSharedFlow<Boolean>()

    fun setIsRunning(state: Boolean) {
        _isRunning.value = state
    }

    fun setCrtTime(time: Long) {
        _crtTime.value = time
    }

    fun setCrtSecond(second: Long) {
        _crtSecond.value = second
    }

    fun setCrtInterval(newInterval: PracticeInterval) {
        _crtInterval.value = newInterval
    }

    fun setInterval(newInterval: PracticeInterval) {
        _interval.value = newInterval
    }

    fun setLastNotifyTime(time: Int) {
        _lastNotifyTime.value = time
    }

    fun setIsOnStop(state: Boolean) {
        _isOnStop.value = state
    }

    /*fun requestSetRunning(state: Boolean) {
        requestSetRunning.
    }*/
}