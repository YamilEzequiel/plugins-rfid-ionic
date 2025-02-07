package com.ylazzari.plugins.rfidread;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.rscja.deviceapi.RFIDWithUHFUART;
import com.rscja.deviceapi.entity.UHFTAGInfo;
import android.os.AsyncTask;
import android.view.KeyEvent;

@CapacitorPlugin(name = "RFIDUHF")
public class RFIDPlugin extends Plugin {
    private RFIDWithUHFUART mReader = null;
    private boolean loopStarted = false;
    private AsyncTask<Integer, String, Void> asyncTask = null;

    // Constantes para configuraciónte
    private static final int DEFAULT_POWER = 30;
    private static final int MIN_POWER = 5;
    private static final int MAX_POWER = 30;

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

                startAsyncTask(10);

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
                boolean success = mReader.stopInventory();
                if (asyncTask != null) {
                    asyncTask.cancel(true);
                    asyncTask = null;
                }
                loopStarted = false;

                JSObject ret = new JSObject();
                ret.put("success", success);
                ret.put("message", success ? "Lectura detenida correctamente" : "Error al detener la lectura");
                call.resolve(ret);
            } else {
                call.reject("El lector no está en funcionamiento");
            }
        } catch (Exception e) {
            call.reject("Error al detener el lector RFID: " + e.getMessage());
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
            if (tagInfo != null) {
                JSObject ret = new JSObject();
                ret.put("epc", tagInfo.getEPC()); // Obtener EPC del objeto UHFTAGInfo
                ret.put("rssi", tagInfo.getRssi()); // Obtener RSSI del objeto UHFTAGInfo
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

    private void startAsyncTask(int waitTime) {
        asyncTask = new AsyncTask<Integer, String, Void>() {
            String lastEpc = null;

            @Override
            protected Void doInBackground(Integer... integers) {
                while (loopStarted) {
                    try {
                        UHFTAGInfo tagInfo = mReader.readTagFromBuffer();
                        if (tagInfo != null) {
                            String epc = tagInfo.getEPC();
                            if (epc != null && !epc.matches("[0]+")) {
                                if (lastEpc == null || !lastEpc.equalsIgnoreCase(epc)) {
                                    JSObject tagData = new JSObject();
                                    tagData.put("epc", epc);
                                    tagData.put("rssi", tagInfo.getRssi());

                                    // Notificar en el hilo principal
                                    getActivity().runOnUiThread(() -> {
                                        notifyListeners("tagFound", tagData);
                                    });

                                    lastEpc = epc;
                                }
                            }
                        }
                        Thread.sleep(integers[0]);
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        // Log error pero continuar el loop
                        e.printStackTrace();
                    }
                }
                return null;
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



    // 📌 Método para capturar teclas presionadas
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 139 || keyCode == 280 || keyCode == 293) {
            JSObject data = new JSObject();
            data.put("message", "Gatillo presionado");
            notifyListeners("triggerPressed", data);
            return true;
        }
        return false;
    }

    // 📌 Método para capturar teclas liberadas
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == 139 || keyCode == 280 || keyCode == 293) {
            JSObject data = new JSObject();
            data.put("message", "Gatillo liberado");
            notifyListeners("triggerReleased", data);
            return true;
        }
        return false;
    }

} 