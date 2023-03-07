package com.example.signalsupervisor3.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.signalsupervisor3.ui.home.BluetoothFragment;

import java.util.ArrayList;
import java.util.List;

public class BluetoothController {
    private static final String TAG = BluetoothController.class.getSimpleName();
    private static final BluetoothAdapter mBluetoothAdapter;


    static {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public static BluetoothAdapter getBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    /**
     * 打开蓝牙
     */
    public static void turnOnBlueTooth(Activity activity, int requestCode) {
        // 若蓝牙未开启
        if (!mBluetoothAdapter.isEnabled()) {
            // 开启请求开启蓝牙的Activity
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(intent, requestCode);
        }
        Log.i(TAG, "蓝牙已开启");
    }

    /**
     * 打开蓝牙可见性
     */
    public static void enableVisibily(Context context) {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        // 设置可见时间为300s
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        context.startActivity(intent);
        Log.i(TAG, "可见性已开启");
    }

    /**
     * 查找设备
     */
    public static void startDiscovery() {
        //关闭正在进行的服务查找
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        //重新开始查找
        mBluetoothAdapter.startDiscovery();
    }

    /**
     * 获取已绑定设备
     */
    public static List<BluetoothDevice> getBondedDeviceList() {
        assert (mBluetoothAdapter != null);
        return new ArrayList<>(mBluetoothAdapter.getBondedDevices());
    }

    public static void stopDiscovery() {
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }
    }
}
