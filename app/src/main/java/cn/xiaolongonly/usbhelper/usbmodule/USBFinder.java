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
