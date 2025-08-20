package com.ylazzari.plugins.rfidread;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.lang.ref.WeakReference;

public class KeyEventManager {
    private static final String TAG = "KeyEventManager";
    private static KeyEventManager instance;
    private List<KeyEventListener> listeners = new ArrayList<>();
    private WeakReference<RFIDPlugin> rfidPluginRef;
    
    public interface KeyEventListener {
        void onKeyEvent(int keyCode, boolean isPressed);
    }
    
    private KeyEventManager() {
        Log.d(TAG, "🔧 KeyEventManager instance created");
    }
    
    public static synchronized KeyEventManager getInstance() {
        if (instance == null) {
            instance = new KeyEventManager();
        }
        return instance;
    }
    
    public void addListener(KeyEventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            Log.d(TAG, "✅ Key event listener added. Total listeners: " + listeners.size());
        }
    }
    
    public void removeListener(KeyEventListener listener) {
        if (listeners.remove(listener)) {
            Log.d(TAG, "✅ Key event listener removed. Total listeners: " + listeners.size());
        }
    }
    
    public void setRFIDPlugin(RFIDPlugin plugin) {
        this.rfidPluginRef = new WeakReference<>(plugin);
        Log.d(TAG, "✅ RFIDPlugin reference set directly in KeyEventManager");
    }
    
    public void notifyKeyEvent(int keyCode, boolean isPressed) {
        Log.d(TAG, "🎯 Notifying " + listeners.size() + " listeners + RFIDPlugin of key event: " + keyCode + 
              ", pressed: " + isPressed);
              
        // Method 1: Notify regular listeners (MainActivity)
        List<KeyEventListener> currentListeners = new ArrayList<>(listeners);
        for (KeyEventListener listener : currentListeners) {
            try {
                listener.onKeyEvent(keyCode, isPressed);
            } catch (Exception e) {
                Log.e(TAG, "❌ Error notifying listener: " + e.getMessage(), e);
            }
        }
        
        // Method 2: DIRECT notification to RFIDPlugin (BYPASS MainActivity)
        if (rfidPluginRef != null) {
            RFIDPlugin plugin = rfidPluginRef.get();
            if (plugin != null) {
                Log.i(TAG, "🚀 DIRECT notification to RFIDPlugin - BYPASSING MainActivity");
                try {
                    plugin.handleKeyEventFromActivity(keyCode, isPressed);
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error notifying RFIDPlugin directly: " + e.getMessage(), e);
                }
            } else {
                Log.w(TAG, "⚠️ RFIDPlugin reference is null or garbage collected");
            }
        } else {
            Log.w(TAG, "⚠️ No RFIDPlugin reference set");
        }
    }
    
    public void clearListeners() {
        listeners.clear();
        Log.d(TAG, "🧹 All listeners cleared");
    }
}
