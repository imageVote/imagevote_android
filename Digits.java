package at.imagevote;

import android.content.Context;
import android.util.Log;

import com.digits.sdk.android.AuthCallback;
import com.digits.sdk.android.DigitsException;
import com.digits.sdk.android.DigitsOAuthSigning;
import com.digits.sdk.android.DigitsSession;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterAuthToken;
import com.twitter.sdk.android.core.TwitterCore;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Map;

import at.wouldyourather.R;
import io.fabric.sdk.android.Fabric;

public class Digits {

    private Context ctx;
    VoteImageActivity activity;

    private String logName = this.getClass().getName();
    public boolean askingPhone = false;

    public Digits(Context context) {
        ctx = context;
        activity = (VoteImageActivity) ctx;
    }

    public void digitsAuth() {
        //once
        askingPhone = true;

        //Twitter Digits:
        startFabric();

        com.digits.sdk.android.Digits.authenticate(new AuthCallback() { //new AuthCallback() returns number phone to Digits.authenticate() function
            @Override
            public void success(DigitsSession session, String phoneNumber) {
                Log.i(logName, "start digitsAuth()");

                askingPhone = false;
                activity.isPublicActivation = true;

                activity.webView.js("window.loadingPublicKey = true");

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
                            String appPath = ctx.getResources().getString(R.string.url);
                            URL url = new URL("http://" + appPath + "/phone/verify.php?nocache=" + (new Date()).getTime());

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
                                activity.webView.JSerror(str.substring(1));
                                return;
                            }

                            Log.i(logName, "Digits response = " + str + " !!!");

                            String[] keys = str.split("\\|");
                            String publicId = keys[0]; //if retrieve stringed id
                            String digitsKey = keys[1];
                            String phonePrefix = keys[2];
                            Log.i(logName, "phonePrefix = " + phonePrefix);

                            activity.prefs.edit()
                                    .putString("publicId", publicId)
                                    // TODO: digitsKey WILL BE MORE SECURE IF SAVED ENCRYPTED ON FILE TEXT WITH REST OF DATA
                                    .putString("digitsKey", md5("digitsKey=" + digitsKey))
                                    .putString("phonePrefix", phonePrefix)
                                    .commit();

                            activity.users.jsAddUser();
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

    private boolean fabricStarted = false;

    public void startFabric() {
        if (!fabricStarted) {
            fabricStarted = true;
            TwitterAuthConfig authConfig = new TwitterAuthConfig("K4G5F4rG76943qA1wYrmDwXZp", "bhgDztwlBzNCAWH7WaHbfi7FZJ3gdYUEgEn2LI5rExpWzN2Z5w");
            Fabric.with(ctx, new TwitterCore(authConfig), new com.digits.sdk.android.Digits());
        }
    }

    public void SaveDataOnLogin() {
        PollData pollData = activity.webView.webviewInterface.pollData;
        if (null == pollData) {
            Log.i(logName, "Poll Data is null");
            if (null != activity.callback) {
                activity.webView.js(activity.callback);
            }
            return;
        }

        Requests requests = new Requests(ctx);
        requests.new SaveData().execute(pollData.action, pollData.token, pollData.key, pollData.data, pollData.isPublic, pollData.country, pollData.callback);
        activity.webView.webviewInterface.pollData = null;
    }

    public final String md5(final String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
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

}
