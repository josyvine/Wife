package com.tradeanalyst.app;

import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.ConsoleMessage;
import android.util.Log;

public class MyWebChromeClient extends WebChromeClient {
    private static final String TAG = "MyWebChromeClient";
    private final MainActivity mActivity;

    public MyWebChromeClient(MainActivity activity) {
        this.mActivity = activity;
    }

    @Override
    public void onPermissionRequest(final PermissionRequest request) {
        // Grant permissions for audio recording automatically inside headless WebView
        Log.d(TAG, "onPermissionRequest: Granting WebRTC permissions automatically inside sandbox.");
        request.grant(request.getResources());
    }

    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        Log.d(TAG, "Console message (" + consoleMessage.messageLevel() + "): " 
            + consoleMessage.message() + " -- From line " 
            + consoleMessage.lineNumber() + " of " 
            + consoleMessage.sourceId());
        return true;
    }
}
