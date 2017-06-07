package at.imagevote;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.net.*;
import android.os.*;
import android.provider.*;
import android.util.*;
import android.view.View;
import android.webkit.*;

import at.wouldyourather.*;

import java.io.*;

public class VoteImageActivity extends Activity {

    private static Context ctx;
    private String logName = this.getClass().getName();
    public SharedPreferences prefs;

    private boolean isActivityRestarting = false;
    public boolean premium = false;
    public boolean needPermission = false;
    public boolean loading = false;
    public boolean isPublicActivation = false;
    public boolean isSharing = false;
    public String translucent = "";
    public String callback = null;

    public WebviewLayout webView = null;
    public WebView custom = null;
    public int customBackgroundColor = 0x11FFFFFF;
    private final String assetsUrl = "file:///android_asset/";
    public String indexUrl = assetsUrl + "index.html";

    public MessageLayout edText;

    public final int PICK_IMAGE = 1;
    public final int MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS = 2;
    public final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 3;
    public final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE_MANUAL = 4;

    private Utils utils;
    public Requests requests;
    public Digits digits;
    public Users users;
    public ParseRequests parseRequests;
    public GoogleIndex googleIndex;
    public Interstitial interstitial;

    //public HashMap<String, String> dataKeys = new HashMap<>();
    public Share sharing;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String url = getIntent().getDataString();
        Log.i(logName, "on create data = " + url);

        //ctx = getApplicationContext();
        ctx = this;
        prefs = getSharedPreferences(getResources().getString(R.string.url), Activity.MODE_PRIVATE);

        utils = new Utils(ctx);
        requests = new Requests(ctx);
        digits = new Digits(ctx);
        users = new Users(ctx);
        parseRequests = new ParseRequests(ctx);
        googleIndex = new GoogleIndex(ctx);

        //setContentView after start() -> let show translucent mode
        Log.i(logName, "setContentView(R.layout.main); " + url);
        setContentView(R.layout.main);

        boolean shareRun = null != url && url.contains("/share");
        webView = (WebviewLayout) findViewById(R.id.webview);
        if (shareRun) {
//            Log.i(logName, "shareRun -> webView.setVisibility(View.GONE);");
//            webView.setVisibility(View.GONE);
            webView.js("$('html').addClass('translucent'); loading('.wrapper', true);");
        }
        webView.start(ctx);
        webView.load();

        edText = (MessageLayout) findViewById(R.id.send_message);

        String packageName = getResources().getString(R.string.package_name);

        //PREMIUM
        boolean wasPremium = prefs.getBoolean("isPremium", false);
        premium = isPremium();
        if (premium) {
            if (!wasPremium) {
                prefs.edit().putBoolean("isPremium", true).commit();

                getPackageManager().setComponentEnabledSetting(
                        new ComponentName(packageName, packageName + ".FreeActivity"),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

                getPackageManager().setComponentEnabledSetting(
                        new ComponentName(packageName, packageName + ".PremiumActivity"),
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            }

        } else if (wasPremium) {
            prefs.edit().putBoolean("isPremium", false).commit();

            getPackageManager().setComponentEnabledSetting(
                    new ComponentName(packageName, packageName + ".PremiumActivity"),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

            getPackageManager().setComponentEnabledSetting(
                    new ComponentName(packageName, packageName + ".FreeActivity"),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        }
        
        interstitial = new Interstitial(ctx);

        Log.i(logName, "url = " + url);
        if (null == url) {
            return;
        }

        ///////////////////////////////////////////////////////////////////////
        //UNSOLVED DEPRECATED 4.0.3 BUG :(
        if(Build.VERSION.SDK_INT == Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1){
            indexUrl = "http://wouldyourather.co";
        }
        start(url);
    }

    // (sharing app from browser case)
    private void start(String url) {
        //path
        String[] arrUrl = url.split("/");
        if (arrUrl.length < 2) {
            webView.js("$('html').removeClass('translucent'); defaultPage()");
            return;
        }
        String keyId = arrUrl[arrUrl.length - 1];
        Log.i(logName, "keyId: " + keyId + " from " + url);

        if (keyId.isEmpty()) {
            return;
        }

        //lastUrl
        webView.lastUrl = indexUrl + "?" + keyId; //this is needed to load in assets index.html ???
        Log.i(logName, "webView.lastUrl = " + webView.lastUrl);

        //prevent when not resume not loading screen
        webView.js("loading(null, true)"); //true: hidding all
        translucent = "true";
        loading = true;

        //path or domain
        if (!url.contains("/share")) {
            //w8 html app loading..
            return;
        }

        ///////////////////////////////////////////////////////////////////////
        // ON SHARE LOAD:
        //moveTaskToBack(true); //allow web app working
        String url_request = "";
        String params = "";
        String countryUrl = "";

        String[] share_url_arr = url.split("://");
        String share_url = share_url_arr[share_url_arr.length - 1].split("/")[0]; //define url for sharing (optional)
        Log.i(logName, "share_url: " + share_url);

        //if direct sharing app:
        String[] data = url.split("/share_");
        if (data.length < 2) {
            data = url.split("#_");
        }
        String[] extra = new String[0];
        String type = "";
        if (data.length > 1) {
            extra = data[1].split("/")[0].split("_");
            type = "show";
        }

        String defineVotes = "";
        for (int i = 0; i < extra.length; i++) {
            defineVotes += "obj.options[" + i + "][2] = " + extra[i] + "; ";
        }

        //String path = "http://" + ctx.getResources().getString(R.string.url_keys) + "/";
        String path = "testing..";
        
        //remove 'share.' if needed
        String link_url_arr[] = share_url.split("share.");
        String link_url = link_url_arr[link_url_arr.length - 1];
        
        String js_post_callback_default = ""
                + "var canvas = document.createElement('canvas'); "
                + "canvas.id = 'shareCanvas'; "
                + "canvas.display = 'none'; "
                + "$('body').append(canvas); "
                //                + "setTimeout(function(){" //w8 all rly ready
                + "  getCanvasImage('#shareCanvas', obj, screenPoll.key, 0, '" + type + "', function(imgData){"
                + "    var done = votationEvents_deviceShare(imgData, screenPoll.key, '" + link_url + "'); "
                + "    if(false !== done){"
                + "      Device.close('JAVA js_post_callback'); "
                + "    }"
                + "  });"
                //                + "}, 1);"
                + "";
        //setTimeout -> allow fonts load!

        if (keyId.contains("-")) {
            if ('-' != keyId.charAt(0)) {
                Log.i(logName, "ERROR: PUBLIC POLL IS FOR NOW DEPRECATED");
                return;

//                //public
//                String[] keyParts = keyId.split("-");
//                String key = keyParts[1];
//                countryUrl = "~" + keyParts[0] + "/";
//
//                url_request = path + "core/get.php";
//                params = "url=public/" + countryUrl + "/" + key;
            } else {
                path += "private/";
                url_request = path + keyId;
            }

            //CALL:
            String js_callback = "screenPoll.key = '" + keyId + "'; "
                    + " var shareDevice = new RequestPollByKeyCallback";

            String js_post_callback = "function(){"
                    + "var obj = shareDevice.poll.obj; " + defineVotes
                    + js_post_callback_default
                    + "}";
            Log.i(logName, "js_post_callback:" + js_post_callback);

            Requests.SimpleRequest simpleRequest = requests.new SimpleRequest();
            simpleRequest.nextLine = "\\n"; //add nextLine to js parse!
            simpleRequest.execute(url_request, params, js_callback, js_post_callback);

        } else if (keyId.contains("_")) {
            //TODO:
            //parseRequests.selectById(keyId);

            String[] keyParts = keyId.split("_");
            String table = "preguntas" + keyParts[0];
            String id = keyParts[1];

            String js_callback = "screenPoll.key = '" + keyId + "'; "
                    + " window.gameAndroid = new GamePoll('#pollsPage', " + id + ", 'gameAndroid', '" + keyParts[0] + "'); "
                    + " gameAndroid.requestCallback";

            String js_post_callback = "var obj = gameAndroid.obj; " + js_post_callback_default;

            new ParseRequests(ctx).new select().execute(table, "", id, js_callback, js_post_callback);
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        googleIndex.start();
    }

    @Override
    public void onStop() {
        webView.saveLocalStorage();
        googleIndex.stop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        interstitial.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onRestart() {
        super.onRestart();
        isActivityRestarting = true;
    }

    @Override
    protected void onPause() {
        Log.i(logName, "onPause");
        //custom.setBackgroundColor(0xFF000000);
        //webView.setBackgroundColor(0xFF000000);

        super.onPause();
        if (connectivityChangeReceiver != null) {
            try {
                unregisterReceiver(connectivityChangeReceiver);
            } catch (IllegalArgumentException e) {
                Log.i(logName, "cant unregister receiver cose not exists");
            }
        }
        //will
        isActivityRestarting = true;
    }

    @Override
    public void onResume() {
        Log.i(logName, "activity onResume()");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        super.onResume();

//        String url = getIntent().getDataString();
//        if (null != url && url.contains("/share")) {
//            Log.i(logName, "shareRun -> webView.setVisibility(View.GONE);");
//            webView.loadWebviewUrl(webView.lastUrl);
//            webView.setVisibility(View.GONE);
//            return;
//        }
        //connected to network detection
        IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(connectivityChangeReceiver, intentFilter);
        //webView.setNetworkAvailable(isNetworkAvailable());

        if (null != custom) {
            custom.setBackgroundColor(customBackgroundColor);
            return;
        }
        //webView.setBackgroundColor(webView.webviewBackgroundColor);

//        boolean firstTime = prefs.getBoolean("firstTime", true);
        boolean firstTime = prefs.getBoolean("firstTime", false);
        if (firstTime) {
            prefs.edit().putBoolean("firstTime", false).commit();
            //webView.loadWebviewUrl(webView.indexUrl + "#translucent");

            String bool = "";
            //TODO: prevent activate permission: very agressive now
//            if (needUsagePermission()) {
//                Log.i(logName, "NEEDS USAGE PERMISSION");
//                needPermission = true;
//                bool = "true";
//            }

            //webView.loadWebviewUrl(webView.indexUrl + "#firstTime");
            //webView.js("firstTime(" + bool + ")");
        } else if (digits.askingPhone) {
            //when goes back te get key activation 
            Log.i(logName, "askingPhone");
            //startFabric(); //this not work
            digits.digitsAuth(); //duplicates services?
            digits.askingPhone = false;

        } else if (loading) {
            //open existing votations            
            if ("".equals(webView.lastUrl)) {
                Log.i(logName, "#loading");
                webView.loadWebviewUrl(indexUrl + "#loading");
            } else {
                Log.i(logName, "loading lastUrl: " + webView.lastUrl);
                webView.loadWebviewUrl(webView.lastUrl);
            }

        } else if (!isActivityRestarting && !isSharing && !isPublicActivation) {
            //open new votations
            Log.i(logName, "!isActivityRestarting");
            webView.loadWebviewUrl(indexUrl);

        } else {
            Log.i(logName, "resume()");
            //go back or return to votation
            webView.js("hashManager.resume()");
        }
        //else nothing

        loading = false;
        isActivityRestarting = false;
        isPublicActivation = false;
        isSharing = false;
    }

    @Override
    public void onNewIntent(Intent intent
    ) {
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data
    ) {
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
                            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), webView.webviewInterface.imageUri);
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

                    webView.js("customStyles.newIconLoad('" + image + "')");
                }
                break;

            case MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS:
                Log.i(logName, "MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS done");
                if (utils.hasUsageStatsPermission()) {
                    startLastSocialApp();
                }
                //finish anyway ?
                Log.i(logName, "finish in MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS");
                finish();
                break;
        }

    }

    @Override
    public void onBackPressed() {
        String webUrl = webView.getUrl();
        Log.i(logName, "logUrl: " + webUrl);

        if (null != translucent) {
            Log.i(logName, "finish in onBackPressed");
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

    @Override
    public void onConfigurationChanged(Configuration newConfig
    ) {
        super.onConfigurationChanged(newConfig);
        // Checks whether a hardware keyboard is available
        if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
            webView.js("$(window).trigger('resize')");
        } else if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
            webView.js("$(window).trigger('resize')");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    sharing.shareImageJS();
                } else {
                    String txt = ctx.getResources().getString(R.string.readExternalStorageResponse);
                    webView.js("flash('" + txt + "')");
                }
            }
        }
    }
    // FUNCTIONS: //////////////////////////////////////////////////////////////////////////////////

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null;
        } catch (Exception e) {
            Log.e(logName, "isNetworkAvailable()", e);
        }
        return true;
    }

    private BroadcastReceiver connectivityChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            boolean online = isNetworkAvailable();
            if (null != webView) {
                webView.setNetworkAvailable(online);
            }
            if (null != custom) {
                custom.setNetworkAvailable(online);
            }
            Log.i(logName, "isNetworkAvailable() " + Boolean.toString(online));
        }
    };

    public void startLastSocialApp() {
        String lastTask = utils.getLastAppTask();
        if (null != lastTask) {
            Intent intent = getPackageManager().getLaunchIntentForPackage(lastTask);
            if (null != intent) {
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            }
        }
    }

    private boolean isPremium() {
        String packageName = getResources().getString(R.string.package_name);
        if (getPackageManager().checkSignatures(ctx.getPackageName(), packageName + ".pro") == PackageManager.SIGNATURE_MATCH) {
            Log.i(logName, "PREMIUM IS INSTALLED");
            return true;
        }
        return false;
    }

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
}

class PollData {

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
