package com.poozh.translator

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast

/**
 * Invisible activity whose sole purpose is to host the system's
 * MediaProjection permission dialog. It has no UI and finishes itself the
 * moment the dialog is dismissed.
 *
 * The manifest gives this activity a dedicated task affinity. Removing that
 * isolated task after the permission result reveals the app the user was
 * actually using, instead of briefly resuming the translator's MainActivity.
 */
class CapturePermissionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
        // Android restores the pending activity result after a configuration
        // change. Starting another permission intent would show two dialogs.
        if (savedInstanceState == null) requestScreenCapture()
    }

    private fun requestScreenCapture() {
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            projectionManager.createScreenCaptureIntent(
                android.media.projection.MediaProjectionConfig.createConfigForDefaultDisplay()
            )
        } else {
            projectionManager.createScreenCaptureIntent()
        }
        startActivityForResult(intent, REQUEST_MEDIA_PROJECTION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK && data != null) {
                val serviceIntent = Intent(this, FloatingTranslatorService::class.java)
                    .setAction(FloatingTranslatorService.ACTION_CAPTURE_RESULT)
                    .putExtra(FloatingTranslatorService.EXTRA_RESULT_CODE, resultCode)
                    .putExtra(FloatingTranslatorService.EXTRA_RESULT_DATA, data)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
                Toast.makeText(this, "屏幕捕获已授权", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "未获得屏幕捕获权限", Toast.LENGTH_SHORT).show()
            }
            finishAndRemoveTask()
            overridePendingTransition(0, 0)
        }
    }

    companion object {
        private const val REQUEST_MEDIA_PROJECTION = 1001
    }
}
