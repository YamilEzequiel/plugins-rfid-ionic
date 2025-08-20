package com.ylazzari.plugins.rfidread;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class KeyInterceptorService extends AccessibilityService {
    private static final String TAG = "KeyInterceptorService";
    
    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "🔧 KeyInterceptorService connected");
        
        // Configure the service to intercept key events
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY |
                     AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS |
                     AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        info.notificationTimeout = 100;
        
        setServiceInfo(info);
        Log.d(TAG, "✅ Service configured for key interception");
    }
    
    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        int action = event.getAction();
        
        Log.d(TAG, "🎯 Hardware key intercepted: " + keyCode + ", action: " + 
              (action == KeyEvent.ACTION_DOWN ? "DOWN" : action == KeyEvent.ACTION_UP ? "UP" : "OTHER"));
        
        // Check for trigger keys
        if (keyCode == 139 || keyCode == 280 || keyCode == 293) {
            Log.d(TAG, "✅ Trigger key detected in AccessibilityService: " + keyCode);
            
            boolean isPressed = (action == KeyEvent.ACTION_DOWN);
            
            // Notify via KeyEventManager
            KeyEventManager.getInstance().notifyKeyEvent(keyCode, isPressed);
            
            Log.d(TAG, "📡 Key event notified via KeyEventManager");
            
            // Return true to consume the event and prevent InfoWedge from getting it
            return true;
        }
        
        // Let other keys pass through
        return super.onKeyEvent(event);
    }
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // We don't need to handle accessibility events for key interception
        // This method is required by AccessibilityService but can be empty
    }
    
    @Override
    public void onInterrupt() {
        Log.d(TAG, "⚠️ KeyInterceptorService interrupted");
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "🔴 KeyInterceptorService destroyed");
    }
}
