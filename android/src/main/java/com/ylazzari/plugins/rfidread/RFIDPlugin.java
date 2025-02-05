package com.ylazzari.plugins.rfidread;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.rscja.deviceapi.RFIDWithUHFUART;
import com.rscja.deviceapi.entity.UHFTAGInfo;
import android.os.AsyncTask;
import android.app.ProgressDialog;

@CapacitorPlugin(name = "RFIDUHF")
public class RFIDPlugin extends Plugin {
    private RFIDWithUHFUART mReader = null;
    private boolean loopStarted = false;
    private AsyncTask<Integer, String, Void> asyncTask = null;
    
    // Constantes para configuración
    private static final int DEFAULT_POWER = 30;
    private static final int MIN_POWER = 5;
    private static final int MAX_POWER = 30;

    @Override
    public void load() {
        try {
            mReader = RFIDWithUHFUART.getInstance();
            if (mReader != null) {
                // Inicializar sin PluginCall ya que load() es un método del ciclo de vida
                boolean result = mReader.init();
                if (!result) {
                    notifyListeners("initError", new JSObject().put("message", "Fallo en la inicialización del lector"));
                } else {
                    notifyListeners("initSuccess", new JSObject().put("message", "Lector inicializado correctamente"));
                }
            }
        } catch (Exception e) {
            // Manejar el error de inicialización
            notifyListeners("initError", new JSObject().put("message", "Error: " + e.getMessage()));
        }
    }

    @PluginMethod
    public void startReading(PluginCall call) {
        try {
            if (mReader != null && !loopStarted) {
                loopStarted = true;
                mReader.startInventoryTag();
                startAsyncTask(10);
                JSObject ret = new JSObject();
                ret.put("success", true);
                call.resolve(ret);
            } else {
                call.reject("El lector ya está en funcionamiento o no está inicializado");
            }
        } catch (Exception e) {
            call.reject("Error starting RFID reader", e);
        }
    }

    @PluginMethod
    public void stopReading(PluginCall call) {
        try {
            if (mReader != null && loopStarted) {
                mReader.stopInventory();
                if (asyncTask != null) {
                    asyncTask.cancel(true);
                }
                loopStarted = false;
                JSObject ret = new JSObject();
                ret.put("success", true);
                call.resolve(ret);
            } else {
                call.reject("El lector no está en funcionamiento");
            }
        } catch (Exception e) {
            call.reject("Error stopping RFID reader", e);
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
                    UHFTAGInfo tagInfo = mReader.readTagFromBuffer();
                    if (tagInfo != null) {
                        String epc = tagInfo.getEPC();
                        if (epc != null && !epc.matches("[0]+")) {
                            if (lastEpc == null || !lastEpc.equalsIgnoreCase(epc)) {
                                notifyListeners("tagRead", createTagResult(epc, String.valueOf(tagInfo.getRssi())));
                                lastEpc = epc;
                            }
                        }
                    }
                    try {
                        Thread.sleep(integers[0]);
                    } catch (InterruptedException e) {
                        break;
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
} 