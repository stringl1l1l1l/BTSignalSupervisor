package com.example.signalsupervisor3;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import com.example.signalsupervisor3.bluetooth.BluetoothController;
import com.example.signalsupervisor3.bluetooth.connect.AcceptThread;
import com.example.signalsupervisor3.bluetooth.connect.ConnectThread;
import com.example.signalsupervisor3.bluetooth.connect.Constant;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GlobalData extends Application {
    public static int CONNECT_TIME_MS = 6000;
    public static int BUFFER_SIZE = 256;
    public static String FREQ_REGEX = "[0-9]{1,4}.[0-9]";
    public static String VOL_REGEX = "[0-3].[0-9]{3}";
    public static String SPLIT_REGEX = " +";
    public static int PARAM_MAX_SIZE = 6; // 一次发送的幅值数据的字节数(不包含空格)
    public static float sVMaxCh1;
    public static float[] sCh1FreqArray;
    public static float[] sCh2FreqArray;
    public static float[] sCh1VMaxArray;
    public static float[] sCh2VMaxArray;
    private static AcceptThread sAcceptThread;
    private static BluetoothServerSocket sBluetoothServerSocket;
    private static BluetoothDevice sBluetoothDevice;
    private static ConnectThread sConnectThread;
    private static int connectType;
    public static final ExecutorService sConnectedThreadExec = Executors.newCachedThreadPool();
    public static final ExecutorService sConnectThreadExec = Executors.newSingleThreadExecutor();
    public static final ExecutorService sAcceptedThreadExec = Executors.newSingleThreadExecutor();

    public static int getConnectType() {
        return connectType;
    }

    public static void setConnectType(int connectType) {
        GlobalData.connectType = connectType;
    }

    public static AcceptThread getAcceptThread() {
        return sAcceptThread;
    }

    public static void setAcceptThread(AcceptThread acceptThread) {
        connectType = Constant.SERVER_TYPE;
        sAcceptThread = acceptThread;

    }

    public static BluetoothServerSocket getBluetoothServerSocket() {
        return sBluetoothServerSocket;
    }

    public static void setBluetoothServerSocket(BluetoothServerSocket bluetoothServerSocket) {
        sBluetoothServerSocket = bluetoothServerSocket;
    }

    public static BluetoothDevice getBluetoothDevice() {
        return sBluetoothDevice;
    }

    public static void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        sBluetoothDevice = bluetoothDevice;
    }

    public static ConnectThread getConnectThread() {
        return sConnectThread;
    }

    public static void setConnectThread(ConnectThread connectThread) {
        connectType = Constant.CLIENT_TYPE;
        sConnectThread = connectThread;
    }

    public static void clear() {
        sConnectThread = null;
        sAcceptThread = null;
        sCh1FreqArray = null;
        sCh2FreqArray = null;
        sCh1VMaxArray = null;
        sCh2VMaxArray = null;
        sVMaxCh1 = 0;
    }
}
