package com.xinyu.app.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BleManager {

    public interface ScanCallback2 {
        void onDeviceFound(String name, String address, int rssi);
        void onScanFinished();
    }

    public interface HeartRateCallback {
        void onHeartRateReceived(int bpm);
        void onDisconnected();
    }

    private static final UUID HR_SERVICE_UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private static final UUID HR_MEASUREMENT_UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    private static final UUID CCC_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner scanner;
    private BluetoothGatt bluetoothGatt;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isScanning = false;

    private ScanCallback2 scanCallback2;
    private HeartRateCallback heartRateCallback;
    private final List<BluetoothDevice> foundDevices = new ArrayList<>();

    public BleManager(Context context) {
        this.context = context;
        BluetoothManager btManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = btManager != null ? btManager.getAdapter() : null;
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public boolean isBleSupported() {
        return context.getPackageManager().hasSystemFeature(android.content.pm.PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public void startScan(ScanCallback2 callback) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) return;
        scanner = bluetoothAdapter.getBluetoothLeScanner();
        if (scanner == null) return;

        this.scanCallback2 = callback;
        foundDevices.clear();
        isScanning = true;

        scanner.startScan(scanCallback);

        // 10秒后自动停止
        mainHandler.postDelayed(this::stopScan, 10000);
    }

    public void stopScan() {
        if (scanner != null && isScanning) {
            scanner.stopScan(scanCallback);
            isScanning = false;
            if (scanCallback2 != null) scanCallback2.onScanFinished();
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (foundDevices.contains(device)) return;
            foundDevices.add(device);

            String name = device.getName();
            if (name == null || name.isEmpty()) name = "未知设备";

            if (scanCallback2 != null) {
                final String fname = name;
                final String faddr = device.getAddress();
                final int frssi = result.getRssi();
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        scanCallback2.onDeviceFound(fname, faddr, frssi);
                    }
                });
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            isScanning = false;
            if (scanCallback2 != null) {
                mainHandler.post(() -> scanCallback2.onScanFinished());
            }
        }
    };

    public void connectToDevice(String address, HeartRateCallback callback) {
        if (bluetoothAdapter == null) return;
        this.heartRateCallback = callback;

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) return;

        bluetoothGatt = device.connectGatt(context, false, gattCallback);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (heartRateCallback != null) {
                    mainHandler.post(() -> heartRateCallback.onDisconnected());
                }
            } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                // Connection in progress, wait
            } else {
                // Connection failed
                if (heartRateCallback != null) {
                    mainHandler.post(() -> heartRateCallback.onDisconnected());
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) return;

            BluetoothGattService hrService = gatt.getService(HR_SERVICE_UUID);
            if (hrService == null) return;

            BluetoothGattCharacteristic hrChar = hrService.getCharacteristic(HR_MEASUREMENT_UUID);
            if (hrChar == null) return;

            gatt.setCharacteristicNotification(hrChar, true);
            BluetoothGattDescriptor descriptor = hrChar.getDescriptor(CCC_DESCRIPTOR);
            if (descriptor != null) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid().equals(HR_MEASUREMENT_UUID)) {
                int heartRate = parseHeartRate(characteristic);
                if (heartRateCallback != null) {
                    mainHandler.post(() -> heartRateCallback.onHeartRateReceived(heartRate));
                }
            }
        }
    };

    private int parseHeartRate(BluetoothGattCharacteristic characteristic) {
        int flags = characteristic.getValue()[0];
        boolean is16Bit = (flags & 0x01) != 0;
        if (is16Bit) {
            return characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1);
        } else {
            return characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
        }
    }

    public void disconnect() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }
}
