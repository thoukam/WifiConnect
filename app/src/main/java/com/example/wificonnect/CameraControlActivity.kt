package com.example.wificonnect

import android.content.Context
import android.os.*
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

class CameraControlActivity : AppCompatActivity() {

    private lateinit var infoTextView: TextView
    private lateinit var modeButton: Button
    private lateinit var photoButton: Button
    private lateinit var videoButton: Button

    private var currentMode = "image"
    private var isRecording = false
    private var lastBatteryLevel = 100

    private val baseUrl = "http://192.168.1.1/osc"
    private val handler = Handler(Looper.getMainLooper())
    private val refreshInterval = 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_control)

        infoTextView = findViewById(R.id.info_text)
        modeButton = findViewById(R.id.mode_toggle_button)
        photoButton = findViewById(R.id.take_picture_button)
        videoButton = findViewById(R.id.video_control_button)

        modeButton.setOnClickListener { toggleMode() }
        photoButton.setOnClickListener { takePicture() }
        videoButton.setOnClickListener { handleVideo() }

        startCameraStatusUpdates()
        getFileList()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    private fun startCameraStatusUpdates() {
        handler.post(object : Runnable {
            override fun run() {
                getCameraState()
                handler.postDelayed(this, refreshInterval)
            }
        })
    }

    private fun getCameraState() {
        Thread {
            try {
                val url = URL("$baseUrl/state")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 3000
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)

                val battery = (json.getJSONObject("state").optDouble("batteryLevel", -1.0) * 100).toInt()
                val status = json.getJSONObject("state").optString("_captureStatus", "unknown")
                val uptime = json.getJSONObject("state").optLong("_uptime", 0)

                runOnUiThread {
                    infoTextView.text = "Batterie : $battery%\nÉtat : $status\nUptime : ${uptime}s"

                    if (battery < 30 && battery != lastBatteryLevel) {
                        infoTextView.setTextColor(getColor(android.R.color.holo_red_dark))
                        Toast.makeText(this, "⚠ Batterie faible ($battery%)", Toast.LENGTH_LONG).show()
                        vibrate()
                    } else {
                        infoTextView.setTextColor(getColor(android.R.color.black))
                    }

                    lastBatteryLevel = battery
                }

            } catch (e: Exception) {
                Log.e("CameraControl", "Erreur état caméra", e)
                runOnUiThread {
                    infoTextView.text = "Erreur de connexion à la caméra"
                }
            }
        }.start()
    }

    private fun vibrate() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }

    private fun toggleMode() {
        val newMode = if (currentMode == "image") "video" else "image"
        val options = JSONObject().put("captureMode", newMode)
        val body = JSONObject().put("name", "camera.setOptions")
            .put("parameters", JSONObject().put("options", options))

        sendCommand(body) {
            currentMode = newMode
            runOnUiThread {
                Toast.makeText(this, "Mode changé en $newMode", Toast.LENGTH_SHORT).show()
                updateModeUI()
            }
        }
    }

    private fun takePicture() {
        if (currentMode != "image") {
            Toast.makeText(this, "Changer en mode photo d'abord", Toast.LENGTH_SHORT).show()
            return
        }

        val body = JSONObject().put("name", "camera.takePicture")
        sendCommand(body) {
            runOnUiThread {
                Toast.makeText(this, "Photo prise !", Toast.LENGTH_SHORT).show()
                getFileList()
            }
        }
    }

    private fun handleVideo() {
        if (currentMode != "video") {
            Toast.makeText(this, "Changer en mode vidéo d'abord", Toast.LENGTH_SHORT).show()
            return
        }

        val command = if (!isRecording) "camera.startCapture" else "camera.stopCapture"
        val body = JSONObject().put("name", command)

        sendCommand(body) {
            runOnUiThread {
                isRecording = !isRecording
                videoButton.text = if (isRecording) "Arrêter l'enregistrement" else "Démarrer l'enregistrement"
                val msg = if (isRecording) "Enregistrement démarré" else "Enregistrement arrêté"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                if (!isRecording) getFileList()
            }
        }
    }

    private fun getFileList() {
        val body = JSONObject().apply {
            put("name", "camera.listFiles")
            put("parameters", JSONObject().apply {
                put("fileType", "all")
                put("entryCount", 20)
                put("maxThumbSize", 640)
            })
        }

        sendCommand(body) { response ->
            val items = mutableListOf<GalleryItem>()
            try {
                val entries = response.getJSONObject("results").getJSONArray("entries")
                for (i in 0 until entries.length()) {
                    val file = entries.getJSONObject(i)
                    val url = file.getString("fileUrl")
                    val name = file.getString("name")
                    val type = file.optString("fileType", "image") // ✅ cette ligne est essentielle
                    items.add(GalleryItem(url, type, name))
                }
            } catch (e: Exception) {
                Log.e("Gallery", "Erreur parsing fichiers", e)
            }

            runOnUiThread {
                val recycler = findViewById<RecyclerView>(R.id.gallery_recycler_view)
                recycler.layoutManager = GridLayoutManager(this, 3)
                recycler.adapter = GalleryAdapter(this, items)
            }
        }
    }

    private fun updateModeUI() {
        modeButton.text = if (currentMode == "image") "Passer en mode Vidéo" else "Passer en mode Photo"
        photoButton.isEnabled = currentMode == "image"
        videoButton.isEnabled = currentMode == "video"
    }

    private fun sendCommand(jsonBody: JSONObject, callback: (JSONObject) -> Unit) {
        Thread {
            try {
                val url = URL("$baseUrl/commands/execute")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.doOutput = true

                val out = OutputStreamWriter(conn.outputStream)
                out.write(jsonBody.toString())
                out.flush()

                val responseText = conn.inputStream.bufferedReader().readText()
                val response = JSONObject(responseText)
                callback(response)
            } catch (e: Exception) {
                Log.e("CameraControl", "Erreur API Theta", e)
                runOnUiThread {
                    Toast.makeText(this, "Erreur de communication avec la caméra", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}