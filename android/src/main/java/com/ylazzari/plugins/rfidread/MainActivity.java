package com.ylazzari.plugins.rfidread;

import android.util.Log;
import android.view.KeyEvent;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.Bridge;
import com.ylazzari.plugins.rfidread.RFIDPlugin;

public class MainActivity extends BridgeActivity {
  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    if (event.getAction() == KeyEvent.ACTION_DOWN || event.getAction() == KeyEvent.ACTION_UP) {
      String state = (event.getAction() == KeyEvent.ACTION_DOWN) ? "pressed" : "released";
      int keyCode = event.getKeyCode();
      String keyName = KeyEvent.keyCodeToString(keyCode);
      
      JSObject data = new JSObject();
      data.put("state", state);
      data.put("keyCode", keyCode);
      data.put("keyName", keyName);
      
      notifyListeners("keyEvent", data);
    }
    return super.dispatchKeyEvent(event);
  }
}
