package cn.zhuchuangsoft.footstone.controller;

import cn.zhuchuangsoft.footstone.controller.ex.ControlUnsuccessfulException;
import cn.zhuchuangsoft.footstone.controller.ex.ModificationNameException;
import cn.zhuchuangsoft.footstone.entity.Device;
import cn.zhuchuangsoft.footstone.entity.DeviceLine;
import cn.zhuchuangsoft.footstone.entity.Lines;
import cn.zhuchuangsoft.footstone.entity.device.WarmingSetting;
import cn.zhuchuangsoft.footstone.entity.warming.Warming;
import cn.zhuchuangsoft.footstone.service.IDeviceService;
import cn.zhuchuangsoft.footstone.service.IUserAndDeviceService;
import cn.zhuchuangsoft.footstone.service.IUserService;
import cn.zhuchuangsoft.footstone.service.IWarmingService;
import cn.zhuchuangsoft.footstone.utils.JsonResult;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;


@RestController
@RequestMapping("device")
public class DeviceController extends BaseController {

    @Autowired
    IDeviceService deviceServiceImpl;
    @Autowired
    IWarmingService warmingServiceImpl;
    @Autowired
    IUserService userServiceImpl;
    @Autowired
    IUserAndDeviceService userAndDeviceServiceImpl;

    @PostMapping("set-switch")
    @ApiOperation("控制产品开关")
    public JsonResult<String> setSwitch(@ApiParam("控制器ID") String controllerId, @ApiParam("线路编号") Integer lineNo, @ApiParam("0:关闭 1:开启") Integer status) {
        List<String> parameter = new ArrayList<String>();
        parameter.add("ControllerID=" + controllerId);
        Lines lines = new Lines(lineNo, status);
        List<Lines> lineList = new ArrayList<>();
        lineList.add(lines);
        parameter.add("Lines=" + JSONArray.toJSONString(lineList));
        String authorization = getAuthorization(parameter);
        String url = "http://ex-api.jalasmart.com/api/v2/status/" + controllerId;
        try {
            HttpPut put = (HttpPut) getRequestByUrl(url, authorization, 3);
            JSONObject bodyJson = new JSONObject(true);
            bodyJson.put("ControllerID", controllerId);
            JSONObject bodyJson2 = new JSONObject(true);
            bodyJson2.put("LineNo", lineNo);
            bodyJson2.put("Status", status);
            JSONArray jsonArray = new JSONArray();
            jsonArray.add(bodyJson2);
            bodyJson.put("Lines", jsonArray);
            put.setEntity(new StringEntity(bodyJson.toJSONString()));
            String json = getJson(put);
            JSONObject jsonObject = JSON.parseObject(json);
            Integer code = jsonObject.getInteger("Code");
            if (code != 1) {
                throw new ControlUnsuccessfulException("控制失败");
            }
            return new JsonResult<String >(SUCCESS);
        } catch (IOException e) {
            return new JsonResult<String >(FAILED,"控制失败");
        }

    }

    @PostMapping("set/device/name")
    @ApiOperation("设置设备名称")
    public JsonResult<String> setDeviceName(@ApiParam("设备ID") String deviceId, @ApiParam("设备名称") String name) {
        List<String> parameter = new ArrayList<String>();
        parameter.add("DeviceID=" + deviceId);
        parameter.add("Name=" + name);
        String authorization = getAuthorization(parameter);
        String url = "http://ex-api.jalasmart.com/api/v2/devices/" + deviceId + "/name";
        try {
            HttpPut put = (HttpPut) getRequestByUrl(url, authorization, 3);
            JSONObject bodyJson = new JSONObject(true);
            bodyJson.put("DeviceID", deviceId);
            bodyJson.put("Name", name);
            put.setEntity(new StringEntity(bodyJson.toJSONString(), "UTF-8"));
            JSONObject jsonObject = JSON.parseObject(getJson(put));
            if (jsonObject.getInteger("Code") != 1) {
                throw new ModificationNameException("修改设备名称失败 ");
            }
            return new JsonResult<String>(SUCCESS);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new JsonResult<String>(FAILED,"修改设备名称失败");
    }

    @ApiOperation("获取设备警告信息")
    @GetMapping("get-warning/{userName}/page/{page}")
    public JsonResult<List<Warming>> getWarning(@PathVariable("userName") @ApiParam("用户名字") String userName, @PathVariable("page") @ApiParam("分页，每页最大100条数据，从0开始") Integer page) {
        String userCode = userServiceImpl.selectUserCode(userName);
        if (userCode != null) {
            List<String> deivceCodes = userAndDeviceServiceImpl.selectDeviceCodeByUserCode(userCode);
            if (deivceCodes.size() > 0) {
                List<Warming> warmings = new ArrayList<>();
                for (String deviceCode : deivceCodes) {
                    List<Warming> warmings1 = warmingServiceImpl.selectWarmingByDeviceCode(deviceCode);
                    if (warmings1.size() > 0)
                        warmings.addAll(warmings1);
                }
                return new JsonResult<List<Warming>>(SUCCESS, warmings);
            }
        }
        return new JsonResult<>(FAILED);
    }

    @GetMapping("set/lines/name")
    @ApiOperation("修改线路名称")
    public JsonResult<String> setLineName(@ApiParam("设备ID") String deviceId, @ApiParam("线路ID") String lineId, @ApiParam("线路名称") String name) {
        List<String> parameter = new ArrayList<String>();
        parameter.add("DeviceID=" + deviceId);
        parameter.add("LineID=" + lineId);
        parameter.add("Name=" + name);
        String authorization = getAuthorization(parameter);
        String url = "http://ex-api.jalasmart.com/api/v2/devices/" + deviceId + "/lines/" + lineId + "/name";
        try {
            HttpPut put = (HttpPut) getRequestByUrl(url, authorization, 3);
            JSONObject bodyJson = new JSONObject(true);
            bodyJson.put("DeviceID", deviceId);
            bodyJson.put("LineID", lineId);
            bodyJson.put("Name", name);
            put.setEntity(new StringEntity(bodyJson.toJSONString(), "UTF-8"));
            JSONObject jsonObject = JSON.parseObject(getJson(put));
            if (jsonObject.getInteger("Code") != 1) {
                throw new ModificationNameException("修改线路名称失败");
            }
            return new JsonResult<>(SUCCESS);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new JsonResult<>(FAILED,"修改线路名称失败");
    }

    @PostMapping("set/line/max-current")
    @ApiOperation("设置最大电流")
    public JsonResult<String> setMaxCurrent(@ApiParam("设备ID") String deviceId, @ApiParam("线路ID") String lineId, @ApiParam("最大电流,大于6小于63") Integer max, @ApiParam("超过最大电流持续多久断开，0-60秒") Integer duration) {



        List<String> parameter = new ArrayList<String>();
        parameter.add("DeviceID=" + deviceId);
        parameter.add("LineID=" + lineId);
        parameter.add("Max=" + max);
        parameter.add("Duration=" + duration);
        String authorization = getAuthorization(parameter);
        String url = "http://ex-api.jalasmart.com/api/v2/devices/" + deviceId + "/lines/" + lineId + "/max";
        try {
            HttpPut put = (HttpPut) getRequestByUrl(url, authorization, 3);
            JSONObject bodyJson = new JSONObject(true);
            bodyJson.put("DeviceID", deviceId);
            bodyJson.put("LineID", lineId);
            bodyJson.put("Max", max);
            bodyJson.put("Duration", duration);
            put.setEntity(new StringEntity(bodyJson.toJSONString(), "UTF-8"));
            JSONObject jsonObject = JSON.parseObject(getJson(put));

            if (jsonObject.getInteger("Code") != 1) {
                throw new ModificationNameException("修改最大电流失败");
            }


            return new JsonResult<>(SUCCESS);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new JsonResult<>(FAILED,"修改最大电流失败");
    }

    @PostMapping("set/line/voltage")
    @ApiOperation("设置过压和欠压值")
    public JsonResult<Void> setVoltage(@ApiParam("设备ID") String deviceId, @ApiParam("线路ID") String lineId, @ApiParam("欠压值 范围175-205") Integer under, @ApiParam("过压值 范围235-265") Integer over) {
        List<String> parameter = new ArrayList<String>();
        parameter.add("DeviceID=" + deviceId);
        parameter.add("LineID=" + lineId);
        parameter.add("Under=" + under);
        parameter.add("Over=" + over);
        String authorization = getAuthorization(parameter);
        String url = "http://ex-api.jalasmart.com/api/v2/devices/" + deviceId + "/lines/" + lineId + "/under";
        try {
            HttpPut put = (HttpPut) getRequestByUrl(url, authorization, 3);
            JSONObject bodyJson = new JSONObject(true);
            bodyJson.put("DeviceID", deviceId);
            bodyJson.put("LineID", lineId);
            bodyJson.put("Under", under);
            bodyJson.put("Over", over);
            put.setEntity(new StringEntity(bodyJson.toJSONString(), "UTF-8"));
            JSONObject jsonObject = JSON.parseObject(getJson(put));
            if (jsonObject.getInteger("Code") != 1) {
                return new JsonResult<>(FAILED);
            }
            return new JsonResult<>(SUCCESS);
        } catch (IOException e) {
            return new JsonResult<>(FAILED);
        }
    }

    @PostMapping("set/line/enabled")
    @ApiOperation("锁定手动，只有单相的需要使用手柄锁定。")
    public JsonResult<Void> setEnabled(@ApiParam("设备ID") String deviceId, @ApiParam("线路ID") String lineId, @ApiParam("锁定手动是否开启 0:关闭 1:开启") Integer enabled) {
        List<String> parameter = new ArrayList<String>();
        parameter.add("DeviceID=" + deviceId);
        parameter.add("LineID=" + lineId);
        parameter.add("Enabled=" + enabled);
        String authorization = getAuthorization(parameter);
        String url = "http://ex-api.jalasmart.com/api/v2/devices/" + deviceId + "/lines/" + lineId + "/enabled";
        try {
            HttpPut put = (HttpPut) getRequestByUrl(url, authorization, 3);
            JSONObject bodyJson = new JSONObject(true);
            bodyJson.put("DeviceID", deviceId);
            bodyJson.put("LineID", lineId);
            bodyJson.put("Enabled", enabled);
            put.setEntity(new StringEntity(bodyJson.toJSONString(), "UTF-8"));
            JSONObject jsonObject = JSON.parseObject(getJson(put));
            if (jsonObject.getInteger("Code") != 1) {
                throw new ModificationNameException("设置手动开关失败");
            }
            return new JsonResult<>(SUCCESS);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new JsonResult<>(FAILED);
    }

    @PostMapping("set/line/leak")
    @ApiOperation("设置漏电预警值，需要根据isLeakage字段判断是否可以设置该值")
    public JsonResult<Void> setLeak(@ApiParam("设备ID") String deviceId, @ApiParam("线路ID") String lineId, @ApiParam("漏电预警值 单位mA") Integer leakValue) {
        List<String> parameter = new ArrayList<String>();
        parameter.add("DeviceID=" + deviceId);
        parameter.add("LineID=" + lineId);
        parameter.add("LeakValue=" + leakValue);
        String authorization = getAuthorization(parameter);
        String url = "http://ex-api.jalasmart.com/api/v2/devices/" + deviceId + "/lines/" + lineId + "/leak";
        try {
            HttpPut put = (HttpPut) getRequestByUrl(url, authorization, 3);
            JSONObject bodyJson = new JSONObject(true);
            bodyJson.put("DeviceID", deviceId);
            bodyJson.put("LineID", lineId);
            bodyJson.put("LeakValue", leakValue);
            put.setEntity(new StringEntity(bodyJson.toJSONString(), "UTF-8"));
            JSONObject jsonObject = JSON.parseObject(getJson(put));
            if (jsonObject.getInteger("Code") != 1) {
                throw new ModificationNameException("设置漏电预警值失败");
            }
            return new JsonResult<>(SUCCESS);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new JsonResult<>(FAILED);
    }

    @PostMapping("set/line/err-leak")
    @ApiOperation("设置漏电动作值，需要根据isLeakage字段判断是否可以设置该值")
    public JsonResult<Void> setErrLeak(@ApiParam("设备ID") String deviceId, @ApiParam("线路ID") String lineId, @ApiParam("漏电动作值 单位mA") Integer errLeakValue) {
        List<String> parameter = new ArrayList<String>();
        parameter.add("DeviceID=" + deviceId);
        parameter.add("LineID=" + lineId);
        parameter.add("Err_LeakValue=" + errLeakValue);
        String authorization = getAuthorization(parameter);
        String url = "http://ex-api.jalasmart.com/api/v2/devices/" + deviceId + "/lines/" + lineId + "/err_leak";
        try {
            HttpPut put = (HttpPut) getRequestByUrl(url, authorization, 3);
            JSONObject bodyJson = new JSONObject(true);
            bodyJson.put("DeviceID", deviceId);
            bodyJson.put("LineID", lineId);
            bodyJson.put("Err_LeakValue", errLeakValue);
            put.setEntity(new StringEntity(bodyJson.toJSONString(), "UTF-8"));
            JSONObject jsonObject = JSON.parseObject(getJson(put));
            if (jsonObject.getInteger("Code") != 1) {
                throw new ModificationNameException("设置漏电动作值失败");
            }
            warmingServiceImpl.updateHeightLeakage(deviceServiceImpl.selectDeviceCode(deviceId, lineId, "J191291284776"), errLeakValue.toString());
            return new JsonResult<>(SUCCESS);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new JsonResult<>(FAILED);
    }

    /**
     * 更新：修改LineId为LineNo字段 原因：数据库缺少LineId字段
     *
     * @param deviceId 设备编号
     * @param lineNo   线路编号
     * @param highTemp 过温动作值
     * @return
     */
    @PostMapping("set/line/err-temp")
    @ApiOperation("设置过温动作值，暂定85℃")
    public JsonResult<String> setTemp(@ApiParam("设备ID") String deviceId, @ApiParam("线路ID") String lineNo, @ApiParam("过温动作值 单位℃(暂定限制<=200)") Integer highTemp) {

        //判断温度是否设置过高，暂定标准<=200
        if (highTemp > 85) {
            return new JsonResult<String>(FAILED,"设置温度过高，请设置温度小于85℃");
        }
        // 修改成功与否反馈
        String highTempStr = String.valueOf(highTemp);
        String deviceCode = deviceServiceImpl.selectDeviceCode(deviceId, lineNo, "J191291284776");
        String flag = warmingServiceImpl.updateHighTemp(deviceCode, highTempStr);
        if (flag != highTempStr)
            return new JsonResult<String>(FAILED,"修改失败，请设置温度小于85℃");
        return new JsonResult(SUCCESS);
    }

    @PostMapping("set/line/err-power")
    @ApiOperation("设置过高功率")
    public JsonResult setHeightPower(@ApiParam("设备ID") String deviceId, @ApiParam("线路ID") String lineNo, @ApiParam("最大功率（power）") Integer highPower) {
        //判断设置功率范围。
        if (highPower <= 0) {
            return new JsonResult(FAILED, "范围不能小于等于0");
        }
        String highPowerStr = highPower.toString();
        // 修改成功与否反馈
        String deviceCode = deviceServiceImpl.selectDeviceCode(deviceId, lineNo, "J191291284776");
        String flag = warmingServiceImpl.updateHeightPower(deviceCode, highPowerStr);
        if (highPowerStr.equals(flag))
            return new JsonResult<>(SUCCESS);
        return new JsonResult(FAILED);
    }
    /**
     * 设置监测不监测电弧。
     *
     * @param deviceCode
     * @param arc
     * @return
     */
    @PostMapping("set/line/ARC")
    public JsonResult<String> setArc(@ApiParam("deviceCode") String deviceCode, @ApiParam("arc") String arc) {
        if (deviceCode == null&&arc==null&&"off".equals(arc)&&"on".equals(arc))
            return new JsonResult<>(FAILED,"请检查数据");
        String flag = warmingServiceImpl.updateARC(deviceCode, arc);

        if (flag==null||"".equals(flag))
            return new JsonResult<>(FAILED,"更改失败,请检查数据");
        return new JsonResult<>(SUCCESS);
    }


    /**
     * 受理前端warming消息
     *
     * @param id
     * @param handleMsg
     * @return
     */
    @PostMapping("set/deal-warming")
    @ApiOperation("处理报警信息")
    public JsonResult<Void> dealWarming(@ApiParam("id") String id, @ApiParam("isHandle") String handleMsg) {
//    public JsonResult<Void> dealWarming() {
        if (id == null)
            return new JsonResult<>(FAILED);
        int flag = warmingServiceImpl.warmingIsHandle(Integer.parseInt(id), handleMsg);
//        int flag =0;
        if (flag <= 0)
            return new JsonResult<>(FAILED);
        return new JsonResult<>(SUCCESS);
    }

    /**
     * 获取line阀值设置的接口
     * @param deviceId
     * @param lineId
     * @return
     */
    @GetMapping("get/line/set")
    @ApiOperation("获取设置")
    public JsonResult<WarmingSetting> setThresholdValue(@ApiParam("设备ID") String deviceId, @ApiParam("线路ID") String lineId) {

        // 获取设备，取出阀值
        List<String> parameter = new ArrayList<String>();
        parameter.add("SN=" + "J191291284776");
        String authorization = getAuthorization(parameter);
        String url = "";
        url = "http://ex-api.jalasmart.com/api/v2/devices/J191291284776";
        try {
            HttpRequestBase httpRequestBase = getRequestByUrl(url, authorization, 1);
            String json = getJson(httpRequestBase);
            JSONObject jsonObject = JSON.parseObject(json);
            JSONObject data = jsonObject.getJSONObject("Data");
            JSONArray lines = data.getJSONArray("Lines");
            if (lines.size() == 0)
                return new JsonResult<>(FAILED);

            for (int i = 0; i < lines.size(); i++) {
                JSONObject line = lines.getJSONObject(i);
                if (deviceId != null && lineId != null && deviceId.equals(data.getString("DeviceID")) && lineId.equals(line.getString("LineID"))) {
                    String deviceCode = deviceServiceImpl.selectDeviceCode(deviceId, line.getInteger("LineNo").toString(), "J191291284776");
                    WarmingSetting warmingSetting = new WarmingSetting();
                    warmingSetting.setDeviceCode(deviceCode);
                    warmingSetting.setDeviceId(deviceId);
                    warmingSetting.setHeightCurrent(line.getDouble("Max").intValue());
                    warmingSetting.setHeightVoltage(line.getInteger("Over"));
                    warmingSetting.setLowVoltage(line.getInteger("Under"));
                    String highTemp = warmingServiceImpl.selectHighTemp(deviceCode);
                    warmingSetting.setHighTemp(Integer.parseInt(highTemp));
                    warmingSetting.setLineNo(line.getInteger("LineNo"));
                    warmingSetting.setHeightLeakage(line.getInteger("Err_LeakValue"));
                    String arc = warmingServiceImpl.selectARC(deviceCode);
                    warmingSetting.setArc(arc);
                    warmingSetting.setLineId(lineId);
                    warmingSetting.setName(line.getString("Name"));
                    String highPower = warmingServiceImpl.selectHightPower(deviceCode);
                    if (highPower != null && "".equals(highPower))
                        warmingSetting.setHeightPower(Integer.parseInt(highPower));

                    return new JsonResult<WarmingSetting>(SUCCESS, warmingSetting);
                }
            }


            return new JsonResult<WarmingSetting>();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new JsonResult<>(FAILED);
    }

    /**
     * 设备实时监测数据
     *
     * @param deviceId 设备id
     * @param lineId   线路id
     * @param model    线路型号
     * @return
     */
    @GetMapping("get/lines/model")
    @ApiOperation("实时监控数据")
    public JsonResult<DeviceLine> setThresholdValue(@ApiParam("设备ID") String deviceId, @ApiParam("线路ID") String lineId, @ApiParam("型号") String model) {
        List<String> parameter = new ArrayList<String>();
        parameter.add("DeviceID=" + deviceId);
        parameter.add("LineID=" + lineId);
        String authorization = getAuthorization(parameter);
        String url = "";
        // 根据型号进行区分，（将来会进行将包装类加入 deviceCode ,以后只传递deviceCode）
        if (model.startsWith("3P")) {
            url = "http://ex-api.jalasmart.com/api/v2/devices/" + deviceId + "/lines/" + lineId + "/threephase";
        } else {
            url = "http://ex-api.jalasmart.com/api/v2/devices/" + deviceId + "/lines/" + lineId;
        }
        try {
            HttpRequestBase httpRequestBase = getRequestByUrl(url, authorization, 1);
            String json = getJson(httpRequestBase);
            JSONObject jsonObject = JSON.parseObject(json);
            JSONObject data = jsonObject.getJSONObject("Data");
            DeviceLine deviceLine = new DeviceLine(data);
            String deviceCode = deviceServiceImpl.selectDeviceCode(deviceId,deviceLine.getLineNo().toString(),"J191291284776");
            String arc = warmingServiceImpl.selectARC(deviceCode);
            deviceLine.setArc(arc);
            return new JsonResult<DeviceLine>(SUCCESS, deviceLine);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new JsonResult<>(FAILED);
    }

    /**
     * 获取用户（现为默认用户）网关下的所有线路
     * 更新：更改传输对象为Device对象吧。理由：获取线路详情需要DeviceId 更新时间 ：2020年4月26日11:06:29
     *
     * @return
     */
    @GetMapping("get-line-all")
    @ApiOperation("获取所有线路")
    public JsonResult<List<Device>> getDeviceLineAll() {

        //以下获取整个网关
        String authorization = getAuthorization(null);
        String url = "http://ex-api.jalasmart.com/api/v2/devices/" + ID;
        try {
            HttpRequestBase httpRequestBase = getRequestByUrl(url, authorization, 1);
            String json = getJson(httpRequestBase);
            JSONObject jsonObject = JSON.parseObject(json);
            JSONArray data = jsonObject.getJSONArray("Data");
            Set<String> sns = new HashSet<>();
            Integer count = data.size();
            for (int i = 0; i < count; i++) {
                JSONObject js = data.getJSONObject(i);
                Device device = new Device(js);
                sns.add(device.getSn());
            }
            // 获取每个网关
            List<Device> devices = new ArrayList<>();
            for (String sn : sns) {
                List<String> parameter = new ArrayList<String>();
                parameter.add("SN=" + sn);
                authorization = getAuthorization(parameter);
                url = "http://ex-api.jalasmart.com/api/v2/devices/" + sn;
                httpRequestBase = getRequestByUrl(url, authorization, 1);
                json = getJson(httpRequestBase);
                jsonObject = JSON.parseObject(json);
                JSONObject devices1 = jsonObject.getJSONObject("Data");
                Device device = new Device(devices1);
                devices.add(device);
            }
            return new JsonResult<List<Device>>(SUCCESS, devices);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new JsonResult<>(FAILED);
    }


//    @GetMapping("get-line-Power")
//    @ApiOperation("获取线路power")
//    public JsonResult<List<Device>> getLinePower(@ApiParam("设备ID") @PathVariable("DeviceID") String deviceId) {
//        List<String> parameter = new ArrayList<String>();
//        parameter.add("DeviceID=" + deviceId);
//        parameter.add("Time=" + new);
//        String authorization = getAuthorization(parameter);
//        String url = "";
//        try {
//            HttpRequestBase httpRequestBase = getRequestByUrl(url, authorization, 1);
//            String json = getJson(httpRequestBase);
//            JSONObject jsonObject = JSON.parseObject(json);
//            JSONArray data = jsonObject.getJSONArray("Data");
//            Set<String> sns = new HashSet<>();
//            Integer count = data.size();
//            for (int i = 0; i < count; i++) {
//                JSONObject js = data.getJSONObject(i);
//                Device device = new Device(js);
//                sns.add(device.getSn());
//            }
//            // 获取每个网关
//            List<Device> deviceLines = new ArrayList<>();
//            for (String sn : sns) {
//                List<String> parameter = new ArrayList<String>();
//                parameter.add("SN=" + sn);
//                authorization = getAuthorization(parameter);
//                url = "http://ex-api.jalasmart.com/api/v2/devices/" + sn;
//                httpRequestBase = getRequestByUrl(url, authorization, 1);
//                json = getJson(httpRequestBase);
//                jsonObject = JSON.parseObject(json);
//                JSONObject devices = jsonObject.getJSONObject("Data");
//                Device device = new Device(devices);
//                deviceLines.add(device);
//            }
//            return new JsonResult<List<Device>>(SUCCESS, deviceLines);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return new JsonResult<>(FAILED);
//    }

}
