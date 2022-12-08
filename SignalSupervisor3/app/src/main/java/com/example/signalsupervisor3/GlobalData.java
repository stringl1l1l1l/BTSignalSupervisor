package com.example.signalsupervisor3;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import com.example.signalsupervisor3.bluetooth.BluetoothController;
import com.example.signalsupervisor3.bluetooth.connect.AcceptThread;
import com.example.signalsupervisor3.bluetooth.connect.ConnectThread;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GlobalData extends Application {
    public static final int BUFFER_SIZE = 256;
    public static final String FREQ_REGEX = "[0-9]{1,4}.[0-9]";
    public static final String VOL_REGEX = "[0-3].[0-9]{3}";
    public static final String SPLIT_REGEX = " +";
    public static final int CLIENT_TYPE = 6;
    public static final int SERVER_TYPE = 7;
    public static int PARAM_MAX_SIZE = 6; // 一次发送的幅值数据的字节数(不包含空格)
    public static float sVMaxCh1;
    public static float[] sCh1FreqArray;
    public static float[] sCh2FreqArray;
    public static float[] sCh1VMaxArray;
    public static float[] sCh2VMaxArray;
    private static BluetoothSocket sBluetoothSocket;
    private static AcceptThread sAcceptThread;
    private static BluetoothServerSocket sBluetoothServerSocket;
    private static BluetoothController sBluetoothController;
    private static BluetoothDevice sBluetoothDevice;
    private static Handler sHandler;
    private static ConnectThread sConnectThread;
    private static byte[] sBuffer;
    private static int connectType;
    public static final Executor EXECUTOR = Executors.newCachedThreadPool();

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
        connectType = SERVER_TYPE;
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

    public static BluetoothSocket getBluetoothSocket() {
        return sBluetoothSocket;
    }

    public static void setBluetoothSocket(BluetoothSocket bluetoothSocket) {
        sBluetoothSocket = bluetoothSocket;
    }

    public static BluetoothController getBluetoothController() {
        return sBluetoothController;
    }

    public static void setBluetoothController(BluetoothController bluetoothController) {
        sBluetoothController = bluetoothController;
    }

    public static Handler getHandler() {
        return sHandler;
    }

    public static void setHandler(Handler handler) {
        sHandler = handler;
    }

    public static ConnectThread getConnectThread() {
        return sConnectThread;
    }

    public static void setConnectThread(ConnectThread connectThread) {
        connectType = CLIENT_TYPE;
        sConnectThread = connectThread;
    }

    public static byte[] getBuffer() {
        return sBuffer;
    }

    public static void setBuffer(byte[] buffer) {
        sBuffer = buffer;
    }

    public static void clear() {
        sBluetoothSocket = null;
        sBluetoothController = null;
        sBluetoothDevice = null;
        sHandler = null;
        sConnectThread = null;
        sBuffer = null;
        sBluetoothServerSocket = null;
        sAcceptThread = null;
        sCh1FreqArray = null;
        sCh2FreqArray = null;
        sCh1VMaxArray = null;
        sCh2VMaxArray = null;
        sVMaxCh1 = 0;
    }
}
