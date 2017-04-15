package at.imagevote;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import at.wouldyourather.R;
import com.parse.Parse;
import com.parse.ParseObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

import static java.lang.Integer.parseInt;

public class ParseRequests {

    private Context ctx;
    private VoteImageActivity activity;

    public ParseRequests(Context context) {
        ctx = context;
        activity = (VoteImageActivity) ctx;
    }

    public class select extends AsyncTask<String, Void, String> {

        String logName = this.getClass().getName();
        WebviewLayout webView = null;

        private String table;
        private String lastId;
        private String id = "";
        private String callback = "";

        @Override
        protected String doInBackground(String... urls) {
            table = urls[0];
            lastId = urls[1];
            id = urls[2];
            callback = urls[3];

            String query = "{\"approved\":1,\"idQ\":{\"$gte\":" + lastId + "}}" + "&order=idQ";
            if (!id.isEmpty()) {
                query = "{\"idQ\":" + id + "}";
            }

            URL url = null;
            try {
                url = new URL("https://api.parse.buddy.com/parse/classes/" + table + "?where=" + query);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                String parseId = ctx.getResources().getString(R.string.parse_id);
                urlConnection.setRequestProperty("X-Parse-Application-Id", parseId);
                //connection.setRequestProperty("X-Parse-Client-Version", "php1.2.1");
                //connection.setRequestProperty("Expect", "");
                urlConnection.setUseCaches(false);

                InputStream is = new BufferedInputStream(urlConnection.getInputStream());
                String result = convertStreamToString(is);
                urlConnection.disconnect();

                return result;

            } catch (Exception e) {
                Log.e(logName, "ERROR " + query, e);
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String response) {
            if (null != callback && !"".equals(callback)) {
                //Log.i(logName, "response: " + response);
                String js = callback + "('" + response.replace("\\", "\\\\").replace("'", "\\'") + "'); ";
                activity.webView.js(js);

            } else {
                Log.i(logName, "not callback on saveData");
            }
        }
    }

    public class update extends AsyncTask<String, Void, String> {

        String logName = this.getClass().getName();
        WebviewLayout webView = null;

        private String table;
        private String id;
        private int add;
        private int sub;
        private boolean is_sub = false;
        private String idQ;
        private String callback = "";

        @Override
        protected String doInBackground(String... urls) {
            table = urls[0];
            id = urls[1];
            add = parseInt(urls[2]);
            if (!urls[3].isEmpty()) {
                is_sub = true;
                sub = parseInt(urls[3]);
            }
            idQ = urls[4];
            callback = urls[5];

            String prefix_add = "";
            switch (add) {
                case 0:
                    prefix_add = "first";
                    break;
                case 1:
                    prefix_add = "second";
                    break;
            }

            String prefix_sub = "";
            switch (sub) {
                case 0:
                    prefix_sub = "first";
                    break;
                case 1:
                    prefix_sub = "second";
                    break;
            }

            String post_values = "\"" + prefix_add + "_nvotes\":{\"__op\":\"Increment\",\"amount\":1}";
            if (is_sub) {
                post_values += ",\"" + prefix_sub + "_nvotes\":{\"__op\":\"Increment\",\"amount\":-1}";
            }

            String post = "{" + post_values + "}";
            String postLength = "" + post.length();

            try {
                URL url = new URL("https://api.parse.buddy.com/parse/classes/" + table + "/" + id);

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("PUT");

                String parseId = ctx.getResources().getString(R.string.parse_id);
                urlConnection.setRequestProperty("X-Parse-Application-Id", parseId);
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty("Content-Length", postLength);
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setUseCaches(false);

                byte[] postData = post.getBytes(Charset.forName("UTF-8"));
                int postDataLength = postData.length;
                urlConnection.setRequestProperty("Content-Length", Integer.toString(postDataLength));
                urlConnection.getOutputStream().write(postData);

                InputStream is = new BufferedInputStream(urlConnection.getInputStream());
                String result = convertStreamToString(is);
                urlConnection.disconnect();

                return result;

            } catch (Exception e) {
                Log.e(logName, "error width " + post, e);
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String response) {
            if (null != callback && !"".equals(callback)) {

                try {
                    JSONObject obj = new JSONObject(response);
                    obj.put("idQ", idQ);
                    obj.put("add", add);
                    response = obj.toString(4);

                    //Log.i(logName, "response: " + response);
                    String js = callback + "('" + response.replace("\\", "\\\\").replace("'", "\\'") + "'); ";
                    activity.webView.js(js);

                } catch (JSONException e) {
                    Log.e(logName, "ERROR " + response, e);
                    e.printStackTrace();
                }

            } else {
                Log.i(logName, "not callback on saveData");
            }
        }
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

}
