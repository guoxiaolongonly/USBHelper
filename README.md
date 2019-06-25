## 背景知识
USB是一种数据通信方式，也是一种数据总线，而且是最复杂的总线之一。 
硬件上，它是用插头连接。一边是公头（plug），一边是母头（receptacle）。例如，PC上的插座就是母头，USB设备使用公头与PC连接。 
目前USB硬件接口分三种，普通PC上使用的叫Type；原来诺基亚功能机时代的接口为Mini USB；目前Android手机使用的Micro USB/TYPE-C。

####  Host
USB是由Host端控制整个总线的数据传输的。单个USB总线上，只能有一个Host。 
#### OTG 
On The Go，这是在USB2.0引入的一种mode，提出了一个新的概念叫主机协商协议（Host Negotiation Protocol），允许两个设备间商量谁去当Host。

想了解更多USB知识，请参考USB官网以及下面这篇文章： 
http://www.crifan.com/files/doc/docbook/usb_basic/release/html/usb_basic.html

#### USB HOST/DEVICE/OTG概念：

1.USB 设备分为 HOST (主设备)和SLAVE (从设备),只有当一台 HOST 与一台 SLAVE 连接时才能实现数据的传输。
(1) USBHOST 是指主机。
(2) USBOTG 设备既能做主机,又能做设备。 OTG 技术就是实现在没有 Host 的情况下,实现从设备间的数据传送。
当 OTG 插到电脑上时. OTG 的角色就是连接电脑的 device (读卡器),也就是 SLAVE (从设备);当USB/SD device插到 OTG 上, OTG 的角色就是 HOST 主机。（有些手机也经常用到 OTG的功能）

### Android使用（Host连接）
首先应该介绍，USBManager这个类，这个类用于访问USB状态并与USB设备通信。目前只开放HOST模式。
 初始化USBManager mUsbManager = ((UsbManager) context.getSystemService(USB_SERVICE));
里面有几个重要的方法：

1.getDeviceList()——获取当前USB连接从设备的列表，返回一个存储UsbDevice的HashMap<String,UsbDevice>
2.getAccessoryList()——这个就是获取Host的列表（用不到）
3.openDevice(UsbDevice)—— 这个方法是USB连接方法，连接成功后会返回一个UsbDeviceConnection对象。
4.requestPermission(UsbDevice,PendingIntent)——这个方法用于请求USB权限。

#### 监听USB插入/拔出
通过广播意图android.hardware.usb.action.USB_STATE，实现插入拔出监听
```java
  class USBStateReceive extends BroadcastReceiver {
        public static final String ACTION_USB_STATE = "android.hardware.usb.action.USB_STATE";
        public static final String USB_CONNECTED = "connected";
        private IUsbStateChangeListener mUsbStateChangeListener;


        public void onReceive(Context paramContext, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "usb action + " + action);
            switch (action) {
                case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    Log.d(TAG, "device_attached," + device.toString());
                    if (mUsbStateChangeListener != null) {
                        mUsbStateChangeListener.onAttach(device);
                    }
                    break;
                case UsbManager.ACTION_USB_DEVICE_DETACHED:
                    device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    Log.d(TAG, "device_detached," + device.toString());
                    if (mUsbStateChangeListener != null) {
                        mUsbStateChangeListener.onDetach(device);
                    }
                    break;
                case ACTION_USB_STATE:
                    boolean connected = intent.getBooleanExtra(USB_CONNECTED, false);
                    break;
            }

        }

        public void setUsbStateChangeListener(IUsbStateChangeListener usbStateChangeListener) {
            this.mUsbStateChangeListener = usbStateChangeListener;
        }
    }

```
这个广播也可以获取到当前插入/拔出的设备信息。可以通过这个广播来监听新插入/拔出的USB设备。


#### 当前连接的USB设备
可以通过getDeviceList获取当前连接的USB设备
为此我们编写一个USBFinder 用来查找USB设备。
```java

package cn.xiaolongonly.usbhelper.usbmodule;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import java.util.HashMap;
import java.util.Iterator;

/**
 * <USB设备查询器>
 *
 * @author xiaolong 719243738@qq.com
 * @version v1.0
 * @since 2019/4/10 15:36
 */
public class USBFinder {
    private UsbManager mUsbManager;
    private static USBFinder usbFinder;

    private USBFinder(UsbManager usbManager) {
        this.mUsbManager = usbManager;
    }

    /**
     * Application Context;
     *
     */
    public static void init(UsbManager usbManager) {
        usbFinder = new USBFinder(usbManager);
    }

    public static USBFinder getInstance() {
        if (usbFinder == null) {
            throw new RuntimeException("需要在Application中執行USBFinder.init()");
        }
        return usbFinder;
    }

    public HashMap<String, UsbDevice> findUsbMap() {
        HashMap<String, UsbDevice> deviceMap = mUsbManager.getDeviceList();
        return deviceMap;
    }


    public UsbDevice findUsbDevice(int vendorId, int productId) {
        HashMap<String, UsbDevice> deviceMap = mUsbManager.getDeviceList();
        Iterator iterator = deviceMap.values().iterator();
        UsbDevice localUsbDevice;
        while (iterator.hasNext()) {
            localUsbDevice = (UsbDevice) iterator.next();
            if (localUsbDevice.getVendorId() == vendorId || localUsbDevice.getProductId() == productId) {
                return localUsbDevice;
            }
        }
        return null;
    }
}

```
1. findUsbMap 用于找到当前已连接的USB设备列表，这个用户选择连接非常有用，比如有多个USB设备，选择一个连接。（跟上一个通过广播监听的区别是，有时候一些USB设备默认是插着的，通过上面的广播并不能知道）
2. findUsbDevice 遍历设备列表，通过vendorId和productId找到合适的USB设备，通过这个可以省掉让用户选择的过程，如果连接的设备只有一个的话可以直接用这个。


#### USB连接，如何确认USB连接及USB授权？
Android 在USBManager 提供了一个 requestPermission(UsbDevice device, PendingIntent pi)，这个模式是手机作为HOST对设备做授权。

```java
    public void getPermission(Context context, UsbManager usbManager, UsbDevice usbDevice, final UsbPermissionReceiverListener callBack) {
        if (usbManager.hasPermission(usbDevice)) {
            callBack.onSuccessful();
            return;
        }
        mContext = context;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.android.example.USB_PERMISSION");
        mContext.registerReceiver(mUsbReceiver, intentFilter);//注册广播接收者
        mUsbReceiver.setUsbPermissionReceiverListener(new UsbPermissionReceiverListener() {
            @Override
            public void onSuccessful() {
                mContext.unregisterReceiver(mUsbReceiver);
                callBack.onSuccessful();
            }

            @Override
            public void onFail(String msg) {
                mContext.unregisterReceiver(mUsbReceiver);
                callBack.onFail(msg);
            }

        });
        mUsbReceiver.setUsbDeviceName(usbDevice.getDeviceName());//为广播接受者设置DeviceName名称用于与接收的名称进行对比
        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
        usbManager.requestPermission(usbDevice, mPermissionIntent);
    }
```
授权广播
```java
 class PermissionReceiver extends BroadcastReceiver {

        protected UsbPermissionReceiverListener mUsbPeramissionRecevierListener = null;
        protected String mUsbDeviceName = "";

        public void onReceive(Context paramContext, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "usb action + " + action);

            if (intent == null) {
                mUsbPeramissionRecevierListener.onFail("授权失败：未知授权申请！");
            } else {
                if (ACTION_USB_PERMISSION.equals(intent.getAction())) {
                    UsbDevice localUsbDevice = intent.getParcelableExtra("device");
                    if (localUsbDevice != null) {
                        if (!(intent.getBooleanExtra("permission", false))) {//用户取消授权
                            if (this.mUsbPeramissionRecevierListener != null) {
                                mUsbPeramissionRecevierListener.onFail("用户取消授权，授权失败！");
                            }
                        } else {
                            String deviceName = localUsbDevice.getDeviceName();
                            if (deviceName.equals(this.mUsbDeviceName)) {//若获得的deviceName与_usbDeviceName一致
                                if (this.mUsbPeramissionRecevierListener != null) {
                                    mUsbPeramissionRecevierListener.onSuccessful();
                                }
                            } else {
                                if (this.mUsbPeramissionRecevierListener != null) {
                                    mUsbPeramissionRecevierListener.onFail("授權失敗：設備名稱不一致！");
                                }
                            }
                        }
                    } else {
                        return;
                    }
                } else {
                    mUsbPeramissionRecevierListener.onFail("授權失敗：未知授权申请！");
                    return;
                }
            }
        }


        public void setUsbPermissionReceiverListener(UsbPermissionReceiverListener paramAsyncTaskListener) {
            this.mUsbPeramissionRecevierListener = paramAsyncTaskListener;
        }

        public void setUsbDeviceName(String usbDeviceName) {
            this.mUsbDeviceName = usbDeviceName;
        }
    }
```
授权广播类，监听的是com.android.example.USB_PERMISSION

通过以上几个方法我们可以获取到当前连接的USB设备了，接下来就是建立传输通道

#### 数据传输

这边先要获取一个UsbDeviceConnection 对象， 也就是通过USBManager.openDevice(UsbDevice)获取连接的对象。

```java
    /**
     * 选择USB设备和interface,打开。
     *
     * @return
     */
    public int open(UsbDevice usbDevice, int interfaceIndex) {
        mDevice = usbDevice;
        if (!setInterface(this.mDevice, interfaceIndex)) {
            Log.d(TAG, "interfaceIndex OutOfBound!");
            return -1;
        }
        this.mInterface = mDevice.getInterface(interfaceIndex);
        if (!(setEndpoint(this.mInterface))) {
            return -1;
        }
        this.mDeviceConnection = this.mManager.openDevice(usbDevice);
        if (this.mDeviceConnection == null) {
            return -1;
        }
        return 0;
    }
```

讲一下为什么这个类需要传interfaceIndex，一般来讲一个UsbDevice 会有多个UsbInterface， 不同的interface是分开的，有不同的功能。每个UsbInterface又会有多个UsbEndpoint，用于处理输入和输出，也就是数据传输。

检验interface和endpoint 是否符合
```java
  protected boolean setInterface(UsbDevice mDevice, int interfaceIndex) {
        if (mDevice != null && mDevice.getInterfaceCount() > interfaceIndex) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * 设置Endpoint,根据Device的Interface
     *
     * @param paramUsbInterface
     * @return
     */
    protected boolean setEndpoint(UsbInterface paramUsbInterface) {
        if (paramUsbInterface.getEndpointCount() != 2) {
            Log.d(TAG, "Endpoint count less than 2..");
            return false;
        }
        for (int m = 0; m < paramUsbInterface.getEndpointCount(); m++) {
            UsbEndpoint ep = paramUsbInterface.getEndpoint(m);
            if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                    this.mEndpointOut = ep;
                } else {
                    this.mEndpointIn = ep;
                }
            } else {
                return false;
            }
        }
        return true;
    }
```

这两个方法就是用来看是不是能找到对应的UsbInterface和UsbEndpoint，因为我们的需求是要这个既有输入又有输出的UsbInterface。所以做了相应的校验。

接下来是数据传输
```java
    public void sendData(byte[] data, IDataSendCallBack iDataSendCallBack, int timeOut) {
        if (this.mDeviceConnection == null) {
            if (iDataSendCallBack != null) {
                iDataSendCallBack.onSendResult(CODE_DEVICE_NO_OPEN, null, "未开启设备，请先执行open()！");
            }

        }
        this.mDeviceConnection.claimInterface(this.mInterface, true);//调用了本地
        bulkTransfer(this.mDeviceConnection, this.mEndpointOut, this.mEndpointIn, data, data.length, timeOut, iDataSendCallBack);
        this.mDeviceConnection.releaseInterface(this.mInterface);//释放接口

    }

    protected void bulkTransfer(final UsbDeviceConnection usbDeviceConnection, UsbEndpoint outPoint, final UsbEndpoint inPoint, byte[] datas, int dataLength, final int timeLimit, final IDataSendCallBack iDataSendCallBack) {
        int bulkTransferLength = usbDeviceConnection.bulkTransfer(outPoint,
                datas, dataLength, timeLimit);
        if (bulkTransferLength < 0) {
            if (iDataSendCallBack != null) {
                iDataSendCallBack.onSendResult(CODE_SEND_FAIL, datas, "发送失败！");
            }
        }
        if (iDataSendCallBack != null) {
            iDataSendCallBack.onSendResult(CODE_SEND_SUCCESS, datas, "發送成功！");

        }
        if (mEndpointIn == null) {
            if (iDataSendCallBack != null) {
                iDataSendCallBack.onSendResult(CODE_NO_INPOINT, null,
                        "該interface底下沒有接口端口，无法接收数据！");
            }
        }
        new Thread(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    byte[] inByte = new byte[1024];
                    int inLength = usbDeviceConnection.bulkTransfer(inPoint,
                            inByte, inByte.length, timeLimit);
                    if (inLength > 0) {
                        inByte = Arrays.copyOf(inByte, inLength);
                        if (iDataSendCallBack != null) {
                            iDataSendCallBack.onSendResult(CODE_DATA_RECEIVE,
                                    inByte, "收到数据！");
                        }
                    } else {
                        break;
                    }
                }
            }
        }).start();
    }
```
DeviceConnection.claimInterface()占用了这个interface，在发送结束后releaseInterface 相当于释放interface.
项目要求是一次输入，一次输出。所以每次都会开开合合，如果不是这种需求的话应该可以长期开着等全部结束后关闭，类似于Socket。

Talk is cheap, here the code.

