package com.tuvin.pixelautomations.actions

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Takes a single JPEG from a camera with no preview, for the Capture Photo action. */
object CameraCapture {
    /** [front] = true picks the front (selfie) camera, false the back. Returns JPEG bytes, or throws. */
    @Suppress("MissingPermission")
    suspend fun capture(ctx: Context, front: Boolean): ByteArray {
        val manager = ctx.getSystemService(CameraManager::class.java)
        val facing = if (front) CameraMetadata.LENS_FACING_FRONT else CameraMetadata.LENS_FACING_BACK
        val cameraId = manager.cameraIdList.firstOrNull {
            manager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == facing
        } ?: error("no ${if (front) "front" else "back"} camera")

        val thread = HandlerThread("capture").apply { start() }
        val handler = Handler(thread.looper)
        val reader = ImageReader.newInstance(1280, 960, ImageFormat.JPEG, 1)
        try {
            return withTimeout(15_000) {
                val device = openCamera(manager, cameraId, handler)
                try {
                    captureStill(device, reader, handler)
                } finally {
                    device.close()
                }
            }
        } finally {
            reader.close()
            thread.quitSafely()
        }
    }

    @Suppress("MissingPermission")
    private suspend fun openCamera(manager: CameraManager, id: String, handler: Handler): CameraDevice =
        suspendCancellableCoroutine { cont ->
            manager.openCamera(id, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) { if (cont.isActive) cont.resume(camera) }
                override fun onDisconnected(camera: CameraDevice) { camera.close() }
                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    if (cont.isActive) cont.resumeWithException(IllegalStateException("camera error $error"))
                }
            }, handler)
        }

    private suspend fun captureStill(device: CameraDevice, reader: ImageReader, handler: Handler): ByteArray =
        suspendCancellableCoroutine { cont ->
            reader.setOnImageAvailableListener({ r ->
                val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
                try {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
                    if (cont.isActive) cont.resume(bytes)
                } finally {
                    image.close()
                }
            }, handler)

            @Suppress("DEPRECATION")
            device.createCaptureSession(listOf(reader.surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    val request = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                        addTarget(reader.surface)
                    }
                    // A couple of frames of auto-exposure settle time, then capture.
                    handler.postDelayed({ runCatching { session.capture(request.build(), null, handler) } }, 350)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    if (cont.isActive) cont.resumeWithException(IllegalStateException("capture session failed"))
                }
            }, handler)
        }
}
