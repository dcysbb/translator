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
 * Key behaviour: after delivering the result to the service, it calls
 * [moveTaskToBack] instead of [finish] so the user is returned to whatever app
 * they were in before the re-auth was triggered — NOT the translator's main UI.
 */
class CapturePermissionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestScreenCapture()
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
            // Send the whole task to the background so the user lands back in
            // the app they were using (e.g. the browser/book they're reading),
            // not in our main UI. The floating overlay stays on top regardless.
            moveTaskToBack(true)
            finish()
        }
    }

    companion object {
        private const val REQUEST_MEDIA_PROJECTION = 1001
    }
}
