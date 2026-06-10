package com.tradeanalyst.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.webkit.WebViewAssetLoader;

/**
 * CUSTOM WEBVIEW CLIENT: PuterWebViewClient
 * Role: Intercepts assets requests and serves them from local folders under a secure virtual origin [1].
 * Location: App flat package (side-by-side with MainActivity).
 */
public class PuterWebViewClient extends WebViewClient {

    private static final String TAG = "PuterWebViewClient";
    private final WebViewAssetLoader mAssetLoader;
    private final Context mContext;

    /**
     * Constructor for PuterWebViewClient.
     * Configures the WebViewAssetLoader with a path handler targeting local assets.
     * 
     * @param context App context.
     */
    public PuterWebViewClient(Context context) {
        this.mContext = context;
        
        // Configure AssetLoader to mount virtual HTTPS origin
        this.mAssetLoader = new WebViewAssetLoader.Builder()
                .setDomain("appassets.androidplatform.net")
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(context))
                .build();
                
        Log.d(TAG, "PuterWebViewClient: WebViewAssetLoader successfully initialized targeting 'appassets.androidplatform.net'.");
    }

    /**
     * Intercepts incoming resource loading requests from the WebView.
     * If the request matches our virtual origin scheme, it serves the file directly from local assets.
     */
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        // Intercept asset routes (e.g. appassets.androidplatform.net/assets/live_agent.html)
        WebResourceResponse localResponse = mAssetLoader.shouldInterceptRequest(request.getUrl());
        if (localResponse != null) {
            Log.d(TAG, "shouldInterceptRequest: Redirecting resource request to local asset: " + request.getUrl().toString());
            return localResponse;
        }
        
        // Pass standard external network requests (such as Retrofit and raw WebSockets) natively
        return super.shouldInterceptRequest(view, request);
    }

    /**
     * Intercepts URL overrides to ensure we retain the navigation context inside our WebView session.
     */
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();
        Log.d(TAG, "shouldOverrideUrlLoading: Processing page redirection to: " + url);
        
        // Return false to let the WebView proceed with the page load natively without creating external browser intents
        return false;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        Log.d(TAG, "onPageStarted: Page load started for: " + url);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        Log.d(TAG, "onPageFinished: Page load completed for: " + url);
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);
        Log.e(TAG, "onReceivedError: WebView Error (" + errorCode + "): " + description + " at: " + failingUrl);
    }
}
