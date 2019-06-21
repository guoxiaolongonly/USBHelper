package cn.xiaolongonly.usbhelper.usbmodule;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.HashMap;

import static android.content.Context.USB_SERVICE;
import static cn.xiaolongonly.usbhelper.usbmodule.USBMonitor.USBStateReceive.ACTION_USB_STATE;

/**
 * <描述功能>
 *
 * @author xiaolong 719243738@qq.com
 * @version v1.0
 * @since 2019/4/15 9:24
 */
public class USBMonitor {
    private static final String TAG = USBMonitor.class.getSimpleName();
    public static final int DEAULT_WAIT_TIME = 10;
    public static final int DEFAULT_MAX_SEND_TIMES = 2;
    private Context mContext;
    private static USBMonitor instance = null;
    private UsbManager mUsbManager;
    private boolean mOpenState = false;
    private UsbOTGService mService;
    private USBFinder mUsbFinder;

    private USBStateReceive usbStateReceive;

    public static USBMonitor getInstance() {
        return instance;
    }

    public static void init(Context context) {
        instance = new USBMonitor(context);
    }


    /**
     * 初始化，判断蓝牙状态
     *
     * @param context
     */
    private USBMonitor(Context context) {
        this.mContext = context;
        mUsbManager = ((UsbManager) context.getSystemService(USB_SERVICE));
        UsbOTGService.init(mUsbManager);
        mService = UsbOTGService.getInstance();
        USBFinder.init(mUsbManager);
        mUsbFinder = USBFinder.getInstance();
        registerReceiver(mContext);
    }


    /**
     * 注册蓝牙扫描广播接收器
     */
    private void registerReceiver(Context context) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        intentFilter.addAction(ACTION_USB_STATE);
        context.registerReceiver(usbStateReceive = new USBStateReceive(), intentFilter);
    }

    public void setUsbStateChangeListener(IUsbStateChangeListener usbStateListener) {
        if (usbStateReceive != null) {
            usbStateReceive.setUsbStateChangeListener(usbStateListener);
        }
    }


    /**
     * 通过权限开启
     *
     * @param usbDevice
     * @param interfaceIndex
     */
    public void permissionConnect(final UsbDevice usbDevice, final int interfaceIndex, final UsbPermissionReceiverListener usbPermissionReceiverListener) {
        if (usbDevice == null) {
            usbPermissionReceiverListener.onFail("未找到USB设备，启动失败");
            return;
        }
        if (!mUsbManager.hasPermission(usbDevice)) {
            USBPermissionUtil.getInstance().getPermission(mContext, mUsbManager, usbDevice, new UsbPermissionReceiverListener() {
                @Override
                public void onSuccessful() {
                    openUsbDevice(usbDevice, interfaceIndex, usbPermissionReceiverListener);
                }

                @Override
                public void onFail(String msg) {
                    usbPermissionReceiverListener.onFail(msg);
                    mOpenState = false;
                }
            });
        } else {
            openUsbDevice(usbDevice, interfaceIndex, usbPermissionReceiverListener);
        }

    }

    /**
     * 开启打开USB
     *
     * @param usbDevice
     * @param interfaceIndex
     * @param usbPermissionReceiverListener
     */
    private void openUsbDevice(UsbDevice usbDevice, int interfaceIndex, UsbPermissionReceiverListener usbPermissionReceiverListener) {
        int openCode = mService.open(usbDevice, interfaceIndex);
        if (openCode != 0) {
//            Toast.makeText(mContext, "打开失败," + openCode, Toast.LENGTH_SHORT).show();
            usbPermissionReceiverListener.onFail("打开失败," + openCode);
            mOpenState = false;
        } else {
            mOpenState = true;
            usbPermissionReceiverListener.onSuccessful();
        }
    }


    public void sendData(byte[] data, UsbOTGService.IDataSendCallBack iDataSendCallBack, int timeOut) {
        if (mService == null) {
            return;
        }
        mService.sendData(data, iDataSendCallBack, timeOut);
    }

    public void disconnected() {
        if (mService != null) {
            mService.close();
        }
        instance = null;
        destroyReceiver();
    }


    /**
     * 獲取全部鏈接USB設備
     *
     * @return
     */
    public HashMap<String, UsbDevice> findDeviceList() {
        return mUsbFinder.findUsbMap();
    }


    /**
     * 獲取單個USB設備
     *
     * @return
     */
    public UsbDevice findDevice(int vendorId, int productId) {
        return mUsbFinder.findUsbDevice(vendorId, productId);
    }


    /**
     * 解除广播接收器
     */
    private void destroyReceiver() {
        if (usbStateReceive != null) {
            mContext.unregisterReceiver(usbStateReceive);
        }

    }

    /**
     * USB接入的兩種狀態 但是接入設備不一定是指定設備。
     * attach 接入
     * detach 移除
     */
    public interface IUsbStateChangeListener {
        void onAttach(UsbDevice device);

        void onDetach(UsbDevice device);
    }

    public boolean connectState() {
        return mOpenState;
    }

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

}
