package at.clicktovote;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import android.content.*;

public class Share {

    public String logName = this.getClass().getName();
    private Context ctx;

    public Share(Context context) {
        ctx = context;
    }

    public void shareImageJS(String img, String key) {
        Log.i(logName, "key = " + key + " on shareImageJS()");

        //remove old files first
        for (File file : Environment.getExternalStorageDirectory().listFiles()) {
            if (file.isFile() && file.getName().startsWith("clicktovote_")) {
                file.delete();
            }
        }

        if (null != img && !"".equals(img)) {
            String filename = "clicktovote_" + key + ".jpeg";
            saveImage(img, filename);
        }

        //INTENT ///////////////////////////////////////////////////////////
        //main intent to send
        Intent sendIntent;

        //add 'more' option
        Intent intent = new Intent(ctx, MoreShareOptions.class);
        intent.putExtra("key", key);
        intent.setFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);

        //get list
        Intent plainIntent = new Intent(Intent.ACTION_SEND);
        plainIntent.setType("text/plain");

        PackageManager pm = ctx.getPackageManager();
        List<ResolveInfo> resInfo = pm.queryIntentActivities(plainIntent, 0);

        //if else
        Utils utils = new Utils(ctx);
        String lastTask = utils.getLastAppTask();
        Log.i(logName, "getLastAppTask() = " + lastTask);

        Log.i(logName, "createChooser..");
        List<LabeledIntent> intentList = new ArrayList<LabeledIntent>();

        //order by 'n' importance value
        for (int n = 0; n < 5; n++) {

            for (int i = 0; i < resInfo.size(); i++) {
                // Extract the label, append it, and repackage it in a LabeledIntent
                ResolveInfo ri = resInfo.get(i);
                String packageName = ri.activityInfo.packageName;
                //next if is redirection pachagename
                if (null != lastTask && packageName.equals(lastTask)) {
                    continue;
                }

                String name = ri.activityInfo.name;
                boolean isSocial = isSocialApp(packageName, name, n);

                if (isSocial) {
                    Log.i(logName, packageName + " added");
                    Intent addIntent = new Intent();
                    addIntent = updateIntent(addIntent, key);
                    addIntent.setComponent(new ComponentName(packageName, ri.activityInfo.name));
                    LabeledIntent labeled = new LabeledIntent(addIntent, packageName, ri.loadLabel(pm), ri.icon);
                    intentList.add(labeled);
                }
            }
        }

        //again for redirection
        if (null != lastTask) {
            for (int i = 0; i < resInfo.size(); i++) {
                // Extract the label, append it, and repackage it in a LabeledIntent
                ResolveInfo ri = resInfo.get(i);
                String packageName = ri.activityInfo.packageName;

                //if redirection app
                if (packageName.equals(lastTask)) {
                    Intent addIntent = new Intent();
                    addIntent = updateIntent(addIntent, key);
                    addIntent.setComponent(new ComponentName(packageName, ri.activityInfo.name));
                    LabeledIntent labeled = new LabeledIntent(addIntent, packageName, ri.loadLabel(pm), ri.icon);
                    //add first
                    intentList.add(0, labeled);
                    break;
                }
            }
        }

        //chooser
        String title = ctx.getResources().getString(R.string.shareWith);
        if (0 == intentList.size()) {
            title = ctx.getResources().getString(R.string.noSocialApps);
        } else if (null == lastTask) {
            title = ctx.getResources().getString(R.string.considerUsageAccess);
        }
        sendIntent = Intent.createChooser(intent, title);

        // convert intentList to array
        LabeledIntent[] extraIntents = intentList.toArray(new LabeledIntent[intentList.size()]);
        sendIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents);

        try {
            sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(sendIntent);
        } catch (android.content.ActivityNotFoundException ex) {
            Log.i(logName, "ERROR");
        }
    }

    private static String imgSaved;

    private void saveImage(String base64ImageData, String name) {

        //REMOVE OLD CONTENT RESOLVER IMAGES
        ContentResolver contentResolver = ctx.getContentResolver();

        Uri queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.Media._ID};
        String selection = MediaStore.Images.Media.DESCRIPTION + " LIKE ?";
        String[] selectionArgs = new String[]{"clicktovote_%"};

        Cursor c = contentResolver.query(queryUri, projection, selection, selectionArgs, null);
        while (c.moveToNext()) {
            long id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
            Uri deleteUri = ContentUris.withAppendedId(queryUri, id);
            contentResolver.delete(deleteUri, null, null);
        }

        //SAVE NEW SHARE IMAGE AND CONTENT-RESOLVER MediaStore
        String path = Environment.getExternalStorageDirectory() + "/" + name;
        File file = new File(path);

        if (base64ImageData == null) {
            Log.i(logName, "base64ImageData == null");
            return;
        }
        String data = base64ImageData.replace("data:image/png;base64,", "");

        try {
            byte[] decodedString = android.util.Base64.decode(data, 0);

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(decodedString);
            fos.close();

            imgSaved = android.provider.MediaStore.Images.Media.insertImage(
                    ctx.getContentResolver(), path, name, name);

        } catch (Exception e) {
            Log.e(logName, "base64ImageData = " + base64ImageData, e);
        }
    }

    //only can order adding first
    private boolean isSocialApp(String packageName, String name, int value) {
        //TODO: add and test more social apps

        return (packageName.contains("twitter") && !name.contains("DM")
                && (0 == value || -1 == value)) //twitter
                //
                || (packageName.contains("facebook")
                && (1 == value || -1 == value)) //facebook
                //
                || (packageName.contains("android.apps.plus")
                && (2 == value || -1 == value)) //google+
                //
                || (packageName.contains("whatsapp")
                && (3 == value || -1 == value)) //whattsapp
                //
                || (packageName.contains("telegram")
                && (4 == value || -1 == value)) //telegram
                //
                || (packageName.contains("android.talk")
                && (5 == value || -1 == value)); //hangouts
    }

    public Intent updateIntent(Intent sendIntent, String key) {
        sendIntent.setAction(Intent.ACTION_SEND);

        //image
        sendIntent.setType("image/*");//IMAGE
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);//??  

        //Uri uri;
        //try {
        //    uri = Uri.parse(imgSaved);
        //} catch (FileNotFoundException ex) {
        //    Logger.getLogger(logName).log(Level.SEVERE, null, ex);
        //    return null;
        //}
        Uri uri = Uri.parse(imgSaved);

        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);

        //text
        sendIntent.putExtra(Intent.EXTRA_TEXT, "click-to-vote.at/" + key);

        return sendIntent;
    }
}
