package at.imagevote;

import android.content.*;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.webkit.*;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import at.wouldyourather.R;

public class WebviewLayout extends WebView {

    private String logName = this.getClass().getName();
    public int webviewBackgroundColor = 0x11FFFFFF;

    private Context ctx;
    private VoteImageActivity activity;
    private boolean javascriptInterfaceBroken = false;

    public WebviewInterface webviewInterface;
    public List<String> code = new ArrayList<String>();
    public boolean loadingFinished = false;
    public String lastUrl = "";

    public WebviewLayout(Context context) {
        super(context);
        //start(context);
    }

    public WebviewLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        //start(context);
    }

    public WebviewLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        //start(context);
    }

    public void start(Context context) {
        ctx = context;
        activity = (VoteImageActivity) ctx;
    }

    public void load() {
        //right white margin 2.3
        this.setBackgroundColor(webviewBackgroundColor);

        //Cant copy PRO prefs because not have acces to 'package.pro'
        try { //WebView
            if ("2.3".equals(Build.VERSION.RELEASE)) {
                javascriptInterfaceBroken = true;
            }
        } catch (Exception e) {
            // Ignore, and assume user javascript interface is working correctly.
        }

        // Add javascript interface only if it's not broken
        this.setWebViewClient(new WebViewClient() {

            //not use 'shouldOverride..', called more times than 'onPageFinished'
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);

                Log.i(logName, "onPageStarted()");
                loadingFinished = false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.i(logName, "url change: '" + lastUrl + "' to '" + url + "'");

                //if only changes hash
                if (!"".equals(lastUrl) && url.contains(lastUrl) && url.contains("#")) {
                    //runStoredCode(view);
                    Log.i(logName, "only hash changed");
                    return;
                }

                //call super if not hash url!
                super.onPageFinished(view, url);
                //dont remove search '?' from url, cose is update
                lastUrl = url.split("#")[0];

                // If running on 2.3, send javascript to the WebView to handle the function(s)
                if (javascriptInterfaceBroken) {
                    view.loadUrl(webviewInterface.handleGingerbreadStupidity);
                }
                activity.users.jsAddUser();

                //EXTRAS
                if (activity.premium) {
                    js("$('#premium').remove(); $('body').append($('<div id=\"premium\">').load('~premium/premium.html'))");
                }

                //runStoredCode(view);
                Log.i(logName, "url loaded: " + url);
            }
        });

        if (!javascriptInterfaceBroken) {
            webviewInterface = new WebviewInterface(activity);
            addJavascriptInterface(webviewInterface, "Device");
        }

        WebSettings webSettings = getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true); //HTML5 localStorage?

        //to load vote.html. lower versions are enabled by default
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            webSettings.setAllowFileAccessFromFileURLs(true);
            webSettings.setAllowUniversalAccessFromFileURLs(true);
        }

        webSettings.setAllowFileAccess(true);

        loadLocalStorage();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setWebContentsDebuggingEnabled(true);
        }
    }

//    public void startWebview(String[] arrUrl, String url_request, String params) {
//        //if not pathname '/'
//        if (arrUrl.length < 2) {
//            js("$('html').removeClass('translucent'); defaultPage()");
//            return;
//        }
//
//        String keyId = arrUrl[arrUrl.length - 1];
//
//        //key
//        lastUrl = activity.indexUrl + "?" + keyId; //this is needed to load in assets index.html
//        Log.i(logName, "webView.lastUrl = " + lastUrl);
//
//        if (!"".equals(keyId)) {
//            //prevent when not resume not loading screen
//            js("loading()");
//            activity.translucent = "true";
//            activity.loading = true;
//
////            String[] key_arr = keyId.split("_");
////            if (key_arr.length == 2) {
//////                TODO: ver como prevenir esto de manera elegante
//////                String table = "preguntas" + key_arr[0];
//////                String key = key_arr[1];
//////                activity.parseRequests.new select().execute(table, null, key, "new RequestPollByKeyCallback");
////            } else {
//                //activity.requests.new GetData().execute(keyId);
//                activity.requests.new SimpleRequest().execute(url_request, params, null, null);
////            }
//
//            return;
//        }
//
//        Log.i(logName, "URL NOT FOUND");
//    }
    public void js(String text) {
        //dont try catch, let window.onerror js to detect line and file
        String run = "javascript:;" + text + "; ";

        if (!loadingFinished) {
            Log.i(logName, "!loadingFinished");
            code.add(run);
        } else {
            Log.i(logName, "INJECTING = " + text);
            loadWebviewUrl(run);
//            loadWebviewJS(run);
        }
    }

    public void JSerror(String js) {
        String err = "error('" + js.replace("\"", "\\\"").replace("'", "\\'") + "')";
        js(err);
        Log.i(logName, "JSerror() = " + err);
    }

    public void loadWebviewUrl(final String url) {
        post(new Runnable() {
            @Override
            public void run() {
                Log.i(logName, "URL: " + url);
                loadUrl(url);
            }
        });
    }

    //Layout.java
    public void runStoredCode(final WebView view) {
        Log.i(logName, "runStoredCode()");
        loadingFinished = true;
        
        post(new Runnable() {
            @Override
            public void run() {
                //check finish
                for (int i = 0; i < code.size(); i++) {
                    Log.i(logName, "LOAD STORED CODE: " + code.get(i));
                    view.loadUrl(code.get(i));
                }
                //clean inject code
                code.clear();
            }
        });
    }

    private void loadLocalStorage() {
        String localStorage = activity.prefs.getString("localStorage", "");
        if (!"".equals(localStorage)) {
            Map<String, String> map = new Gson().fromJson(
                    localStorage, new TypeToken<Map<String, String>>() {
                    }.getType()
            );

            String js_code = "";
            for (Map.Entry<String, String> entry : map.entrySet()) {
                js_code += "localStorage.setItem(" + entry.getKey() + "," + entry.getValue() + "); ";
            }
            js(js_code);
        }
    }

    public void saveLocalStorage() {
        js("Device.saveLocalStorage(JSON.stringify(localStorage))");
    }

//    public void visible() {
//        activity.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                if (getVisibility() == View.GONE) {
//                    js("$('html').addClass('translucent')");
//                }
//                setVisibility(View.VISIBLE);
//            }
//        });
//    }

}
