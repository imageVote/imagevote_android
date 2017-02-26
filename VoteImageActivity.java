package at.imagevote;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.net.*;
import android.os.*;
import android.provider.*;
import android.support.annotation.NonNull;
import android.util.*;
import android.webkit.*;
import at.wouldyourather.*;

import java.io.*;
import java.util.*;

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
    public String translucent = null;
    public String callback = null;

    public WebviewLayout webView;
    public WebView custom;
    public MessageLayout edText;

    public final int PICK_IMAGE = 1;
    public final int MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS = 2;
    public final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 3;
    public final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE_MANUAL = 4;

    private Utils utils;
    public Requests requests;
    public Digits digits;
    public Users users;
    private GoogleIndex googleIndex;

    public HashMap<String, String> dataKeys = new HashMap<>();

    public Share sharing;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //start transparent loading
        custom = new WebView(this);
        custom.setBackgroundColor(0x00FFFFFF);
//        custom.loadUrl("file:///android_asset/~commons/loading.html");        
//        setContentView(custom);

        String url = getIntent().getDataString();
        Log.i(logName, "on create data = " + url);

        //ctx = getApplicationContext();
        ctx = this;
        prefs = getSharedPreferences(getResources().getString(R.string.url), Activity.MODE_PRIVATE);

        utils = new Utils(ctx);
        requests = new Requests(ctx);
        digits = new Digits(ctx);
        users = new Users(ctx);
        googleIndex = new GoogleIndex(ctx);

        //setContentView after start() -> let show translucent mode
        if (null == url || (!url.contains("/share_") && !url.contains("/share/"))) {
            Log.i(logName, "setContentView(R.layout.main);");
            setContentView(R.layout.main);

            webView = (WebviewLayout) findViewById(R.id.webview);
            webView.start(ctx);
            edText = (MessageLayout) findViewById(R.id.send_message);
        }

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

        if (null != url) {
            //webView.lastUrl = url; //dont get web url!
            webView.startWebview(url);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        googleIndex.start();
    }

    @Override
    public void onStop() {
        super.onStop();
        webView.saveLocalStorage();
        googleIndex.stop();
    }

    @Override
    public void onRestart() {
        super.onRestart();
        isActivityRestarting = true;
    }

    @Override
    protected void onPause() {
        Log.i(logName, "onPause");
        custom.setBackgroundColor(0xFF000000);
        //webView.setBackgroundColor(0xFF000000);

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

        custom.setBackgroundColor(webView.customBackgroundColor);
        //webView.setBackgroundColor(webView.webviewBackgroundColor);

        //connected to network detection
        IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(connectivityChangeReceiver, intentFilter);
        //webView.setNetworkAvailable(isNetworkAvailable());

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
                webView.loadWebviewUrl(webView.indexUrl + "#loading");
            } else {
                Log.i(logName, "loading lastUrl: " + webView.lastUrl);
                webView.loadWebviewUrl(webView.lastUrl);
            }

        } else if (!isActivityRestarting && !isSharing && !isPublicActivation) {
            //open new votations
            Log.i(logName, "!isActivityRestarting");
            webView.loadWebviewUrl(webView.indexUrl);

        } else {
            Log.i(logName, "resume()");
            //go back or return to votation
            webView.js("resume()");
        }
        //else nothing

        loading = false;
        isActivityRestarting = false;
        isPublicActivation = false;
        isSharing = false;

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
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
            webView.startWebview(data);
        }
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
                finish();
                break;
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Checks whether a hardware keyboard is available
        if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
            webView.js("$(window).trigger('resize')");
        } else if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
            webView.js("$(window).trigger('resize')");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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
            webView.setNetworkAvailable(online);
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
