package at.imagevote;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.webkit.WebView;

import com.digits.sdk.android.Digits;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.regex.Pattern;

import at.wouldyourather.R;

public class Requests {

    private Context ctx;
    private VoteImageActivity activity;
    private String logName = this.getClass().getName();

    public Requests(Context context) {
        ctx = context;
        activity = (VoteImageActivity) ctx;
    }

    public void saveDATA(String action, String data, String token, String key, String isPublic, String country, String callback) {
        String params = "";
//        params += "action=" + action;
        params += "&data=" + data;
        if (!key.isEmpty()) {
            params += "&key=" + key;
        }

        String userId = activity.prefs.getString("userId", null);
        Log.i(logName, "user android id: " + userId);

        Log.i(logName, "isPublic: " + isPublic);
        String pub = isPublic;
        if (null != pub && !"".equals(pub) && !"null".equals(pub) && !"undefined".equals(pub) && !"false".equals(pub)) {

            userId = activity.prefs.getString("publicId", null);

            String digitsKey = activity.prefs.getString("digitsKey", null);
            Log.i(logName, "digitsKey to send: " + digitsKey);
            if (null == digitsKey) {
                Log.i(logName, "EMPTY digitsKey!");
                //return "_1 from android app"; //need digitsKey error
                //return "_again";
                return;
            }

            Log.i(logName, "sending as public value. isPublic: " + isPublic);
            params += "&public=true";
            if (!"".equals(country)) {
                params += "&pollCountry=" + country;
            }

            params += "&digitsKey=" + digitsKey;
            String ISO = activity.users.getUserISO();
            params += "&ISO=" + ISO;
        }

        params += "&userId=" + userId;
        if ("create".equals(action)) {
            new SimpleRequest().execute("create.php", params, callback, "");
        } else if ("update".equals(action)) {
            new SimpleRequest().execute("add.php", params, callback, "");
        }
    }

    public class SimpleRequest extends AsyncTask<String, Void, String> {

        String logName = this.getClass().getName();
        WebviewLayout webView = null;

        private String pathname;
        private String params;
        private String callback = "";
        private String post_callback = "";
        public String nextLine = "";

        SimpleRequest() {
            //nothing
        }

        SimpleRequest(WebviewLayout view) {
            webView = view;
        }

        @Override
        protected String doInBackground(String... urls) {
            pathname = urls[0];
            params = urls[1];
            callback = urls[2];
            post_callback = urls[3];

            Pattern pattern = Pattern.compile("^(?:[a-z]+:)?//.*");
            boolean absolute = pattern.matcher(pathname).matches();

            String url = pathname;
            if (!absolute) {
                String url_core = ctx.getResources().getString(R.string.url_core);
                url = "http://" + url_core + "/" + pathname;
            }

            Log.i(logName, "SimpleRequest on: " + url);

            try {
                return postRequest(url, params, nextLine);
            } catch (Exception e) {
                Log.e(logName, "error", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String response) {
            Log.i(logName, "SimpleRequest.onPostExecute " + response);
            if (null != callback && !"".equals(callback)) {
                //Log.i(logName, "response: " + response);
                String js = callback + "('" + response.replace("\\", "\\\\").replace("'", "\\'") + "'); ";
                if (null != post_callback && !"".equals(post_callback)) {
                    js += "(" + post_callback + ")()";
                }

                if (null != webView) {
                    webView.js(js);
                } else {
                    activity.webView.js(js);
                }
            } else {
                Log.i(logName, "not callback on saveData");
            }
        }
    }

    public String postRequest(String url_string, String params) {
        return postRequest(url_string, params, "");
    }

    public String postRequest(String url_string, String params, String nextLine) {
        String result = "postRequest android error on: " + url_string + " with: " + params;

        try {
            /*HttpPost httppost = new HttpPost("http://" + url_core + "/update.php");
                httppost.setEntity(new UrlEncodedFormEntity(params));
                HttpResponse response = httpclient.execute(httppost);*/
            URL url = new URL(url_string);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setDoOutput(true);
            urlConnection.setInstanceFollowRedirects(false);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConnection.setRequestProperty("charset", "utf-8");
            urlConnection.setUseCaches(false);

            if (null != params && !"".equals(params)) {
                byte[] postData = params.getBytes(Charset.forName("UTF-8"));
                int postDataLength = postData.length;
                urlConnection.setRequestProperty("Content-Length", Integer.toString(postDataLength));
                urlConnection.getOutputStream().write(postData);
            }

            InputStream is = new BufferedInputStream(urlConnection.getInputStream());
            result = convertStreamToString(is, nextLine);
            urlConnection.disconnect();

        } catch (Exception e) {
            String logName = this.getClass().getName();
            Log.e(logName, "postRequest error", e);
        }

        return result;
    }

    private String convertStreamToString(InputStream is, String nextLine) throws Exception {
        Log.i(logName, "convertStreamToString() '" + nextLine + "'");
        //ISO-8859-1 shows good accents, ñ, etc..
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "ISO-8859-1"));
        StringBuilder sb = new StringBuilder();

        String line = null;
        boolean first = true;
        while ((line = reader.readLine()) != null) {
            if (first) {
                sb.append(line);
                first = false;
            } else {
                sb.append(nextLine + line);
            }
        }
        is.close();
        return sb.toString();
    }

}
