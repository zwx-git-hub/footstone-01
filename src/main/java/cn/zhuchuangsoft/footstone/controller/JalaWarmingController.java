package cn.zhuchuangsoft.footstone.controller;

import cn.zhuchuangsoft.footstone.entity.Device;
import cn.zhuchuangsoft.footstone.entity.DeviceLine;
import cn.zhuchuangsoft.footstone.entity.Lines;
import cn.zhuchuangsoft.footstone.entity.device.Device1;
import cn.zhuchuangsoft.footstone.entity.warming.Warming;
import cn.zhuchuangsoft.footstone.service.IDeviceService;
import cn.zhuchuangsoft.footstone.service.IDeviceWarmingService;
import cn.zhuchuangsoft.footstone.service.IUserService;
import cn.zhuchuangsoft.footstone.service.IWarmingService;
import cn.zhuchuangsoft.footstone.utils.WarmingMsgUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 拉取Jala设备信息和报警信息
 */
@Component
@Slf4j

public class JalaWarmingController extends BaseController {
    @Autowired
    private IDeviceWarmingService deviceWarmingService;
    @Autowired
    IUserService userServiceImpl;
    @Autowired
    IDeviceService deviceServiceImpl;
    @Autowired
    IWarmingService warmingServiceImpl;

    /**
     * 拉取jala设备的时时数据
     * 0/3 * * * * ?  10s拉取一次
     */
//    @Scheduled(cron = "0/50 * * * * ?")
    @Scheduled(fixedDelay = 1000)//上一次执行完毕时间点3秒再次执行
    @Async
    public void getPullDevice() {

        // 循环拉取数据 获取Temp(温度)Power(功率)信息
        String authorization = getAuthorization(null);
        String url = "http://ex-api.jalasmart.com/api/v2/devices/" + ID;
        try {
            HttpRequestBase httpRequestBase = getRequestByUrl(url, authorization, 1);
            String json = getJson(httpRequestBase);
            JSONObject jsonObject = JSON.parseObject(json);
            JSONArray data = jsonObject.getJSONArray("Data");
            List<Device> datas = new ArrayList<>();
            Integer count = data.size();
            for (int i = 0; i < count; i++) {
                JSONObject js = data.getJSONObject(i);
                Device device = new Device(js);
                datas.add(device);
            }
            if (!datas.isEmpty()) {
                for (Device d : datas) {
                    List<String> parameter = new ArrayList<String>();
                    String sn = new String(d.getSn());
                    parameter.add("SN=" + sn);
                    String authorization2 = getAuthorization(parameter);
                    String url2 = "http://ex-api.jalasmart.com/api/v2/devices/" + sn;
                    HttpRequestBase httpRequestBase2 = getRequestByUrl(url2, authorization2, 1);
                    String json2 = getJson(httpRequestBase2);
                    JSONObject jsonObject2 = JSON.parseObject(json2);
                    JSONObject data2 = jsonObject2.getJSONObject("Data");
                    Device device = new Device(data2);
                    List<DeviceLine> lines = device.getLines();
                    List<String> massages = new ArrayList<>();
                    for (DeviceLine line : lines) {
                        // 获取deviceCode ,若没有则为空。
//                        String deviceCode = deviceServiceImpl.selectDeviceCode(d.getDeviceId(), line.getLineNo().toString());
                        // 若没有deviceCode添加新增的设备。（地址手机方面暂定假数据）
//                        if (deviceCode!=null|| "".equals(deviceCode)) {


                        String deviceCode = 2 + "-" + line.getModel() + "-" + line.getLineID() + "-" + d.getSn();

//                        if (!deviceServiceImpl.existDeviceCode(deviceCode)) {
//                            Device1 device1 = new Device1();
//                            device1.setDeviceCode(deviceCode);
//                            device1.setDeviceId(d.getDeviceId());
//                            device1.setControllerId(d.getControllerId());
//                            device1.setSn(sn);
//                            device1.setLineNo(line.getLineNo().toString());
//                            device1.setDeviceName(line.getName());
//
//                            // devicetype属性
//                            DeviceType typeCode = new DeviceType();
//                            typeCode.setId(1);
//                            typeCode.setDeviceTypeCode("2");
//                            typeCode.setDeviceTypeName("集成用电设备");
//                            typeCode.setDeviceTypeHistoryTableName("t_Jala_history");
//                            typeCode.setDeviceTypeTableName("t_Jala");
//                            device1.setTypeCode(typeCode);
//
//                            device1.setMobile("15070304182");
//                            device1.setDeviceCity("温州市");
//                            device1.setDeviceProvince("浙江省");
//                            device1.setDeviceCounty("平阳县");
//                            device1.setDeviceAddress("浙江越创电子科技有限公司");
//                            device1.setLat("120.56154");
//                            device1.setLon("27.613918");
//                            device1.setDeviceState("正常");
//
//                            // proxy 代理商编码
//                            Proxy proxy = new Proxy();
//                            proxy.setId(2);
//                            proxy.setProxyName("代理商2");
//                            proxy.setProxyMobile("45647645");
//                            device1.setProxy(proxy);
//                            device1.setOperater("admin");
//                            device1.setIsDelete(0);
//                            deviceServiceImpl.increaseDevice(device1);
//                        }


                        String warmingCode2temp = "";
                        // 获取设定过温进行对
                        String highTemp = warmingServiceImpl.selectHighTemp(deviceCode);
                        if (null != highTemp && !"".equals(highTemp)) {
                            int highTempNum = Integer.parseInt(highTemp);
                            if (line.getModel().startsWith("3P")) {
                                boolean flag = false;
                                // 三相的判断方式推送过温消息
                                if (highTempNum <= line.getTempA()) {
                                    warmingCode2temp += "A相";
                                    flag = true;
                                }
                                if (highTempNum <= line.getTempB()) {
                                    warmingCode2temp += "B相";
                                    flag = true;
                                }
                                if (highTempNum <= line.getTempC()) {
                                    warmingCode2temp += "C相";
                                    flag = true;
                                }
                                if (highTempNum <= line.getTempN()) {
                                    warmingCode2temp += "D相";
                                    flag = true;
                                }
                                if (flag == true) {
                                    warmingCode2temp += "过温";
                                }

                            } else if (line != null && line.getModel().startsWith("1P")) {
                                // 单相的判断方式推送过温消息
                                if (highTempNum <= line.getTemp()) {
                                    warmingCode2temp = "过温";
                                }
                            } else {
                                // 其他型号。
                            }

                            // 过温信息不为空
                            if (!"".equals(warmingCode2temp)) {
                                handingWarming(deviceCode, warmingCode2temp);
                                // 过温关掉开关
                                turnOffSwitch(deviceCode);
                            }
                        }


                        // 获取设定功率进行对比
                        String highPower = warmingServiceImpl.selectHightPower(deviceCode);
                        if (null != highPower && !"".equals(highPower)) {
                            Double highPowerNum = Double.parseDouble(highPower);
                            Double power = line.getPower();
                            // 判断是否正常
                            if (highPowerNum <= power) {
                                handingWarming(deviceCode, "功率过高");
                                // 功率过高
                                turnOffSwitch(deviceCode);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 处理产生的warming
     *
     * @return
     */
    private void handingWarming(String deviceCode, String warmingCode) {
        Warming warming = new Warming();
        warming.setDeviceCodes(deviceCode);
        warming.setWarmingTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        warming.setWarmingCode(warmingCode);
        warming = WarmingMsgUtils.getWarmingMsg(warming);
        warmingServiceImpl.insertWarming(warming);
    }

    /**
     * 关闭开关
     *
     * @param deviceCode
     */
    private void turnOffSwitch(String deviceCode) {
        if (deviceCode == null && "".equals(deviceCode)) {
            return;
        }
        Device1 d = warmingServiceImpl.selectDevice1(deviceCode);

        String lineNo = deviceServiceImpl.getLineNoByDeviceCode(deviceCode);
//        String lineNo = deviceCode.split("-")[1];
        int lineNoNum = Integer.parseInt(lineNo);
        List<String> switchParameter = new ArrayList<String>();
        switchParameter.add("ControllerID=" + d.getControllerId());
        Lines switchline = new Lines(lineNoNum, 0);
        List<Lines> lineList = new ArrayList<>();
        lineList.add(switchline);
        switchParameter.add("Lines=" + JSONArray.toJSONString(lineList));
        String switchAuthorization = getAuthorization(switchParameter);
        String setSwitch = "http://ex-api.jalasmart.com/api/v2/status/" + d.getControllerId();
        HttpPut put = null;
        try {
            put = (HttpPut) getRequestByUrl(setSwitch, switchAuthorization, 3);
            JSONObject bodyJson = new JSONObject(true);
            bodyJson.put("ControllerID", d.getControllerId());
            JSONObject bodyJson2 = new JSONObject(true);
            bodyJson2.put("LineNo", lineNoNum);
            bodyJson2.put("Status", 0);
            JSONArray jsonArray = new JSONArray();
            jsonArray.add(bodyJson2);
            bodyJson.put("Lines", jsonArray);
            put.setEntity(new StringEntity(bodyJson.toJSONString()));
            getJson(put);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 拉取jala报警信息
     * 定时异步
     */
//    @Scheduled(cron = "0/5 * * * * ?")
    @Scheduled(fixedDelay = 1000)//上一次执行完毕时间点1秒再次执行
    @Async
    public void getPullWarming() {

//        // 获取设备用户信息 根据用户的信息获取ControllerId
//        /*String authorization = getAuthorization(null);
//        String url = "http://ex-api.jalasmart.com/api/v2/devices/" + ID;
//        try {
//            HttpRequestBase httpRequestBase = getRequestByUrl(url, authorization, 1);
//            String json = getJson(httpRequestBase);
//            JSONObject jsonObject = JSON.parseObject(json);
//            JSONArray data = jsonObject.getJSONArray("Data");
//            List<Device> datas = new ArrayList<>();
//            Integer count = data.size();
//            for (int i = 0; i < count; i++) {
//                JSONObject js = data.getJSONObject(i);
//                Device device = new Device(js);
//                datas.add(device);
//            }
//            for (Device d : datas) {*/
        try {
            // 获取对应的网关下的所有的设备告警信息
                List<String> parameter = new ArrayList<String>();
//                parameter.add("ControllerID=" + d.getControllerId());
            parameter.add("ControllerID=" + "5e0955c013d81a2d60137ea5");
                parameter.add("Page=" + 0);
            parameter.add("Limit=" + 10);
                String authorization2 = getAuthorization(parameter);
            String url2 = "http://ex-api.jalasmart.com/api/v2/messages/5e0955c013d81a2d60137ea5/" + 0 + "/" + 10;
//                String url2 = "http://ex-api.jalasmart.com/api/v2/messages/" + d.getControllerId() + "/" + 0;
                HttpRequestBase httpRequestBase2 = getRequestByUrl(url2, authorization2, 1);
                String json2 = getJson(httpRequestBase2);
                JSONObject jsonObject2 = JSON.parseObject(json2);
//                JSONArray data2 = jsonObject2.getJSONArray("Data");
            JSONObject jsonObject3 = jsonObject2.getJSONObject("Data");
            JSONArray data2 = jsonObject3.getJSONArray("Messages");
                if (data2.size() > 0) {
                    for (int i = 0; i < data2.size(); i++) {
                        // 用warming封装
//                        String deviceId = d.getDeviceId();
                        String deviceId = "5e1e9c8d4c1d6d2650e9aceb";
                        Integer lineNo = data2.getJSONObject(i).getInteger("LineNo");
                        long createTime = data2.getJSONObject(i).getDate("AddTime").getTime();
                        String code = purseCode(data2.getJSONObject(i).getString("Code"));

                        // code为空 是我们处理的警告。不需要拉
                        if ("".equals(code))
                            continue;
                        // 离线的警报 暂定为sn下所有的设备都创建一条报警信息。
                        if ("离线".equals(code)) {
//                            outLineWarming(d.getSn(),createTime);
                            outLineWarming("J191291284776", createTime);
                            continue;
                        }

                        String deviceCode = "";
                        Warming warming = new Warming();
                        if (lineNo != null) {
//                            String deviceCode = deviceServiceImpl.selectDeviceCode(deviceId, lineNo.toString(),d.getSn());
                            deviceCode = deviceServiceImpl.selectDeviceCode(deviceId, lineNo.toString(), "J191291284776");
                            warming.setDeviceCodes(deviceCode);
                        }

                        // 电弧预警的拦截，
                        if ("电弧预警".equals(code) && !"".equals(deviceCode)) {
                            String arc = warmingServiceImpl.selectARC(deviceCode);
                            if (!"1".equals(arc))
                                continue;
                        }

                        warming.setWarmingTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(createTime)));
                        warming.setWarmingCode(code);

                        warming = WarmingMsgUtils.getWarmingMsg(warming);
                        // 判断是否需要插入
                        if (warmingServiceImpl.isExsits(warming)) {
                            continue;
                        }
                        warmingServiceImpl.insertWarming(warming);
                    }
                }
//            }
        } catch (IOException e) {
            log.info(e.getMessage());
        }
    }

    /**
     * 处理离线警告
     *
     * @param sn
     * @param createTime
     */
    private void outLineWarming(String sn, long createTime) {
        List<Device1> devices = deviceServiceImpl.getDevicesBySn(sn);
        for (Device1 device : devices) {
            // 使用
            Warming warming = new Warming();
            warming.setDeviceCodes(device.getDeviceCode());
            warming.setWarmingCode("离线");
            warming.setWarmingTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(createTime)));
            StringBuilder msg = new StringBuilder();

            String installPlace = deviceServiceImpl.selInstallPlaceByDeviceCode(device.getDeviceCode());
            msg.append(new SimpleDateFormat("yyyy年MM月dd日HH时mm分，").format(createTime))

//                    .append("发生<span style=font-size:16px;color:red;font-weight:bold>")
//                    .append(device.getDeviceName())
//                    .append("</span>")
                    .append(installPlace)
                    .append(device.getDeviceName())
                    .append("发生")
//                    .append("发生<span style=font-size:16px;color:red;font-weight:bold>")
                    .append("离线");
//                    .append("</span>");

            warming.setWarmingMsg(msg.toString());
            if (warmingServiceImpl.isExsits(warming))
                continue;
            warmingServiceImpl.insertWarming(warming);
        }
    }

    /**
     * 处理code对应警告
     *
     * @param code
     * @return
     */
    String purseCode(String code) {
        String purseCode = "";
        if ("HV".equals(code)) {
            purseCode = "过压告警";
        }
        if ("LV".equals(code)) {
            purseCode = "欠压告警";
        }
        if ("HC".equals(code)) {
            purseCode = "过流告警";
        }
        if ("LEAK".equals(code)) {
            purseCode = "漏电告警";
        }
        if ("Err_HV".equals(code)) {
            purseCode = "过压故障";
        }
        if ("Err_LV".equals(code)) {
            purseCode = "欠压故障";
        }
        if ("Err_HC".equals(code)) {
            purseCode = "过流故障";
        }
        if ("Err_LEAK".equals(code)) {
            purseCode = "漏电故障";
        }
        if ("OFFLINE".equals(code)) {
            purseCode = "离线";
        }
        if ("TEST".equals(code)) {
            purseCode = "漏电自检";
        }
        if ("Err_ARC".equals(code)) {
            purseCode = "电弧故障";
        }
        if ("Test_ARC".equals(code)) {
            purseCode = "电弧探测器自检";
        }
        if ("Key_TEST".equals(code)) {
            purseCode = "三相按键测试";
        }
        if ("TRANS".equals(code)) {
            purseCode = "短路故障";
        }
        if ("OPEND".equals(code)) {
            purseCode = "缺相故障";
        }
        if ("HIGH_ARC".equals(code)) {
            purseCode = "电弧预警";
        }
        return purseCode;
    }

}
