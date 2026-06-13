package com.example.vitasegura;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import java.util.UUID;

/**
 * Singleton que centraliza la conexión GATT con la pulsera de salud por
 * Bluetooth Low Energy.
 *
 * Conserva la referencia al BluetoothGatt activo para que distintas pantallas y
 * servicios compartan la misma conexión, y expone enviarComandoVibrar() para
 * disparar la vibración háptica de la pulsera. Utiliza el perfil Nordic UART.
 */
public class BluetoothServiceManager {
    private static BluetoothServiceManager instance;
    private BluetoothGatt bluetoothGatt;

    // UUIDs del servicio Nordic UART expuesto por el firmware de la pulsera
    private final String SERVICE_UUID = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E";
    private final String CHAR_RX_UUID = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E";

    private BluetoothServiceManager() {}

    public static synchronized BluetoothServiceManager getInstance() {
        if (instance == null) {
            instance = new BluetoothServiceManager();
        }
        return instance;
    }

    public void setGatt(BluetoothGatt gatt) {
        this.bluetoothGatt = gatt;
    }

    public BluetoothGatt getGatt() {
        return bluetoothGatt;
    }

    /** Envía el comando "VIBRAR" a la pulsera para generar feedback háptico. */
    @SuppressLint("MissingPermission")
    public void enviarComandoVibrar() {
        if (bluetoothGatt != null) {
            BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(SERVICE_UUID));
            if (service != null) {
                BluetoothGattCharacteristic rxChar = service.getCharacteristic(UUID.fromString(CHAR_RX_UUID));
                if (rxChar != null) {
                    rxChar.setValue("VIBRAR");
                    bluetoothGatt.writeCharacteristic(rxChar);
                }
            }
        }
    }
}