package at.clicktovote;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.net.*;
import android.os.*;
import android.provider.*;
import android.provider.Settings.*;
import android.util.*;
import android.webkit.*;
import android.widget.*;
import at.clicktovote.*;
import com.digits.sdk.android.*;
import com.twitter.sdk.android.core.*;
import io.fabric.sdk.android.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import org.apache.http.*;
import org.apache.http.client.entity.*;
import org.apache.http.client.methods.*;
import org.apache.http.conn.*;
import org.apache.http.entity.*;
import org.apache.http.impl.client.*;
import org.apache.http.message.*;
import org.json.*;

import java.lang.System;
import android.graphics.drawable.*;
import android.view.*;

public class VoteImageActivity extends Activity {

    //android
    private static Context ctx;
    private static String logName;
    public SharedPreferences prefs;
    public boolean firstTime;

    //custom
    public static WebView webView;
    private WebView custom;
    public boolean javascriptInterfaceBroken = false;
    public int customBackgroundColor = 0x00000000;
    public int webviewBackgroundColor = 0x00000000;

    public boolean loadingFinished = false;
    public List<String> code = new ArrayList<String>();
    private String translucent = "";
    private String lastUrl = "";
    private boolean premium = false;

    private Uri imageUri;
    public PollData pollData = null;
    public String callback = null;

    private final String assetsUrl = "file:///android_asset/";
    private final String indexUrl = assetsUrl + "index.html";
    private String appPath = "click-to-vote.at";
    private String keysPath = "http://keys." + appPath + "/";
    private final String alternativePath = "http://dl.dropboxusercontent.com/u/70345137/key/";

    public final int PICK_IMAGE = 1;
    public final int MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS = 2;

    private Utils utils;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //start transparent loading
        custom = new WebView(this);
        custom.loadUrl(assetsUrl + "loading.html");
        custom.setBackgroundColor(customBackgroundColor);
        setContentView(custom);

        String url = getIntent().getDataString();
        Log.i(logName, "on create data = " + url);

        super.onCreate(savedInstanceState);

        //WebView
        webView = new WebView(this);

        //right white margin 2.3
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setBackgroundColor(webviewBackgroundColor);
//        webView.setBackgroundColor(Color.rgb(196,220,230));

//        BitmapDrawable bg = new BitmapDrawable(BitmapFactory.decodeResource(getResources(), R.drawable.background));
//        bg.setGravity(Gravity.TOP);
//        webView.setBackgroundDrawable(bg);
        ctx = getApplicationContext();
        utils = new Utils(ctx);
        logName = VoteImageActivity.class.getName();

        prefs = ctx.getSharedPreferences("clicktovote", Activity.MODE_PRIVATE);

        firstTime = prefs.getBoolean("firstTime", true);
        prefs.edit().putBoolean("firstTime", false).commit();

        //Cant copy PRO prefs because not have acces to 'package.pro'
        try { //WebView
            if ("2.3".equals(Build.VERSION.RELEASE)) {
                javascriptInterfaceBroken = true;
            }
        } catch (Exception e) {
            // Ignore, and assume user javascript interface is working correctly.
        }

        // Add javascript interface only if it's not broken
        webView.setWebViewClient(
                new WebViewClient() {

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
                    runStoredCode(view);
                    Log.i(logName, "only hash changed");
                    return;
                }

                //call super if not hash url!
                super.onPageFinished(view, url);
                //dont remove search '?' from url, cose is update
                lastUrl = url.split("#")[0];

                // If running on 2.3, send javascript to the WebView to handle the function(s)
                if (javascriptInterfaceBroken) {
                    String handleGingerbreadStupidity = "javascript:;"
                            + "function handler() {"
                            + "this.loadKey = function(url) {"
                            + "    window.location = 'http://Device:loadKey': + url;"
                            + "};"
                            + "this.loadProfile = function(callback) {"
                            + "    window.location = 'http://Device:loadProfile': + callback;"
                            + "};"
                            + "this.newKey = function() {"
                            + "    window.location = 'http://Device:newKey';"
                            + "};"
                            + "this.nameUpdate = function(name) {"
                            + "    window.location = 'http://Device:nameUpdate': + name;"
                            + "};"
                            + "this.firstTimeOk = function() {"
                            + "    window.location = 'http://Device:firstTimeOk';"
                            + "};"
                            + "this.loadDefault = function() {"
                            + "    window.location = 'http://Device:loadDefault';"
                            + "};"
                            + "this.loadUrl = function(url) {"
                            + "    window.location = 'http://Device:loadUrl': + url;"
                            + "};"
                            + "this.askPhone = function(callB) {"
                            + "    window.location = 'http://Device:askPhone': + callB;"
                            + "};"
                            + "this.save = function(data, key) {"
                            + "    var sep = '[Device]';"
                            + "    window.location = 'http://Device:save:' + encodeURIComponent(data + sep + key);"
                            + "};"
                            + "this.share = function(img, key) {"
                            + "    var sep = '[Device]';"
                            + "    window.location = 'http://Device:share:' + encodeURIComponent(img + sep + key);"
                            + "};"
                            + "this.pickIconImage = function() {"
                            + "    window.location = 'http://Device:pickIconImage';"
                            + "};"
                            + "this.getKeyData = function(key) {"
                            + "    window.location = 'http://Device:getKeyData': + key;"
                            + "};"
                            + "this.error = function(text) {"
                            + "    window.location = 'http://Device:error': + text;"
                            + "};"
                            + "this.log = function(text) {"
                            + "    window.location = 'http://Device:log': + text;"
                            + "};"
                            + "this.close = function() {"
                            + "    window.location = 'http://Device:close';"
                            + "};"
                            + "}"
                            + "var Device = new handler();";
                    view.loadUrl(handleGingerbreadStupidity);
                }
                //EXTRAS
                if (premium) {
                    js("$('#premium').remove(); $('body').append($('<div id=\"premium\">').load('~premium/premium.html'))");
                }

                runStoredCode(view);
                Log.i(logName, "url loaded: " + url);
            }
        }
        );

        if (!javascriptInterfaceBroken) {
            webView.addJavascriptInterface(new WebAppInterface(), "Device");
        }

        //PREMIUM
        boolean wasPremium = prefs.getBoolean("isPremium", false);
        premium = isPremium();
        if (premium) {
            if (!wasPremium) {
                prefs.edit().putBoolean("isPremium", true).commit();

                getPackageManager().setComponentEnabledSetting(
                        new ComponentName("at.clicktovote", "at.clicktovote.FreeActivity"),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

                getPackageManager().setComponentEnabledSetting(
                        new ComponentName("at.clicktovote", "at.clicktovote.PremiumActivity"),
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            }

        } else if (wasPremium) {
            prefs.edit().putBoolean("isPremium", false).commit();

            getPackageManager().setComponentEnabledSetting(
                    new ComponentName("at.clicktovote", "at.clicktovote.PremiumActivity"),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

            getPackageManager().setComponentEnabledSetting(
                    new ComponentName("at.clicktovote", "at.clicktovote.FreeActivity"),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        }

        //
        WebSettings webSettings = webView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true); //HTML5 local storage?

        //to load vote.html. lower versions are enabled by default
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            webSettings.setAllowFileAccessFromFileURLs(true);
            webSettings.setAllowUniversalAccessFromFileURLs(true);
        }

        webSettings.setAllowFileAccess(true);

        if (null != url) {
            //lastUrl = url; //dont get web url!
            start(url);
        }

        //setContentView after start() -> let show translucent mode
        if (null == url || !url.contains("/share/")) {
            Log.i(logName, "setContentView(webView);");
            setContentView(webView);
        }
    }

    private void runStoredCode(WebView view) {
        Log.i(logName, "runStoredCode()");
        loadingFinished = true;

        //check finish                        
        for (int i = 0; i < code.size(); i++) {
            Log.i(logName, "LOAD STORED CODE: " + code.get(i));
            view.loadUrl(code.get(i));
        }
        //clean inject code
        code.clear();
    }

    private class WebAppInterface {

        @JavascriptInterface
        public String isTranslucent() {
            String res = translucent;
            //translucent = "";
            return res;
        }

        @JavascriptInterface
        public void loadKeyData(String keyId) {
            Log.i(logName, "Key id = " + keyId);
            new GetData().execute(keyId);
        }

        @JavascriptInterface
        public void loadProfile() {
            jsAddUser();
            if (null != callback && !"".equals(callback)) {
                js(callback);
            }
        }

        @JavascriptInterface
        public void newKey(String token) {
            //get new key
            String[] profile = getUserProfile();
            //token for know is same call
            new GetNewKey().execute(profile[0], token);
        }

        //not seems to work by the way
        @JavascriptInterface
        public void nameUpdate(String name) {
            prefs.edit().putString("userName", name).commit();
        }

        @JavascriptInterface
        public void firstTimeOk() {
            if (needPermission) {
                startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS);
                // finish() on startActivityForResult
            } else {
                startLastSocialApp();
                finish();
            }
        }

        @JavascriptInterface
        public void loadUrl(String url) {
            loadWebviewUrl("file:///android_asset/" + url);
        }

        @JavascriptInterface
        public void askPhone(String callB) {
            Log.i(logName, "askPhone() callback = " + callB);
            //if askPhone needs force
            startFabric();
            Digits.getSessionManager().clearActiveSession();

            if (!"".equals(callB)) {
                callback = callB;
            }
            digitsAuth();
        }

        @JavascriptInterface
        public void save(String action, String data, String token, String key, String isPublic, String country, String callback) {
            //prevent amateur hacks
            if (Build.FINGERPRINT.startsWith("generic")) {
                return;
            }

            Log.i(logName, "SAVE. key: " + key + ", publicKey: " + isPublic + ", action: " + action);

            pollData = new PollData(action, token, key, data, isPublic, country, callback);
            if (null != isPublic && !"".equals(isPublic)) { //if is public
                String digitsKey = prefs.getString("digitsKey", null);
                Log.i(logName, "stored digitsKey: " + digitsKey);

                // VALIDATE USER FIRST TIME
                if (null == digitsKey) {
                    //new GetCaptchaToken().execute(); ////////////////////////
                    Log.i(logName, "digitsAuth()");
                    digitsAuth();
                    return;
                }

            }

            new SaveData().execute(action, token, key, data, isPublic, country, callback);
        }

        @JavascriptInterface
        public void share(String img, String key) {
            if ("".equals(img)) {
                Log.i(logName, "EMPTY SHARED IMG");
                return;
            }
            Log.i(logName, "key = " + key + " on share()");
            isSharing = true;

            Share shareClass = new Share(ctx);
            shareClass.shareImageJS(img, key);

            //trying remove after all shareImage done
            js("$('.absoluteLoading').remove(); sharingPoll = false;");
        }

        @JavascriptInterface
        public void pickIconImage() {
            try {
                //image
                Intent pickIntent = new Intent(Intent.ACTION_GET_CONTENT, null);
                pickIntent.setType("image/*");

                pickImage(pickIntent);

            } catch (ActivityNotFoundException e) {
                //OLD VERSIONS
                try {
                    Intent pickIntent = new Intent(Intent.ACTION_PICK, null);
                    pickIntent.setType("image/*");

                    pickImage(pickIntent);

                } catch (ActivityNotFoundException e2) {
                    Toast.makeText(ctx, getResources().getString(R.string.questionGoogleServices), Toast.LENGTH_LONG).show();
                }
            }
        }

        @JavascriptInterface
        public String getKeyData(String keyId) {
            String data = dataKeys.get(keyId);
            Log.i(logName, "data: " + data);
            return data;
        }

        @JavascriptInterface
        public void error(final String text) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String urlString = "http://click-to-vote.at/error.php";
                    HttpPost httppost = new HttpPost(urlString);

                    ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
                    params.add(new BasicNameValuePair("error", text));

                    try {
                        httppost.setEntity(new UrlEncodedFormEntity(params));
                        DefaultHttpClient client = new DefaultHttpClient();
                        client.execute(httppost);

                    } catch (Exception e) {
                        Log.i(logName, "error on " + urlString);
                    }

                }
            }).start();
        }

        @JavascriptInterface
        public void log(final String text) {
            Log.i("CONSOLE", text);
        }

        @JavascriptInterface
        public void close() {
            finish();
        }
    }

    private boolean fabricStarted = false;

    private void startFabric() {
        if (!fabricStarted) {
            fabricStarted = true;
            TwitterAuthConfig authConfig = new TwitterAuthConfig("K4G5F4rG76943qA1wYrmDwXZp", "bhgDztwlBzNCAWH7WaHbfi7FZJ3gdYUEgEn2LI5rExpWzN2Z5w");
            Fabric.with(this, new TwitterCore(authConfig), new Digits());
        }
    }

    private void jsAddUser() {
        //add me
        String[] profile = getUserProfile();//userId, name, ISOS
        String publicId = prefs.getString("publicId", ""); //send empty string!
        js("addUser('" + profile[0] + "', '" + profile[1] + "', '" + profile[2] + "', '" + publicId + "')");
    }

    private boolean askingPhone = false;

    private void digitsAuth() {
        //once
        askingPhone = true;

        //Twitter Digits:
        startFabric();

        Digits.authenticate(new AuthCallback() { //new AuthCallback() returns number phone to Digits.authenticate() function
            @Override
            public void success(DigitsSession session, String phoneNumber) {
                Log.i(logName, "start digitsAuth()");

                askingPhone = false;
                isPublicActivation = true;

                js("window.loadingPublicKey = true");

                TwitterAuthConfig authConfig = TwitterCore.getInstance().getAuthConfig();

                TwitterAuthToken authToken = (TwitterAuthToken) session.getAuthToken();
                DigitsOAuthSigning oauthSigning = new DigitsOAuthSigning(authConfig, authToken);

                final Map<String, String> authHeaders = oauthSigning.getOAuthEchoHeadersForVerifyCredentials();

                Log.i(logName, "start Runnable digitsAuth()");

                //prevent android.os.NetworkOnMainThreadException
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(logName, "run Runnable digitsAuth()");
                        try {
                            URL url = new URL("http://click-to-vote.at/verify.php?nocache=" + (new Date()).getTime());

                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                            connection.setRequestMethod("GET");

                            // Add OAuth Echo headers to request
                            for (Map.Entry<String, String> entry : authHeaders.entrySet()) {
                                Log.i(logName, entry.getKey() + " : " + entry.getValue());
                                connection.setRequestProperty(entry.getKey(), entry.getValue());
                            }

                            // retrieve status gives 'android.os.NetworkOnMainThreadException' !
                            //int status = connection.getResponseCode();
                            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                            String inputLine;
                            StringBuffer response = new StringBuffer();

                            while ((inputLine = in.readLine()) != null) {
                                response.append(inputLine);
                            }
                            in.close();

                            String str = response.toString();

                            if (0 == str.length()) {
                                Log.i(logName, "error retrieve key on: " + response.toString());
                                return;
                            }

                            if (str.charAt(0) == '_') {
                                Log.i(logName, "ERROR: " + str);
                                JSerror(str.substring(1));
                                return;
                            }

                            Log.i(logName, "Digits response = " + str + " !!!");

                            String[] keys = str.split("\\|");
                            String publicId = keys[0]; //if retrieve stringed id
                            String digitsKey = keys[1];
                            String phonePrefix = keys[2];
                            Log.i(logName, "phonePrefix = " + phonePrefix);

                            prefs.edit()
                                    .putString("publicId", publicId)
                                    // TODO: digitsKey WILL BE MORE SECURE IF SAVED ENCRYPTED ON FILE TEXT WITH REST OF DATA
                                    .putString("digitsKey", md5("digitsKey=" + digitsKey))
                                    .putString("phonePrefix", phonePrefix)
                                    .commit();

                            jsAddUser();
                            Log.i(logName, "publicId = " + publicId);

                            //connection.disconnect();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        SaveDataOnLogin();
                        Log.i(logName, "DONE digitsAuth()");
                    }
                }).start();
            }

            @Override
            public void failure(DigitsException exception) {
                Log.i(logName, "DIGITS LOGIN failure !!");
                askingPhone = false;
            }
        }, R.style.CustomDigitsTheme);
    }

    private class PollData {

        String action;
        String token;
        String key;
        String data;
        String isPublic;
        String country;
        String callback;

        public PollData(String action, String token, String key, String data, String isPublic, String country, String callback) {
            this.action = action;
            this.token = token;
            this.key = key;
            this.data = data;
            this.isPublic = isPublic;
            this.country = country;
            this.callback = callback;
        }
    }

    private void loadWebviewUrl(final String url) {
        webView.post(new Runnable() {
            @Override
            public void run() {
                Log.i(logName, "URL: " + url);
                webView.loadUrl(url);
            }
        });
    }

//    private void loadWebviewJS(final String js) {
//        webView.post(new Runnable() {
//            @Override
//            public void run() {
//                Log.i(logName, "JS: " + js);
//                //webView.loadUrl(js);
//                webView.loadDataWithBaseURL(null, js, "text/html", "utf-8", null);
//            }
//        });
//    }
    public void pickImage(Intent pickIntent) {
        //photo
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        imageUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(), "fname_"
                + String.valueOf(System.currentTimeMillis()) + ".jpg"));
        takePhotoIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageUri);

        //choser
        Intent chooserIntent = Intent.createChooser(pickIntent, getResources().getString(R.string.SelectImage));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takePhotoIntent});

        startActivityForResult(chooserIntent, PICK_IMAGE);
    }

    private boolean loading = false;
    private boolean isActivityRestarting = false;
    private boolean isPublicActivation = false;
    private boolean isSharing = false;

    @Override
    public void onRestart() {
        super.onRestart();
        isActivityRestarting = true;
    }

    private BroadcastReceiver connectivityChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            boolean online = isNetworkAvailable();
            webView.setNetworkAvailable(online);
            Log.i(logName, "isNetworkAvailable() " + Boolean.toString(online));
        }
    };

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null;
        } catch (Exception e) {
            Log.e(logName, "isNetworkAvailable()", e);
        }
        return true;
    }

    @Override
    protected void onPause() {
        Log.i(logName, "onPause");
        custom.setBackgroundColor(0xFF000000);
        webView.setBackgroundColor(0xFF000000);

        super.onPause();
        if (connectivityChangeReceiver != null) {
            unregisterReceiver(connectivityChangeReceiver);
        }
        //will
        isActivityRestarting = true;
    }

    @Override
    public void onResume() {
        Log.i(logName, "activity onResume()");
        super.onResume();

        custom.setBackgroundColor(customBackgroundColor);
        webView.setBackgroundColor(webviewBackgroundColor);

        //connected to network detection
        IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(connectivityChangeReceiver, intentFilter);
        //webView.setNetworkAvailable(isNetworkAvailable());

        if (firstTime) {
            Log.i(logName, "firstTime");
            loadWebviewUrl(indexUrl + "#translucent");

            String bool = "";
            //TODO: prevent activate permission: very agressive now
//            if (needUsagePermission()) {
//                Log.i(logName, "NEEDS USAGE PERMISSION");
//                needPermission = true;
//                bool = "true";
//            }

            js("firstTime(" + bool + ")");
            firstTime = false; //set anymore

        } else if (askingPhone) {
            //when goes back te get key activation 
            Log.i(logName, "askingPhone");
            //startFabric(); //this not work
            digitsAuth(); //duplicates services?
            askingPhone = false;

        } else if (loading) {
            //open existing votations            
            if ("".equals(lastUrl)) {
                Log.i(logName, "#loading");
                loadWebviewUrl(indexUrl + "#loading");
            } else {
                Log.i(logName, "loading lastUrl: " + lastUrl);
                loadWebviewUrl(lastUrl);
            }

        } else if (!isActivityRestarting && !isSharing && !isPublicActivation) {
            //open new votations
            Log.i(logName, "!isActivityRestarting");
            loadWebviewUrl(indexUrl);

        } else {
            Log.i(logName, "resume()");
            //go back or return to votation
            js("resume()");
        }
        //else nothing

        loading = false;
        isActivityRestarting = false;
        isPublicActivation = false;
        isSharing = false;
    }

    private boolean needPermission = false;

    public boolean needUsagePermission() {
//        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false;
        }
        //checked seems good way
//        final UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService("usagestats");
//        final List<UsageStats> queryUsageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, 0, System.currentTimeMillis());
//        return queryUsageStats.isEmpty();
        return !utils.hasUsageStatsPermission();
    }

    @Override
    public void onNewIntent(Intent intent) {
        Log.i(logName, "ON NEW INTENT");
        super.onNewIntent(intent);
        //prevents bug?
        //setIntent(intent);

        //if not data, can be default not in app intent 
        String data = intent.getDataString();
        if (null != data) {
            start(data);
        }
    }

    public void start(String url) {
        Log.i(logName, "url = " + url);
        if (null == url) {
            return;
        }

        //not transform all toLowerCase() because "key Id"
        if (!url.toLowerCase().contains("click-to-vote")) {
            Log.i(logName, "error: WRONG INTENT URL? " + url);
            return;
        }

        if (url.contains("/share/")) {
            setContentView(custom);

            String[] arrUrl = url.split("/");
            String keyId = arrUrl[arrUrl.length - 1];

            //override
            String js = "var data = Device.getKeyData('" + keyId + "');"
                    + "var arr = JSON.parse(data + ']');"
                    + "var obj = toObject(arr);"
                    + "var canvas = document.createElement('canvas');"
                    + "canvas.id = 'shareCanvas';"
                    + "canvas.display = 'none';"
                    + "$('body').append(canvas);"
                    + "getCanvasImage('#shareCanvas', obj, '" + keyId + "', 0, true, function(imgData){"
                    + "Device.share(imgData, '" + keyId + "');"
                    + "Device.close();"
                    + "});";

            new GetData(js).execute(keyId);
            return;
        }

        String[] parts = url.split("//");
        String[] array = parts[parts.length - 1].split("/");
        //if not pathname '/'
        if (array.length < 2) {
            js("$('html').removeClass('translucent'); defaultPage()");
            return;
        }
        //key
        String keyId = array[array.length - 1];
        lastUrl = indexUrl + "?" + keyId; //this will load next
        Log.i(logName, "lastUrl = " + lastUrl);

        if (!"".equals(keyId)) {
            //prevent when not resume not loading screen
            js("loading();");
            translucent = "true";
            loading = true;

            new GetData().execute(keyId);
            return;
        }
        Log.i(logName, "URL NOT FOUND");
    }

    public String[] getUserProfile() {
        String userId = prefs.getString("userId", null); //null for check exists
        String userName = prefs.getString("userName", ""); //empty for send empty name
        String ISO = getUserISO();

        //stored
        if (null != userId) {
            return new String[]{userId, userName, ISO};
        }

        Log.i(logName, "SEARCH USER PROFILE FIRST TIME");
        ContentResolver contentResolver = getContentResolver();
        userId = Secure.getString(contentResolver, Secure.ANDROID_ID);

        prefs.edit()
                .putString("userId", userId)
                .commit();

        return new String[]{userId, userName, ISO};
    }

    public String getUserISO() {
        String prefix = prefs.getString("phonePrefix", null);
        if (null == prefix) {
            Log.i(logName, "EMPTY phonePrefix pref");
            return "";
        }

        String ISO = "";
        String json = readJSON("phoneCodes.json");
        //JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(json);

            //prefix 4
            JSONObject country = jsonObject.optJSONObject(prefix);

            if (null == country) {
                //prefix 3
                prefix = prefix.substring(0, prefix.length() - 1);
                country = jsonObject.optJSONObject(prefix);

                if (null == country) {
                    //prefix 2
                    prefix = prefix.substring(0, prefix.length() - 1);
                    country = jsonObject.optJSONObject(prefix);

                    if (null == country) {
                        //prefix 1
                        prefix = prefix.substring(0, prefix.length() - 1);
                        country = jsonObject.optJSONObject(prefix);
                        if (null == country) {
                            Log.i(logName, "invalid prefix = " + prefs.getString("phonePrefix", ""));
                            return null;
                        }
                    }
                }
            }

            ISO = country.optString("ISO");
            if ("".equals(ISO)) {
                Log.i(logName, "CANT RETRIEVE ISO FROM PREFIX OBJECT = " + prefix);
            }

        } catch (JSONException e) {
            String completePrefix = prefs.getString("phonePrefix", "");
            Log.e(logName, "error retrieving iso countries from json with prefix = " + completePrefix, e);
        }

        return ISO;
    }

    private List<String> getOrgs(String country) {
        List<String> ISOS = new ArrayList();

        String json = readJSON("orgs.json");
        Log.i(logName, "json: " + json);
        JSONObject jsonObject = null;

        try {
            jsonObject = new JSONObject(json);

            Iterator<?> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                Log.i(logName, "key: " + key);
                //check is object
                if (jsonObject.get(key) instanceof JSONArray) {
                    JSONArray jsonArray = (JSONArray) jsonObject.get(key);
                    if (null == jsonArray) {
                        continue;
                    }

                    for (int i = 0; i < jsonArray.length(); i++) {
                        String ISO = jsonArray.getString(i);
                        Log.i(logName, "iso: " + ISO);
                        if (ISO.equals(country)) {
                            ISOS.add(key);
                        }
                    }
                }
            }

        } catch (Exception e) {
            Log.e(logName, "error on getOrgs()", e);
        }

        return ISOS;
    }

    private String readJSON(String name) {
        String json = null;
        try {
            InputStream is = getAssets().open(name);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public void js(String text) {
        //dont try catch, let window.onerror js to detect line and file
        String run = "javascript:;" + text;

        if (!loadingFinished) {
            Log.i(logName, "!loadingFinished");
            code.add(run);
        } else {
            Log.i(logName, "INJECTION = " + text);
            loadWebviewUrl(run);
//            loadWebviewJS(run);
        }
    }

    // BACKGROUND CONNECTION WORKS
    final public DefaultHttpClient httpclient = new DefaultHttpClient();
    final public HttpPost httppost = new HttpPost("http://click-to-vote.at/update.php");

    private class GetData extends AsyncTask<String, Void, String> {

        private String override = null;

        //constructors
        public GetData() {
            //nothing
        }

        public GetData(String def) {
            override = def;
        }

        private String keyId;
        private String key;

        @Override
        protected String doInBackground(String... urls) {
            keyId = urls[0];

            //path
            String path = keysPath;
            String url;
            if ('-' != keyId.charAt(0)) {
                //public
                key = keyId;
                String countryUrl = "";
                if (keyId.indexOf('-') > 0) {
                    String[] arr = keyId.split("-");
                    key = arr[1];

                    countryUrl = "~" + arr[0] + "/";
                }

                url = path + "get.php?url=public/" + countryUrl + "/" + key + "&";

            } else {
                path += "private/";
                url = path + keyId + "?";
            }

            //request
            String data;

            try {
                HttpGet httpget = new HttpGet(url + "nocache=" + (new java.util.Date()).getTime());
                HttpResponse response = httpclient.execute(httpget);

                //fail?
                int code = response.getStatusLine().getStatusCode();
                if (200 != code) {
                    if (path.contains("//sml.town")) {
                        return "_error when request: " + path + ", with key: '" + key + "'. Check your internet connection.";
                    }
                    return null;
                }

                HttpEntity ht = response.getEntity();

                BufferedHttpEntity buf = new BufferedHttpEntity(ht);
                InputStream is = buf.getContent();
                //ISO-8859-1 shows good accents, ñ, etc..
                BufferedReader r = new BufferedReader(new InputStreamReader(is, "ISO-8859-1"));

                StringBuilder total = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    total.append(line);
                }
                data = total.toString();

            } catch (Exception e) {
                Log.e(logName, "error", e);
                return null;
            }

            return data;
        }

        @Override
        protected void onPostExecute(String data) {
            Log.i(logName, "FILE DATA = " + data);

            if (null == data) {
                JSerror(getResources().getString(R.string.connectionFailed));
                Log.i(logName, "error on key: " + keyId);
                return;

            } else if (data.charAt(0) == '_') {

                //try alternative path
                if (!keysPath.equals(alternativePath)) {
                    keysPath = alternativePath;
                    new GetData().execute(keyId);
                    //warn
                } else {
                    JSerror(data.substring(1));
                }
                return;
            }

            dataKeys.put(keyId, data);
            if (null == override) {
                //tell js request the huge data                
                js("dataIsReady('" + keyId + "')");

            } else {
                js(override);
            }

        }
    }

    private HashMap<String, String> dataKeys = new HashMap<>();

    private class GetNewKey extends AsyncTask<String, Void, String> {

        String token;

        @Override
        protected String doInBackground(String... urls) {
            String id = urls[0];
            token = urls[1];

            ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("action", "newkey"));
            params.add(new BasicNameValuePair("id", id));

            try {
                httppost.setEntity(new UrlEncodedFormEntity(params));
                HttpResponse response = httpclient.execute(httppost);
                Log.i(logName, "HTTP Entiry: " + convertStreamToString(httppost.getEntity().getContent()));

//                BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
//                StringBuffer sb = new StringBuffer("");
//                String line = "";
//                while ((line = in.readLine()) != null) {
//                    sb.append(line);
//                }
//                in.close();
//                return sb.toString();
                return convertStreamToString(response.getEntity().getContent());

            } catch (ConnectTimeoutException e) {
                js("newKeyConnectionError");
                return null;

            } catch (Exception e) {
                Log.e(logName, "error", e);
            }

            return "_key_error";
        }

        @Override
        protected void onPostExecute(String key) {
            if (null == key) {
                // controlled error ?
                Log.i(logName, "NULL KEY");
                return;
            }

            if ("".equals(key)) {
                //doInBbackground error?
                Log.i(logName, "EMPTY KEY !!!!!!!!!!!!!!!!!!!!!!!!");
                return;
            }

            if ('_' == key.charAt(0)) {
                Log.i(logName, "error: " + key);
                return;
            }

            Log.i(logName, "loadKey('" + key + "') on GetNewKey");
            js("loadKey(" + token + ",'" + key + "')");
        }
    }

    private class SaveData extends AsyncTask<String, Void, String> {

        private String action;
        private String token = null;
        private String key;
        private String value;
        private String isPublic;
        private String country;
        private String callback = null;

        @Override
        protected String doInBackground(String... urls) {
            action = urls[0];
            token = urls[1];
            key = urls[2];
            value = urls[3];
            isPublic = urls[4];
            country = urls[5];
            callback = urls[6];

            ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("action", action));
            params.add(new BasicNameValuePair("key", key));
            params.add(new BasicNameValuePair("value", value));

            String userId = prefs.getString("userId", null);
            Log.i(logName, "user android id: " + userId);

            Log.i(logName, "isPublic: " + isPublic);
            if (null != isPublic && !"".equals(isPublic) && !"null".equals(isPublic) && !"undefined".equals(isPublic) && !"false".equals(isPublic)) {
                userId = prefs.getString("publicId", null);

                String digitsKey = prefs.getString("digitsKey", null);
                Log.i(logName, "digitsKey to send: " + digitsKey);
                if (null == digitsKey) {
                    Log.i(logName, "EMPTY digitsKey!");
                    //return "_1 from android app"; //need digitsKey error
                    return "_again";
                }

                Log.i(logName, "sending as public value. isPublic: " + isPublic);
                params.add(new BasicNameValuePair("public", "true"));
                if (!"".equals(country)) {
                    params.add(new BasicNameValuePair("pollCountry", country));
                }

                params.add(new BasicNameValuePair("digitsKey", digitsKey));
                String ISO = getUserISO();
                params.add(new BasicNameValuePair("ISO", ISO));

            }

            params.add(new BasicNameValuePair("id", userId));

            try {
                httppost.setEntity(new UrlEncodedFormEntity(params));
                HttpResponse response = httpclient.execute(httppost);
                Log.i(logName, "HTTP Entiry: " + convertStreamToString(httppost.getEntity().getContent()));

//                BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
//                StringBuffer sb = new StringBuffer("");
//                String line = "";
//                while ((line = in.readLine()) != null) {
//                    sb.append(line);
//                }
//                in.close();
//                return sb.toString();
                return convertStreamToString(response.getEntity().getContent());

            } catch (Exception e) {
                Log.e(logName, "error", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String key) {
            if (null == key) {
                JSerror("cant connect");
                return;
            }
            if ("".equals(key)) {
                JSerror(getResources().getString(R.string.emptyKey));
                return;
            }
            if ('_' == key.charAt(0)) { //error
                if ('1' == key.charAt(1)) {
                    Log.i(logName, "error response: " + key);
                    //clear data
                    startFabric();
                    Digits.getSessionManager().clearActiveSession();
                    //start
                    digitsAuth();

                } else if ("_again".equals(key)) {
                    digitsAuth();

                } else if ('3' == key.charAt(1)) { //key already exists, make new
                    new SaveData().execute(action, token, null, value, isPublic, country, callback);
                    Log.i(logName, "error '3': " + key);

                } else {
                    Log.i(logName, "error again: " + key);
                    JSerror(key.substring(1));
                }
                return;
            }
            Log.i(logName, "RES: " + key);

            //load key like if public poll save without previous key!
            // - Identify key return when callback send:
            js("loadKey('" + token + "','" + key + "');");
            if (null != callback && !"".equals(callback)) {
                Log.i(logName, "callback = " + callback);
                js("(" + callback + ")();");
            } else {
                Log.i(logName, "not callback on saveData");
            }
        }
    }

    public void SaveDataOnLogin() {
        if (null == pollData) {
            Log.i(logName, "Poll Data is null");
            if (null != callback) {
                js(callback);
            }
            return;
        }

        new SaveData().execute(pollData.action, pollData.token, pollData.key, pollData.data, pollData.isPublic, pollData.country, pollData.callback);
        pollData = null;
    }

    public static final String md5(final String s) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(s.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        Log.i(logName, "ERROR ON GETTING MD5 !!!!!");
        return "";
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case PICK_IMAGE:
                if (resultCode == Activity.RESULT_OK) {
                    if (data == null) {
                        //Display an error?
                        Log.i(logName, "image data = null");
                        return;
                    }
                    Bitmap bitmap;

                    try {
                        if (null == data.getData()) {
                            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                        } else {
                            InputStream inputStream = ctx.getContentResolver().openInputStream(data.getData());
                            bitmap = BitmapFactory.decodeStream(inputStream);
                            inputStream.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }

                    // Convert bitmap to Base64 encoded image for web
                    ByteArrayOutputStream output = new ByteArrayOutputStream();

                    //make
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
                    byte[] byteArray = output.toByteArray();
                    String image = Base64.encodeToString(byteArray, Base64.NO_WRAP);

                    js("customStyles.newIconLoad('" + image + "')");
                }
                break;

            case MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS:
                Log.i(logName, "MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS done");
                if (utils.hasUsageStatsPermission()) {
                    startLastSocialApp();
                }
                //finish anyway ?
                finish();
                break;
        }

    }

    private void startLastSocialApp() {
        String lastTask = utils.getLastAppTask();
        if (null != lastTask) {
            Intent intent = getPackageManager().getLaunchIntentForPackage(lastTask);
            if (null != intent) {
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onBackPressed() {
        String webUrl = webView.getUrl();
        Log.i(logName, "logUrl: " + webUrl);

        if (null != translucent) {
            finish();

        } else if (!webUrl.contains("#") && webView.canGoBack()) { //like if was '/~vote' url
            //if key url ?
            Log.i(logName, "webView.goBack()");
            webView.goBack();

        } else {
            super.onBackPressed();
            Log.i(logName, "super.onBackPressed()");
        }
    }

    private boolean isPremium() {
        if (getPackageManager().checkSignatures(ctx.getPackageName(), "at.clicktovote.pro") == PackageManager.SIGNATURE_MATCH) {
            Log.i(logName, "PREMIUM IS INSTALLED");
            //updateGame("$('#log').text('OH YEAH!')");
            return true;
        }
        return false;
    }

    //4 DEBUG
    private String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        is.close();
        return sb.toString();

//                BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
//                StringBuffer sb = new StringBuffer("");
//                String line = "";
//                while ((line = in.readLine()) != null) {
//                    sb.append(line);
//                }
//                in.close();
//                return sb.toString();
    }

    private void JSerror(String js) {
        String err = "error('" + js.replace("\"", "\\\"").replace("'", "\\'") + "')";
        js(err);
        Log.i(logName, "JSerror() = " + err);
    }
}
