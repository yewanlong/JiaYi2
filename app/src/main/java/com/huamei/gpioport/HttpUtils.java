package com.huamei.gpioport;

import java.util.Date;

/**
 * Created by Lkn on 2018/3/21.
 */

public class HttpUtils {
    protected final static String TCP_URL = "zxy.vpandian.com";
    public final static String TCP_IP = TCP_URL;
    protected final static int TCP_PRO = 1368;
    public final static int TCP_PRO_IP = TCP_PRO;
    public static String IMEI = "868575021770443";

    public static String getDoor(String fan_id, String imei, String type, String status) {
        return "Action=Door&Imei=" + imei + "&fan_id=" + fan_id + "&type="
                + type + "&MsgId=1&Timer=" + new Date().getTime() + "&status=" + status;
    }

    public static String getCheckIn(long msgId, String imei) {
        return "Action=CheckIn&Imei=" + imei + "&MsgId=" + msgId + "&Timer="
                + new Date().getTime() + "&devicefrom=1";
    }

    public static String getCSQ(long msgId, String imei) {
        return "Action=CSQ&Imei=" + imei + "&MsgId=" + msgId + "&Timer="
                + new Date().getTime();
    }
}
