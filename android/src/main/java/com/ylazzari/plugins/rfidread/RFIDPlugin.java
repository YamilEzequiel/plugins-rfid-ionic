package com.ylazzari.plugins.rfidread;

import com.getcapacitor.JSObject;
import com.getcapacitor.JSArray;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.rscja.deviceapi.RFIDWithUHFUART;
import com.rscja.deviceapi.entity.UHFTAGInfo;
import com.rscja.deviceapi.interfaces.IUHFInventoryCallback;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;
import android.annotation.SuppressLint;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;



@CapacitorPlugin(name = "RFIDUHF")
public class RFIDPlugin extends Plugin {
    private RFIDWithUHFUART mReader = null;
    private boolean loopStarted = false;
    private ExecutorService executorService = null;
    private boolean isInventoryRunning = false;
    private boolean isFilteredInventoryRunning = false;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private Set<String> foundTags = new HashSet<>();
    private Set<String> targetTags = new HashSet<>();

    // Constantes para configuración
    private static final int DEFAULT_POWER = 15;
    private static final int MIN_POWER = 5;
    private static final int MAX_POWER = 30;
    private static final String TAG = "RFIDPlugin";

    // Variables para manejo de eventos de teclado
    private long lastKeyEventTime = 0;
    private static final long KEY_DEBOUNCE_DELAY = 300; // 300ms de debounce
    private boolean isKeyPressed = false;
    private static final long KEY_TIMEOUT = 5000; // 5 segundos timeout para auto-reset

    @Override
    public void load() {
        try {
            Log.d(TAG, "Iniciando carga del plugin RFID");
            mReader = RFIDWithUHFUART.getInstance();
            if (mReader != null) {
                boolean result = mReader.init();
                if (!result) {
                    Log.e(TAG, "Error en la inicialización del lector");
                    notifyListeners("initError", new JSObject().put("message", "Fallo en la inicialización del lector"));
                } else {
                    Log.d(TAG, "Lector inicializado correctamente");
                    // Configurar potencia por defecto
                    mReader.setPower(DEFAULT_POWER);
                    notifyListeners("initSuccess", new JSObject().put("message", "Lector inicializado correctamente"));
                }
            } else {
                Log.e(TAG, "No se pudo obtener instancia del lector");
                notifyListeners("initError", new JSObject().put("message", "No se pudo obtener instancia del lector"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en load(): " + e.getMessage(), e);
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
            Log.d(TAG, "startReading called");

            if (mReader == null) {
                mReader = RFIDWithUHFUART.getInstance();
                if (mReader == null) {
                    call.reject("No se pudo obtener instancia del lector");
                    return;
                }
            }

            if (isInventoryRunning) {
                call.reject("La lectura ya está en funcionamiento");
                return;
            }

            // Reinicializar el lector
            if (!mReader.init()) {
                call.reject("Error al inicializar el lector RFID");
                return;
            }

            // Limpiar buffer antes de empezar
            clearBufferInternal();

            // Configurar callback usando la interfaz correcta
            mReader.setInventoryCallback(new IUHFInventoryCallback() {
                @Override
                public void callback(UHFTAGInfo uhftagInfo) {
                    if (uhftagInfo != null && isInventoryRunning) {
                        String epc = uhftagInfo.getEPC();
                        String rssi = uhftagInfo.getRssi();

                        if (epc != null && !epc.isEmpty() && !epc.matches("[0]+")) {
                            JSObject tagData = new JSObject();
                            tagData.put("epc", epc);
                            tagData.put("rssi", rssi);
                            tagData.put("timestamp", System.currentTimeMillis());

                            Log.d(TAG, "Tag detectado: " + epc + " RSSI: " + rssi);
                            notifyListeners("tagFound", tagData);
                        }
                    }
                }
            });

            // Iniciar inventario
            boolean success = mReader.startInventoryTag();
            if (!success) {
                call.reject("Error al iniciar la lectura del RFID");
                return;
            }

            isInventoryRunning = true;
            loopStarted = true;

            JSObject ret = new JSObject();
            ret.put("success", true);
            ret.put("message", "Lectura RFID iniciada correctamente");
            call.resolve(ret);

            Log.d(TAG, "Lectura iniciada exitosamente");

        } catch (Exception e) {
            Log.e(TAG, "Error en startReading: " + e.getMessage(), e);
            isInventoryRunning = false;
            loopStarted = false;
            call.reject("Error al iniciar el lector RFID: " + e.getMessage());
        }
    }

    @PluginMethod(returnType = PluginMethod.RETURN_PROMISE)
    public void startFilteredReading(PluginCall call) {
        try {
            Log.d(TAG, "startFilteredReading called");

            if (mReader == null) {
                mReader = RFIDWithUHFUART.getInstance();
                if (mReader == null) {
                    call.reject("Could not get reader instance");
                    return;
                }
            }

            if (isFilteredInventoryRunning || isInventoryRunning) {
                call.reject("Reading is already running");
                return;
            }

            JSArray defaultEmptyArray = new JSArray();
            List<String> targetEpcs = call.getArray("targetTags", defaultEmptyArray).toList();


            if (targetEpcs.isEmpty()) {
                call.reject("targetTags array is required and cannot be empty");
                return;
            }

            targetTags.clear();
            foundTags.clear();
            for (Object epc : targetEpcs) {
                if (epc instanceof String) {
                    targetTags.add((String) epc);
                }
            }

            if (!mReader.init()) {
                call.reject("Error initializing RFID reader");
                return;
            }

            clearBufferInternal();

            mReader.setInventoryCallback(new IUHFInventoryCallback() {
                @Override
                public void callback(UHFTAGInfo uhftagInfo) {
                    if (uhftagInfo != null && isFilteredInventoryRunning) {
                        String epc = uhftagInfo.getEPC();
                        String rssi = uhftagInfo.getRssi();

                        if (epc != null && !epc.isEmpty() && !epc.matches("[0]+")) {
                            if (targetTags.contains(epc) && !foundTags.contains(epc)) {
                                foundTags.add(epc);

                                JSObject tagData = new JSObject();
                                tagData.put("epc", epc);
                                tagData.put("rssi", rssi);
                                tagData.put("timestamp", System.currentTimeMillis());

                                Log.d(TAG, "New target tag found: " + epc + " RSSI: " + rssi);
                                notifyListeners("filteredTagFound", tagData);
                            }
                        }
                    }
                }
            });

            boolean success = mReader.startInventoryTag();
            if (!success) {
                call.reject("Error starting RFID reading");
                return;
            }

            isFilteredInventoryRunning = true;
            loopStarted = true;

            JSObject ret = new JSObject();
            ret.put("success", true);
            ret.put("message", "Filtered RFID reading started successfully");
            ret.put("targetCount", targetTags.size());
            call.resolve(ret);

            Log.d(TAG, "Filtered reading started successfully with " + targetTags.size() + " target tags");

        } catch (Exception e) {
            Log.e(TAG, "Error in startFilteredReading: " + e.getMessage(), e);
            isFilteredInventoryRunning = false;
            loopStarted = false;
            call.reject("Error starting filtered RFID reader: " + e.getMessage());
        }
    }

    @PluginMethod(returnType = PluginMethod.RETURN_PROMISE)
    public void stopFilteredReading(PluginCall call) {
        try {
            Log.d(TAG, "stopFilteredReading called");

            if (!isFilteredInventoryRunning) {
                JSObject ret = new JSObject();
                ret.put("success", false);
                ret.put("message", "Filtered reader is not running");
                call.resolve(ret);
                return;
            }

            isFilteredInventoryRunning = false;
            loopStarted = false;

            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
                executorService = null;
            }

            if (mReader != null) {
                boolean success = mReader.stopInventory();

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                clearBufferInternal();

                try {
                    mReader.free();
                } catch (Exception e) {
                    Log.w(TAG, "Error freeing resources: " + e.getMessage());
                }

                JSObject ret = new JSObject();
                ret.put("success", success);
                ret.put("message", success ? "Filtered reading stopped successfully" : "Error stopping filtered reading");
                ret.put("foundCount", foundTags.size());
                ret.put("targetCount", targetTags.size());
                call.resolve(ret);

                Log.d(TAG, "Filtered reading stopped successfully. Found " + foundTags.size() + " of " + targetTags.size() + " target tags");
            } else {
                JSObject ret = new JSObject();
                ret.put("success", false);
                ret.put("message", "Reader not available");
                call.resolve(ret);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in stopFilteredReading: " + e.getMessage(), e);
            isFilteredInventoryRunning = false;
            loopStarted = false;

            JSObject ret = new JSObject();
            ret.put("success", false);
            ret.put("message", "Error stopping filtered RFID reader: " + e.getMessage());
            call.resolve(ret);
        }
    }

    @PluginMethod
    public void getFilteredReadingStatus(PluginCall call) {
        try {
            JSObject ret = new JSObject();
            ret.put("isRunning", isFilteredInventoryRunning);
            ret.put("foundCount", foundTags.size());
            ret.put("targetCount", targetTags.size());
            ret.put("success", true);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("Error getting filtered reading status", e);
        }
    }

    @PluginMethod
    public void clearFoundTags(PluginCall call) {
        try {
            int previousSize = foundTags.size();
            foundTags.clear();

            JSObject ret = new JSObject();
            ret.put("success", true);
            ret.put("cleared", previousSize);
            ret.put("message", "Found tags cleared successfully");
            call.resolve(ret);

            Log.d(TAG, "Found tags cleared, " + previousSize + " tags removed from memory");
        } catch (Exception e) {
            call.reject("Error clearing found tags", e);
        }
    }


    @PluginMethod(returnType = PluginMethod.RETURN_PROMISE)
    public void stopReading(PluginCall call) {
        try {
            Log.d(TAG, "stopReading called");

            if (!isInventoryRunning) {
                JSObject ret = new JSObject();
                ret.put("success", false);
                ret.put("message", "El lector no está en funcionamiento");
                call.resolve(ret);
                return;
            }

            // Marcar que debe parar
            isInventoryRunning = false;
            loopStarted = false;

            // Shutdown del ExecutorService si existe
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
                executorService = null;
            }

            if (mReader != null) {
                // Parar el inventario
                boolean success = mReader.stopInventory();

                // Pequeña pausa para asegurar que se detenga
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Limpiar el buffer completamente
                clearBufferInternal();

                // Liberar recursos temporalmente
                try {
                    mReader.free();
                } catch (Exception e) {
                    Log.w(TAG, "Error liberando recursos: " + e.getMessage());
                }

                JSObject ret = new JSObject();
                ret.put("success", success);
                ret.put("message", success ? "Lectura detenida correctamente" : "Error al detener la lectura");
                call.resolve(ret);

                Log.d(TAG, "Lectura detenida exitosamente");
            } else {
                JSObject ret = new JSObject();
                ret.put("success", false);
                ret.put("message", "Lector no disponible");
                call.resolve(ret);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en stopReading: " + e.getMessage(), e);
            isInventoryRunning = false;
            loopStarted = false;

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

    /**
     * Método interno para limpiar el buffer del lector
     */
    private void clearBufferInternal() {
        if (mReader != null) {
            try {
                // Limpiar el buffer múltiples veces para asegurar que esté vacío
                for (int i = 0; i < 10; i++) {
                    UHFTAGInfo tagInfo = mReader.readTagFromBuffer();
                    if (tagInfo == null) {
                        break;
                    }
                }
                Log.d(TAG, "Buffer limpiado internamente");
            } catch (Exception e) {
                Log.w(TAG, "Error limpiando buffer interno: " + e.getMessage());
            }
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
                initReaderAsync(call);
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

    /**
     * Método para obtener el estado actual del inventario
     */
    @PluginMethod
    public void getInventoryStatus(PluginCall call) {
        try {
            JSObject ret = new JSObject();
            ret.put("isRunning", isInventoryRunning);
            ret.put("success", true);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("Error obteniendo estado del inventario", e);
        }
    }

    private JSObject createTagResult(String epc, String rssi) {
        JSObject result = new JSObject();
        result.put("epc", epc);
        result.put("rssi", rssi);
        return result;
    }

    private void initReaderAsync(PluginCall call) {
        if (executorService == null) {
            executorService = Executors.newSingleThreadExecutor();
        }

        executorService.execute(() -> {
            boolean result = mReader.init();

            // Ejecutar en el hilo principal para UI/callbacks
            mainHandler.post(() -> {
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
            });
        });
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

                Log.d(TAG, "Buffer limpiado manualmente, " + cleared + " tags removidos");
            } else {
                call.reject("Lector no inicializado");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error limpiando buffer: " + e.getMessage(), e);
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
                mainHandler.post(() -> {
                    notifyListeners("triggerAutoReset", resetData);
                });
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

                notifyListeners("triggerReleased", data);
            }
            return true; 
        }
        return false; 
    }
}

