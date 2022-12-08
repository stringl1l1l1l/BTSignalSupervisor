package com.example.signalsupervisor3.bluetooth;

import static com.example.signalsupervisor3.utils.AppUtils.showToast;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.signalsupervisor3.GlobalData;
import com.example.signalsupervisor3.R;
import com.example.signalsupervisor3.SupervisorActivity;
import com.example.signalsupervisor3.bluetooth.connect.ConnectThread;
import com.example.signalsupervisor3.bluetooth.connect.ConnectedThread;
import com.example.signalsupervisor3.bluetooth.connect.Constant;
import com.example.signalsupervisor3.utils.AppUtils;


import java.io.Serializable;
import java.util.List;

public class BluetoothDeviceAdapter extends RecyclerView.Adapter<BluetoothDeviceAdapter.ViewHolder> {
    private static final String TAG = BluetoothDeviceAdapter.class.getSimpleName();
    private Context mContext;
    private Handler mHandler;
    private List<BluetoothDevice> mDeviceList;

    public BluetoothDeviceAdapter(Context context, List<BluetoothDevice> deviceList) {
        this.mContext = context;
        mHandler = new MyHandler();
        mDeviceList = deviceList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = View.inflate(mContext, R.layout.item_device, null);
        final ViewHolder holder = new ViewHolder(view);
        holder.deviceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int pos = holder.getAdapterPosition();
                BluetoothDevice device = mDeviceList.get(pos);
                // 开始连接
                ConnectThread connectThread = new ConnectThread(device, mHandler);
                connectThread.start();
                showToast(mContext, "正在连接……");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(3500);
                        } catch (InterruptedException e) {
                            Log.e(TAG, "error", e);
                        }
                        if (connectThread.getSocket().isConnected()) {
                            GlobalData.setConnectThread(connectThread);
                            Intent intent = new Intent(mContext, SupervisorActivity.class);
                            mContext.startActivity(intent);
                        } else {
                            mHandler.sendEmptyMessage(Constant.CONNECT_FAIL);
//                                showToast(mContext, "连接失败,请重试");
                        }
                    }
                }).start();
                // 连接成功后再跳转
//                Object lock = new Object();
//                synchronized (lock) {
//                    try {
//                        // 主线程暂停6s
//                        lock.wait(6000);
//                        // 开启新线程轮询是否连接成功，成功后唤醒主线程
//                        new Thread(new Runnable() {
//                            @Override
//                            public void run() {
//                                while (!connectThread.getSocket().isConnected()) {
//                                    try {
//                                        Thread.sleep(100);
//                                    } catch (InterruptedException e) {
//                                        e.printStackTrace();
//                                    }
//                                }
//                                lock.notify();
//                            }
//                        }).start();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//                if (connectThread.getSocket().isConnected()) {
//                    GlobalData.setConnectThread(connectThread);
//                    Intent intent = new Intent(mContext, SupervisorActivity.class);
//                    mContext.startActivity(intent);
//                } else {
//                    Log.i(TAG, "未连接");
//                    // mHandler.sendEmptyMessage(Constant.CONNECT_FAIL);
//                    showToast(mContext, "连接失败,请重试");
//                }
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BluetoothDevice BluetoothDevice = mDeviceList.get(position);
        if (BluetoothDevice.getName() != null)
            holder.name.setText(BluetoothDevice.getName());
        else
            holder.name.setText("未知");
        holder.address.setText(BluetoothDevice.getAddress());
        if (BluetoothDevice.getBondState() == android.bluetooth.BluetoothDevice.BOND_BONDED)
            holder.isBound.setText("已配对");
        else
            holder.isBound.setText("未配对");
    }

    @Override
    public int getItemCount() {
        if (mDeviceList != null)
            return mDeviceList.size();
        else
            return 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView address;
        final TextView isBound;
        View deviceView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceView = itemView;
            name = itemView.findViewById(R.id.name);
            address = itemView.findViewById(R.id.address);
            isBound = itemView.findViewById(R.id.is_bound);
        }
    }

    private class MyHandler extends Handler {
        public void handleMessage(Message message) {
            super.handleMessage(message);
            switch (message.what) {
                case Constant.MSG_GOT_DATA:
                    Log.i(TAG, "data:" + message.obj);
                    break;
                case Constant.MSG_ERROR:
                    Log.e(TAG, "error:" + message.obj);
                    break;
                case Constant.MSG_CONNECTED_TO_SERVER:
                    Log.i(TAG, "已连接到服务端");
                    showToast(mContext, "已连接到服务端");
                    break;
                case Constant.MSG_GOT_A_CLINET:
                    showToast(mContext, "已连接到客户端");
                    break;
                case Constant.CONNECT_FAIL:
                    showToast(mContext, "连接失败,请重试");
                    break;
                default:
                    break;
            }
        }
    }
}
