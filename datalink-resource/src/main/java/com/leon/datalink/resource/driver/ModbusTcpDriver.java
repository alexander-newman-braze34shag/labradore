package com.leon.datalink.resource.driver;

import cn.hutool.core.exceptions.ValidateException;
import com.leon.datalink.core.config.ConfigProperties;
import com.leon.datalink.core.utils.Loggers;
import com.leon.datalink.resource.AbstractDriver;
import com.leon.datalink.resource.constans.DriverModeEnum;
import org.apache.plc4x.java.PlcDriverManager;
import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.exceptions.PlcConnectionException;
import org.apache.plc4x.java.api.messages.PlcReadRequest;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.apache.plc4x.java.api.types.PlcResponseCode;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class ModbusTcpDriver extends AbstractDriver {

    private PlcConnection plcConnection;

    @Override
    public void create(DriverModeEnum driverMode, ConfigProperties properties) throws Exception {
        if (StringUtils.isEmpty(properties.getString("ip"))) throw new ValidateException();
        if (StringUtils.isEmpty(properties.getString("port"))) throw new ValidateException();
        if (StringUtils.isEmpty(properties.getString("salve"))) throw new ValidateException();
        if (StringUtils.isEmpty(properties.getString("timeout"))) throw new ValidateException();
        plcConnection = new PlcDriverManager().getConnection(String.format("modbus-tcp:tcp://%s:%s?unit-identifier=%s&request-timeout=%s",
                properties.getString("ip"),
                properties.getString("port"),
                properties.getString("salve"),
                properties.getString("timeout")));
    }

    @Override
    public void destroy(DriverModeEnum driverMode, ConfigProperties properties) throws Exception {
        plcConnection.close();
    }

    @Override
    public boolean test(ConfigProperties properties) {
        if (StringUtils.isEmpty(properties.getString("ip"))) return false;
        if (StringUtils.isEmpty(properties.getString("port"))) return false;
        if (StringUtils.isEmpty(properties.getString("salve"))) return false;
        try {
            PlcConnection plcConnection = new PlcDriverManager().getConnection(String.format("modbus-tcp:tcp://%s:%s?unit-identifier=%s",
                    properties.getString("ip"),
                    properties.getString("port"),
                    properties.getString("salve")));
            return plcConnection.isConnected();
        } catch (PlcConnectionException e) {
            Loggers.DRIVER.error("driver test {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void scheduleTrigger(ConfigProperties properties) throws Exception {
        List<Map<String, Object>> points = properties.getList("points");
        if (null == points || points.isEmpty()) throw new ValidateException();

        PlcReadRequest.Builder builder = plcConnection.readRequestBuilder();

        points.forEach(point -> {
            String item = String.format("%s:%s:%s", point.get("func"), point.get("address"), point.get("type"));
            builder.addItem((String) point.get("tag"), item);
        });
        PlcReadRequest readRequest = builder.build();

        PlcReadResponse response = readRequest.execute().get();
        for (String tag : response.getFieldNames()) {
            if (response.getResponseCode(tag) == PlcResponseCode.OK) {
                Collection<Object> allValues = response.getAllObjects(tag);
                allValues.forEach(value -> {
                    HashMap<String, Object> result = new HashMap<>();
                    result.put("tag", tag);
                    result.put("value", value);
                    produceData(result);
                });
            } else {
                Loggers.DRIVER.error("modbus read {} error: {}", tag, response.getResponseCode(tag).name());
            }
        }
    }

}
