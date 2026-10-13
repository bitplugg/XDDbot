package xdbot.bitplugg.ru.ble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import xdbot.bitplugg.ru.MainActivity
import xdbot.bitplugg.ru.R
import xdbot.bitplugg.ru.XddbotApp

/**
 * Keeps the BLE link alive while the app is in the background and exposes
 * lock/unlock from the notification shade.
 */
class ScooterService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val client get() = XddbotApp.instance.bleClient

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForegroundCompat(buildNotification())
        scope.launch {
            combine(client.state, client.status) { _, _ -> Unit }.collect {
                if (client.state.value == ConnState.IDLE) {
                    stopSelf()
                } else {
                    notify(buildNotification())
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_UNLOCK -> client.unlock()
            ACTION_LOCK -> client.lock()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val state = client.state.value
        val status = client.status.value
        val title = when (state) {
            ConnState.READY -> getString(R.string.notification_connected)
            ConnState.CONNECTING, ConnState.DISCOVERING, ConnState.AUTHENTICATING ->
                getString(R.string.notification_connecting)
            else -> getString(R.string.app_name)
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_xddbot)
            .setContentTitle(title)
            .setContentText(status)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .addAction(0, getString(R.string.action_unlock), action(ACTION_UNLOCK))
            .addAction(0, getString(R.string.action_lock), action(ACTION_LOCK))
            .build()
    }

    private fun action(action: String): PendingIntent = PendingIntent.getService(
        this,
        action.hashCode(),
        Intent(this, ScooterService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun notify(notification: Notification) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "xddbot_connection"
        const val NOTIFICATION_ID = 1001
        const val ACTION_UNLOCK = "xdbot.bitplugg.ru.UNLOCK"
        const val ACTION_LOCK = "xdbot.bitplugg.ru.LOCK"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, ScooterService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScooterService::class.java))
        }
    }
}
