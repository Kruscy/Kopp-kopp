package hu.apk.kidsapp.data

import android.app.Activity
import android.app.ActivityManager
import android.content.Context

/**
 * Thin wrapper around Android's built-in Screen Pinning (startLockTask / stopLockTask).
 * Requires no special permissions; on some OEM builds the user must enable
 * "App pinning" once under Settings > Security before it works.
 */
object KioskLock {

    fun isPinned(activity: Activity): Boolean {
        val am = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    }

    fun lock(activity: Activity): Boolean {
        return try {
            if (!isPinned(activity)) {
                activity.startLockTask()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun unlock(activity: Activity) {
        try {
            if (isPinned(activity)) {
                activity.stopLockTask()
            }
        } catch (e: Exception) {
            // Not pinned or not allowed; nothing to do.
        }
    }
}
