package at.imagevote;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.digits.sdk.android.Digits;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Date;

import at.wouldyourather.R;

public class Requests {

    private Context ctx;
    private VoteImageActivity activity;

    public Requests(Context context) {
        ctx = context;
        activity = (VoteImageActivity) ctx;
    }

    public class GetData extends AsyncTask<String, Void, String> {

        String logName = this.getClass().getName();
        private String override = null;

        private String keysPath = "";
        //other key servers?
        private final String alternativePath = null;

        //constructors
        public GetData() {
            //nothing
            start();
        }

        public GetData(String def) {
            override = def;
            start();
        }

        public void start() {
            keysPath = "http://" + ctx.getResources().getString(R.string.url_keys) + "/";
        }

        private String keyId;
        private String key;
        private String url;

        @Override
        protected String doInBackground(String... urls) {
            keyId = urls[0];

            //path
            String path = keysPath;
            if ('-' != keyId.charAt(0)) {
                //public
                key = keyId;
                String countryUrl = "";
                if (keyId.indexOf('-') > 0) {
                    String[] arr = keyId.split("-");
                    key = arr[1];

                    countryUrl = "~" + arr[0] + "/";
                }

                url = path + "core/get.php?url=public/" + countryUrl + "/" + key + "&";

            } else {
                path += "private/";
                url = path + keyId + "?";
            }

            //request
            String data = "";

            try {
                //HttpGet httpget = new HttpGet(url + "nocache=" + (new java.util.Date()).getTime());
                //HttpResponse response = httpclient.execute(httpget);

                ////fail?
                //int code = response.getStatusLine().getStatusCode();
                //if (200 != code) {
                //    String app_url = getResources().getString(R.string.url);
                //    if (path.contains("//" + app_url)) {
                //        JSerror(getResources().getString(R.string.keyNotFound));
                //        if (false == isNetworkAvailable()) {
                //            JSerror(getResources().getString(R.string.connectionFailed));
                //        }
                //    }
                //    return null;
                //}
                //HttpEntity ht = response.getEntity();
                //BufferedHttpEntity buf = new BufferedHttpEntity(ht);
                //InputStream is = buf.getContent();
                URL new_url = new URL(url + "nocache=" + (new Date()).getTime());
                HttpURLConnection urlConnection = (HttpURLConnection) new_url.openConnection();

                try {
                    InputStream is = new BufferedInputStream(urlConnection.getInputStream());

                    //ISO-8859-1 shows good accents, ñ, etc..
                    BufferedReader r = new BufferedReader(new InputStreamReader(is, "ISO-8859-1"));

                    StringBuilder total = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) {
                        total.append(line);
                    }
                    data = total.toString();
                    urlConnection.disconnect();

                } catch (Exception e) {
                    String app_url = ctx.getResources().getString(R.string.url);
                    if (path.contains("//" + app_url)) {
                        if (false == activity.isNetworkAvailable()) {
                            request_error(ctx.getResources().getString(R.string.connectionFailed));
                        } else {
                            js("errorParse('votationNotFound')");
                        }
                    }
                    urlConnection.disconnect();
                    return null;
                }

            } catch (Exception e) {
                Log.e(logName, "error", e);
                request_error(ctx.getResources().getString(R.string.wrongJsonPoll));
                return null;
            }

            return data;
        }

        @Override
        protected void onPostExecute(String data) {
            Log.i(logName, "url = " + url);
            Log.i(logName, "FILE DATA = " + data);

            if (null == data) {
                activity.translucent = "false";
                Log.i(logName, "error on key: " + keyId);
                return;

            } else if (data.charAt(0) == '_') {
                activity.translucent = "false";

                //try alternative path
                if (null != alternativePath && !keysPath.equals(alternativePath)) {
                    keysPath = alternativePath;
                    new GetData().execute(keyId);
                    //warn
                } else {
                    activity.webView.JSerror(data.substring(1));
                }
                return;
            }

            activity.dataKeys.put(keyId, data);
            if (null == override) {
                //tell js request the huge data
                activity.webView.js("dataIsReady('" + keyId + "')");

            } else {
                activity.webView.js(override);
            }

        }

        private void js(String error) {
            activity.webView.js(error);
        }
    };

    public class GetNewKey extends AsyncTask<String, Void, String> {

        String logName = this.getClass().getName();
        String token;

        @Override
        protected String doInBackground(String... urls) {
            String id = urls[0];
            token = urls[1];

            /*ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("action", "newkey"));
            params.add(new BasicNameValuePair("id", id));*/
            String params = "";
            params += "action=newkey";
            params += "&id=" + id;

            String url_core = ctx.getResources().getString(R.string.url_core);

            try {
                String result = getPostData("http://" + url_core + "/update.php", params);
                return result;

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
            activity.webView.js("loadKey(" + token + ",'" + key + "')");
        }

    }

    public class SaveData extends AsyncTask<String, Void, String> {

        String logName = this.getClass().getName();
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

            /*ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("action", action));
            params.add(new BasicNameValuePair("key", key));
            params.add(new BasicNameValuePair("value", value));*/
            String params = "";
            params += "action=" + action;
            params += "&value=" + value;
            if (!key.isEmpty()) {
                params += "&key=" + key;
            }

            String userId = activity.prefs.getString("userId", null);
            Log.i(logName, "user android id: " + userId);

            Log.i(logName, "isPublic: " + isPublic);
            if (null != isPublic && !"".equals(isPublic) && !"null".equals(isPublic) && !"undefined".equals(isPublic) && !"false".equals(isPublic)) {
                userId = activity.prefs.getString("publicId", null);

                String digitsKey = activity.prefs.getString("digitsKey", null);
                Log.i(logName, "digitsKey to send: " + digitsKey);
                if (null == digitsKey) {
                    Log.i(logName, "EMPTY digitsKey!");
                    //return "_1 from android app"; //need digitsKey error
                    return "_again";
                }

                Log.i(logName, "sending as public value. isPublic: " + isPublic);
                //params.add(new BasicNameValuePair("public", "true"));
                params += "&public=true";
                if (!"".equals(country)) {
                    //params.add(new BasicNameValuePair("pollCountry", country));
                    params += "&pollCountry=" + country;
                }

                //params.add(new BasicNameValuePair("digitsKey", digitsKey));
                params += "&digitsKey=" + digitsKey;
                String ISO = activity.users.getUserISO();
                //params.add(new BasicNameValuePair("ISO", ISO));
                params += "&ISO=" + ISO;

            }

            //params.add(new BasicNameValuePair("id", userId));
            params += "&id=" + userId;

            try {
                String url_core = ctx.getResources().getString(R.string.url_core);
                String result = getPostData("http://" + url_core + "/update.php", params);
                Log.i(logName, "HTTP Entiry: " + result);
                return result;

            } catch (Exception e) {
                Log.e(logName, "error", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String key) {
            if (null == key) {
                activity.webView.JSerror("cant connect");
                return;
            }
            if ("".equals(key)) {
                activity.webView.JSerror(ctx.getResources().getString(R.string.emptyKey));
                return;
            }
            if ('_' == key.charAt(0)) { //error
                if ('1' == key.charAt(1)) {
                    Log.i(logName, "error response: " + key);
                    //clear data
                    activity.digits.startFabric();
                    Digits.getSessionManager().clearActiveSession();
                    //start
                    activity.digits.digitsAuth();

                } else if ("_again".equals(key)) {
                    activity.digits.digitsAuth();

                } else if ('3' == key.charAt(1)) { //key already exists, reset key
                    //new SaveData().execute(action, token, null, value, isPublic, country, callback);
                    //this can be caused trying to create polls without read external storge permission
                    activity.webView.js("screenPoll.key = null; $('.loading').remove();");
                    Log.i(logName, "error '3': " + key);

                } else {
                    Log.i(logName, "error again: " + key);
                    activity.webView.JSerror(key.substring(1));
                }
                return;
            }
            Log.i(logName, "RES: " + key);

            //load key like if public poll save without previous key!
            // - Identify key return when callback send:
            activity.webView.js("loadKey('" + token + "','" + key + "');");
            if (null != callback && !"".equals(callback)) {
                Log.i(logName, "callback = " + callback);
                activity.webView.js("(" + callback + ")();");
            } else {
                Log.i(logName, "not callback on saveData");
            }
        }

    }

    public class SimpleRequest extends AsyncTask<String, Void, String> {

        String logName = this.getClass().getName();
        private String core_file;
        private String callback = null;

        @Override
        protected String doInBackground(String... urls) {
            core_file = urls[0];
            callback = urls[1];

            try {
                String url_core = ctx.getResources().getString(R.string.url_core);
                String url = "http://" + url_core + "/" + core_file;
                Log.i(logName, "SimpleRequest on: " + url);
                return getGetData(url);
            } catch (Exception e) {
                Log.e(logName, "error", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String response) {
            if (null != callback && !"".equals(callback)) {
                //Log.i(logName, "response: " + response);
                activity.webView.js(callback + "('" + response.replace("\\", "\\\\") + "')");
            } else {
                Log.i(logName, "not callback on saveData");
            }
        }
    }

    //lib
    public String getGetData(String url_string) {
        String result = "getPostData java error";

        try {
            URL url = new URL(url_string);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setDoOutput(true);
            urlConnection.setInstanceFollowRedirects(false);
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConnection.setRequestProperty("charset", "utf-8");
            urlConnection.setUseCaches(false);

            InputStream is = new BufferedInputStream(urlConnection.getInputStream());
            result = convertStreamToString(is);
            urlConnection.disconnect();

        } catch (Exception e) {
            String logName = this.getClass().getName();
            Log.e(logName, "error");
        }

        return result;
    }

    public String getPostData(String url_string, String params) {
        String result = "getPostData java error";

        try {
            byte[] postData = params.getBytes(Charset.forName("UTF-8"));
            int postDataLength = postData.length;

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
            urlConnection.setRequestProperty("Content-Length", Integer.toString(postDataLength));
            urlConnection.setUseCaches(false);

            urlConnection.getOutputStream().write(postData);
            InputStream is = new BufferedInputStream(urlConnection.getInputStream());
            result = convertStreamToString(is);
            urlConnection.disconnect();

        } catch (Exception e) {
            String logName = this.getClass().getName();
            Log.e(logName, "error");
        }

        return result;
    }

    private String convertStreamToString(InputStream is) throws Exception {
        //ISO-8859-1 shows good accents, ñ, etc..
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "ISO-8859-1"));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        is.close();
        return sb.toString();
    }

    private void request_error(String err) {
        activity.webView.JSerror(err);
    }
}
