package cn.xiaolongonly.usbhelper;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import cn.xiaolongonly.usbhelper.usbmodule.USBMonitor;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        USBMonitor.init(getApplicationContext());
        //写一个USBMonitor把之前的功能聚合，然后可以在做一些扩展。
        USBMonitor usbMonitor=USBMonitor.getInstance();
        usbMonitor.findDeviceList(); //——获取列表
        usbMonitor.connectState();//当前连接状态
//        usbMonitor.findDevice(); //获取指定设备
//        usbMonitor.permissionConnect(); //连接指定设备

       // usbMonitor.sendData();//发送数据

    }

}
