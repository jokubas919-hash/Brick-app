package com.example.nfcbrick

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * Watches which app comes to the foreground. If the phone is "locked" and the
 * foreground app is in the blocked list, immediately send the user back to
 * the home screen. This runs as long as Accessibility permission is granted,
 * independent of whether MainActivity is open.
 */
class LockAccessibilityService : AccessibilityService() {

    // Always allow these so the user isn't fully bricked out of essentials.
    private val alwaysAllowed = setOf(
        "com.example.nfcbrick",
        "com.android.systemui"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return

        if (!PrefsHelper.isLocked(applicationContext)) return
        if (pkg in alwaysAllowed) return

        val blocked = PrefsHelper.getBlockedApps(applicationContext)
        // Empty blocked list = "block everything except launcher/allowed" mode.
        val shouldBlock = if (blocked.isEmpty()) true else pkg in blocked

        if (shouldBlock) {
            goHome()
        }
    }

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
    }

    override fun onInterrupt() {}
}
