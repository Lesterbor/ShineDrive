package com.example.shinedrive;

import static java.lang.Math.abs;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import android.widget.TextView;

import com.example.shinedrive.util.BlueToothController;
import com.example.shinedrive.util.BlueToothListAdapter;
import com.example.shinedrive.util.SlideMenu;
import com.example.shinedrive.util.ToastUtil;
import com.example.shinedrive.util.LoadIconUtil;
import com.wang.avi.AVLoadingIndicatorView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    //蓝牙-------------------------------------------------------------------------------------------
    //自定义类蓝牙控制器实例化
    private final BlueToothController mBlueToothController = new BlueToothController();
    //菜单部分蓝牙列表控件
    private ListView BlueToothList;
    //软件内部自带的蓝牙开关
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    Switch BlueToothSwitch;
    //定义一个列表，存蓝牙设备的地址。
    public ArrayList<String> arrayList = new ArrayList<>();
    //定义一个列表，存蓝牙设备地址，用于显示。
    public ArrayList<String> deviceName = new ArrayList<>();

    //蓝牙列表数据渲染部分
    private SimpleAdapter BluetoothSimpleAdapter;
    private List<Map<String, Object>> mBluetoothListData;

    //原生的蓝牙Device列表
    //自定义类蓝牙控制器实例化
    private final BlueToothListAdapter mBlueToothListAdapter = new BlueToothListAdapter();

    //蓝牙状态广播
    public IntentFilter filter;

    //蓝牙扫描按钮
    public Button ScanBluetoothButton;

    //蓝牙状态字
    public TextView blueToothStaticFlag;

    //蓝牙UUID
    private final String SERVICES_UUID    = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";      //服务UUID
    private final String WRITE_UUID       = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";      //写入特征UUID
    private final String NOTIFY_UUID      = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";      //监听通知特征的UUID

    private boolean BlueLinkFlag = false;

    private BluetoothGatt mBluetoothGatt;

    //页面插件----------------------------------------------------------------------------------------
    //实例化滑动
    private SlideMenu slideMenu;

    //加载控件设置
    private AVLoadingIndicatorView avi;


    //----------------------------------------------------------------------------------------------
    //摇杆相关控件
    LinearLayout wheelLeftBackground = null;
    ImageView wheelLeftRocker = null;

    LinearLayout wheelRightBackground = null;
    ImageView wheelRightRocker = null;

    //组件中心坐标
    private int LeftRockerX = 0;
    private int LeftRockerY = 0;
    private int RightRockerX = 0;
    private int RightRockerY = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //寻找相关组件
        slideMenu = findViewById(R.id.slideMenu);
        BlueToothSwitch = findViewById(R.id.BtSwitch);
        BlueToothList = findViewById(R.id.BlueToothList);
        ScanBluetoothButton = findViewById(R.id.ScanBluetoothButton);

        RelativeLayout topInfoView = findViewById(R.id.topInfoView);
        blueToothStaticFlag = topInfoView.findViewById(R.id.blueLinkStatue);
        avi = (AVLoadingIndicatorView) findViewById(R.id.avi);

        //加载摇杆控件
        MainActivity.this.wheelLeftBackground = (LinearLayout)findViewById(R.id.wheelLeftBackground);
        MainActivity.this.wheelLeftRocker = (ImageView)findViewById(R.id.wheelLeftRocker);

        MainActivity.this.wheelRightBackground = (LinearLayout)findViewById(R.id.wheelRightBackground);
        MainActivity.this.wheelRightRocker = (ImageView)findViewById(R.id.wheelRightRocker);

        //绑定左摇杆回调函数
        MainActivity.this.wheelLeftBackground.setOnTouchListener(LeftRockerOnTouch);

        MainActivity.this.wheelRightBackground.setOnTouchListener(RightRockerOnTouch);

        //加载控件隐藏
        LoadIconUtil.closeLoadIcon(MainActivity.this.avi);

        // 注册蓝牙广播接收器
        filter = new IntentFilter();

        //蓝牙状态的变化广播
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        //搜索蓝牙的广播
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        //注册广播
        registerReceiver(bluetoothReceiver, filter);

        //加载软件的时候判断当前蓝牙是否打开
        if (mBlueToothController.getBlueToothStatus()) {
            ScanBluetoothButton.setVisibility(View.VISIBLE);
            BlueToothSwitch.setChecked(true);
        } else {
            ScanBluetoothButton.setVisibility(View.INVISIBLE);
            BlueToothSwitch.setChecked(false);
        }

        //创建蓝牙设备渲染列表
        mBluetoothListData = new ArrayList<>();
        BluetoothSimpleAdapter = new SimpleAdapter(
                this,
                mBluetoothListData,
                R.layout.bluetoothitem,
                new String[]{"ico", "name", "ssid"},
                new int[]{R.id.item_icon, R.id.item_name, R.id.item_ssid}
        );
        BlueToothList.setAdapter(BluetoothSimpleAdapter);

        //Switch的回调函数
        //蓝牙开关选择是否打开蓝牙
        BlueToothSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    //
                    mBlueToothController.turnOnBlueTooth(MainActivity.this, 1);
                } else {
                    mBlueToothController.turnOffBlueTooth(MainActivity.this);
                }
            }
        });
    }



    //左摇杆回调函数
    private final View.OnTouchListener LeftRockerOnTouch = new View.OnTouchListener() {
        @SuppressLint({"ClickableViewAccessibility", "DefaultLocale"})
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            float LeftRockerXBuff;
            float LeftRockerYBuff;

            switch (event.getAction()) {
                //手指按下时执行的方法
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    if(((event.getX()-255)*(event.getX()-255)+(event.getY()-255)*(event.getY()-255))<=255*255){
                        LeftRockerXBuff = event.getX()-255;
                        LeftRockerYBuff = event.getY()-255;
                    }else{
                        //计算角度与坐标
                        double RockerAngle = Math.atan2(event.getX() - 255,event.getY() - 255);
                        LeftRockerXBuff = (float) (255*sin(RockerAngle));
                        LeftRockerYBuff = (float) (255*cos(RockerAngle));
                    }
                    MainActivity.this.wheelLeftRocker.setTranslationX(LeftRockerXBuff);
                    MainActivity.this.wheelLeftRocker.setTranslationY(LeftRockerYBuff);

//                    MainActivity.this.LeftRockerX = ((int)LeftRockerXBuff + 273) * (255 - (-255)) / (273 - (-273)) + (-255);
//                    MainActivity.this.LeftRockerY = ((int)LeftRockerYBuff + 273) * (255 - (-255)) / (273 - (-273)) + (-255);

                    MainActivity.this.LeftRockerX = ((int)LeftRockerXBuff);
                    MainActivity.this.LeftRockerY = ((int)LeftRockerYBuff);
                    MainActivity.this.LeftRockerY = -MainActivity.this.LeftRockerY;

                    Log.v("----Blue","POS-LEFT---X:"+LeftRockerX+"Y:"+LeftRockerY);
                    sendRockerData(LeftRockerX,LeftRockerY,RightRockerX,RightRockerY);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    MainActivity.this.wheelLeftRocker.setTranslationX(0);
                    MainActivity.this.wheelLeftRocker.setTranslationY(0);
                    MainActivity.this.LeftRockerX = 0;
                    MainActivity.this.LeftRockerY = 0;

                    while(!sendRockerData(0,0,RightRockerX,RightRockerY));
                    break;
            }
            return true;
        }
    };

    //右摇杆回调函数
    private final View.OnTouchListener RightRockerOnTouch = new View.OnTouchListener() {
        @SuppressLint({"ClickableViewAccessibility", "DefaultLocale"})
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            float RightRockerXBuff;
            float RightRockerYBuff;

            switch (event.getAction()) {
                //手指按下时执行的方法
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
//                    Log.v("----Blue","POS-RIGHT---X:"+event.getX()+"Y:"+event.getY());
                    if(((event.getX()-277)*(event.getX()-255)+(event.getY()-255)*(event.getY()-255))<=255*255){
                        RightRockerXBuff = event.getX()-255;
                        RightRockerYBuff = event.getY()-255;
                    }else{
                        //计算角度与坐标
                        double RockerAngle = Math.atan2(event.getX() - 255,event.getY() - 255);
                        RightRockerXBuff = (float) (255*sin(RockerAngle));
                        RightRockerYBuff = (float) (255*cos(RockerAngle));
                    }
                    MainActivity.this.wheelRightRocker.setTranslationX(RightRockerXBuff);
                    MainActivity.this.wheelRightRocker.setTranslationY(RightRockerYBuff);

//                    MainActivity.this.RightRockerX = ((int)RightRockerXBuff + 277) * (255 - (-255)) / (277 - (-277)) + (-255);
//                    MainActivity.this.RightRockerY = ((int)RightRockerYBuff + 277) * (255 - (-255)) / (277 - (-277)) + (-255);

                    MainActivity.this.RightRockerX = ((int)RightRockerXBuff);
                    MainActivity.this.RightRockerY = ((int)RightRockerYBuff);

                    MainActivity.this.RightRockerY = -MainActivity.this.RightRockerY;


                    sendRockerData(LeftRockerX,LeftRockerY,RightRockerX,RightRockerY);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    MainActivity.this.wheelRightRocker.setTranslationX(0);
                    MainActivity.this.wheelRightRocker.setTranslationY(0);
                    MainActivity.this.RightRockerX = 0;
                    MainActivity.this.RightRockerY = 0;

                    while(!sendRockerData(0,0,RightRockerX,RightRockerY));
                    break;
            }
            return true;
        }
    };




    //申请权限的回调
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //此处的1为当时请求权限的Code
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //权限获取成功，什么都不提示
            } else {
                ToastUtil.showMsg(getApplicationContext(), "权限未获取 应用将无法正常使用");
                BlueToothSwitch.setChecked(false);
                // 跳转至当前应用的详细信息页面
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        }
    }

    //按下按钮请求时判断用户是否允许了打开蓝牙
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //此处的1为当时请求权限的Code
        if (requestCode == 1) {
            if (resultCode == 0) {
                BlueToothSwitch.setChecked(false);
            } else {
                BlueToothSwitch.setChecked(true);
            }
            return;
        }
    }

    //打开或关闭设置菜单回调
    public void openMenu(View view) {
        //反转菜单
        slideMenu.switchMenu();
    }

    public void clearActivityData() {
        //清除列表数据
        deviceName.clear();
        arrayList.clear();

        mBluetoothListData.clear();
        BluetoothSimpleAdapter.notifyDataSetChanged();

        //蓝牙驱动部分的List更新
        mBlueToothListAdapter.clear();
        blueToothStaticFlag.setText("未连接设备");
        LoadIconUtil.closeLoadIcon(MainActivity.this.avi);
    }

    //打开或关闭设置菜单回调
    public void startScanBlueTooth(View view) {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!isEnabled) {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
            builder.setTitle("蓝牙扫描需要开启GPS，是否进入设置开启？").setNegativeButton("取消", null);
            builder.setPositiveButton("确认",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(myIntent);
                        }
                    });
            builder.show();
        } else if (!mBlueToothController.getBlueToothStatus()) {

        } else {
            // 启动蓝牙扫描
            mBlueToothController.beginBlueToothScan(MainActivity.this);
            //清除数据列表并更新
            MainActivity.this.clearActivityData();
        }
    }

    //蓝牙打开或断开的回调函数
    //在onCreate的时候已经注册了
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            switch (action) {
                case BluetoothAdapter.ACTION_STATE_CHANGED: {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF: {
                            // 处理关闭蓝牙的情况
                            ToastUtil.showMsg(getApplicationContext(), "蓝牙已关闭");
                            ScanBluetoothButton.setVisibility(View.INVISIBLE);
                            //清除数据列表并更新
                            MainActivity.this.clearActivityData();
                            MainActivity.this.BlueToothSwitch.setChecked(false);



                            break;
                        }
                        case BluetoothAdapter.STATE_TURNING_OFF: {
                            // 处理正在关闭蓝牙的情况
                            //清除数据列表并更新
                            MainActivity.this.clearActivityData();
                            break;
                        }
                        case BluetoothAdapter.STATE_ON: {
                            // 处理蓝牙已打开的情况
                            ToastUtil.showMsg(getApplicationContext(), "蓝牙已打开");
                            BlueToothSwitch.setChecked(true);
                            ScanBluetoothButton.setVisibility(View.VISIBLE);
                            break;
                        }
                        case BluetoothAdapter.STATE_TURNING_ON: {
                            // 处理正在打开蓝牙的情况
                        }
                    }
                    break;
                }
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED: {
//                    ToastUtil.showMsg(getApplicationContext(),"开始搜索");
                    Log.v("----Blue", "开始搜索");
                    break;
                }
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED: {
//                    ToastUtil.showMsg(getApplicationContext(),"结束搜索");
                    Log.v("----Blue", "搜索结束");
                    break;
                }
                case BluetoothDevice.ACTION_FOUND: {
                    String s;
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        //如果权限没获取就获取权限
                        mBlueToothController.getPermissions(MainActivity.this);
                        return;
                    }


                    //判断存在的列表里面有没有查询到的这个名称并且判断这个名称是不是null
                    if (!MainActivity.this.deviceName.contains(device.getName()) && device.getName() != null) {
                        //备份数据 连接的时候要用
                        MainActivity.this.deviceName.add(device.getName());//将搜索到的蓝牙名称和地址添加到列表。
                        MainActivity.this.arrayList.add(device.getAddress());//将搜索到的蓝牙地址添加到列表。
                        //将扫描到的BleDevice存放到列表
                        mBlueToothListAdapter.addDevice(device);


                        //构建数据格式更新到ListView
                        Map<String, Object> map = new HashMap<>();
                        map.put("ico", R.drawable.icon_bluetooth);
                        map.put("name", device.getName());
                        map.put("ssid", device.getAddress());
                        MainActivity.this.mBluetoothListData.add(map);
                        MainActivity.this.BluetoothSimpleAdapter.notifyDataSetChanged();

                        //设置item的点击回调函数
                        BlueToothList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                    //如果权限没获取就获取权限
                                    MainActivity.this.mBlueToothController.getPermissions(MainActivity.this);
                                    return;
                                }
                                if (MainActivity.this.BlueLinkFlag) {
                                    MainActivity.this.mBluetoothGatt.close();
                                }
                                //显示加载控件
                                MainActivity.this.blueToothStaticFlag.setText("连接中...");
                                LoadIconUtil.openLoadIcon(MainActivity.this.avi);

                                //关闭菜单
                                slideMenu.switchMenu();

                                Log.v("----Blue", "点击了" + position + deviceName.get(position) + " : " + arrayList.get(position));
                                //从这里进入连接函数
                                //关闭蓝牙扫描

                                MainActivity.this.mBlueToothController.cancelSearch(MainActivity.this);
                                BluetoothDevice mBluetoothDevice;
                                mBluetoothDevice = mBlueToothListAdapter.getDevice(position);
                                MainActivity.this.mBluetoothGatt = mBluetoothDevice.connectGatt(MainActivity.this, false, userGattCallback);
                            }
                        });
                    }
                    break;
                }
                default:
                    break;
            }
        }
    };

    private final BluetoothGattCallback userGattCallback = new BluetoothGattCallback() {
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            Log.v("----Blue", "触发事件" + newState);
            //连接状态改变的Callback
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    //如果权限没获取就获取权限
                    mBlueToothController.getPermissions(MainActivity.this);
                    return;
                }
                // 连接成功后，开始扫描服务
                gatt.discoverServices();
            } else {
                gatt.close();
            }
            if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                // 连接断开
                Log.v("----Blue", "连接已断开");
                MainActivity.this.blueToothStaticFlag.setText("未连接设备");
                MainActivity.this.BlueLinkFlag = false;
                LoadIconUtil.closeLoadIcon(MainActivity.this.avi);

                //添加是否显示矩阵的代码

            }
        }

        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            //服务发现成功的Callback
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.v("----Blue", "服务发现成功");

                List<BluetoothGattService> gattServicesList = gatt.getServices();
                int i;
                for (i = 0; i < gattServicesList.size(); i++) {
                    BluetoothGattService  gattServicesListBuff =  gattServicesList.get(i);
                    //如果servicesUUID相同，则做如下处理

                    if (gattServicesListBuff.getUuid().equals(UUID.fromString(SERVICES_UUID))) {
                        Log.v("----Blue", "比对相同了");
                        //设置写入特征UUID
                        BluetoothGattCharacteristic notifyCharacteristic = gattServicesListBuff.getCharacteristic(UUID.fromString(MainActivity.this.NOTIFY_UUID));
                        BluetoothGattCharacteristic writeCharacteristic  = gattServicesListBuff.getCharacteristic(UUID.fromString(MainActivity.this.WRITE_UUID));
                        //设置监听特征UUID
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            //如果权限没获取就获取权限
                            mBlueToothController.getPermissions(MainActivity.this);
                        }

                        //开启监听

                        if (MainActivity.this.mBluetoothGatt.setCharacteristicNotification(notifyCharacteristic, true)) {
                            List<BluetoothGattDescriptor> descriptors = notifyCharacteristic.getDescriptors();
                            for (BluetoothGattDescriptor descriptor : notifyCharacteristic.getDescriptors()) {
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                MainActivity.this.mBluetoothGatt.writeDescriptor(descriptor);
                                Log.d("----Blue", "描述 UUID :" + descriptor.getUuid().toString());

                            }
                            Log.d("----Blue", "startRead: " + "监听接收数据开始");
                        }

                        MainActivity.this.BlueLinkFlag = true;
                        //MainActivity.this.mBluetoothGatt = gatt;
                        Log.v("----Blue", "蓝牙已连接");
                        MainActivity.this.blueToothStaticFlag.setText("设备已连接");

                        //添加是否显示矩阵的代码
                        LoadIconUtil.closeLoadIcon(MainActivity.this.avi);
                        break;
                    }
                }
                //没有相关的UUID
                if(gattServicesList.size() == i){
                    Log.v("----Blue", "没有相关UUID");
                    MainActivity.this.blueToothStaticFlag.setText("设备连接错误");
                    BlueLinkFlag = false;
                    //添加是否显示矩阵的代码
                    LoadIconUtil.closeLoadIcon(MainActivity.this.avi);


                }
            } else {
                Log.v("----Blue", "连接失败");
                blueToothStaticFlag.setText("连接失败");
                //隐藏加载控件
                LoadIconUtil.closeLoadIcon(MainActivity.this.avi);
                MainActivity.this.BlueLinkFlag = false;
                //添加是否显示矩阵的代码


                Log.v("----Blue", "连接失败");
            }
        }

        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            //写入Characteristic
        }

        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            // 当接收到数据时
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) { // 接收数据成功时
                Log.w("----Blue", "12345a");
            }
        }

        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            // For all other profiles, writes the data formatted in HEX.
            Log.w("----Blue", "收到数据:");
            final byte[] data = characteristic.getValue();

            if (data != null && data.length > 0) {

                for(int i=0;i<data.length;i++){
                    Log.w("----Blue", " "+ Integer.toHexString(data[i]));
                }
            }
        }

        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.w("----Blue", "开启监听成功");

        }

        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            //读取Descriptor
            Log.w("----Blue", "12345c");

        }

    };

    //发送数据
    public void TraData(View view) {
        BluetoothGattService service = mBluetoothGatt.getService(UUID.fromString(MainActivity.this.SERVICES_UUID));
        if (service == null) {
            Log.v("----Blue", "writeRXCharacteristic: Service not supported");
        }
        //获取特性
        assert service != null;
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(MainActivity.this.WRITE_UUID));
        if (characteristic == null) {
            Log.v("----Blue", "writeRXCharacteristic: Service not supported");
        }
        assert characteristic != null;
        //将字符串command转Byte后进行写入
        String txBuff = "AT+NAME?";
        byte[] DataBuff = {0x00,0x02,0x25,0x36};

        //characteristic.setValue(Arrays.toString(DataBuff));


        characteristic.setValue(DataBuff);

        //写入类型  WRITE_TYPE_DEFAULT  默认有响应， WRITE_TYPE_NO_RESPONSE  无响应。
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            //如果权限没获取就获取权限
            mBlueToothController.getPermissions(MainActivity.this);
            return;
        }
        boolean result = mBluetoothGatt.writeCharacteristic(characteristic);
        Log.v("----Blue","status:"+result);
        if(result){
            Log.v("----Blue", "发送成功");
        }

        int properties = characteristic.getProperties();//16
        Log.v("----Blue", "properties:" + properties);
    }

    public boolean sendRockerData(int LeftX,int LeftY,int RightX,int RightY){
        int byteL = LeftX & 0XFF;
        int byteH = (LeftX & 0XFFFF) >>> 8;
        byte[] DataBuff = {
                (byte) 0XAA, (byte) 0xAA,
                ((byte) (LeftX & 0XFF)),     ((byte) (LeftX >> 8 & 0XFF)),
                ((byte) (LeftY & 0XFF)),     ((byte) (LeftY >> 8 & 0XFF)),
                ((byte) (RightX & 0XFF)),    ((byte) (RightX >> 8 & 0XFF)),
                ((byte) (RightY & 0XFF)),    ((byte) (RightY >> 8 & 0XFF)),
                (byte) 0xBB, (byte) 0xBB,
        };

        BluetoothGattService service = mBluetoothGatt.getService(UUID.fromString(MainActivity.this.SERVICES_UUID));
        if (service == null) {
            Log.v("----Blue", "writeRXCharacteristic: Service not supported");
        }
        //获取特性
        assert service != null;
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(MainActivity.this.WRITE_UUID));
        if (characteristic == null) {
            Log.v("----Blue", "writeRXCharacteristic: Service not supported");
        }
        assert characteristic != null;
        characteristic.setValue(DataBuff);

        //写入类型  WRITE_TYPE_DEFAULT  默认有响应， WRITE_TYPE_NO_RESPONSE  无响应。
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            //如果权限没获取就获取权限
            mBlueToothController.getPermissions(MainActivity.this);
            return false;
        }
        boolean result = mBluetoothGatt.writeCharacteristic(characteristic);
        Log.v("----Blue","status:"+result);
        if(result){
            Log.v("----Blue", "发送成功");
            return true;
        }else{
            return false;
        }
    }
}



