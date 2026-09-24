package com.example.nfcbrick

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.nfcbrick.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        binding.btnOpenAccessibilitySettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.btnClearTag.setOnClickListener {
            PrefsHelper.setEnrolledTagId(this, "")
            PrefsHelper.setLocked(this, false)
            refreshUi()
            Toast.makeText(this, "Enrolled tag cleared. Tap a new tag to enroll it.", Toast.LENGTH_SHORT).show()
        }

        binding.btnPickApps.setOnClickListener {
            showAppPicker()
        }

        refreshUi()
        handleNfcIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Foreground dispatch so we intercept NFC taps while this activity is on screen too.
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, pendingIntentFlags)
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
        refreshUi()
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNfcIntent(intent)
    }

    /**
     * Core logic:
     *  - No tag enrolled yet -> enroll whatever tag was just tapped.
     *  - Tag enrolled and it matches -> toggle lock state.
     *  - Tag enrolled and it does NOT match -> ignore (wrong tag).
     */
    private fun handleNfcIntent(intent: Intent) {
        val action = intent.action ?: return
        if (action != NfcAdapter.ACTION_NDEF_DISCOVERED &&
            action != NfcAdapter.ACTION_TECH_DISCOVERED &&
            action != NfcAdapter.ACTION_TAG_DISCOVERED
        ) return

        val tag: Tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        } ?: return

        val tagId = bytesToHex(tag.id)
        val enrolled = PrefsHelper.getEnrolledTagId(this)

        when {
            enrolled.isNullOrEmpty() -> {
                PrefsHelper.setEnrolledTagId(this, tagId)
                Toast.makeText(this, "Tag enrolled! Tap it again anytime to lock/unlock.", Toast.LENGTH_LONG).show()
            }
            enrolled == tagId -> {
                val newState = !PrefsHelper.isLocked(this)
                PrefsHelper.setLocked(this, newState)
                Toast.makeText(
                    this,
                    if (newState) "🔒 Locked" else "🔓 Unlocked",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> {
                Toast.makeText(this, "This isn't your enrolled tag — ignored.", Toast.LENGTH_SHORT).show()
            }
        }
        refreshUi()
    }

    private fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02X".format(it) }

    private fun refreshUi() {
        val enrolled = PrefsHelper.getEnrolledTagId(this)
        val locked = PrefsHelper.isLocked(this)

        binding.tvTagStatus.text = if (enrolled.isNullOrEmpty())
            "No tag enrolled yet. Tap any NFC tag to enroll it."
        else
            "Tag enrolled: $enrolled"

        binding.tvLockStatus.text = if (locked) "Status: 🔒 LOCKED" else "Status: 🔓 Unlocked"

        val blocked = PrefsHelper.getBlockedApps(this)
        binding.tvBlockedApps.text = if (blocked.isEmpty())
            "Blocked apps: (none selected — locks everything except launcher)"
        else
            "Blocked apps: ${blocked.joinToString(", ") { pkgLabel(it) }}"

        binding.tvAccessibilityHint.text =
            "Don't forget: enable \"NFC Brick\" under Settings > Accessibility for the lock to actually work."
    }

    private fun pkgLabel(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        pkg
    }

    private fun showAppPicker() {
        val pm = packageManager
        val launchableApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .filter { it.packageName != packageName }
            .sortedBy { pm.getApplicationLabel(it).toString().lowercase() }

        val labels = launchableApps.map { pm.getApplicationLabel(it).toString() }.toTypedArray()
        val packages = launchableApps.map { it.packageName }
        val currentlyBlocked = PrefsHelper.getBlockedApps(this)
        val checked = packages.map { it in currentlyBlocked }.toBooleanArray()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Choose apps to block")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton("Save") { _, _ ->
                val newBlocked = packages.filterIndexed { index, _ -> checked[index] }.toSet()
                PrefsHelper.setBlockedApps(this, newBlocked)
                refreshUi()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
