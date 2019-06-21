package cn.xiaolongonly.usbhelper.usbmodule;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.Arrays;

/**
 * <底层控制工具>
 *
 * @author xiaolong 719243738@qq.com
 * @version v1.0
 * @since 2019/4/11 9:45
 */
public class UsbOTGService {
    private static final String TAG = UsbOTGService.class.getSimpleName();

    public final static int CODE_DATA_RECEIVE = 0x61;
    public final static int CODE_DEVICE_NO_OPEN = 0x65;
    public final static int CODE_SEND_SUCCESS = 0X66;
    public final static int CODE_NO_INPOINT = 0X67;
    public final static int CODE_SEND_FAIL = 0X68;
    public final static int CODE_OUT_OF_TIME = 0X69;
    private UsbManager mManager;
    private UsbDevice mDevice;
    private UsbDeviceConnection mDeviceConnection;
    private UsbEndpoint mEndpointIn;
    private UsbEndpoint mEndpointOut;
    private UsbInterface mInterface;

    private static UsbOTGService usbOTGService;



    private UsbOTGService(UsbManager usbManager) {
        this.mManager = usbManager;
    }

    public static void init(UsbManager usbManager) {
        usbOTGService = new UsbOTGService(usbManager);
    }

    public static UsbOTGService getInstance() {
        if (usbOTGService == null) {
            throw new RuntimeException("需要在Application中執行UsbOTGService.init()");
        }
        return usbOTGService;
    }

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

    /**
     * 关闭mDeviceConnection
     */
    public void close() {
        this.mDeviceConnection.close();
    }


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

    public interface IDataSendCallBack {
        void onSendResult(int code, byte[] data, String msg);
    }
}
