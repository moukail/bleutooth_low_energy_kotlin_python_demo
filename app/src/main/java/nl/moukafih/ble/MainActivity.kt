package nl.moukafih.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.moukafih.ble.ui.theme.AppTheme
import java.util.UUID

class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"
    private val SERVICE_UUID: UUID = UUID.fromString("A07498CA-AD5B-474E-940D-16F1FBE7E8CC")
    private val CHARACTERISTIC_UUID: UUID = UUID.fromString("51FF12BB-3ED8-46E5-B4F9-D64E2FEC021B")

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    private var gatt: BluetoothGatt? = null
    private var selectedDevice: BluetoothDevice? = null
    private var isConnected by mutableStateOf(false)
    private val bleDevices = mutableStateListOf<BluetoothDevice>()
    private val characteristics = mutableStateListOf<BluetoothGattCharacteristic>()
    private val services = mutableStateListOf<BluetoothGattService>()

    private lateinit var navController : NavHostController

    // Request Bluetooth permissions
    private val requestBluetoothPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.BLUETOOTH_CONNECT] == true -> startScan()
            else -> Toast.makeText(this@MainActivity, "Bluetooth permissions are required.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request permissions if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestBluetoothPermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        } else {
            startScan()
        }

        setContent {
            AppTheme {
                navController = rememberNavController()
                NavHost(navController = navController, startDestination = "device_list") {
                    composable("device_list") {
                        DeviceListScreen(navController, bleDevices, ::startScan, ::connectToDevice)
                    }
                    composable("device_details") {
                        DeviceDetailsScreen(navController, selectedDevice, isConnected, services, characteristics, ::disconnect)
                    }
                }
            }
        }
    }

    private fun startScan() {

        bleDevices.clear()

        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(SERVICE_UUID))
                .build()
        )

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)
                result?.device?.let { device ->
                    if (!bleDevices.contains(device)) {
                        bleDevices.add(device)
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                Toast.makeText(this@MainActivity, "Scan failed: $errorCode", Toast.LENGTH_SHORT).show()
            }
        }

        bluetoothAdapter?.bluetoothLeScanner?.startScan(scanCallback)
        //bluetoothAdapter?.bluetoothLeScanner?.startScan(filters, settings, scanCallback)
    }

    private fun connectToDevice(device: BluetoothDevice, navController: NavHostController) {
        selectedDevice = device
        gatt = device.connectGatt(this@MainActivity, false, gattCallback)
        navController.navigate("device_details")
    }

    private fun disconnect() {
        navController.navigateUp()
        characteristics.clear()
        services.clear()
        gatt?.disconnect()
        isConnected = false
    }

    private fun reconnect() {
        selectedDevice?.let { device ->
            CoroutineScope(Dispatchers.IO).launch {
                delay(5000) // Delay before attempting to reconnect
                gatt = device.connectGatt(this@MainActivity, false, gattCallback)
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            Log.w(TAG, "onConnectionStateChange")

            if (newState == BluetoothGatt.STATE_CONNECTED) {
                isConnected = true
                gatt?.discoverServices()
                Log.i(TAG, "Connected to device")
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                isConnected = false
                Log.i(TAG, "Disconnected from device")
                gatt?.close()
                reconnect()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            Log.w(TAG, "onServicesDiscovered")

            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt?.services?.forEach { service ->
                    services.add(service)

                    service.characteristics.forEach { characteristic ->
                        characteristics.add(characteristic)
                    }
                }

                val service = gatt?.getService(SERVICE_UUID)
                val characteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)
                if (characteristic != null) {

                    gatt.setCharacteristicNotification(characteristic, true)
                    // Read characteristic value
                    gatt.readCharacteristic(characteristic)

                    //Log.i(TAG, "Write characteristic value")
                    //gatt.writeCharacteristic(characteristic, "Hello from Android".toByteArray(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                }
            } else {
                Log.w(TAG, "Failed to discover services")
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, value, status)
            Log.w(TAG, "onCharacteristicRead")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val value = characteristic.value?.toString(Charsets.UTF_8)
                Log.i(TAG, "Read characteristic value: $value")
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            Log.w(TAG, "onCharacteristicWrite")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Send characteristic value")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            Log.w(TAG, "onCharacteristicChanged")
            val value = characteristic.value?.toString(Charsets.UTF_8)
            Log.i(TAG, "Characteristic changed: $value")
            Toast.makeText(this@MainActivity, "Public IP: $value", Toast.LENGTH_LONG).show()
        }
    }
}