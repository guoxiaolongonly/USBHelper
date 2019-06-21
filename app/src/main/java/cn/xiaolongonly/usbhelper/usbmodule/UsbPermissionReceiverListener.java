package cn.xiaolongonly.usbhelper.usbmodule;

/**
 * <描述功能>
 *
 * @author xiaolong 719243738@qq.com
 * @version v1.0
 * @since 2019/4/11 16:41
 */
public interface UsbPermissionReceiverListener {
    void onSuccessful();

    void onFail(String msg);
}