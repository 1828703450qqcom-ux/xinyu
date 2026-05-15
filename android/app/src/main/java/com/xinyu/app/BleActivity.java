package com.xinyu.app;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xinyu.app.bluetooth.BleManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BleActivity extends AppCompatActivity {

    private TextView tvHeartRate, tvConnectionStatus, tvEmpty, btnScan, tvHeartIcon, tvScanHint;
    private RecyclerView deviceList;
    private BleManager bleManager;
    private DeviceAdapter deviceAdapter;
    private boolean isScanning = false;
    private BluetoothAdapter bluetoothAdapter;
    private Handler scanHandler = new Handler(Looper.getMainLooper());

    private static final int PERMISSION_REQUEST_BT = 2001;

    // Classic Bluetooth discovery receiver
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    String name = device.getName();
                    if (name == null || name.isEmpty()) name = "未知设备";
                    String address = device.getAddress();
                    int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) 0);
                    deviceAdapter.addDevice(name, address, String.valueOf(rssi), "经典蓝牙");
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                isScanning = false;
                btnScan.setText("开始扫描");
                tvScanHint.setVisibility(View.GONE);
                if (deviceAdapter.getItemCount() == 0) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("未发现设备，请确保附近有开启的蓝牙设备");
                    deviceList.setVisibility(View.GONE);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);

        initViews();
        initBle();
    }

    private void initViews() {
        tvHeartRate = findViewById(R.id.tv_heart_rate);
        tvConnectionStatus = findViewById(R.id.tv_connection_status);
        tvEmpty = findViewById(R.id.tv_empty);
        tvHeartIcon = findViewById(R.id.tv_heart_icon);
        btnScan = findViewById(R.id.btn_scan);
        deviceList = findViewById(R.id.device_list);
        tvScanHint = findViewById(R.id.tv_scan_hint);

        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { finish(); }
        });

        deviceAdapter = new DeviceAdapter();
        deviceList.setLayoutManager(new LinearLayoutManager(this));
        deviceList.setAdapter(deviceAdapter);

        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isScanning) {
                    stopScan();
                } else {
                    startScan();
                }
            }
        });
    }

    private void initBle() {
        bleManager = new BleManager(this);
        BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = btManager != null ? btManager.getAdapter() : null;

        if (!bleManager.isBleSupported()) {
            Toast.makeText(this, "设备不支持蓝牙BLE", Toast.LENGTH_SHORT).show();
        }

        if (!bleManager.isBluetoothEnabled()) {
            Toast.makeText(this, "请开启蓝牙", Toast.LENGTH_SHORT).show();
        }

        // Register receiver for classic Bluetooth discovery
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bluetoothReceiver, filter);
    }

    private void startScan() {
        if (!checkBluetoothPermissions()) return;

        if (!bleManager.isBluetoothEnabled()) {
            Toast.makeText(this, "请先开启蓝牙", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check location permission for BLE scan on Android 6-11
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_REQUEST_BT);
                return;
            }
        }

        // Check location services enabled
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager != null && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Toast.makeText(this, "建议开启定位以提高蓝牙扫描效果", Toast.LENGTH_LONG).show();
        }

        deviceAdapter.clear();
        tvEmpty.setVisibility(View.GONE);
        deviceList.setVisibility(View.VISIBLE);
        isScanning = true;
        btnScan.setText("停止扫描");
        tvScanHint.setVisibility(View.VISIBLE);
        tvScanHint.setText("正在扫描附近蓝牙设备...");

        // First add already paired/bonded devices
        addPairedDevices();

        // Start BLE scan
        bleManager.startScan(new BleManager.ScanCallback2() {
            @Override
            public void onDeviceFound(String name, String address, int rssi) {
                deviceAdapter.addDevice(name, address, String.valueOf(rssi), "BLE");
            }

            @Override
            public void onScanFinished() {
                // After BLE scan, also start classic Bluetooth discovery
                if (bluetoothAdapter != null && !bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.startDiscovery();
                } else {
                    isScanning = false;
                    btnScan.setText("开始扫描");
                    tvScanHint.setVisibility(View.GONE);
                    if (deviceAdapter.getItemCount() == 0) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText("未发现设备，请确保附近有开启的蓝牙设备");
                        deviceList.setVisibility(View.GONE);
                    }
                }
            }
        });

        // Auto stop after 15 seconds
        scanHandler.postDelayed(() -> {
            if (isScanning) {
                stopScan();
            }
        }, 15000);
    }

    private void addPairedDevices() {
        if (bluetoothAdapter == null) return;
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices != null) {
            for (BluetoothDevice device : pairedDevices) {
                String name = device.getName();
                if (name == null || name.isEmpty()) name = "未知设备";
                deviceAdapter.addDevice(name, device.getAddress(), "已配对", "已配对");
            }
        }
    }

    private void stopScan() {
        if (isScanning) {
            bleManager.stopScan();
            if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            isScanning = false;
            btnScan.setText("开始扫描");
            tvScanHint.setVisibility(View.GONE);
            if (deviceAdapter.getItemCount() == 0) {
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("未发现设备，请确保附近有开启的蓝牙设备");
                deviceList.setVisibility(View.GONE);
            }
        }
    }

    private void connectDevice(String name, String address) {
        // Check BLUETOOTH_CONNECT permission before connecting (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        PERMISSION_REQUEST_BT);
                Toast.makeText(this, "需要蓝牙连接权限", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        tvConnectionStatus.setText("正在连接 " + name + " ...");
        tvHeartIcon.setText("⏳");

        bleManager.connectToDevice(address, new BleManager.HeartRateCallback() {
            @Override
            public void onHeartRateReceived(int bpm) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvHeartRate.setText(String.valueOf(bpm));
                        tvConnectionStatus.setText("已连接: " + name);
                        tvHeartIcon.setText("❤️");

                        // Color based on heart rate
                        if (bpm < 60) {
                            tvHeartRate.setTextColor(getResources().getColor(R.color.accent, null));
                        } else if (bpm <= 100) {
                            tvHeartRate.setTextColor(getResources().getColor(R.color.primary, null));
                        } else {
                            tvHeartRate.setTextColor(getResources().getColor(R.color.accent, null));
                        }
                    }
                });
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvHeartRate.setText("--");
                        tvConnectionStatus.setText("连接失败或已断开（请确保设备支持心率功能）");
                        tvHeartIcon.setText("💔");
                    }
                });
            }
        });

        Toast.makeText(this, "正在连接 " + name, Toast.LENGTH_SHORT).show();
    }

    private boolean checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this,
                            Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.ACCESS_FINE_LOCATION
                        },
                        PERMISSION_REQUEST_BT);
                return false;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.BLUETOOTH,
                                Manifest.permission.BLUETOOTH_ADMIN,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        },
                        PERMISSION_REQUEST_BT);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_BT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "蓝牙权限已授予", Toast.LENGTH_SHORT).show();
                // Auto start scan after permission granted
                startScan();
            } else {
                Toast.makeText(this, "需要蓝牙权限才能扫描设备", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isScanning) {
            bleManager.stopScan();
            if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
        }
        bleManager.disconnect();
        unregisterReceiver(bluetoothReceiver);
        scanHandler.removeCallbacksAndMessages(null);
    }

    // Device Adapter
    private class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {
        private final List<String[]> devices = new ArrayList<>(); // {name, address, rssi, type}

        void addDevice(String name, String address, String rssi, String type) {
            // Check duplicate
            for (String[] d : devices) {
                if (d[1].equals(address)) return;
            }
            devices.add(new String[]{name, address, rssi, type});
            notifyDataSetChanged();
        }

        void clear() {
            devices.clear();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_ble_device, parent, false);
            return new DeviceViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
            String[] device = devices.get(position);
            holder.tvName.setText(device[0]);
            holder.tvAddress.setText(device[1]);
            String typeInfo = device[3] != null ? device[3] : "";
            if ("已配对".equals(device[2])) {
                holder.tvRssi.setText("已配对 · " + typeInfo);
            } else {
                holder.tvRssi.setText(device[2] + "dBm · " + typeInfo);
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stopScan();
                    connectDevice(device[0], device[1]);
                }
            });
        }

        @Override
        public int getItemCount() {
            return devices.size();
        }

        class DeviceViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvAddress, tvRssi;

            DeviceViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_device_name);
                tvAddress = itemView.findViewById(R.id.tv_device_address);
                tvRssi = itemView.findViewById(R.id.tv_rssi);
            }
        }
    }
}
