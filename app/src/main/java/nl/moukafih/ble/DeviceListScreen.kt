package nl.moukafih.ble

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun DeviceListScreen(
    navController: NavHostController,
    bleDevices: List<BluetoothDevice>,
    startScan: () -> Unit,
    connectToDevice: (BluetoothDevice, NavHostController) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "BLE Devices", modifier = Modifier.padding(bottom = 16.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .padding(bottom = 16.dp)
        ) {
            items(bleDevices) { device ->
                Text(
                    text = device.name ?: "Unknown Device",
                    modifier = Modifier
                        .clickable { connectToDevice(device, navController) }
                        .padding(8.dp)
                )
                Text(text = device.address)
            }
        }
        Button(onClick = { startScan() }) {
            Text(text = "Scan for Devices")
        }
    }
}