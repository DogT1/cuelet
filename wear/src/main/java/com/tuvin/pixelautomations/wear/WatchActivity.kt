package com.tuvin.pixelautomations.wear

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** One screen: allow Physical activity, start watching workouts, and send a test to the phone. */
class WatchActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = TextView(this).apply { gravity = Gravity.CENTER; textSize = 13f }
        val test = Button(this).apply {
            text = "Send test"
            setOnClickListener {
                status.text = "Sending…"
                Watch.send(applicationContext, true, "WORKOUT") { n ->
                    runOnUiThread { status.text = if (n > 0) "Test sent to the phone" else "No phone connected" }
                }
            }
        }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 48, 24, 48)
            addView(TextView(this@WatchActivity).apply { text = "Cuelet"; textSize = 16f; gravity = Gravity.CENTER })
            addView(status)
            addView(test)
        }
        setContentView(ScrollView(this).apply { addView(column) })
        if (checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) start()
        else requestPermissions(arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), 1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        if (results.firstOrNull() == PackageManager.PERMISSION_GRANTED) start()
        else status.text = "Allow Physical activity so workouts can be detected"
    }

    private fun start() {
        status.text = "Starting…"
        Watch.register(applicationContext) { ok, error ->
            runOnUiThread { status.text = if (ok) "Watching workouts" else "Couldn't start: $error" }
        }
    }
}
