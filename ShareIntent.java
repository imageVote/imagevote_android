package at.imagevote;

import android.app.*;
import android.content.*;
import android.os.*;
import android.util.*;
import java.util.*;

import at.wouldyourather.R;

public class ShareIntent extends Activity {

    //android
    private static Context ctx;
    private static String logName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ctx = getApplicationContext();
        logName = this.getClass().getName();

        String url = getIntent().getDataString();
        Log.i(logName, "on create data = " + url);
        if (null != url) {
            //lastUrl = url; //dont get web url!
            start(url);
        }

    }

    @Override
    public void onResume() {
        Log.i(logName, "activity onResume()");
        super.onResume();

    }

    @Override
    public void onNewIntent(Intent intent) {
        Log.i(logName, "ON NEW INTENT");
        super.onNewIntent(intent);

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
        String packageName = ctx.getResources().getString(R.string.package_name);
        if (!url.toLowerCase().contains(packageName)) {
            Log.i(logName, "error: WRONG INTENT URL? " + url);
            return;
        }
    }

}
