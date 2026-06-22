package com.antispy.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class AntiSpyTileService : TileService() {

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateTileState()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
        
        val filter = IntentFilter("com.antispy.app.STATUS_CHANGED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(statusReceiver, filter)
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        try {
            unregisterReceiver(statusReceiver)
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun onClick() {
        super.onClick()

        if (!Settings.canDrawOverlays(this)) {
            // Send user to MainActivity to request permission
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = android.app.PendingIntent.getActivity(
                    this, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
            return
        }

        if (AntiSpyOverlayService.isRunning) {
            val intent = Intent(this, AntiSpyOverlayService::class.java).apply {
                action = AntiSpyOverlayService.ACTION_STOP
            }
            startService(intent)
        } else {
            val intent = Intent(this, AntiSpyOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val active = AntiSpyOverlayService.isRunning
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (active) "Aktif" else "Nonaktif"
        }
        
        tile.updateTile()
    }
}
