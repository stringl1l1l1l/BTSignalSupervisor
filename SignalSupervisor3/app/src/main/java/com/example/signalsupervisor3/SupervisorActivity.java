package com.example.signalsupervisor3;

import static com.example.signalsupervisor3.utils.AppUtils.showToast;

import static java.lang.Float.parseFloat;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.signalsupervisor3.bluetooth.connect.AcceptThread;
import com.example.signalsupervisor3.bluetooth.connect.ConnectThread;
import com.example.signalsupervisor3.bluetooth.connect.ConnectedThread;
import com.example.signalsupervisor3.bluetooth.connect.Constant;
import com.example.signalsupervisor3.ui.views.CanvasView;
import com.example.signalsupervisor3.utils.AppUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class SupervisorActivity extends AppCompatActivity {
    private static final String TAG = SupervisorActivity.class.getSimpleName();
    private static int BUFFER_SIZE = GlobalData.PARAM_MAX_SIZE * 500;
    private ConnectThread mConnectThread;
    private BluetoothSocket mBluetoothSocket;
    private BluetoothDevice mBluetoothDevice;
    private ConnectedThread mConnectedThread;
    private AcceptThread mAcceptThread;
    private String[] mCh1FreqStream;
    private String[] mCh2FreqStream;
    private String[] mCh1VMaxStream;
    private String[] mCh2VMaxStream;
    private Handler mHandler;
    private byte[] mBuffer;
    private int mBufferPtr;
    private float[] freq;
    private float[] magnitude;
    private boolean isRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supervisor);
        // 初始化
        mBuffer = new byte[BUFFER_SIZE];
        Arrays.fill(mBuffer, (byte) 32);
        mBufferPtr = 0;
        mHandler = new SupervisorHandler();
        // 获取控件
        Button btnBeginTrans = findViewById(R.id.btn_begin_trans);
        Button btnShowCh1 = findViewById(R.id.btn_show_ch1);
        Button btnShowCh2 = findViewById(R.id.btn_show_ch2);
        Button btnDraw = findViewById(R.id.btn_draw);
        Button btnClearBuffer = findViewById(R.id.btn_clear_buffer);
        Button btnClearCh1 = findViewById(R.id.btn_clear_ch1);
        Button btnClearCh2 = findViewById(R.id.btn_clear_ch2);
        Button btnConfirm = findViewById(R.id.btn_confirm);
        EditText editBuffer = findViewById(R.id.edit_buffer);
        editBuffer.setText(String.valueOf(BUFFER_SIZE));
        btnBeginTrans.setOnClickListener(new BeginTransListener());
        btnShowCh1.setOnClickListener(new ShowCH1Listener());
        btnShowCh2.setOnClickListener(new ShowCH2Listener());
        btnDraw.setOnClickListener(new DrawListener());
        btnClearBuffer.setOnClickListener(new ClearListener());
        btnClearCh1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GlobalData.sCh1VMaxArray = null;
                GlobalData.sCh1FreqArray = null;
                TextView textCH1VMax = findViewById(R.id.text_CH1_VMax);
                TextView textCH1Freq = findViewById(R.id.text_CH1_Freq);
                textCH1VMax.setText("0");
                textCH1Freq.setText("0");
                showToast(SupervisorActivity.this, "已清除");
            }
        });
        btnClearCh2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GlobalData.sCh2VMaxArray = null;
                GlobalData.sCh2FreqArray = null;
                TextView textCH2VMax = findViewById(R.id.text_CH2_VMax);
                TextView textCH2Freq = findViewById(R.id.text_CH2_Freq);
                textCH2VMax.setText("0");
                textCH2Freq.setText("0");
                showToast(SupervisorActivity.this, "已清除");
            }
        });
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = editBuffer.getText().toString();
                BUFFER_SIZE = Integer.parseInt(text);
                editBuffer.setText(String.valueOf(BUFFER_SIZE));
                mBuffer = new byte[BUFFER_SIZE];
                showToast(SupervisorActivity.this, "缓冲区大小已修改为: " + BUFFER_SIZE + "Byte");
            }
        });
    }

    private class ClearListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            mBuffer = new byte[BUFFER_SIZE];
            mBufferPtr = 0;
            showToast(SupervisorActivity.this, "缓冲区已清空");
        }
    }

    private class BeginTransListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            beginTransfer();
        }
    }

    private class ShowCH1Listener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            // 防止数据过少
            if (mBufferPtr < BUFFER_SIZE / 2) {
                showToast(SupervisorActivity.this, "缓冲区数据量过少");
            } else {
                String[][] streams = phraseTwoStreamFromBuffer();
                mCh1FreqStream = streams[0];
                mCh1VMaxStream = streams[1];
                GlobalData.sCh1FreqArray = sstream2floats(mCh1FreqStream);
                GlobalData.sCh1VMaxArray = sstream2floats(mCh1VMaxStream);
                TextView textCH1VMax = findViewById(R.id.text_CH1_VMax);
                TextView textCH1Freq = findViewById(R.id.text_CH1_Freq);
                textCH1VMax.setText((String.valueOf(GlobalData.sCh1VMaxArray[GlobalData.sCh1VMaxArray.length - 1])));
                textCH1Freq.setText((String.valueOf(GlobalData.sCh1FreqArray[GlobalData.sCh1FreqArray.length - 1])));
                // 设置滤波前的幅值稳定后的定值
                int temLen = GlobalData.sCh1VMaxArray.length;
                GlobalData.sVMaxCh1 = GlobalData.sCh1VMaxArray[temLen - 1];
                mConnectedThread.cancel();
                isRunning = false;
                showToast(SupervisorActivity.this, "解析成功");
            }
        }
    }

    private class ShowCH2Listener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            // 防止数据过少
            if (mBufferPtr < GlobalData.PARAM_MAX_SIZE * 12) {
                showToast(SupervisorActivity.this, "缓冲区数据量过少");
            } else {
                String[][] streams = phraseTwoStreamFromBuffer();
                mCh2FreqStream = streams[0];
                mCh2VMaxStream = streams[1];
                GlobalData.sCh2FreqArray = sstream2floats(mCh2FreqStream);
                GlobalData.sCh2VMaxArray = sstream2floats(mCh2VMaxStream);
                TextView textCH2VMax = findViewById(R.id.text_CH2_VMax);
                TextView textCH2Freq = findViewById(R.id.text_CH2_Freq);
                textCH2VMax.setText(String.valueOf(GlobalData.sCh2VMaxArray[GlobalData.sCh2VMaxArray.length - 1]));
                textCH2Freq.setText(String.valueOf(GlobalData.sCh2FreqArray[GlobalData.sCh2FreqArray.length - 1]));
                mConnectedThread.cancel();
                isRunning = false;
                showToast(SupervisorActivity.this, "解析成功");
            }
        }
    }

    private float[] sstream2floats(String[] strings) {
        float[] floats = new float[strings.length];
        int i;
        for (i = 0; i < strings.length && strings[i] != null; i++) {
            floats[i] = parseFloat(strings[i]);
        }
        float[] res = new float[i];
        System.arraycopy(floats, 0, res, 0, res.length);
        return res;
    }

    private String phraseOneParamFromBuffer() {
        String bufferStr = new String(mBuffer);
        String[] split = bufferStr.split(GlobalData.SPLIT_REGEX);
        String res = "";
        for (String s : split) {
            if (s.matches(GlobalData.VOL_REGEX)) {
                res = s;
                break;
            }
            Log.d(TAG + ": DrawVppListener", s);
            Log.d(TAG + ": DrawVppListener", Arrays.toString(s.getBytes(StandardCharsets.US_ASCII)));
        }
        Log.d(TAG + " DrawVppListener length", String.valueOf(split.length));
        return res;
    }

    @SuppressLint("LongLogTag")
    private String[][] phraseTwoStreamFromBuffer() {
        String bufferStr = new String(mBuffer);
        String[] split = bufferStr.split(GlobalData.SPLIT_REGEX);
        String[] stream1 = new String[split.length / 2];
        String[] stream2 = new String[split.length / 2];
        int i = 0, p1 = 0, p2 = 0;
        //找到第一个有效的频率
        while (!split[i].matches(GlobalData.FREQ_REGEX)) i++;
        for (int j = i; j < split.length - 1; ) {
            // 参数1 偶 频率
            if (((j - i) & 0b1) == 0) {
                if (split[j].matches(GlobalData.FREQ_REGEX))
                    stream1[p1++] = split[j++];
                else {
                    j += 2;
                }
            } else {
                if (split[j].matches(GlobalData.VOL_REGEX))
                    stream2[p2++] = split[j++];
                else {
                    p1--;
                    j++;
                }
            }
        }
        Log.i("phraseTwoStreamFromBuffer" + ": buffer", bufferStr);
        Log.i("phraseTwoStreamFromBuffer" + ": 频率", Arrays.toString(stream1));
        Log.i("phraseTwoStreamFromBuffer" + "stream1 len", String.valueOf(stream1.length));
        Log.i("phraseTwoStreamFromBuffer" + ": 幅值", Arrays.toString(stream2));
        Log.i("phraseTwoStreamFromBuffer" + "stream2 len", String.valueOf(stream2.length));
        return new String[][]{stream1, stream2};
    }

    private void beginTransfer() {
        if (!isRunning) {
            int type = GlobalData.getConnectType();
            // 若本机作为客户端
            if (type == GlobalData.CLIENT_TYPE) {
                mConnectThread = GlobalData.getConnectThread();
                mBluetoothSocket = mConnectThread.getSocket();
                mConnectedThread = new ConnectedThread(mBluetoothSocket, mHandler);
                mConnectedThread.start();
                isRunning = true;
                showToast(this, "开始接收");
            } else if (type == GlobalData.SERVER_TYPE) {
                mAcceptThread = GlobalData.getAcceptThread();
                mBluetoothSocket = mAcceptThread.getBluetoothSocket();
                mConnectedThread = new ConnectedThread(mBluetoothSocket, mHandler);
                mConnectedThread.start();
                isRunning = true;
                showToast(this, "开始接收");
            } else {
                Log.e(TAG, "连接未成功");
            }
        } else {
            showToast(this, "传输已开始, 请勿重复点击");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (GlobalData.getConnectThread() != null)
            GlobalData.getConnectThread().cancel();
        if (mConnectedThread != null)
            mConnectedThread.cancelTotally();
        if (mAcceptThread != null)
            mAcceptThread.cancel();
        GlobalData.clear();
    }

    private class SupervisorHandler extends Handler {
        public void handleMessage(Message message) {
            super.handleMessage(message);
            switch (message.what) {
                case Constant.MSG_GOT_DATA:
                    // showToast(SupervisorActivity.this, "data:" + message.obj);
                    // 这里一次传过来的不一定是一个完整的参数,也不能保证只有一个参数
                    byte[] bytes = (byte[]) message.obj;
                    for (byte aByte : bytes) {
                        // 将获取到的数据流全部放入mBuffer里,超过BUFFER_SIZE时断开线程
                        if (mBufferPtr == BUFFER_SIZE) {
                            mConnectedThread.cancel();
                            isRunning = false;
                            showToast(SupervisorActivity.this, "缓冲区已满,传输停止,请解析后清空");
                            return;
                        }
                        mBuffer[mBufferPtr++] = aByte;
                    }
                    Log.i(TAG, "data:" + new String(bytes));
//                    Log.i(TAG, "data:" + Arrays.toString(String.valueOf(message.obj).getBytes(StandardCharsets.US_ASCII)));
                    Log.i(TAG, "pos: " + mBufferPtr);
                    break;
                case Constant.MSG_ERROR:
                    showToast(SupervisorActivity.this, "连接断开");
                    Log.e(TAG, "error:" + message.obj);
                    break;
            }
        }

    }

    private class DrawListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            float[] points = AppUtils.getPointsFromGlobal();
            CanvasView canvasView = findViewById(R.id.supervisor_canvas);
            Log.i("DrawListener", Arrays.toString(points));
            canvasView.mPoints = points;
            canvasView.requestLayout();
            showToast(SupervisorActivity.this, "绘制成功");
        }
    }


}

