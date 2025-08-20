package com.ylazzari.plugins.rfidread;

import com.getcapacitor.BridgeActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.util.Log;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;


public class MainActivity extends BridgeActivity {
    private static final String TAG = "MainActivity";
    private RFIDPlugin rfidPlugin;
    private KeyContentObserver keyObserver;
    private Handler mainHandler;
    private KeyEventManager.KeyEventListener keyEventListener;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "MainActivity created");
        
        // Initialize handler for ContentObserver
        mainHandler = new Handler(Looper.getMainLooper());
        
        // Setup key event listener for AccessibilityService (MAIN SOLUTION)
        setupKeyEventListener();
        
        // Log that we're ready for AccessibilityService
        Log.i(TAG, "🚀 MainActivity ready for AccessibilityService events");
        
        // Disable old methods to reduce noise in logs
        // setupKeyObserver(); // Commented out - causes SELinux errors
    }

    @Override
    public void onResume() {
        super.onResume();
        // Get reference to the RFID plugin with retry logic
        obtainPluginReference();
    }
    
    private void obtainPluginReference() {
        try {
            if (this.getBridge() != null) {
                rfidPlugin = (RFIDPlugin) this.getBridge().getPlugin("RFIDUHF").getInstance();
                if (rfidPlugin == null) {
                    Log.w(TAG, "RFID Plugin not found with name RFIDUHF, will retry on key events");
                } else {
                    Log.d(TAG, "RFID Plugin reference obtained successfully");
                }
            } else {
                Log.w(TAG, "Bridge is null, cannot get plugin reference");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting RFID plugin reference: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "🔹 MainActivity onKeyDown called with keyCode: " + keyCode + ", event: " + event.toString());
        
        // Log ALL key events for debugging
        Log.i(TAG, "🔍 KEY DEBUG - Code: " + keyCode + ", Action: DOWN, Repeat: " + event.getRepeatCount());
        
        // Forward key events to the RFID plugin
        if (keyCode == 139 || keyCode == 280 || keyCode == 293) {
            Log.d(TAG, "✅ Trigger key detected: " + keyCode);
            
            // Try to get plugin reference if it's null
            if (rfidPlugin == null) {
                Log.d(TAG, "🔄 Plugin reference is null, attempting to obtain it");
                obtainPluginReference();
            }
            
            if (rfidPlugin != null) {
                try {
                    // Call the plugin's handleKeyEvent method
                    rfidPlugin.handleKeyEventFromActivity(keyCode, true);
                    Log.d(TAG, "✅ Key down event forwarded successfully to plugin");
                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error forwarding key down event: " + e.getMessage(), e);
                }
            } else {
                Log.w(TAG, "⚠️ RFID Plugin is still null after retry, cannot forward key event");
            }
        } else {
            Log.d(TAG, "ℹ️ Ignoring key code: " + keyCode + " (not a trigger key)");
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "🔸 MainActivity onKeyUp called with keyCode: " + keyCode + ", event: " + event.toString());
        
        // Log ALL key events for debugging
        Log.i(TAG, "🔍 KEY DEBUG - Code: " + keyCode + ", Action: UP, Repeat: " + event.getRepeatCount());
        
        // Forward key events to the RFID plugin
        if (keyCode == 139 || keyCode == 280 || keyCode == 293) {
            Log.d(TAG, "✅ Trigger key released: " + keyCode);
            
            // Try to get plugin reference if it's null
            if (rfidPlugin == null) {
                Log.d(TAG, "🔄 Plugin reference is null, attempting to obtain it");
                obtainPluginReference();
            }
            
            if (rfidPlugin != null) {
                try {
                    // Call the plugin's handleKeyEvent method
                    rfidPlugin.handleKeyEventFromActivity(keyCode, false);
                    Log.d(TAG, "✅ Key up event forwarded successfully to plugin");
                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error forwarding key up event: " + e.getMessage(), e);
                }
            } else {
                Log.w(TAG, "⚠️ RFID Plugin is null, cannot forward key event");
            }
        } else {
            Log.d(TAG, "ℹ️ Ignoring key code: " + keyCode + " (not a trigger key)");
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        boolean isDown = event.getAction() == KeyEvent.ACTION_DOWN;
        boolean isUp = event.getAction() == KeyEvent.ACTION_UP;
        
        Log.i(TAG, "🎯 dispatchKeyEvent - Code: " + keyCode + ", Action: " + 
              (isDown ? "DOWN" : isUp ? "UP" : "OTHER") + ", Repeat: " + event.getRepeatCount());
              
        // Check for trigger keys in dispatch event as well
        if ((keyCode == 139 || keyCode == 280 || keyCode == 293) && (isDown || isUp)) {
            Log.d(TAG, "🎯 Trigger key in dispatchKeyEvent: " + keyCode + " (action: " + (isDown ? "DOWN" : "UP") + ")");
            
            // Try to get plugin reference if it's null
            if (rfidPlugin == null) {
                Log.d(TAG, "🔄 Plugin reference is null in dispatch, attempting to obtain it");
                obtainPluginReference();
            }
            
            if (rfidPlugin != null) {
                try {
                    rfidPlugin.handleKeyEventFromActivity(keyCode, isDown);
                    Log.d(TAG, "✅ Key event forwarded from dispatchKeyEvent successfully");
                    // Don't return true here, let the normal flow continue
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error forwarding key event from dispatch: " + e.getMessage(), e);
                }
            }
        }
        
        return super.dispatchKeyEvent(event);
    }

    // ========== ACCESSIBILITY SERVICE SOLUTION ==========
    
    private void setupKeyEventListener() {
        try {
            Log.i(TAG, "🔧 Setting up KeyEventListener for AccessibilityService");
            
            keyEventListener = new KeyEventManager.KeyEventListener() {
                @Override
                public void onKeyEvent(int keyCode, boolean isPressed) {
                    Log.i(TAG, "🎯 RECEIVED KEY EVENT from AccessibilityService: " + keyCode + 
                          ", pressed: " + isPressed);
                    
                    // Forward to plugin
                    if (rfidPlugin == null) {
                        Log.d(TAG, "🔄 Plugin is null, trying to obtain reference");
                        obtainPluginReference();
                    }
                    
                    if (rfidPlugin != null) {
                        Log.i(TAG, "📤 FORWARDING key event to RFIDPlugin");
                        rfidPlugin.handleKeyEventFromActivity(keyCode, isPressed);
                    } else {
                        Log.e(TAG, "❌ RFID Plugin is still null - cannot forward event");
                    }
                }
            };
            
            // Register the listener
            KeyEventManager keyManager = KeyEventManager.getInstance();
            keyManager.addListener(keyEventListener);
            
            Log.i(TAG, "✅ KeyEventListener REGISTERED successfully");
            
            // Test the registration immediately
            new android.os.Handler().postDelayed(() -> {
                Log.i(TAG, "🧪 Testing KeyEventManager with simulated event");
                keyManager.notifyKeyEvent(999, true); // Test event
            }, 1000);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error setting up key event listener: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }

    // ========== CONTENT OBSERVER SOLUTION ==========
    
    private void setupKeyObserver() {
        try {
            Log.d(TAG, "🔧 Setting up ContentObserver for key events");
            
            keyObserver = new KeyContentObserver(mainHandler);
            
            // Multiple URIs to monitor key events from different sources
            String[] uris = {
                "content://com.rscja.infowedge.keystate",
                "content://com.chainway.key.provider",
                "content://settings/system",
                "content://com.rscja.scanner.keystate"
            };
            
            for (String uriString : uris) {
                try {
                    Uri uri = Uri.parse(uriString);
                    getContentResolver().registerContentObserver(uri, true, keyObserver);
                    Log.d(TAG, "✅ Registered observer for: " + uriString);
                } catch (Exception e) {
                    Log.w(TAG, "⚠️ Failed to register observer for: " + uriString);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error setting up key observer: " + e.getMessage(), e);
        }
    }
    
    private class KeyContentObserver extends ContentObserver {
        private long lastEventTime = 0;
        private static final long DEBOUNCE_TIME = 200; // 200ms debounce
        
        public KeyContentObserver(Handler handler) {
            super(handler);
        }
        
        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }
        
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastEventTime < DEBOUNCE_TIME) {
                return; // Debounce rapid events
            }
            lastEventTime = currentTime;
            
            Log.d(TAG, "🔍 ContentObserver onChange - URI: " + uri + ", selfChange: " + selfChange);
            
            // Try to detect key events by monitoring system changes
            detectKeyEvent();
        }
    }
    
    private void detectKeyEvent() {
        try {
            // Method 1: Check system settings for key state
            checkSystemKeyState();
            
            // Method 2: Monitor specific content providers
            checkKeyProviders();
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error detecting key event: " + e.getMessage(), e);
        }
    }
    
    private void checkSystemKeyState() {
        try {
            // Try to read from various system locations
            String[] keySettings = {
                "key_state_293",
                "scanner_key_state", 
                "trigger_key_state",
                "hardware_key_state"
            };
            
            for (String setting : keySettings) {
                try {
                    String value = android.provider.Settings.System.getString(
                        getContentResolver(), setting);
                    
                    if (value != null) {
                        Log.d(TAG, "🔑 Found key setting: " + setting + " = " + value);
                        parseKeyState(setting, value);
                    }
                } catch (Exception e) {
                    // Ignore individual failures
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "⚠️ Error checking system key state: " + e.getMessage());
        }
    }
    
    private void checkKeyProviders() {
        String[] providerUris = {
            "content://com.rscja.infowedge.keystate",
            "content://com.chainway.key.provider/keys",
            "content://com.rscja.scanner.keystate"
        };
        
        for (String uriString : providerUris) {
            try {
                Uri uri = Uri.parse(uriString);
                Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        // Process cursor data to detect key events
                        for (int i = 0; i < cursor.getColumnCount(); i++) {
                            String columnName = cursor.getColumnName(i);
                            String value = cursor.getString(i);
                            
                            Log.d(TAG, "🔍 Provider data: " + columnName + " = " + value);
                            
                            if (columnName.contains("key") || columnName.contains("state")) {
                                parseKeyState(columnName, value);
                            }
                        }
                    }
                    cursor.close();
                }
            } catch (Exception e) {
                // Provider might not exist, continue
            }
        }
    }
    
    private void parseKeyState(String key, String value) {
        try {
            if (value == null) return;
            
            // Look for key code 293 and state information
            if (key.contains("293") || value.contains("293")) {
                Log.d(TAG, "🎯 Found trigger key data: " + key + " = " + value);
                
                // Try to determine if it's press or release
                boolean isPressed = value.contains("1") || value.contains("true") || 
                                  value.contains("pressed") || value.contains("down");
                
                Log.d(TAG, "🔄 Parsed key state - Pressed: " + isPressed);
                
                // Forward to plugin
                forwardKeyEventToPlugin(293, isPressed);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error parsing key state: " + e.getMessage(), e);
        }
    }
    
    private void forwardKeyEventToPlugin(int keyCode, boolean isPressed) {
        try {
            // Get plugin reference if needed
            if (rfidPlugin == null) {
                obtainPluginReference();
            }
            
            if (rfidPlugin != null) {
                Log.d(TAG, "📤 Forwarding key event via ContentObserver: " + keyCode + ", pressed: " + isPressed);
                rfidPlugin.handleKeyEventFromActivity(keyCode, isPressed);
            } else {
                Log.w(TAG, "⚠️ Cannot forward key event - plugin is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error forwarding key event: " + e.getMessage(), e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Unregister key event listener
        if (keyEventListener != null) {
            try {
                KeyEventManager.getInstance().removeListener(keyEventListener);
                Log.d(TAG, "✅ KeyEventListener unregistered");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error unregistering listener: " + e.getMessage(), e);
            }
        }
        
        // Unregister content observer
        if (keyObserver != null) {
            try {
                getContentResolver().unregisterContentObserver(keyObserver);
                Log.d(TAG, "✅ ContentObserver unregistered");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error unregistering observer: " + e.getMessage(), e);
            }
        }
    }
}

