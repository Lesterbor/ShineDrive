package com.example.shinedrive.util;

import android.bluetooth.BluetoothDevice;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;

public class BlueToothListAdapter extends BaseAdapter {
    private ArrayList<BluetoothDevice> mLeDevices;

    public BlueToothListAdapter() {
        super();
        mLeDevices = new ArrayList<BluetoothDevice>();
    }


    //添加BleDevice
    public void addDevice(BluetoothDevice device) {
        if(!mLeDevices.contains(device)) {
            mLeDevices.add(device);
        }
    }

    //取出BleDevice
    public BluetoothDevice getDevice(int position) {
        return mLeDevices.get(position);
    }

    //清空BulDevice列表
    public void clear() {
        mLeDevices.clear();
    }

    //获取BulDevice列表条数
    public int getCount() {
        return mLeDevices.size();
    }

    @Override
    public Object getItem(int position) {
        return mLeDevices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return null;
    }
}
