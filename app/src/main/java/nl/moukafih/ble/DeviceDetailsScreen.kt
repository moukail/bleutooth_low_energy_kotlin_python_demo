package nl.moukafih.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
fun DeviceDetailsScreen(
    navController: NavHostController,
    device: BluetoothDevice?,
    isConnected: Boolean,
    services: List<BluetoothGattService>,
    characteristics: List<BluetoothGattCharacteristic>,
    disconnect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Device Details", modifier = Modifier.padding(bottom = 16.dp))
        device?.let {
            Text(text = "Name: ${it.name}", modifier = Modifier.padding(bottom = 8.dp))
            Text(text = "Address: ${it.address}", modifier = Modifier.padding(bottom = 8.dp))
        }
        Text(text = if (isConnected) "Status: Connected" else "Status: Disconnected", modifier = Modifier.padding(bottom = 8.dp))
        Text(text = "Services", modifier = Modifier.padding(bottom = 8.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(bottom = 16.dp)
        ) {
            items(services) { service ->
                Text(
                    text = "UUID: ${service.uuid}",
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
        Text(text = "Characteristics", modifier = Modifier.padding(bottom = 8.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(bottom = 16.dp)
        ) {
            items(characteristics) { characteristic ->
                Text(
                    text = "UUID: ${characteristic.uuid}",
                    modifier = Modifier.padding(8.dp)
                )
                Text(
                    text = "Value: ${characteristic.value?.toString(Charsets.UTF_8)}",
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
        Button(onClick = { disconnect() }) {
            Text(text = "Disconnect")
        }
    }
}