package com.ylazzari.plugins.rfidread;

import android.view.KeyEvent;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginHandle;

public class MainActivity extends BridgeActivity {
  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    int keyCode = event.getKeyCode();

    if (keyCode > 0 && (keyCode == 139 || keyCode == 280 || keyCode == 293)) {
      try {
        Plugin plugin = getBridge().getPlugin("RFIDUHF").getInstance();
        if (plugin != null && plugin instanceof RFIDPlugin) {
          RFIDPlugin rfidPlugin = (RFIDPlugin) plugin;
          if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            boolean handled = rfidPlugin.onKeyDown(keyCode, event);
            if (handled) return true;
          } else if (event.getAction() == KeyEvent.ACTION_UP) {
            boolean handled = rfidPlugin.onKeyUp(keyCode, event);
            if (handled) return true;
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return super.dispatchKeyEvent(event);
  }
}
