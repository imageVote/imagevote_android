package at.imagevote;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Users {

    private VoteImageActivity activity;
    private String logName = this.getClass().getName();
    private SharedPreferences prefs;

    public Users(Context ctx){
        activity = (VoteImageActivity) ctx;
        prefs = activity.prefs;
    }

    public void jsAddUser() {
        //add me
        String[] profile = getUserProfile();//userId, ISOS
        activity.webView.js("addUser('" + profile[0] + "', '" + profile[1] + "')");
    }

    public String[] getUserProfile() {
        String userId = prefs.getString("userId", null); //null for check exists
        String ISO = getUserISO();

        //stored
        if (null != userId) {
            return new String[]{userId, ISO};
        }

        Log.i(logName, "SEARCH USER PROFILE FIRST TIME");
        ContentResolver contentResolver = activity.getContentResolver();
        userId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID);

        prefs.edit()
                .putString("userId", userId)
                .commit();

        return new String[]{userId, ISO};
    }

    public String getUserISO() {
        String prefix = activity.prefs.getString("phonePrefix", null);
        if (null == prefix) {
            Log.i(logName, "EMPTY phonePrefix pref");
            return "";
        }

        String ISO = "";
        String json = readJSON("phone/phoneCodes.json");
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

        String json = readJSON("/phone/orgs.json");
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

    public String readJSON(String name) {
        String json = null;
        try {
            InputStream is = activity.getAssets().open(name);
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
}
