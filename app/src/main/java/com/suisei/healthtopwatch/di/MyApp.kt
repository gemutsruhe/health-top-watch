package com.suisei.healthtopwatch.di

import android.app.Application
import com.suisei.healthtopwatch.repository.StopwatchRepository
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApp : Application() {
    val stopwatchRepository = StopwatchRepository()
}