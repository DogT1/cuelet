package com.tuvin.pixelautomations.actions

import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.net.Uri
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import com.tuvin.pixelautomations.engine.Notifications
import com.tuvin.pixelautomations.engine.TAG
import com.tuvin.pixelautomations.engine.launchCatching
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Captures a photo or audio in the background (camera / microphone foreground-service types, so it works from
 * the lock screen) and saves it to the phone's gallery. One task per start; it saves and stops itself.
 */
class CaptureService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val task = intent?.getStringExtra(EXTRA_TASK)
        val type = if (task == TASK_AUDIO) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        else ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
        startForeground(Notifications.CAPTURE_ID, Notifications.capture(this), type)

        scope.launchCatching {
            try {
                when (task) {
                    TASK_PHOTO -> capturePhotos(intent?.getBooleanExtra(EXTRA_FRONT, true) ?: true, intent?.getBooleanExtra(EXTRA_BACK, true) ?: true)
                    TASK_AUDIO -> recordAudio(intent?.getIntExtra(EXTRA_SECONDS, 10) ?: 10)
                }
            } catch (e: Exception) {
                Log.e(TAG, "capture task $task failed", e)
            } finally {
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun capturePhotos(front: Boolean, back: Boolean) {
        var saved = 0
        if (front) runCatching { CameraCapture.capture(this, front = true) }
            .onSuccess { if (saveToGallery(it, "front-${stamp()}.jpg", "image/jpeg", IMAGES, "Pictures/Automations")) saved++ }
            .onFailure { Log.w(TAG, "front capture failed", it) }
        if (back) runCatching { CameraCapture.capture(this, front = false) }
            .onSuccess { if (saveToGallery(it, "back-${stamp()}.jpg", "image/jpeg", IMAGES, "Pictures/Automations")) saved++ }
            .onFailure { Log.w(TAG, "back capture failed", it) }
        check(saved > 0) { "no photo captured" }
        Log.i(TAG, "saved $saved photo(s) to the gallery")
    }

    private suspend fun recordAudio(seconds: Int) {
        val file = File(cacheDir, "audio-${stamp()}.m4a")
        val recorder = MediaRecorder(this).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        try {
            delay(seconds.coerceIn(1, 60) * 1000L)
        } finally {
            runCatching { recorder.stop() }
            recorder.release()
        }
        val ok = saveToGallery(file.readBytes(), file.name, "audio/mp4", AUDIO, "Music/Automations")
        file.delete()
        check(ok) { "couldn't save the recording" }
        Log.i(TAG, "saved recording to the phone")
    }

    /** Writes [bytes] into the shared media store under [relativePath] (visible in Gallery / Files, no permission). */
    private fun saveToGallery(bytes: ByteArray, name: String, mime: String, collection: Uri, relativePath: String): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = contentResolver.insert(collection, values) ?: return false
        contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return false
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        contentResolver.update(uri, values, null, null)
        return true
    }

    private fun stamp() = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val TASK_PHOTO = "photo"
        const val TASK_AUDIO = "audio"
        const val EXTRA_TASK = "task"
        const val EXTRA_FRONT = "front"
        const val EXTRA_BACK = "back"
        const val EXTRA_SECONDS = "seconds"

        private val IMAGES: Uri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        private val AUDIO: Uri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        fun start(ctx: Context, task: String, extras: Intent.() -> Unit = {}) {
            val intent = Intent(ctx, CaptureService::class.java).putExtra(EXTRA_TASK, task).apply(extras)
            ctx.startForegroundService(intent)
        }
    }
}
