package com.ylazzari.plugins.rfidread;

import com.getcapacitor.BridgeActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.util.Log;

public class MainActivity extends BridgeActivity {
    private static final String TAG = "MainActivity";
    private RFIDPlugin rfidPlugin;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "MainActivity created");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Get reference to the RFID plugin
        rfidPlugin = (RFIDPlugin) this.getBridge().getPlugin("RFIDUHF").getInstance();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Forward key events to the RFID plugin
        if (rfidPlugin != null && (keyCode == 139 || keyCode == 280 || keyCode == 293)) {
            try {
                // Call the plugin's handleKeyEvent method
                rfidPlugin.handleKeyEventFromActivity(keyCode, true);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error forwarding key down event: " + e.getMessage(), e);
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Forward key events to the RFID plugin
        if (rfidPlugin != null && (keyCode == 139 || keyCode == 280 || keyCode == 293)) {
            try {
                // Call the plugin's handleKeyEvent method
                rfidPlugin.handleKeyEventFromActivity(keyCode, false);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error forwarding key up event: " + e.getMessage(), e);
            }
        }
        return super.onKeyUp(keyCode, event);
    }
}

