package cn.zhuchuangsoft.footstone.utils;

import cn.zhuchuangsoft.footstone.entity.warming.Warming;
import cn.zhuchuangsoft.footstone.service.IDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ClassName:WarmingMsgUtils
 * Package:cn.zhuchuangsoft.footstone.utils
 * Title:
 * Dsicription:
 * 用来维护warmingMsg
 *
 * @Date:2020/5/4 15:17)
 * @Author:1012518118@qq.com
 */
@Component
public class WarmingMsgUtils {

    private static IDeviceService deviceServiceImpl;
    @Autowired
    public void setIDeviceService(IDeviceService deviceServiceImpl){
        WarmingMsgUtils.deviceServiceImpl = deviceServiceImpl;
    }

    /**
     * 根据WarmingCode等信息，获取warmingMsg
     *
     * @param warming
     * @return
     */
    public static Warming getWarmingMsg(Warming warming) {
        if (warming == null)
            return warming;
        StringBuilder msg = new StringBuilder();
        String time = getString2WarmingMsgtime(warming.getWarmingTime());
        String warmingCode = warming.getWarmingCode();
        String deviceCode = warming.getDeviceCodes();
        if (deviceCode == null || "".equals(deviceCode))
            return warming;
        String name = deviceServiceImpl.selectDeviceName(deviceCode);
        String installPlace = deviceServiceImpl.selInstallPlaceByDeviceCode(warming.getDeviceCodes());

        // 根据warmingCode进行报警 警告和离线进行区分，进行不同的msg处理
//        if (warmingCode.endsWith("故障")) {
            msg.append(time)
//                    .append("发生<span style=font-size:16px;color:red;font-weight:bold>")
//                    .append("</span>")
                    .append(installPlace)
                    .append(name)
                    .append("发生")
//                    .append("发生<span style=font-size:16px;color:red;font-weight:bold>")
                    .append(warmingCode);
//                    .append("</span>");
//        } else if (warmingCode.endsWith("告警")) {
//            msg.append(time).append(",").append(name).append(installPlace).append("发出").append(warmingCode);
//        } else {
//            msg.append(time).append(",").append(name).append(installPlace).append("出现").append(warmingCode);
//        }
        warming.setWarmingMsg(msg.toString());

        return warming;
    }

    static String getString2WarmingMsgtime(String date) {
        if (date == null || date == "")
            return date;
        return date2String(string2Date(date, "yyyy-MM-dd HH:mm:ss"), "yyyy年MM月dd日HH时mm分，");
    }

    public static Date string2Date(String date, String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        Date reDate = null;
        try {
            reDate = dateFormat.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return reDate;
    }

    public static String date2String(Date date, String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        String dateString = dateFormat.format(date);
        return dateString;
    }
}
