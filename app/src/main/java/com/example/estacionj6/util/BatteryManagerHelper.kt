package com.example.estacionj6.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

data class BatteryInfo(
    val level: Int,
    val isCharging: Boolean
)

object BatteryManagerHelper {
    fun getBatteryInfo(context: Context): BatteryInfo {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, intentFilter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level != -1 && scale > 0) {
            ((level / scale.toFloat()) * 100).toInt()
        } else {
            -1
        }

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        return BatteryInfo(level = batteryPct, isCharging = isCharging)
    }
}
