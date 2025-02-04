package com.mycompany.plugin.rfid;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.rscja.deviceapi.RFIDWithUHFUART;

@CapacitorPlugin(name = "RFIDUHF")
public class RFIDPlugin extends Plugin {
    private RFIDWithUHFUART rfidWithUHFUART;

    @Override
    public void load() {
        rfidWithUHFUART = RFIDWithUHFUART.getInstance();
    }

    @PluginMethod
    public void initialize(PluginCall call) {
        try {
            boolean success = rfidWithUHFUART.init();
            JSObject ret = new JSObject();
            ret.put("success", success);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("Error initializing RFID reader", e);
        }
    }

    @PluginMethod
    public void startReading(PluginCall call) {
        try {
            boolean success = rfidWithUHFUART.startInventoryTag();
            JSObject ret = new JSObject();
            ret.put("success", success);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("Error starting RFID reader", e);
        }
    }

    @PluginMethod
    public void stopReading(PluginCall call) {
        try {
            boolean success = rfidWithUHFUART.stopInventory();
            JSObject ret = new JSObject();
            ret.put("success", success);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("Error stopping RFID reader", e);
        }
    }

    @PluginMethod
    public void setPower(PluginCall call) {
        try {
            int power = call.getInt("power", 15); // valor por defecto 15
            boolean success = rfidWithUHFUART.setPower(power);
            JSObject ret = new JSObject();
            ret.put("success", success);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("Error configurando potencia del lector", e);
        }
    }

    @PluginMethod
    public void free(PluginCall call) {
        try {
            rfidWithUHFUART.free();
            JSObject ret = new JSObject();
            ret.put("success", true);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("Error liberando recursos del lector", e);
        }
    }

    @PluginMethod
    public void getInventoryTag(PluginCall call) {
        try {
            String[] tagInfo = rfidWithUHFUART.readTagFromBuffer();
            if (tagInfo != null) {
                JSObject ret = new JSObject();
                ret.put("epc", tagInfo[1]); // El EPC está en la posición 1
                ret.put("rssi", tagInfo[2]); // El RSSI está en la posición 2
                call.resolve(ret);
            } else {
                JSObject ret = new JSObject();
                ret.put("success", false);
                ret.put("message", "No hay tags en el buffer");
                call.resolve(ret);
            }
        } catch (Exception e) {
            call.reject("Error leyendo tag del buffer", e);
        }
    }
} 