package com.example.nfcbrick

import android.content.Context
import android.content.SharedPreferences

/**
 * Central place for all persisted state:
 *  - whether the phone is currently "locked"
 *  - the ID of the NFC tag the user enrolled (so random tags don't trigger it)
 *  - the set of package names to block while locked
 */
object PrefsHelper {

    private const val PREFS_NAME = "nfc_brick_prefs"
    private const val KEY_LOCKED = "is_locked"
    private const val KEY_TAG_ID = "enrolled_tag_id"
    private const val KEY_BLOCKED_APPS = "blocked_apps"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isLocked(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LOCKED, false)

    fun setLocked(context: Context, locked: Boolean) {
        prefs(context).edit().putBoolean(KEY_LOCKED, locked).apply()
    }

    fun getEnrolledTagId(context: Context): String? =
        prefs(context).getString(KEY_TAG_ID, null)

    fun setEnrolledTagId(context: Context, tagId: String) {
        prefs(context).edit().putString(KEY_TAG_ID, tagId).apply()
    }

    fun getBlockedApps(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_BLOCKED_APPS, emptySet()) ?: emptySet()

    fun setBlockedApps(context: Context, packages: Set<String>) {
        prefs(context).edit().putStringSet(KEY_BLOCKED_APPS, packages).apply()
    }
}
