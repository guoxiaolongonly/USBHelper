package cn.xiaolongonly.usbhelper.usbmodule;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

/**
 * <权限工具>
 *
 * @author xiaolong 719243738@qq.com
 * @version v1.0
 * @since 2019/4/11 16:41
 */
public class USBPermissionUtil {
    private static final String TAG = USBPermissionUtil.class.getSimpleName();
    public static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private PermissionReceiver mUsbReceiver = new PermissionReceiver();
    private Context mContext;
    private final static USBPermissionUtil M_USB_PERMISSION_UTIL = new USBPermissionUtil();

    public static USBPermissionUtil getInstance() {
        return M_USB_PERMISSION_UTIL;
    }

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
}
