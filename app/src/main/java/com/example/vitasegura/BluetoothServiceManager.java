package com.example.vitasegura;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import java.util.UUID;

public class BluetoothServiceManager {
    private static BluetoothServiceManager instance;
    private BluetoothGatt bluetoothGatt;

    // UUIDs obtenidos de tu app de prueba
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