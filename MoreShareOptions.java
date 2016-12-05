package at.clicktovote;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class MoreShareOptions extends Activity {

    Context ctx;
    private String logName = this.getClass().getName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //moveTaskToBack(true);
        ctx = getApplicationContext();

        String key = getIntent().getStringExtra("key");
        Log.i(logName, "MoreShareOptions key: " + key);

        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        Share share = new Share(ctx);
        sharingIntent = share.updateIntent(sharingIntent, key); //static updateIntent (imgSaved)
        startActivity(Intent.createChooser(sharingIntent, "Share via:"));

        //kill this activity
        finish();
    }

}
