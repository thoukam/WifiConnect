package com.example.wificonnect

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.*
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var wifiManager: WifiManager
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var wifiListView: ListView
    private lateinit var ssidInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var connectButton: Button

    private var selectedSsid: String? = null
    private var scanResults: List<ScanResult> = emptyList()
    private val permissionRequestCode = 101
    private val scanHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wifiListView = findViewById(R.id.wifi_list)
        ssidInput = findViewById(R.id.ssid_input)
        passwordInput = findViewById(R.id.password_input)
        connectButton = findViewById(R.id.connect_button)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(this, "Veuillez activer le WiFi manuellement", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.open_camera_control_button).setOnClickListener {
            val intent = Intent(this, CameraControlActivity::class.java)
            startActivity(intent)
        }

        requestPermissions()
        setupListeners()
    }

    private fun requestPermissions() {
        val permissionsNeeded = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.NEARBY_WIFI_DEVICES,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE
        )

        val permissionsToRequest = permissionsNeeded.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), permissionRequestCode)
        } else {
            scanWifiNetworks()
        }
    }

    private fun setupListeners() {
        wifiListView.setOnItemClickListener { _, _, position, _ ->
            selectedSsid = scanResults[position].SSID
            ssidInput.setText(selectedSsid)
            Toast.makeText(this, "Réseau sélectionné : $selectedSsid", Toast.LENGTH_SHORT).show()
        }

        connectButton.setOnClickListener {
            val ssid = ssidInput.text.toString().trim()
            val password = passwordInput.text.toString()

            if (ssid.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Entrez un nom de réseau et un mot de passe", Toast.LENGTH_SHORT).show()
            } else {
                connectToWifi(ssid, password)
            }
        }
    }

    private fun scanWifiNetworks() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission localisation requise", Toast.LENGTH_SHORT).show()
            return
        }

        val wifiReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                try {
                    unregisterReceiver(this)
                    scanResults = wifiManager.scanResults
                    val ssids = scanResults.map { it.SSID }.filter { it.isNotEmpty() }
                    val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, ssids)
                    wifiListView.adapter = adapter
                } catch (e: SecurityException) {
                    Toast.makeText(this@MainActivity, "Erreur d'accès au scan WiFi : ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        try {
            registerReceiver(wifiReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
            wifiManager.startScan()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Impossible de scanner le WiFi : ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun connectToWifi(ssid: String, password: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission CHANGE_NETWORK_STATE requise", Toast.LENGTH_SHORT).show()
            return
        }

        val specifier = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(password)
            .build()

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(specifier)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                try {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CHANGE_NETWORK_STATE)
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        connectivityManager.bindProcessToNetwork(network)
                    }
                } catch (e: SecurityException) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Erreur de liaison réseau : ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }

                // Optionnel : vérifier SSID
                val currentSsid = try {
                    wifiManager.connectionInfo.ssid
                } catch (e: SecurityException) {
                    null
                }

                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Connecté à $ssid", Toast.LENGTH_SHORT).show()
                    if (currentSsid != null) {
                        Toast.makeText(this@MainActivity, "SSID actuel : $currentSsid", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onUnavailable() {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Échec de la connexion", Toast.LENGTH_SHORT).show()
                }
            }
        }

        try {
            connectivityManager.requestNetwork(request, callback)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Erreur requête réseau : ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                scanWifiNetworks()
            } else {
                Toast.makeText(this, "Les permissions sont nécessaires pour continuer", Toast.LENGTH_LONG).show()
            }
        }
    }
}