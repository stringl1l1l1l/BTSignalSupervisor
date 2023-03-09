package com.example.signalsupervisor3.bluetooth.connect;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.example.signalsupervisor3.bluetooth.BluetoothController;
import com.example.signalsupervisor3.bluetooth.connect.ConnectedThread;
import com.example.signalsupervisor3.bluetooth.connect.Constant;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.UUID;


public class ConnectThread extends Thread {
    private static final UUID MY_UUID = UUID.fromString(Constant.CONNECTTION_UUID);
    private static final String TAG = ConnectThread.class.getSimpleName();
    private final Handler mHandler;
    private BluetoothSocket mBluetoothSocket;
    public boolean isStopped = false;

    public ConnectThread(BluetoothDevice device, Handler handler) {
        // U将一个临时对象分配给mmSocket，因为mmSocket是最终的
        BluetoothSocket tmp = null;
        mHandler = handler;
        // 用BluetoothSocket连接到给定的蓝牙设备
        try {
            // MY_UUID是应用程序的UUID，客户端代码使用相同的UUID
            tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            Log.e(TAG, "error", e);
        }
        mBluetoothSocket = tmp;

    }

    public void run() {
        //搜索占用资源大，关掉提高速度
        BluetoothController.stopDiscovery();
        try {
            if (mBluetoothSocket.isConnected())
                cancel();
            // 通过socket连接设备，阻塞运行直到成功或抛出异常时
            mBluetoothSocket.connect();
            // 这行很重要，开启远程连接
            mBluetoothSocket.getRemoteDevice();
        } catch (Exception connectException) {
            mHandler.sendMessage(mHandler.obtainMessage(Constant.MSG_ERROR, connectException));
            // 如果无法连接则关闭socket并退出
            try {
                mBluetoothSocket.close();
            } catch (IOException closeException) {
                Log.e(TAG, "error", closeException);
            }
            return;
        }
        // 在单独的线程中完成管理连接的工作
        manageConnectedSocket(mBluetoothSocket);
    }

    public BluetoothSocket getSocket() {
        return mBluetoothSocket;
    }

    private void manageConnectedSocket(BluetoothSocket mmSocket) {
        mHandler.sendEmptyMessage(Constant.MSG_CONNECTED_TO_SERVER);
    }

    public void pause() {
        synchronized (this) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void resumeThread() {
        synchronized (this) {
            this.notify();
        }
    }

    /**
     * 取消正在进行的连接并关闭socket
     */
    public void cancel() {
        try {
            mBluetoothSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "error: " + e.toString());
        }
    }

}