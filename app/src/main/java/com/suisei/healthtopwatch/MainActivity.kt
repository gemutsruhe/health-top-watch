package com.suisei.healthtopwatch

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.suisei.healthtopwatch.ui.theme.HealthWatchTheme
import com.suisei.healthtopwatch.view.HealthWatchScreen
import com.suisei.healthtopwatch.viewmodel.StopwatchViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var stopwatchViewModel: StopwatchViewModel
    private val context = this

    private val changeIconRes = R.drawable.change_circle_24dp_1f1f1f_fill0_wght400_grad0_opsz24
    private var toggleIconRes = R.drawable.pause_24dp_1f1f1f_fill1_wght400_grad0_opsz24
    private var isInPipMode = false

    lateinit var changeContentString: String
    lateinit var toggleRunningString: String
    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action) {
                changeContentString -> stopwatchViewModel.changePipContent()
                toggleRunningString -> stopwatchViewModel.toggleStopwatch()
            }
        }
    }

    @SuppressLint("BatteryLife", "SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val packageName = applicationContext.packageName
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager

        val isWhitelisted = pm.isIgnoringBatteryOptimizations(packageName)

        if(!isWhitelisted) {
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }

        changeContentString = getString(R.string.change_pip_content_action)
        toggleRunningString = getString(R.string.toggle_running_action)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()

        stopwatchViewModel = ViewModelProvider(this)[StopwatchViewModel::class.java]
        lifecycleScope.launch {
            stopwatchViewModel.pipMode.collect { pipModeState ->
                if (pipModeState) {
                    enterPipMode()
                }
            }
        }

        lifecycleScope.launch {
            stopwatchViewModel.isRunning.collect { isRunning ->
                run {
                    toggleIconRes =
                        if (isRunning) R.drawable.pause_24dp_1f1f1f_fill1_wght400_grad0_opsz24 else R.drawable.play_arrow_24dp_1f1f1f_fill1_wght400_grad0_opsz24
                    setPipParams()
                }
            }
        }

        setContent {
            HealthWatchTheme {
                HealthWatchScreen()
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)

        stopwatchViewModel.setPipMode(isInPictureInPictureMode)
        isInPipMode = isInPictureInPictureMode
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(pipReceiver)
    }

    private fun getPipParams(): PictureInPictureParams {
        val changeContentAction = getRemoteAction(changeContentString, Icon.createWithResource(context, changeIconRes))
        val toggleRunningAction = getRemoteAction(toggleRunningString, Icon.createWithResource(context, toggleIconRes))

        registerReceiver(changeContentString)
        registerReceiver(toggleRunningString)

        val pipParams = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setActions(listOf(toggleRunningAction, changeContentAction)) // 여기 버튼이 표시됨
            .build()

        return pipParams
    }

    private fun enterPipMode() {
        val pipParams = getPipParams()
        enterPictureInPictureMode(pipParams)
    }

    private fun setPipParams() {
        val pipParams = getPipParams()
        setPictureInPictureParams(pipParams)
    }

    private fun getRemoteAction(action: String, icon: Icon): RemoteAction {
        val intent = PendingIntent.getBroadcast(
            context, toggleIconRes,
            Intent(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remoteAction = RemoteAction(
            icon,
            "Show UI",
            "Show main UI",
            intent
        )

        return remoteAction
    }

    private fun registerReceiver(action: String) {
        val filter = IntentFilter(action)
        ContextCompat.registerReceiver(context, pipReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
    }
}