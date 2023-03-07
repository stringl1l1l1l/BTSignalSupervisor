package com.example.signalsupervisor3.bluetooth.connect;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 监听连接申请的线程
 */
public class AcceptThread extends Thread {
    private static final String NAME = "BlueToothClass";
    private static final UUID MY_UUID = UUID.fromString(Constant.CONNECTTION_UUID);

    private final BluetoothServerSocket mmServerSocket;
    private final BluetoothAdapter mBluetoothAdapter;
    private final Handler mHandler;
    private BluetoothSocket mBluetoothSocket;

    public AcceptThread(BluetoothAdapter adapter, Handler handler) {
        // 使用一个临时对象，该对象稍后被分配给mmServerSocket，因为mmServerSocket是最终的
        mBluetoothAdapter = adapter;
        mHandler = handler;
        BluetoothServerSocket tmp = null;
        try {
            // MY_UUID是应用程序的UUID，客户端代码使用相同的UUID
            tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
        } catch (IOException e) {
        }
        mmServerSocket = tmp;
    }

    public BluetoothServerSocket getMmServerSocket() {
        return mmServerSocket;
    }

    public void run() {
        //持续监听，直到出现异常或返回socket
        while (true) {
            try {
                mHandler.sendEmptyMessage(Constant.MSG_START_LISTENING);
                mBluetoothSocket = mmServerSocket.accept();
            } catch (IOException e) {
                mHandler.sendMessage(mHandler.obtainMessage(Constant.MSG_ERROR, e));
            }
            // 如果一个连接被接受
            if (mBluetoothSocket != null) {
                // 在单独的线程中完成管理连接的工作
                manageConnectedSocket(mBluetoothSocket);
            }
        }
    }

    private void manageConnectedSocket(BluetoothSocket socket) {
        //只支持同时处理一个连接
//        if (mConnectedThread != null) {
//            mConnectedThread.cancel();
//        }
        Message message = mHandler.obtainMessage(Constant.MSG_GOT_A_CLINET, this);
        mHandler.sendMessage(message);
    }

    /**
     * 取消监听socket，使此线程关闭
     */
    public void cancel() {
        try {
//            if (mmServerSocket != null)
//                mmServerSocket.close();
            if (mBluetoothSocket != null)
                mBluetoothSocket.close();
        } catch (IOException e) {
        }
    }

    public BluetoothSocket getBluetoothSocket() {
        return mBluetoothSocket;
    }

    public void setBluetoothSocket(BluetoothSocket bluetoothSocket) {
        mBluetoothSocket = bluetoothSocket;
    }
}