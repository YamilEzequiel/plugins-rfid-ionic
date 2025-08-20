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

import java.util.Timer;
import java.util.TimerTask;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import android.net.Uri;
import android.database.Cursor;
import android.provider.Settings;
import android.content.Intent;
import android.text.TextUtils;



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
    private long lastNotificationTime = 0;
    private static final long NOTIFICATION_THROTTLE = 10; // 10ms minimum between notifications

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
    
    // InfoWedge monitoring
    private Timer infowedgeMonitor;
    private boolean lastInfoWedgeKeyState = false;

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
            
            // Disable InfoWedge monitoring to reduce SELinux errors
            // startInfoWedgeMonitoring(); // Commented out - causes SELinux errors
            
            // DIRECT REGISTRATION: Register this plugin directly with KeyEventManager
            Log.i(TAG, "🔗 Registering RFIDPlugin directly with KeyEventManager");
            KeyEventManager.getInstance().setRFIDPlugin(this);
            
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
                targetEpcs = new ArrayList<>();
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
                            // Simple duplicate check without synchronization for speed
                            if (!foundTags.contains(epc)) {
                                foundTags.add(epc);

                                JSObject tagData = new JSObject();
                                tagData.put("epc", epc);
                                tagData.put("rssi", rssi);
                                tagData.put("timestamp", System.currentTimeMillis());

                                // Direct notification for speed - similar to BuscarTags
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

            // Set flags to false immediately to stop callbacks
            isFilteredInventoryRunning = false;
            loopStarted = false;

            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
                executorService = null;
            }

            boolean success = false;
            if (mReader != null) {
                success = mReader.stopInventory();

                // Faster cleanup - similar to BuscarTags
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                clearBufferInternal();
                lastNotificationTime = 0;
            }

            JSObject ret = new JSObject();
            ret.put("success", success);
            ret.put("message", success ? "Filtered reading stopped successfully" : "Error stopping filtered reading");
            ret.put("foundCount", foundTags.size());
            call.resolve(ret);

            Log.d(TAG, "Filtered reading stopped. Found " + foundTags.size() + " tags total");

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
            lastNotificationTime = 0;

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

    // 📌 Método para manejar eventos de tecla desde la Activity principal
    @PluginMethod
    public void handleKeyEvent(PluginCall call) {
        try {
            int keyCode = call.getInt("keyCode", 0);
            boolean isPressed = call.getBoolean("isPressed", false);
            handleKeyEventFromActivity(keyCode, isPressed);
            call.resolve(new JSObject().put("processed", true));
        } catch (Exception e) {
            Log.e(TAG, "Error handling key event: " + e.getMessage(), e);
            call.reject("Error handling key event", e);
        }
    }

    // 📌 Método público para ser llamado directamente desde MainActivity
    public void handleKeyEventFromActivity(int keyCode, boolean isPressed) {
        try {
            long currentTime = System.currentTimeMillis();
            
            Log.d(TAG, "🔧 handleKeyEventFromActivity - KeyCode: " + keyCode + ", IsPressed: " + isPressed + ", CurrentTime: " + currentTime);

            // Solo procesar códigos de teclas específicos del escáner RFID
            if (keyCode == 139 || keyCode == 280 || keyCode == 293) {
                Log.d(TAG, "🎯 Processing trigger key: " + keyCode);

                if (isPressed) {
                    // Auto-reset si la tecla ha estado presionada por mucho tiempo
                    if (isKeyPressed && (currentTime - lastKeyEventTime > KEY_TIMEOUT)) {
                        Log.w(TAG, "⚠️ Auto-reset: Key was stuck, releasing it");
                        isKeyPressed = false;
                        JSObject resetData = new JSObject();
                        resetData.put("message", "Auto-reset: Gatillo liberado automáticamente");
                        resetData.put("reason", "timeout");
                        notifyListeners("triggerAutoReset", resetData);
                    }

                    // Aplicar debounce para evitar eventos múltiples rápidos
                    if (currentTime - lastKeyEventTime < KEY_DEBOUNCE_DELAY) {
                        Log.d(TAG, "🔄 Debounce: Ignoring rapid key event");
                        return;
                    }

                    // Solo procesar si la tecla no estaba ya presionada
                    if (!isKeyPressed) {
                        isKeyPressed = true;
                        lastKeyEventTime = currentTime;

                        JSObject data = new JSObject();
                        data.put("message", "Gatillo presionado");
                        data.put("keyCode", keyCode);
                        data.put("timestamp", currentTime);

                        Log.d(TAG, "✅ Key pressed: " + keyCode + " - Notifying listeners");
                        notifyListeners("triggerPressed", data);
                    } else {
                        Log.d(TAG, "ℹ️ Key already pressed, ignoring duplicate press event");
                    }
                } else {
                    // Solo procesar si la tecla estaba presionada
                    if (isKeyPressed) {
                        isKeyPressed = false;
                        lastKeyEventTime = currentTime;

                        JSObject data = new JSObject();
                        data.put("message", "Gatillo liberado");
                        data.put("keyCode", keyCode);
                        data.put("timestamp", currentTime);

                        Log.d(TAG, "✅ Key released: " + keyCode + " - Notifying listeners");
                        notifyListeners("triggerReleased", data);
                    } else {
                        Log.d(TAG, "ℹ️ Key was not pressed, ignoring release event");
                    }
                }
            } else {
                Log.d(TAG, "ℹ️ Ignoring non-trigger key: " + keyCode);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error handling key event from activity: " + e.getMessage(), e);
        }
    }

    @PluginMethod
    public void simulateKeyPress(PluginCall call) {
        try {
            int keyCode = call.getInt("keyCode", 293);
            Log.d(TAG, "🧪 Simulating key press for testing - KeyCode: " + keyCode);
            
            // Simulate press
            handleKeyEventFromActivity(keyCode, true);
            
            // Simulate release after a short delay
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                handleKeyEventFromActivity(keyCode, false);
            }, 100);
            
            JSObject result = new JSObject();
            result.put("success", true);
            result.put("message", "Key press simulated for keyCode: " + keyCode);
            call.resolve(result);
        } catch (Exception e) {
            call.reject("Error simulating key press", e);
        }
    }

    // ========== INFOWEDGE MONITORING SOLUTION ==========
    
    private void startInfoWedgeMonitoring() {
        try {
            Log.d(TAG, "🔍 Starting InfoWedge monitoring as fallback solution");
            
            if (infowedgeMonitor != null) {
                infowedgeMonitor.cancel();
            }
            
            infowedgeMonitor = new Timer("InfoWedgeMonitor");
            infowedgeMonitor.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    checkInfoWedgeKeyState();
                }
            }, 0, 100); // Check every 100ms
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error starting InfoWedge monitoring: " + e.getMessage(), e);
        }
    }
    
    private void checkInfoWedgeKeyState() {
        try {
            boolean currentKeyState = false;

            // Method 1: Check system settings
            currentKeyState = checkSystemKeySettings() || currentKeyState;

            // Method 2: Use reflection to access InfoWedge internals
            currentKeyState = checkInfoWedgeReflection() || currentKeyState;

            // Method 3: Monitor content providers
            currentKeyState = checkContentProviders() || currentKeyState;

            // If state changed, trigger event
            if (currentKeyState != lastInfoWedgeKeyState) {
                Log.d(TAG, "🎯 InfoWedge key state changed: " + lastInfoWedgeKeyState + " -> " + currentKeyState);

                lastInfoWedgeKeyState = currentKeyState;

                final boolean keyStateForLambda = currentKeyState;
                mainHandler.post(() -> {
                    handleKeyEventFromActivity(293, keyStateForLambda);
                });
            }

        } catch (Exception e) {
            // Ignore errors in monitoring loop
        }
    }
    
    private boolean checkSystemKeySettings() {
        try {
            Context context = getContext();
            if (context == null) return false;
            
            // Check various system settings that might indicate key state
            String[] settingsKeys = {
                "key_state_293",
                "trigger_key_state",
                "scanner_key_state",
                "hardware_key_293"
            };
            
            for (String key : settingsKeys) {
                String value = android.provider.Settings.System.getString(
                    context.getContentResolver(), key);
                
                if (value != null && (value.equals("1") || value.equals("true"))) {
                    return true;
                }
            }
            
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }
    
    private boolean checkInfoWedgeReflection() {
        try {
            // Try to access InfoWedge service via reflection
            Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
            Method getServiceMethod = serviceManagerClass.getMethod("getService", String.class);
            
            String[] serviceNames = {
                "infowedge",
                "chainway_key",
                "scanner_service",
                "hardware_key"
            };
            
            for (String serviceName : serviceNames) {
                try {
                    Object service = getServiceMethod.invoke(null, serviceName);
                    if (service != null) {
                        // Try to get key state from service
                        Class<?> serviceClass = service.getClass();
                        
                        try {
                            Method getKeyStateMethod = serviceClass.getMethod("getKeyState", int.class);
                            Object result = getKeyStateMethod.invoke(service, 293);
                            
                            if (result instanceof Boolean) {
                                return (Boolean) result;
                            } else if (result instanceof Integer) {
                                return ((Integer) result) == 1;
                            }
                        } catch (Exception e) {
                            // Try alternative method names
                            try {
                                Method isKeyPressedMethod = serviceClass.getMethod("isKeyPressed", int.class);
                                Object result = isKeyPressedMethod.invoke(service, 293);
                                if (result instanceof Boolean) {
                                    return (Boolean) result;
                                }
                            } catch (Exception e2) {
                                // Continue to next service
                            }
                        }
                    }
                } catch (Exception e) {
                    // Continue to next service name
                }
            }
            
        } catch (Exception e) {
            // Reflection not available or failed
        }
        return false;
    }
    
    private boolean checkContentProviders() {
        try {
            Context context = getContext();
            if (context == null) return false;
            
            String[] providerUris = {
                "content://com.rscja.infowedge.keystate",
                "content://com.chainway.key.provider/current",
                "content://com.rscja.scanner.keystate/293"
            };
            
            for (String uriString : providerUris) {
                try {
                    Uri uri = Uri.parse(uriString);
                    Cursor cursor = context.getContentResolver().query(
                        uri, null, "keycode=?", new String[]{"293"}, null);
                    
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            // Look for state column
                            String[] stateColumns = {"state", "pressed", "value", "status"};
                            
                            for (String column : stateColumns) {
                                try {
                                    int columnIndex = cursor.getColumnIndex(column);
                                    if (columnIndex >= 0) {
                                        String value = cursor.getString(columnIndex);
                                        if (value != null && (value.equals("1") || value.equals("true"))) {
                                            cursor.close();
                                            return true;
                                        }
                                    }
                                } catch (Exception e) {
                                    // Continue to next column
                                }
                            }
                        }
                        cursor.close();
                    }
                } catch (Exception e) {
                    // Continue to next provider
                }
            }
            
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }
    
    @PluginMethod
    public void stopInfoWedgeMonitoring(PluginCall call) {
        try {
            if (infowedgeMonitor != null) {
                infowedgeMonitor.cancel();
                infowedgeMonitor = null;
                Log.d(TAG, "✅ InfoWedge monitoring stopped");
            }
            
            JSObject result = new JSObject();
            result.put("success", true);
            result.put("message", "InfoWedge monitoring stopped");
            call.resolve(result);
        } catch (Exception e) {
            call.reject("Error stopping InfoWedge monitoring", e);
        }
    }

    @PluginMethod
    public void checkAccessibilityPermission(PluginCall call) {
        try {
            boolean isEnabled = isAccessibilityServiceEnabled();
            
            JSObject result = new JSObject();
            result.put("enabled", isEnabled);
            result.put("success", true);
            result.put("message", isEnabled ? "Accessibility service is enabled" : "Accessibility service is not enabled");
            
            call.resolve(result);
        } catch (Exception e) {
            call.reject("Error checking accessibility permission", e);
        }
    }

    @PluginMethod
    public void requestAccessibilityPermission(PluginCall call) {
        try {
            if (isAccessibilityServiceEnabled()) {
                JSObject result = new JSObject();
                result.put("success", true);
                result.put("message", "Accessibility service is already enabled");
                call.resolve(result);
                return;
            }

            // Open accessibility settings
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            getContext().startActivity(intent);
            
            JSObject result = new JSObject();
            result.put("success", true);
            result.put("message", "Accessibility settings opened. Please enable the RFID Key Interceptor service.");
            call.resolve(result);
            
        } catch (Exception e) {
            call.reject("Error requesting accessibility permission", e);
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        try {
            String serviceName = getContext().getPackageName() + "/com.ylazzari.plugins.rfidread.KeyInterceptorService";
            
            String enabledServices = Settings.Secure.getString(
                getContext().getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            
            if (enabledServices != null) {
                TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
                splitter.setString(enabledServices);
                
                while (splitter.hasNext()) {
                    String service = splitter.next();
                    if (service.equals(serviceName)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking accessibility service: " + e.getMessage(), e);
        }
        return false;
    }

    @PluginMethod
    public void testKeyEventFlow(PluginCall call) {
        try {
            Log.i(TAG, "🧪 Testing complete key event flow");
            
            // Test 1: Direct KeyEventManager notification
            KeyEventManager.getInstance().notifyKeyEvent(777, true);
            
            // Test 2: Simulate key press
            handleKeyEventFromActivity(293, true);
            
            // Test 3: After delay, simulate release
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                handleKeyEventFromActivity(293, false);
            }, 500);
            
            JSObject result = new JSObject();
            result.put("success", true);
            result.put("message", "Key event flow test executed - check logs");
            call.resolve(result);
            
        } catch (Exception e) {
            call.reject("Error testing key event flow", e);
        }
    }
}

