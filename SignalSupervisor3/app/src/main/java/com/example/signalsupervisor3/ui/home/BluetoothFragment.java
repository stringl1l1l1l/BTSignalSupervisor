package com.example.signalsupervisor3.ui.home;

import static com.example.signalsupervisor3.utils.AppUtils.showToast;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.signalsupervisor3.GlobalData;
import com.example.signalsupervisor3.R;
import com.example.signalsupervisor3.SupervisorActivity;
import com.example.signalsupervisor3.bluetooth.BluetoothController;
import com.example.signalsupervisor3.bluetooth.BluetoothDeviceAdapter;
import com.example.signalsupervisor3.bluetooth.connect.AcceptThread;
import com.example.signalsupervisor3.bluetooth.connect.Constant;
import com.example.signalsupervisor3.utils.AppUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BluetoothFragment extends Fragment {
    public static final int REQUEST_ENABLE_BT = 1;
    private static final String TAG = BluetoothFragment.class.getSimpleName();
    private Context mContext;
    private Activity mActivity;
    private View view;
    private List<BluetoothDevice> mDeviceList = new ArrayList<>();
    private Set<BluetoothDevice> mSearchedDevSet = new HashSet<>();
    private BluetoothReceiver mBluetoothReceiver;
    private BluetoothDeviceAdapter mDeviceAdapter;
    private MyHandler mHandler = new MyHandler();

    @Override
    public void onAttach(@NonNull Context context) {
        mContext = context;
        mBluetoothReceiver = new BluetoothReceiver();
        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (view == null)
            view = inflater.inflate(R.layout.fragment_home, container, false);
        setHasOptionsMenu(true);
        //initData();
        initRecyclerview();
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivity = getActivity();
        IntentFilter filter = new IntentFilter();
        //开始查找
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        //结束查找
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        //查找设备
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        //设备扫描模式改变
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        //绑定状态
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        //连接成功
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        //连接失败
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);

        //注册广播接收器
        mBluetoothReceiver = new BluetoothReceiver();
        mActivity.registerReceiver(mBluetoothReceiver, filter);
        //打开蓝牙
        BluetoothController.turnOnBlueTooth(mActivity, REQUEST_ENABLE_BT);
        //进行搜索
        BluetoothController.startDiscovery();
        //监听客户端连接
        AcceptThread acceptThread = new AcceptThread(BluetoothController.getBluetoothAdapter(), mHandler);
        // acceptThread.start();
        GlobalData.sAcceptedThreadExec.execute(acceptThread);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        //这里设置另外的menu
        menu.clear();
        inflater.inflate(R.menu.menu_main, menu);
        //通过反射让menu的图标可见
        if (menu.getClass() == MenuBuilder.class) {
            try {
                Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                m.setAccessible(true);
                m.invoke(menu, true);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
        //这一行不能忘，否则看不到图标
        //拿到ActionBar后，可以进行设置
        ((AppCompatActivity) mContext).getSupportActionBar();

        //菜单项点击监听器
        MenuItem.OnMenuItemClickListener listener = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                onContextItemSelected(item);
                int id = item.getItemId();
                if (id == R.id.enable_visibility) {
                    BluetoothController.enableVisibily(mContext);
                    Log.i(TAG, "点击打开可见性");
                } else if (id == R.id.find_device) {
                    //mDeviceList = mBluetoothController.getBondedDeviceList();
                    mDeviceList.clear();
                    BluetoothController.startDiscovery();
                    Log.i(TAG, "点击查找设备");
                } else if (id == R.id.cancel_allThread) {
                    int flag = 0;
                    if (GlobalData.getConnectThread() != null) {
                        GlobalData.getConnectThread().cancel();
                        flag = 1;
                    }
                    if (GlobalData.getConnectedThread() != null) {
                        GlobalData.getConnectedThread().cancel();
                        flag = 1;
                    }
                    if (GlobalData.getAcceptThread() != null) {
                        GlobalData.getAcceptThread().cancel();
                        flag = 1;
                    }
                    if (flag == 0)
                        showToast(mContext, "没有可用的连接");
                }
                return true;
            }
        };
        for (int i = 0, n = menu.size(); i < n; i++)
            menu.getItem(i).setOnMenuItemClickListener(listener);
        super.onCreateOptionsMenu(menu, inflater);
    }

//    private void initData() {
//        BluetoothController bluetoothController = new BluetoothController();
//        bluetoothController.turnOnBlueTooth(mActivity, REQUEST_ENABLE_BT);
//        mDeviceList = bluetoothController.getBondedDeviceList();
//    }

    private void initRecyclerview() {
        //获得Recyclerview
        RecyclerView recyclerview = (RecyclerView) view.findViewById(R.id.btRecycler_home);
        //创建adapter类的对象
        mDeviceAdapter = new BluetoothDeviceAdapter(getContext(), mDeviceList);
        //将对象作为参数通过setAdapter方法设置给recylerview；
        recyclerview.setAdapter(mDeviceAdapter);
        //这步骤必须有，这是选择RecylerView的显示方式
        recyclerview.setLayoutManager(new AppUtils.WrapContentLinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
        //添加Android自带的分割线
        recyclerview.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //关闭服务查找
        BluetoothController.stopDiscovery();
        mContext.unregisterReceiver(mBluetoothReceiver);
    }

    private class MyHandler extends Handler {
        public void handleMessage(Message message) {
            super.handleMessage(message);
            switch (message.what) {
                case Constant.MSG_GOT_DATA:
                    showToast(mContext, "data:" + message.obj);
                    break;
                case Constant.MSG_ERROR:
                    showToast(mContext, "error:" + message.obj);
                    break;
                case Constant.MSG_GOT_A_CLINET:
                    showToast(mContext, "已连接到客户端");
                    GlobalData.setAcceptThread((AcceptThread) message.obj);
                    Intent intent = new Intent(mContext, SupervisorActivity.class);
                    mContext.startActivity(intent);
                    break;
            }
        }
    }

    private class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                    if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
//                        mSearchedDevSet.add(device);
//                        Log.i(TAG, "查找到设备: " + device.getName() + " " + device.getAddress());
//                    }
                    mSearchedDevSet.add(device);
                    Log.i(TAG, "查找到设备: " + device.getName() + " " + device.getAddress());
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    showToast(context, "查找开始");
                    Log.i(TAG, "查找开始");
                    mSearchedDevSet.clear();
                    mDeviceList.clear();
                    mDeviceAdapter.notifyDataSetChanged();
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    showToast(context, "查找结束");
                    Log.i(TAG, "查找结束");
                    mActivity.setProgressBarIndeterminateVisibility(false);
                    mDeviceList.addAll(mSearchedDevSet);
                    mDeviceAdapter.notifyDataSetChanged();
                    break;
                case BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:
                    int scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, 0);
                    if (scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                        Log.i(TAG, "可检测");
                    } else {
                        Log.i(TAG, "不可检测");
                    }
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    BluetoothDevice device2 = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device2 != null) {
//                        showToast(mContext, "正在连接" + device2.getAddress());
                        Log.i(TAG, "正在连接" + device2.getAddress());
                    } else
                        Log.e(TAG, "空对象");
                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    showToast(mContext, "连接断开");
                    Log.i(TAG, "连接断开");
                    break;
                default:
                    break;
            }
        }
    }
}
