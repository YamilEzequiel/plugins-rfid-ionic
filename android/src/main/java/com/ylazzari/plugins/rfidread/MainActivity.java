package com.ylazzari.plugins.rfidread;

import android.view.KeyEvent;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginHandle;

public class MainActivity extends BridgeActivity {
  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    // Obtener la instancia del plugin RFIDUHF usando el método correcto
    Plugin plugin = getBridge().getPlugin("RFIDUHF").getInstance();
    
    if (plugin != null && plugin instanceof RFIDPlugin) {
      RFIDPlugin rfidPlugin = (RFIDPlugin) plugin;
      
      // Delegar el evento al plugin según sea ACTION_DOWN o ACTION_UP
      if (event.getAction() == KeyEvent.ACTION_DOWN) {
        return rfidPlugin.onKeyDown(event.getKeyCode(), event);
      } else if (event.getAction() == KeyEvent.ACTION_UP) {
        return rfidPlugin.onKeyUp(event.getKeyCode(), event);
      }
    }
    
    return super.dispatchKeyEvent(event);
  }
}
