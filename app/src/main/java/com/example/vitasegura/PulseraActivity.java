package com.example.vitasegura;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.UUID;

public class PulseraActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private BluetoothDevice targetDevice;
    private BluetoothGatt bluetoothGatt;

    private CardView cvDispositivo;
    private TextView tvNombre, tvMAC, tvMensajeVinculado;
    private Button btnBuscar, btnConectar;
    private ProgressBar progressBar;
    private ImageView ivPulseraLogo;

    private final UUID SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    private final UUID TX_CHAR_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    private final UUID DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pulsera);

        cvDispositivo = findViewById(R.id.cv_dispositivo);
        tvNombre = findViewById(R.id.tv_nombre_dispositivo);
        tvMAC = findViewById(R.id.tv_mac_dispositivo);
        btnBuscar = findViewById(R.id.btn_buscar_pulsera);
        btnConectar = findViewById(R.id.btn_conectar_accion);
        progressBar = findViewById(R.id.pb_buscando);
        tvMensajeVinculado = findViewById(R.id.tv_mensaje_vinculado);
        ivPulseraLogo = findViewById(R.id.iv_pulsera_logo);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        android.bluetooth.BluetoothManager systemBTManager = (android.bluetooth.BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (systemBTManager != null) {
            bluetoothAdapter = systemBTManager.getAdapter();
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                bleScanner = bluetoothAdapter.getBluetoothLeScanner();
            }
        }

        btnBuscar.setOnClickListener(v -> iniciarEscaneo());
        btnConectar.setOnClickListener(v -> conectarDispositivo());
        findViewById(R.id.iv_back_pulsera).setOnClickListener(v -> finish());
    }

    private void iniciarEscaneo() {
        if (bleScanner == null) return;
        cvDispositivo.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        btnBuscar.setText("Buscando...");
        try { bleScanner.startScan(scanCallback); } catch (SecurityException e) { e.printStackTrace(); }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            @SuppressLint("MissingPermission") String name = device.getName();
            if (name != null && name.equals("ESP32-C3-Salud")) {
                targetDevice = device;
                runOnUiThread(() -> {
                    tvNombre.setText(name);
                    tvMAC.setText(device.getAddress());
                    cvDispositivo.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    btnBuscar.setText("Buscar Dispositivos");
                    try { bleScanner.stopScan(scanCallback); } catch (SecurityException e) {}
                });
            }
        }
    };

    private void conectarDispositivo() {
        if (targetDevice == null) return;
        btnConectar.setText("Conectando...");
        try {
            bluetoothGatt = targetDevice.connectGatt(this, false, gattCallback);
        } catch (SecurityException e) { e.printStackTrace(); }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                BluetoothServiceManager.getInstance().setGatt(gatt);
                // Pedir MTU alto para recibir tramas de datos completas
                gatt.requestMtu(512);

                runOnUiThread(() -> {
                    btnConectar.setText("¡Vinculado!");
                    btnConectar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#00BFA5")));
                    tvMensajeVinculado.setVisibility(View.VISIBLE);
                    ivPulseraLogo.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#00BFA5")));
                    cvDispositivo.setVisibility(View.GONE);
                    btnBuscar.setVisibility(View.GONE);
                    Toast.makeText(PulseraActivity.this, "Pulsera conectada", Toast.LENGTH_SHORT).show();
                });
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                BluetoothServiceManager.getInstance().setGatt(null);
                runOnUiThread(() -> {
                    btnConectar.setText("Conectar");
                    btnConectar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#23608C")));
                    tvMensajeVinculado.setVisibility(View.GONE);
                    ivPulseraLogo.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4FC3F7")));
                    btnBuscar.setVisibility(View.VISIBLE);
                });
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            gatt.discoverServices();
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                if (service != null) {
                    BluetoothGattCharacteristic txChar = service.getCharacteristic(TX_CHAR_UUID);
                    if (txChar != null) {
                        gatt.setCharacteristicNotification(txChar, true);
                        BluetoothGattDescriptor desc = txChar.getDescriptor(DESCRIPTOR_UUID);
                        if (desc != null) {
                            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(desc);
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            String data = new String(characteristic.getValue());
            Intent intent = new Intent("DATA_PULSERA_REAL");
            intent.putExtra("valor_raw", data);
            sendBroadcast(intent);
        }
    };
}