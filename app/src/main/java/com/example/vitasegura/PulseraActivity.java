package com.example.vitasegura;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.*;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.ArrayList;
import java.util.List;
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

    // --- CONFIGURACIÓN DE TUS MAC ---
    private final String MAC_PULSERA_1 = "1C:DB:D4:C6:4F:5A";
    private final String MAC_PULSERA_2 = "88:56:A6:5C:2E:E6";

    // Listas para manejar los múltiples hallazgos
    private List<BluetoothDevice> dispositivosEncontrados = new ArrayList<>();
    private List<String> nombresParaMostrar = new ArrayList<>();

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

        dispositivosEncontrados.clear();
        nombresParaMostrar.clear();
        cvDispositivo.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        btnBuscar.setText("Buscando...");

        try {
            bleScanner.startScan(scanCallback);
            // Esperamos 4 segundos para captar ambos dispositivos antes de mostrar la lista
            new Handler().postDelayed(this::detenerEscaneoYMostrarOpciones, 4000);
        } catch (SecurityException e) { e.printStackTrace(); }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String address = device.getAddress();

            if (address.equalsIgnoreCase(MAC_PULSERA_1) || address.equalsIgnoreCase(MAC_PULSERA_2)) {
                if (!dispositivosEncontrados.contains(device)) {
                    dispositivosEncontrados.add(device);
                    @SuppressLint("MissingPermission") String name = device.getName();
                    if (name == null && result.getScanRecord() != null) {
                        name = result.getScanRecord().getDeviceName();
                    }
                    nombresParaMostrar.add((name != null ? name : "Pulsera Vita") + "\n" + address);
                }
            }
        }
    };

    private void detenerEscaneoYMostrarOpciones() {
        try { bleScanner.stopScan(scanCallback); } catch (SecurityException e) {}

        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            btnBuscar.setText("Buscar Dispositivos");

            if (dispositivosEncontrados.isEmpty()) {
                Toast.makeText(this, "No se encontraron tus pulseras", Toast.LENGTH_SHORT).show();
            } else {
                // Mostramos el diálogo para elegir entre las encontradas
                mostrarDialogoSeleccion();
            }
        });
    }

    private void mostrarDialogoSeleccion() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Selecciona tu dispositivo");

        String[] items = nombresParaMostrar.toArray(new String[0]);
        builder.setItems(items, (dialog, which) -> {
            // Al elegir una, llenamos el CardView con sus datos
            targetDevice = dispositivosEncontrados.get(which);
            @SuppressLint("MissingPermission") String name = targetDevice.getName();
            tvNombre.setText(name != null ? name : "Pulsera Vita");
            tvMAC.setText(targetDevice.getAddress());
            cvDispositivo.setVisibility(View.VISIBLE);
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

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