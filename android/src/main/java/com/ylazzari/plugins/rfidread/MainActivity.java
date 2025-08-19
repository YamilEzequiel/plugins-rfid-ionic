package com.ylazzari.plugins.rfidread;

import android.view.KeyEvent;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginHandle;

public class MainActivity extends BridgeActivity {
  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    // Solo procesar eventos de teclas específicas del escáner RFID
    int keyCode = event.getKeyCode();
    if (keyCode == 139 || keyCode == 280 || keyCode == 293) {
      try {
        // Obtener la instancia del plugin RFIDUHF usando el método correcto
        Plugin plugin = getBridge().getPlugin("RFIDUHF").getInstance();
        
        if (plugin != null && plugin instanceof RFIDPlugin) {
          RFIDPlugin rfidPlugin = (RFIDPlugin) plugin;
          
          // Delegar el evento al plugin según sea ACTION_DOWN o ACTION_UP
          if (event.getAction() == KeyEvent.ACTION_DOWN) {
            boolean handled = rfidPlugin.onKeyDown(keyCode, event);
            if (handled) {
              return true; // Evento manejado, no propagar más
            }
          } else if (event.getAction() == KeyEvent.ACTION_UP) {
            boolean handled = rfidPlugin.onKeyUp(keyCode, event);
            if (handled) {
              return true; // Evento manejado, no propagar más
            }
          }
        }
      } catch (Exception e) {
        // Log del error pero continuar con el manejo normal
        e.printStackTrace();
      }
    }
    
    // Solo llamar al super si no se manejó el evento
    return super.dispatchKeyEvent(event);
  }
}
