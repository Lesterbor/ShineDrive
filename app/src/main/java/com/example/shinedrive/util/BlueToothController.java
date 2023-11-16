package com.example.shinedrive.util;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;


import androidx.core.app.ActivityCompat;

import java.util.ArrayList;

//蓝牙适配器
public class BlueToothController {
    //调试
    private static final String BlueController = "BlueTooth";

    private BluetoothSocket btSocket;
    private BluetoothAdapter mAdapter;
    private static final int REQ_PERMISSION_CODE = 1;
    //蓝牙权限列表
    public ArrayList<String> requestList = new ArrayList<>();

    public BlueToothController() {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public BluetoothAdapter returnMAdapter() {
        return this.mAdapter;
    }

    //是否支持蓝牙
    public boolean isSupportBlueTooth() {
        if (mAdapter != null) {
            return true;
        } else {
            return false;
        }
    }

    //判断当前蓝牙状态
    public boolean getBlueToothStatus() {
        assert (mAdapter != null);
        return mAdapter.isEnabled();
    }

    //启动蓝牙扫描
    public void beginBlueToothScan(Activity activity) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            getPermissions(activity);
        }
        assert (mAdapter != null);
        if (mAdapter.isDiscovering()) {
            mAdapter.cancelDiscovery();
        } else {
            mAdapter.startDiscovery();
        }
    }


    /**
     * 打开蓝牙可见性
     * @param context
     */
    public void enableVisibly(Context context, Activity activity) {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            getPermissions(activity);
            return;
        }
        context.startActivity(discoverableIntent);
    }

    //关闭蓝牙搜索
    public void cancelSearch(Activity activity) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            getPermissions(activity);
            return;
        }
        mAdapter.cancelDiscovery();
    }


    //获取权限
    public void getPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestList.add(Manifest.permission.BLUETOOTH_SCAN);
            requestList.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            requestList.add(Manifest.permission.BLUETOOTH_CONNECT);
//            按需屏蔽
            requestList.add(Manifest.permission.ACCESS_FINE_LOCATION);
            requestList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            requestList.add(Manifest.permission.BLUETOOTH);
        }
        if (requestList.size() != 0) {
            ActivityCompat.requestPermissions(activity, requestList.toArray(new String[0]), REQ_PERMISSION_CODE);
        }
    }


    //打开蓝牙设备
    public void turnOnBlueTooth(Activity activity, int requestCode) {
        if (!mAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                getPermissions(activity);
                return;
            }
            activity.startActivityForResult(intent, requestCode);
        }
    }

    //关闭蓝牙设备
    public void turnOffBlueTooth(Activity activity) {
        if (mAdapter.isEnabled()) {
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                getPermissions(activity);
                return;
            }
            mAdapter.disable();
        }
    }
}
