package com.srikar.screenlock

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

/**
 * Dedicated Quick Settings Tile for 1-tap screen locking from the notification shade / Control Center.
 */
@RequiresApi(Build.VERSION_CODES.N)
class ScreenLockTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        val isRunning = ScreenLockAccessibilityService.isServiceRunning()
        tile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.app_name)
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (ScreenLockAccessibilityService.isServiceRunning()) {
            ScreenLockAccessibilityService.lockScreen(this)
        } else {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    301,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        }
    }
}
