package com.paranoiaworks.unicus.android.sse.components;

import android.app.Activity;
import android.app.Dialog;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ScrollView;

import com.paranoiaworks.unicus.android.sse.R;

/**
 * Simple HTML View Dialog
 * used for application "Quick Help"
 * 
 * @author Unicus (unicus<atmark>paranoiaworks.com) for Paranoia Works
 * @version 1.0.2
 * @related Help.js
 */
public class SimpleHTMLDialog extends Dialog {

	private WebView webView;
	private ScrollView scrollView;
	private String scrollToToken = null;
	
    private Handler handler = new Handler();

	
	public SimpleHTMLDialog(View v) 
	{
		this((Activity)v.getContext());
	}	
	
	public SimpleHTMLDialog(Activity context) 
	{
		super(context, R.style.TransparentDialog);
		init();
	}
	
	public void loadURL(String url)
	{	
		webView.loadUrl(url);
	}
	
	private void init()
	{		
		this.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
		this.setContentView(R.layout.lc_simple_html_dialog);
		this.setCancelable(true);
		this.setCanceledOnTouchOutside(false);
		WindowManager.LayoutParams lp = this.getWindow().getAttributes();
		lp.dimAmount=0.6f;  
		this.getWindow().setAttributes(lp);  
		this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		
		View bExit = (View)findViewById(R.id.SHD_exit);
		scrollView = (ScrollView)findViewById(R.id.SHD_scrollView);
		webView = (WebView)findViewById(R.id.SHD_webView);
			
		WebSettings webSettings = webView.getSettings();
        webSettings.setSavePassword(false);
        webSettings.setSaveFormData(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(false);

        webView.setWebChromeClient(new AlteredWebChromeClient());
        webView.addJavascriptInterface(new SimpleJavaScriptInterface(), "SJSI");
              
        bExit.bringToFront();
        bExit.setOnClickListener(new android.view.View.OnClickListener() 
	    {
		    @Override
		    public void onClick(View v)
		    {
		    	cancel();
		    }
	    });
    }

    final class SimpleJavaScriptInterface 
    {
        public void gotoURL(final String url) 
        {
        	gotoURL(url, null);
        }
    	
        public void gotoURL(final String url, final String scrollToElement) 
        {
            handler.post(new Runnable() {
                public void run() {
                	if(scrollToElement != null) scrollToToken = scrollToElement;
                	webView.loadUrl("javascript:loadURL('" + url + "')");
                }
            });
        }
        
        public void scrollToXY(final String x, final String y, final String height)
        {
            handler.post(new Runnable() {
                public void run() {
                	double multiplier = (double)webView.getBottom() / Double.parseDouble(height);
                	scrollView.smoothScrollTo(Integer.parseInt(x), (int)(Double.parseDouble(y) * multiplier));
                }
            });
        }
    }

    final class AlteredWebChromeClient extends WebChromeClient
    {
        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            //SSElog.d("SimpleHTMLDialog: onJsAlert", message);
        	result.confirm();
            return true;
        }
        
        @Override
        public void onProgressChanged(WebView view, int newProgress)
        {
        	if(newProgress == 100)
        	{
        		webView.requestFocus();
        		
                handler.post(new Runnable() {
                    public void run() {
                    	if(scrollToToken != null) webView.loadUrl("javascript:scrollToElement('" + scrollToToken + "')");
                    	//else scrollView.scrollTo(0, 0);
                    	scrollToToken = null;
                    }
                });     		
        	}
        	super.onProgressChanged(view, newProgress);
        }
    }
	
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SEARCH) {            
            return true;
        } else {
        	return super.onKeyDown(keyCode, event);
        }
    }
    
    @Override
    public void onBackPressed()
    {
    	if(webView.canGoBack()) webView.goBack();
    	else super.onBackPressed();
    }
}
