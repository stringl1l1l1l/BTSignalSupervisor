package com.example.signalsupervisor3.bluetooth.connect;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.signalsupervisor3.GlobalData;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ConnectedThread extends Thread {
    private static final String TAG = ConnectedThread.class.getSimpleName();
    private final BluetoothSocket mmSocket;
    private final DataInputStream mmInStream;
    private final OutputStream mmOutStream;
    private final Handler mHandler;
    private byte[] mBuffer = new byte[GlobalData.BUFFER_SIZE];  // 用于流的缓冲存储

    public ConnectedThread(BluetoothSocket socket, Handler handler) {
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        mHandler = handler;
        // 使用临时对象获取输入和输出流，因为成员流是最终的
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "error", e);
        }
        mmInStream = new DataInputStream(tmpIn);
        mmOutStream = tmpOut;
    }


    public void run() {
        // 持续监听InputStream，直到出现异常
        while (true) {
            try {
//                if (this.isInterrupted()) {
//                    return;
//                }
                // 从InputStream读取数据
                mmInStream.readFully(mBuffer);
                // 将获得的bytes发送到UI层activity
                Message message = mHandler.obtainMessage(Constant.MSG_GOT_DATA, mBuffer);
                mHandler.sendMessage(message);
                Log.d(TAG, "message size");
            } catch (Exception e) {
                Log.e(TAG, "error", e);
                mHandler.sendMessage(mHandler.obtainMessage(Constant.MSG_ERROR, e));
                break;
            }
        }
    }

    /**
     * 在main中调用此函数，将数据发送到远端设备中
     */
    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) {
            Log.e(TAG, "error", e);
        }
    }

    public void clearSocketBuffer() {
        try {
            if (mmOutStream != null)
                mmOutStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 在main中调用此函数，断开连接
     */
    public synchronized void cancel() {
        try {
//            this.interrupt();
            this.wait();
        } catch (Exception e) {
            Log.e(TAG, "error", e);
        }
    }

    public void cancelTotally() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
