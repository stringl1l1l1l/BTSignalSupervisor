package com.example.signalsupervisor3.ui.activities;

import static com.example.signalsupervisor3.utils.AppUtils.getFPointsFromGlobal;
import static com.example.signalsupervisor3.utils.AppUtils.getPointsFromGlobal;
import static com.example.signalsupervisor3.utils.AppUtils.interpolate;
import static com.example.signalsupervisor3.utils.AppUtils.showToast;

import static java.lang.Float.parseFloat;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.signalsupervisor3.GlobalData;
import com.example.signalsupervisor3.R;
import com.example.signalsupervisor3.bluetooth.BluetoothController;
import com.example.signalsupervisor3.bluetooth.connect.AcceptThread;
import com.example.signalsupervisor3.bluetooth.connect.ConnectThread;
import com.example.signalsupervisor3.bluetooth.connect.ConnectedThread;
import com.example.signalsupervisor3.bluetooth.connect.Constant;
import com.example.signalsupervisor3.ui.home.BluetoothFragment;
import com.example.signalsupervisor3.ui.views.CanvasView;
import com.example.signalsupervisor3.utils.AppUtils;

import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.util.Arrays;

public class SupervisorActivity extends AppCompatActivity {
    private static final String TAG = SupervisorActivity.class.getSimpleName();
    private static int BUFFER_SIZE = GlobalData.BUFFER_SIZE;
    private ConnectThread mConnectThread;
    private BluetoothSocket mBluetoothSocket;
    private ConnectedThread mConnectedThread;
    private AcceptThread mAcceptThread;
    private Handler mHandler;
    private final StringBuffer mStringBuffer = new StringBuffer(GlobalData.BUFFER_SIZE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supervisor);
        // 初始化
        BluetoothController.stopDiscovery();
        mHandler = new SupervisorHandler();
        IntentFilter filter = new IntentFilter();
        //连接失败
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);

        //注册广播接收器
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                    showToast(SupervisorActivity.this, "连接断开");
                    Log.i(TAG, "连接断开");
                    finish();
                }
            }
        }, filter);
        // 获取控件
        Button btnBeginTrans = findViewById(R.id.btn_begin_trans);
        Button btnShowCh1 = findViewById(R.id.btn_show_ch1);
        Button btnShowCh2 = findViewById(R.id.btn_show_ch2);
        Button btnDrawPoints = findViewById(R.id.btn_draw_points);
        Button btnDrawLine = findViewById(R.id.btn_draw_line);
        Button btnClearBuffer = findViewById(R.id.btn_clear_buffer);
        Button btnClearCh1 = findViewById(R.id.btn_clear_ch1);
        Button btnClearCh2 = findViewById(R.id.btn_clear_ch2);
        Button btnStopTrans = findViewById(R.id.btn_stop_trans);
        btnBeginTrans.setOnClickListener(new BeginTransListener());
        btnStopTrans.setOnClickListener(new StopTransListener());
        btnShowCh1.setOnClickListener(new ShowCH1Listener());
        btnShowCh2.setOnClickListener(new ShowCH2Listener());
        btnDrawPoints.setOnClickListener(new DrawPointsListener());
        btnDrawLine.setOnClickListener(new DrawLineListener());
        btnClearBuffer.setOnClickListener(new ClearBufferListener());
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
    }

    private class ClearBufferListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            TextView textHasTrans = findViewById(R.id.text_has_trans);
            mStringBuffer.setLength(0);
            textHasTrans.setText("0");
            showToast(SupervisorActivity.this, "缓冲区已清空");
        }
    }

    private class BeginTransListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            beginTransfer();
        }
    }

    private class StopTransListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            mConnectedThread.setStopped(true);
            showToast(SupervisorActivity.this, "已暂停");
        }
    }

    private class ShowCH1Listener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            // 防止数据过少

            String[][] streams = phraseTwoStreamFromBuffer(mStringBuffer.toString());
            if (streams != null) {
                String[] ch1FreqStream = streams[0];
                String[] ch1VMaxStream = streams[1];
                GlobalData.sCh1FreqArray = sstream2floats(ch1FreqStream);
                GlobalData.sCh1VMaxArray = sstream2floats(ch1VMaxStream);
                TextView textCH1VMax = findViewById(R.id.text_CH1_VMax);
                TextView textCH1Freq = findViewById(R.id.text_CH1_Freq);
                // 求出平均值
                float avgCH1VMax = 0, avgCH1Freq = 0;
                for (int i = 0; i < GlobalData.sCh1VMaxArray.length; i++) {
                    avgCH1VMax += GlobalData.sCh1VMaxArray[i];
                }
                avgCH1VMax /= GlobalData.sCh1VMaxArray.length;
                for (int i = 0; i < GlobalData.sCh1FreqArray.length; i++) {
                    avgCH1Freq += GlobalData.sCh1FreqArray[i];
                }
                avgCH1Freq /= GlobalData.sCh1FreqArray.length;
                textCH1VMax.setText((String.format("%.3f", avgCH1VMax)));
                textCH1Freq.setText((String.format("%.1f", avgCH1Freq)));

                GlobalData.sVMaxCh1 = avgCH1VMax;
                // mConnectedThread.setStopped(true);
                showToast(SupervisorActivity.this, "解析成功");
            } else {
                showToast(SupervisorActivity.this, "当前缓冲区为空,请传输数据后解析");
            }

        }
    }

    private class ShowCH2Listener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            // 防止数据过少

            String[][] streams = phraseTwoStreamFromBuffer(mStringBuffer.toString());
            if (streams != null) {
                String[] ch2FreqStream = streams[0];
                String[] ch2VMaxStream = streams[1];
                GlobalData.sCh2FreqArray = sstream2floats(ch2FreqStream);
                GlobalData.sCh2VMaxArray = sstream2floats(ch2VMaxStream);
                TextView textCH2VMax = findViewById(R.id.text_CH2_VMax);
                TextView textCH2Freq = findViewById(R.id.text_CH2_Freq);
                // 求出平均值
                float avgCH2VMax = 0, avgCH2Freq = 0;
                for (int i = 0; i < GlobalData.sCh2VMaxArray.length; i++) {
                    avgCH2VMax += GlobalData.sCh2VMaxArray[i];
                }
                avgCH2VMax /= GlobalData.sCh2VMaxArray.length;
                for (int i = 0; i < GlobalData.sCh2FreqArray.length; i++) {
                    avgCH2Freq += GlobalData.sCh2FreqArray[i];
                }
                avgCH2Freq /= GlobalData.sCh2FreqArray.length;
                textCH2VMax.setText((String.format("%.3f", avgCH2VMax)));
                textCH2Freq.setText((String.format("%.1f", avgCH2Freq)));
                // mConnectedThread.setStopped(true);
                showToast(SupervisorActivity.this, "解析成功");
            } else {
                showToast(SupervisorActivity.this, "当前缓冲区为空,请传输数据后解析");
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

//    private String phraseOneParamFromBuffer() {
//        String bufferStr = new String(mBuffer);
//        String[] split = bufferStr.split(GlobalData.SPLIT_REGEX);
//        String res = "";
//        for (String s : split) {
//            if (s.matches(GlobalData.VOL_REGEX)) {
//                res = s;
//                break;
//            }
//            Log.d(TAG + ": DrawVppListener", s);
//            Log.d(TAG + ": DrawVppListener", Arrays.toString(s.getBytes(StandardCharsets.US_ASCII)));
//        }
//        Log.d(TAG + " DrawVppListener length", String.valueOf(split.length));
//        return res;
//    }

    @SuppressLint("LongLogTag")
    private String[][] phraseTwoStreamFromBuffer(String bufferStr) {
        if (bufferStr.length() > 0) {
            String[] split = bufferStr.split(GlobalData.SPLIT_REGEX);
            String[] stream1 = new String[split.length / 2];
            String[] stream2 = new String[split.length / 2];
            int i = 0, p1 = 0, p2 = 0;
            //找到第一个有效的频率
            while (!split[i].matches(GlobalData.FREQ_REGEX)) i++;
            for (int j = i; j < split.length; ) {
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
            Log.i("phraseTwoStreamFromBuffer" + ": split", Arrays.toString(split));
            Log.i("phraseTwoStreamFromBuffer" + ": 频率", Arrays.toString(stream1));
            Log.i("phraseTwoStreamFromBuffer" + "stream1 len", String.valueOf(stream1.length));
            Log.i("phraseTwoStreamFromBuffer" + ": 幅值", Arrays.toString(stream2));
            Log.i("phraseTwoStreamFromBuffer" + "stream2 len", String.valueOf(stream2.length));
            return new String[][]{stream1, stream2};
        } else
            return null;
    }

    @Override
    public void onBackPressed() {
        if (GlobalData.getConnectThread() != null) {
            GlobalData.getConnectThread().cancel();
            GlobalData.setConnectThread(null);
        }
        if (mConnectedThread != null)
            mConnectedThread.cancel();
        if (GlobalData.getAcceptThread() != null)
            GlobalData.getAcceptThread().cancel();
        finish();
    }

    private void beginTransfer() {
        if (mConnectedThread == null) {
            int type = GlobalData.getConnectType();
            // 若本机作为客户端
            if (type == Constant.CLIENT_TYPE) {
                mConnectThread = GlobalData.getConnectThread();
                mBluetoothSocket = mConnectThread.getSocket();
                mConnectedThread = new ConnectedThread(mBluetoothSocket, mHandler);
                //mConnectedThread.start();
                GlobalData.sConnectedThreadExec.execute(mConnectedThread);
                GlobalData.setConnectedThread(mConnectedThread);
                showToast(this, "开始接收");
            }// 若本机作为服务端
            else if (type == Constant.SERVER_TYPE) {
                mAcceptThread = GlobalData.getAcceptThread();
                mBluetoothSocket = mAcceptThread.getBluetoothSocket();
                mConnectedThread = new ConnectedThread(mBluetoothSocket, mHandler);
                //mConnectedThread.start();
                GlobalData.sConnectedThreadExec.execute(mConnectedThread);
                GlobalData.setConnectedThread(mConnectedThread);
                showToast(this, "开始接收");
            } else {
                Log.e(TAG, "连接未成功");
            }
        } else {
            mConnectedThread.resumeThread();
            showToast(this, "继续传输");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (GlobalData.getConnectThread() != null) {
            GlobalData.getConnectThread().cancel();
            GlobalData.setConnectThread(null);
        }
        if (mConnectedThread != null)
            mConnectedThread.cancel();
        if (GlobalData.getAcceptThread() != null) {
            GlobalData.getAcceptThread().cancel();
        }
    }

    private class SupervisorHandler extends Handler {
        public void handleMessage(Message message) {
            super.handleMessage(message);
            switch (message.what) {
                case Constant.MSG_GOT_DATA:
                    // showToast(SupervisorActivity.this, "data:" + message.obj);
                    // 这里一次传过来的不一定是一个完整的参数,也不能保证只有一个参数
                    TextView textHasTrans = findViewById(R.id.text_has_trans);
                    mStringBuffer.append((char) ((byte) message.obj));
                    int len = mStringBuffer.toString().length();
                    textHasTrans.setText(String.valueOf(len));
                    Log.i(TAG, "len: " + String.valueOf(len));
                    // 缓冲区数据过多，自动清除
                    if (len >= GlobalData.BUFFER_SIZE) {
                        mStringBuffer.setLength(0);
                        textHasTrans.setText("0");
                        showToast(SupervisorActivity.this, "缓冲区达上限，自动清空");
                    }
                    break;
                case Constant.MSG_ERROR:
                    showToast(SupervisorActivity.this, "连接断开");
                    finish(); // 关闭当前Activity
                    Log.e(TAG, "error:" + message.obj);
                    break;
            }
        }
    }

    private class DrawPointsListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            CanvasView canvasView = findViewById(R.id.supervisor_canvas);
            canvasView.mFPoints = getFPointsFromGlobal();
            canvasView.requestLayout();
            showToast(SupervisorActivity.this, "描点成功");
        }
    }

    private class DrawLineListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            CanvasView canvasView = findViewById(R.id.supervisor_canvas);
            if (canvasView.mFPoints != null && !canvasView.mFPoints.isEmpty())
                canvasView.mFPoints = interpolate(canvasView.mFPoints, 0.00005);
            else
                showToast(SupervisorActivity.this, "请先描点");
            canvasView.requestLayout();
            showToast(SupervisorActivity.this, "连线成功");
        }
    }
}

