package com.suisei.healthtopwatch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.suisei.healthtopwatch.di.MyApp
import com.suisei.healthtopwatch.repository.StopwatchRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class StopwatchService : Service() {

    private var startTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var notificationManager: NotificationManager
    @Inject
    lateinit var stopwatchRepository: StopwatchRepository
    val service = this
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate() {
        super.onCreate()
        Log.e("TEST", "StopwatchService onCreate")
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        startTime = SystemClock.elapsedRealtime()

        CoroutineScope(Dispatchers.Main).launch {
            stopwatchRepository.crtSecond.collect { time ->

                if(stopwatchRepository.isOnStop.value) {
                    val lastNotifyTime = stopwatchRepository.lastNotifyTime.value
                    val crtRatio = stopwatchRepository.crtInterval.value

                    val hours = time / 3600
                    val minutes = (time / 60) % 60
                    val seconds = time % 60
                    val timeText = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                    val noti = createNotification(timeText, crtRatio.toSeconds(), time.toInt(), lastNotifyTime, true)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startForeground(NOTIFICATION_ID, noti, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                    }
                }


            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            stopwatchRepository.requestSetRunning.collect { state ->
                if(!state) {
                    val lastNotifyTime = stopwatchRepository.lastNotifyTime.value
                    val crtRatio = stopwatchRepository.crtInterval.value

                    val time = stopwatchRepository.crtSecond.value
                    val hours = time / 3600
                    val minutes = (time / 60) % 60
                    val seconds = time % 60
                    val timeText = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                    val noti = createNotification(timeText, crtRatio.toSeconds(), time.toInt(), lastNotifyTime, false)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startForeground(NOTIFICATION_ID, noti, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                    }
                }
            }
        }
    }

    private fun createNotification(timeText: String, max: Int, crtTime: Int, lastNotifyTime: Int, isRunning: Boolean): Notification {
        val channelId = "stopwatch_channel"
        val channel = NotificationChannel(
            channelId,
            "Stopwatch 알림",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)


        var buttonPendingIntent: PendingIntent
        var buttonTitle: String
        if(isRunning) {
            buttonTitle = "일시 정지"
            val stopIntent = Intent(this, StopwatchService::class.java).apply {
                action = "ACTION_STOP"
            }
            buttonPendingIntent = PendingIntent.getService(
                this, 0, stopIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            buttonTitle = "시작"
            val startIntent = Intent(this, StopwatchService::class.java).apply {
                action = "ACTION_START"
            }
            buttonPendingIntent = PendingIntent.getService(
                this, 0, startIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }


        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("스톱워치 실행 중")
            .setContentText("경과 시간: $timeText")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setProgress(max, crtTime - lastNotifyTime, false)
            .addAction(R.drawable.pause_24dp_1f1f1f_fill1_wght400_grad0_opsz24, buttonTitle, buttonPendingIntent)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_STOP" -> {
                Log.e("TEST", "ACTION_STOP")
                //stopwatchRepository.setIsRunning(false)
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    stopwatchRepository.requestSetRunning.emit(false)
                }
            }
            "ACTION_START" -> {
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    stopwatchRepository.requestSetRunning.emit(true)
                }
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1
    }
}