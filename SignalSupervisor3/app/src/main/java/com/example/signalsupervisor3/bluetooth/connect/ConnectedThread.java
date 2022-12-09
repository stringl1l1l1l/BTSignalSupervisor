package com.example.signalsupervisor3.bluetooth.connect;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.signalsupervisor3.GlobalData;

import java.io.BufferedInputStream;
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
    private boolean isStopped = false;
    private byte[] mBuffer = new byte[GlobalData.TEMP_BUFFER_SIZE];  // 用于流的缓冲存储

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

    public boolean isStopped() {
        return isStopped;
    }

    public void setStopped(boolean stopped) {
        isStopped = stopped;
    }

    public void run() {
        // 持续监听InputStream，直到出现异常
//        skipInputBuffer();
        while (true) {
            try {
                if (this.isStopped) {
                    synchronized (this) {
                        wait();
                    }
                }
                // 从InputStream读取数据
                byte oneByte = mmInStream.readByte();
                // 将获得的bytes发送到UI层activity
                Message message = mHandler.obtainMessage(Constant.MSG_GOT_DATA, oneByte);
                mHandler.sendMessage(message);
            } catch (Exception e) {
                Log.e(TAG, "error", e);
                mHandler.sendMessage(mHandler.obtainMessage(Constant.MSG_ERROR, e));
                break;
            }
        }
    }

//    public void skipInputBuffer() {
//        try {
//            while (mmInStream.available() != 0) {
//                mmInStream.readByte();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

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

    public synchronized void resumeThread() {
        try {
            synchronized (this) {
                notify();
                isStopped = false;
            }
        } catch (Exception e) {
            Log.e(TAG, "error", e);
        }
    }

    /**
     * 在main中调用此函数，断开连接
     */
    public void cancel() {
        try {
            mmSocket.close();
            mmInStream.close();
            mmOutStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
