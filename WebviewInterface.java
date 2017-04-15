package at.imagevote;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.util.Map;

import at.wouldyourather.R;

public class WebviewInterface {

    private VoteImageActivity activity;
    private Context ctx;
    private String logName = this.getClass().getName();
    private Requests requests;
    public PollData pollData = null;

    public WebviewInterface(VoteImageActivity act) {
        activity = act;
        ctx = (Context) act;

        requests = new Requests(ctx);
    }

    @JavascriptInterface
    public String isTranslucent() {
        return activity.translucent;
    }

    @JavascriptInterface
    public void newKey() {
        //get new key
        String[] profile = activity.users.getUserProfile();

        String url = "/update.php";
        String params = "action=newkey&id=" + profile[0];
        requests.new SimpleRequest().execute(url, params, "loadKey", null);
    }

    @JavascriptInterface
    public void firstTimeOk() {
        if (activity.needPermission) {
            activity.startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), activity.MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS);
            // finish() on startActivityForResult
        } else {
            activity.startLastSocialApp();
            Log.i(logName, "finish in firstTimeOk");
            activity.finish();
        }
    }

    @JavascriptInterface
    public void askPhone(String callB) {
        Log.i(logName, "askPhone() callback = " + callB);
        //if askPhone needs force
        activity.digits.startFabric();
        com.digits.sdk.android.Digits.getSessionManager().clearActiveSession();

        if (!"".equals(callB)) {
            activity.callback = callB;
        }
        activity.digits.digitsAuth();
    }

    @JavascriptInterface
    public void save(String action, String data, String token, String key, String isPublic, String country, String callback) {
        //prevent amateur hacks
        if (Build.FINGERPRINT.startsWith("generic")) {
            return;
        }

        Log.i(logName, "SAVE. key: " + key + ", publicKey: " + isPublic + ", action: " + action);

        pollData = new PollData(action, token, key, data, isPublic, country, callback);
        if ("true" == isPublic || "1" == isPublic) { //if is public
            String digitsKey = activity.prefs.getString("digitsKey", null);
            Log.i(logName, "stored digitsKey: " + digitsKey);

            // VALIDATE USER FIRST TIME
            if (null == digitsKey) {
                //new GetCaptchaToken().execute(); ////////////////////////
                Log.i(logName, "digitsAuth()");
                activity.digits.digitsAuth();
                return;
            }
        }

        //requests.new SaveData().execute(action, token, key, data, isPublic, country, callback);
        requests.saveDATA(action, data, token, key, isPublic, country, callback);
    }

    @JavascriptInterface
    public boolean share(String img, String key, String url) {
        if ("".equals(img)) {
            Log.i(logName, "EMPTY SHARED IMG");
            return false;
        }
        Log.i(logName, "url = " + url + " + " + key + " on share()");
        activity.isSharing = true;

        Share shareClass = new Share(ctx);
        return shareClass.shareImageJS(img, key, url);
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
                Toast.makeText(ctx, activity.getResources().getString(at.wouldyourather.R.string.questionGoogleServices), Toast.LENGTH_LONG).show();
            }
        }
    }

    public Uri imageUri;

    public void pickImage(Intent pickIntent) {
        //photo
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        imageUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(), "fname_"
                + String.valueOf(System.currentTimeMillis()) + ".jpg"));
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

        //choser
        Intent chooserIntent = Intent.createChooser(pickIntent, activity.getResources().getString(R.string.SelectImage));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takePhotoIntent});

        activity.startActivityForResult(chooserIntent, activity.PICK_IMAGE);
    }

    @JavascriptInterface
    public void error(final String text) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String appPath = activity.getResources().getString(R.string.url);
                String urlString = "http://" + appPath + "/error.php";

                /*ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
                    params.add(new BasicNameValuePair("error", text));*/
                String params = "";
                params += "error=" + text;

                requests.postRequest(urlString, params);
            }
        }).start();
    }

    @JavascriptInterface
    public void log(final String text) {
        Log.i("CONSOLE", text);
    }

    @JavascriptInterface
    public void close(String why) {
        Log.i(logName, "finish in close() JavascriptInterface: " + why);
        activity.finish();
    }

    @JavascriptInterface
    public void showStars() {
        new Stars(ctx, activity.edText);
        activity.webView.js("localStorage.setItem('stars_done', true)");
    }

    @JavascriptInterface
    public void simpleRequest(String url, String params, String callback) {
        requests.new SimpleRequest().execute(url, params, callback, null);
    }

    @JavascriptInterface
    public void saveLocalStorage(String json) {
        Map<String, String> map = new Gson().fromJson(
                json, new TypeToken<Map<String, String>>() {
                }.getType()
        );

        SharedPreferences.Editor edit = activity.prefs.edit();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            edit.putString(entry.getKey(), entry.getValue());
        }
        edit.commit();
    }

    @JavascriptInterface
    public void permissionsRedirection() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", ctx.getPackageName(), null);
        intent.setData(uri);
        activity.startActivityForResult(intent, activity.PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE_MANUAL);
    }

    @JavascriptInterface
    public void loadAd() {
        activity.interstitial.loadInterstitialAd();
    }

    @JavascriptInterface
    public void parseSelect(String table, String lastId, String id, String callback) {
        activity.parseRequests.new select().execute(table, lastId, id, callback);
    }

    @JavascriptInterface
    public void parseUpdate(String table, String id, String add, String sub, String idQ, String callback) {
        activity.parseRequests.new update().execute(table, id, add, sub, idQ, callback);
    }

    //
    public String handleGingerbreadStupidity = "javascript:;"
            + "var _ = '[Device]';"
            + "function handler() {"
            + "this.isTranslucent = function() {"
            + "    window.location = 'http://Device:isTranslucent';"
            + "};"
            + "this.newKey = function() {"
            + "    window.location = 'http://Device:newKey';"
            + "};"
            + "this.firstTimeOk = function() {"
            + "    window.location = 'http://Device:firstTimeOk';"
            + "};"
            + "this.loadDefault = function() {"
            + "    window.location = 'http://Device:loadDefault';"
            + "};"
            + "this.askPhone = function(callB) {"
            + "    window.location = 'http://Device:askPhone:' + callB;"
            + "};"
            + "this.save = function(data, key, lastKeyAsk, realKey, public, country, callback) {"
            + "    window.location = 'http://Device:save:' + encodeURIComponent(data + _ + key + _ + lastKeyAsk + _ + realKey + _ + public + _ + country + _ + callback);"
            + "};"
            + "this.share = function(img, key) {"
            + "    window.location = 'http://Device:share:' + encodeURIComponent(img + _ + key);"
            + "};"
            + "this.pickIconImage = function() {"
            + "    window.location = 'http://Device:pickIconImage';"
            + "};"
            + "this.error = function(text) {"
            + "    window.location = 'http://Device:error:' + text;"
            + "};"
            + "this.log = function(text) {"
            + "    window.location = 'http://Device:log:' + text;"
            + "};"
            + "this.close = function() {"
            + "    window.location = 'http://Device:close' + why;"
            + "};"
            + "this.showStars = function() {"
            + "    window.location = 'http://Device:showStars';"
            + "};"
            + "this.simpleRequest = function() {"
            + "    window.location = 'http://Device:simpleRequest:' + encodeURIComponent(url + _ + params + _ + callback);"
            + "};"
            + "this.saveLocalStorage = function() {"
            + "    window.location = 'http://Device:saveLocalStorage:' + json;"
            + "};"
            + "this.permissionsRedirection = function() {"
            + "    window.location = 'http://Device:permissionsRedirection';"
            + "};"
            + "this.loadAd = function() {"
            + "    window.location = 'http://Device:loadAd';"
            + "};"
            + "this.parseSelect = function() {"
            + "    window.location = 'http://Device:parseSelect:' + encodeURIComponent(table + _ + lastId + _ + id + _ + callback);"
            + "};"
            + "this.parseUpdate = function() {"
            + "    window.location = 'http://Device:parseUpdate:' + encodeURIComponent(table + _ + id + _ + add + _ + sub + _ + idQ + _ + callback);"
            + "};"
            + "}"
            + "var Device = new handler();";
}
