package com.ylazzari.plugins.rfidread;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.rscja.deviceapi.RFIDWithUHFUART;
import com.rscja.deviceapi.entity.UHFTAGInfo;

import android.content.Context;
import android.os.AsyncTask;
import android.provider.Settings;
import android.view.KeyEvent;
import android.annotation.SuppressLint;

@CapacitorPlugin(name = "RFIDUHF")
public class RFIDPlugin extends Plugin {
    private RFIDWithUHFUART mReader = null;
    private boolean loopStarted = false;
    private AsyncTask<Integer, String, Void> asyncTask = null;

    // Constantes para configuraciónte
    private static final int DEFAULT_POWER = 30;
    private static final int MIN_POWER = 5;
    private static final int MAX_POWER = 30;
    
    // Variables para manejo de eventos de teclado
    private long lastKeyEventTime = 0;
    private static final long KEY_DEBOUNCE_DELAY = 300; // 300ms de debounce
    private boolean isKeyPressed = false;
    private static final long KEY_TIMEOUT = 5000; // 5 segundos timeout para auto-reset

    @Override
    public void load() {
        try {
            mReader = RFIDWithUHFUART.getInstance();
            if (mReader != null) {
                boolean result = mReader.init();
                if (!result) {
                    notifyListeners("initError", new JSObject().put("message", "Fallo en la inicialización del lector"));
                } else {
                    notifyListeners("initSuccess", new JSObject().put("message", "Lector inicializado correctamente"));
                }
            }
        } catch (Exception e) {
            notifyListeners("initError", new JSObject().put("message", "Error: " + e.getMessage()));
        }
    }

    @SuppressLint("HardwareIds")
    @PluginMethod
    public void getDeviceId(PluginCall call) {
        try {
            Context context = getContext();
            String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            JSObject result = new JSObject();
            result.put("id", androidId != null ? androidId : "");
            result.put("success", true);
            call.resolve(result);
        } catch (Exception e) {
            JSObject result = new JSObject();
            result.put("success", false);
            result.put("message", "Error obteniendo Android ID: " + e.getMessage());
            call.reject("Error obteniendo Android ID", e);
        }
    }

    @PluginMethod(returnType = PluginMethod.RETURN_PROMISE)
    public void startReading(PluginCall call) {
        try {
            if (mReader != null && !loopStarted) {
                // Asegurarse de que el lector esté inicializado
                if (!mReader.init()) {
                    call.reject("Error al inicializar el lector RFID");
                    return;
                }

                loopStarted = true;
                boolean success = mReader.startInventoryTag();
                if (!success) {
                    loopStarted = false;
                    call.reject("Error al iniciar la lectura del RFID");
                    return;
                }

                startAsyncTask(5); // Reducir delay para mejor velocidad

                JSObject ret = new JSObject();
                ret.put("success", true);
                ret.put("message", "Lectura RFID iniciada correctamente");
                call.resolve(ret);
            } else {
                call.reject("El lector ya está en funcionamiento o no está inicializado");
            }
        } catch (Exception e) {
            loopStarted = false;
            call.reject("Error al iniciar el lector RFID: " + e.getMessage());
        }
    }
    

@PluginMethod(returnType = PluginMethod.RETURN_PROMISE)
public void stopReading(PluginCall call) {
    try {
        if (mReader != null && loopStarted) {
            // Primero marcar que debe parar
            loopStarted = false;
            
            // Cancelar la tarea asíncrona ANTES de parar el inventario
            if (asyncTask != null && !asyncTask.isCancelled()) {
                asyncTask.cancel(true);
                asyncTask = null;
            }
            
            // Esperar un poco para que la tarea termine
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Parar el inventario
            boolean success = mReader.stopInventory();
            
            // Limpiar el buffer múltiples veces para asegurar que esté vacío
            for (int i = 0; i < 5; i++) {
                try {
                    mReader.readTagFromBuffer(); // Leer y descartar
                } catch (Exception e) {
                    // Ignorar errores, solo queremos limpiar
                }
            }

            JSObject ret = new JSObject();
            ret.put("success", success);
            ret.put("message", success ? "Lectura detenida correctamente" : "Error al detener la lectura");
            call.resolve(ret);
        } else {
            JSObject ret = new JSObject();
            ret.put("success", false);
            ret.put("message", "El lector no está en funcionamiento");
            call.resolve(ret);
        }
    } catch (Exception e) {
        JSObject ret = new JSObject();
        ret.put("success", false);
        ret.put("message", "Error al detener el lector RFID: " + e.getMessage());
        call.resolve(ret);
    }
}

    @PluginMethod
    public void free(PluginCall call) {
        try {
            if (mReader != null) {
                stopReading(call);
                mReader.free();
                mReader = null;
                JSObject ret = new JSObject();
                ret.put("success", true);
                call.resolve(ret);
            }
        } catch (Exception e) {
            call.reject("Error liberando recursos del lector", e);
        }
    }

 
    @PluginMethod
    public void getInventoryTag(PluginCall call) {
        try {
            UHFTAGInfo tagInfo = mReader.readTagFromBuffer();
            JSObject ret = new JSObject();
            if (tagInfo != null) {
                ret.put("epc", tagInfo.getEPC());
                ret.put("rssi", tagInfo.getRssi());
                ret.put("success", true);
                call.resolve(ret);
            } else {
                ret.put("success", false);
                ret.put("message", "No hay tags en el buffer");
                call.resolve(ret);
            }
        } catch (Exception e) {
            call.reject("Error leyendo tag del buffer", e);
        }
    }

    @PluginMethod
    public void setInventoryCallback(PluginCall call) {
        try {
            mReader.setInventoryCallback(new InventoryCallback() {
                @Override
                public void onInventoryTag(UHFTAGInfo tagInfo) {
                    notifyListeners("tagFoundInventory", createTagResult(tagInfo.getEPC(), tagInfo.getRssi()));
                }
            });

            mReader.startInventoryTag();
            
            call.resolve(new JSObject().put("success", true));
        } catch (Exception e) {
            call.reject("Error configurando callback de inventario", e);
        }
    }

    @PluginMethod
    public void setPower(PluginCall call) {
        try {
            int power = call.getInt("power", DEFAULT_POWER);

            // Validar rango de potencia
            if (power < MIN_POWER || power > MAX_POWER) {
                call.reject("Potencia debe estar entre " + MIN_POWER + " y " + MAX_POWER);
                return;
            }

            if (mReader != null) {
                boolean success = mReader.setPower(power);
                JSObject ret = new JSObject();
                ret.put("success", success);
                ret.put("power", power);
                call.resolve(ret);
            } else {
                call.reject("Lector no inicializado");
            }
        } catch (Exception e) {
            call.reject("Error configurando potencia", e);
        }
    }

    @PluginMethod
    public void getPower(PluginCall call) {
        try {
            if (mReader != null) {
                int power = mReader.getPower();
                JSObject ret = new JSObject();
                ret.put("success", true);
                ret.put("power", power);
                call.resolve(ret);
            } else {
                call.reject("Lector no inicializado");
            }
        } catch (Exception e) {
            call.reject("Error obteniendo potencia", e);
        }
    }

    @PluginMethod
    public void initReader(PluginCall call) {
        try {
            if (mReader == null) {
                mReader = RFIDWithUHFUART.getInstance();
            }

            if (mReader != null) {
                new InitTask(call).execute();
            } else {
                call.reject("No se pudo obtener instancia del lector");
            }
        } catch (Exception e) {
            call.reject("Error inicializando lector", e);
        }
    }

    @PluginMethod
    public void resetKeyState(PluginCall call) {
        try {
            // Resetear el estado de las teclas
            isKeyPressed = false;
            lastKeyEventTime = 0;
            
            JSObject ret = new JSObject();
            ret.put("success", true);
            ret.put("message", "Estado de teclas reseteado correctamente");
            call.resolve(ret);
            
            // Notificar que se ha reseteado el estado
            notifyListeners("keyStateReset", ret);
        } catch (Exception e) {
            call.reject("Error reseteando estado de teclas", e);
        }
    }

private void startAsyncTask(int waitTime) {
    asyncTask = new AsyncTask<Integer, String, Void>() {
        String lastEpc = null;
        long lastNotificationTime = 0;
        private static final long NOTIFICATION_THROTTLE = 50;

        @Override
        protected Void doInBackground(Integer... integers) {
            int sleepTime = integers[0];
            while (loopStarted && !isCancelled()) {
                try {
                    // Verificar el estado más frecuentemente
                    if (!loopStarted || isCancelled()) {
                        break;
                    }
                    
                    UHFTAGInfo tagInfo = mReader.readTagFromBuffer();
                    if (tagInfo != null && loopStarted && !isCancelled()) {
                        String epc = tagInfo.getEPC();
                        if (epc != null && !epc.isEmpty() && !epc.matches("[0]+")) {
                            long currentTime = System.currentTimeMillis();
                            boolean shouldNotify = lastEpc == null || 
                                                 !lastEpc.equalsIgnoreCase(epc) ||
                                                 (currentTime - lastNotificationTime > 1000);
                            
                            if (shouldNotify && (currentTime - lastNotificationTime > NOTIFICATION_THROTTLE)) {
                                // Verificar nuevamente antes de notificar
                                if (loopStarted && !isCancelled()) {
                                    JSObject tagData = new JSObject();
                                    tagData.put("epc", epc);
                                    tagData.put("rssi", tagInfo.getRssi());
                                    tagData.put("timestamp", currentTime);

                                    notifyListeners("tagFound", tagData);
                                    
                                    lastEpc = epc;
                                    lastNotificationTime = currentTime;
                                }
                            }
                        }
                    }
                    
                    // Verificar estado antes de dormir
                    if (!loopStarted || isCancelled()) {
                        break;
                    }
                    
                    if (tagInfo != null) {
                        Thread.sleep(Math.max(1, sleepTime / 2));
                    } else {
                        Thread.sleep(sleepTime);
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                    if (!loopStarted || isCancelled()) {
                        break;
                    }
                    try {
                        Thread.sleep(sleepTime * 2);
                    } catch (InterruptedException ie) {
                        break;
                    }
                }
            }
            return null;
        }
        
        @Override
        protected void onCancelled() {
            super.onCancelled();
            // Limpiar el buffer cuando se cancela
            if (mReader != null) {
                try {
                    for (int i = 0; i < 3; i++) {
                        mReader.readTagFromBuffer();
                    }
                } catch (Exception e) {
                    // Ignorar errores
                }
            }
        }
    };
    asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, waitTime);
}

    private JSObject createTagResult(String epc, String rssi) {
        JSObject result = new JSObject();
        result.put("epc", epc);
        result.put("rssi", rssi);
        return result;
    }

    private class InitTask extends AsyncTask<String, Integer, Boolean> {
        private final PluginCall call;

        public InitTask(PluginCall call) {
            this.call = call;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            return mReader.init();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (!result) {
                call.reject("Fallo en la inicialización del lector");
                notifyListeners("initError", new JSObject().put("message", "Fallo en la inicialización del lector"));
            } else {
                JSObject ret = new JSObject();
                ret.put("success", true);
                ret.put("message", "Lector inicializado correctamente");
                call.resolve(ret);
                notifyListeners("initSuccess", ret);
            }
        }
    }

    @PluginMethod
public void clearBuffer(PluginCall call) {
    try {
        if (mReader != null) {
            int cleared = 0;
            for (int i = 0; i < 10; i++) {
                UHFTAGInfo tagInfo = mReader.readTagFromBuffer();
                if (tagInfo == null) {
                    break;
                }
                cleared++;
            }
            
            JSObject ret = new JSObject();
            ret.put("success", true);
            ret.put("cleared", cleared);
            ret.put("message", "Buffer limpiado, " + cleared + " tags removidos");
            call.resolve(ret);
        } else {
            call.reject("Lector no inicializado");
        }
    } catch (Exception e) {
        call.reject("Error limpiando buffer", e);
    }
}

    // 📌 Método para capturar teclas presionadas con debounce y control de repetición
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Solo procesar códigos de teclas específicos del escáner RFID
        if (keyCode == 139 || keyCode == 280 || keyCode == 293) {
            long currentTime = System.currentTimeMillis();
            
            // Auto-reset si la tecla ha estado presionada por mucho tiempo
            if (isKeyPressed && (currentTime - lastKeyEventTime > KEY_TIMEOUT)) {
                isKeyPressed = false;
                JSObject resetData = new JSObject();
                resetData.put("message", "Auto-reset: Gatillo liberado automáticamente");
                resetData.put("reason", "timeout");
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        notifyListeners("triggerAutoReset", resetData);
                    });
                }
            }
            
            // Ignorar eventos repetitivos (cuando se mantiene presionada la tecla)
            if (event.getRepeatCount() > 0) {
                return true; // Consumir el evento pero no procesarlo
            }
            
            // Aplicar debounce para evitar eventos múltiples rápidos
            if (currentTime - lastKeyEventTime < KEY_DEBOUNCE_DELAY) {
                return true; // Consumir el evento pero no procesarlo
            }
            
            // Solo procesar si la tecla no estaba ya presionada
            if (!isKeyPressed) {
                isKeyPressed = true;
                lastKeyEventTime = currentTime;
                
                JSObject data = new JSObject();
                data.put("message", "Gatillo presionado");
                data.put("keyCode", keyCode);
                data.put("timestamp", currentTime);
                
                // Notificar de manera asíncrona para mejor rendimiento
                notifyListeners("triggerPressed", data);
            }
            return true; // Consumir el evento
        }
        return false; // No consumir eventos de otras teclas
    }

    // 📌 Método para capturar teclas liberadas con control mejorado
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Solo procesar códigos de teclas específicos del escáner RFID
        if (keyCode == 139 || keyCode == 280 || keyCode == 293) {
            long currentTime = System.currentTimeMillis();
            
            // Solo procesar si la tecla estaba presionada
            if (isKeyPressed) {
                isKeyPressed = false;
                lastKeyEventTime = currentTime;
                
                JSObject data = new JSObject();
                data.put("message", "Gatillo liberado");
                data.put("keyCode", keyCode);
                data.put("timestamp", currentTime);
                
                // Notificar de manera asíncrona para mejor rendimiento
                notifyListeners("triggerReleased", data);
            }
            return true; // Consumir el evento
        }
        return false; // No consumir eventos de otras teclas
    }
}

