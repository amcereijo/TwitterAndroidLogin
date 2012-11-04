package com.example.twitterandroidlogin;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * 
 * @author angelcereijo
 *
 */
public class TwitterWebView extends Activity {
	
	public final static String KEY_URL_TO_LOAD = "urlToLoad";
	public final static int WEBVIEW_RETURN_CODE = 2001;
	
	private Intent intent;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        
        intent = getIntent();
        Bundle extras = intent.getExtras();
        String urlToLoad = (String)extras.get(KEY_URL_TO_LOAD);
        
        WebView webView = (WebView) findViewById(R.id.webView);
        webView.loadUrl(urlToLoad);
        
        CookieSyncManager.createInstance(this); 
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
        
        
        webView.setWebViewClient(new WebViewClient(){
        	@Override
        	public boolean shouldOverrideUrlLoading(WebView view, String url) {
        		intent.setData(Uri.parse(url));
        		setResult(WEBVIEW_RETURN_CODE, intent);
        		finish();
        		return false;
        	}
        });
        
        setResult(WEBVIEW_RETURN_CODE);
    }

}
