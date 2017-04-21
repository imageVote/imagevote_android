package at.imagevote;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import android.content.*;
import at.wouldyourather.*;

public class Share {

    public String logName = this.getClass().getName();
    private Context ctx;
    private VoteImageActivity activity;
    private String sharingImg = null;
    private String sharingKey = null;
    private String sharingUrl = "";
    private boolean fromIntent = false;

    public Share(Context context) {
        ctx = context;
        activity = (VoteImageActivity) ctx;
    }

    //from Intent case constructor (not cast main activity)
    public Share(Context context, String notFromMainContext) {
        ctx = context;
        fromIntent = true;
    }

    public boolean shareImageJS() {
        if (null == sharingImg || null == sharingKey) {
            Log.i(logName, "ERROR: STORED SHARING DATA LOST!?");
            return false;
        }
        return shareImageJS(sharingImg, sharingKey, sharingUrl);
    }

    public boolean shareImageJS(String img, String key, String url_path) {
        String url;
        if (url_path.isEmpty()) {
            url_path = ctx.getResources().getString(R.string.url) + "/";
        }
        Log.i(logName, "url = " + url_path + " + " + key + " on shareImageJS()");
        url = url_path.split("/")[0] + "/" + key; //on sharing from web gamePoll needs add slash!

        //remove old files first
        File[] list = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).listFiles();
        if (null != list) {
            for (File file : list) {
                if (file.isFile() && file.getName().startsWith("imagevote_")) {
                    file.delete();
                }
            }
        }

        if (null != img && !"".equals(img) && !fromIntent) {
            if (false == requestReadExternalStoragePermissions()) {
                activity.sharing = this;
                this.sharingImg = img;
                this.sharingKey = key;
                this.sharingUrl = url;
                return false;
            }
            saveImage(img);
        }

        //INTENT ///////////////////////////////////////////////////////////
        //main intent to send
        Intent sendIntent;

        //add 'more' option
        Intent intent = new Intent(ctx, MoreShareOptions.class);
        intent.putExtra("key", url);
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

        Log.i(logName, "intent image path: " + imgSaved);
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
                    addIntent = updateIntent(addIntent, url);
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
                    addIntent = updateIntent(addIntent, url);
                    if(null == addIntent){
                        return false;
                    }
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

        return true;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean requestReadExternalStoragePermissions() {
        if (activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if (activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // "Never ask again" case
                String txt = ctx.getResources().getString(R.string.readExternalStorageResponse);
                String sub = ctx.getResources().getString(R.string.readExternalStorageResponse2);
                activity.webView.js("modalBox('" + txt + "', '" + sub + "', function(){"
                        + "  Device.permissionsRedirection(); "
                        + "  Device.close('modalBox accept');"
                        + "}, function(){"
                        + "  Device.close('modalBox cancel');"
                        + "})");
                //activity.webView.visible();
                activity.webView.js("loaded()");

            }
//            else {
            activity.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    activity.PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
//            }

            return false;
        }
        return true;
    }

    private static String imgSaved;

    private void saveImage(String base64ImageData) {

        //REMOVE OLD CONTENT RESOLVER IMAGES
        ContentResolver contentResolver = ctx.getContentResolver();

        Uri queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.Media._ID};
        String selection = MediaStore.Images.Media.DESCRIPTION + " LIKE ?";
        String[] selectionArgs = new String[]{"imageVote.jpeg"};

        //http://stackoverflow.com/questions/10716642/android-deleting-an-image
        Cursor c = contentResolver.query(queryUri, projection, selection, selectionArgs, null);
        while (c.moveToNext()) {
            long id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
            Uri deleteUri = ContentUris.withAppendedId(queryUri, id);
            contentResolver.delete(deleteUri, null, null);
        }

        //SAVE NEW SHARE IMAGE AND CONTENT-RESOLVER MediaStore
        String image_name = "imageVote.jpeg";
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + image_name;
        File file = new File(path);

        if (base64ImageData == null) {
            Log.i(logName, "base64ImageData == null");
            return;
        }
//        String data = base64ImageData.replace("data:image/png;base64,", "");
        String data = base64ImageData;

        try {
            byte[] decodedString = android.util.Base64.decode(data, 0);

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(decodedString);
            fos.close();

            imgSaved = android.provider.MediaStore.Images.Media.insertImage(
                    ctx.getContentResolver(), path, image_name, null);

        } catch (Exception e) {
            Log.e(logName, "ERROR IN base64ImageData = " + base64ImageData, e);
        }
    }

    //only can order adding first
    private boolean isSocialApp(String packageName, String name, int value) {
        //TODO: add and test more social apps
        //Log.i(logName, packageName);

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
                && (3 == value || -1 == value)) //whatsapp
                //
                || (packageName.contains("telegram")
                && (4 == value || -1 == value)) //telegram
                //
                || (packageName.contains("android.talk")
                && (5 == value || -1 == value)); //hangouts
    }

    public Intent updateIntent(Intent sendIntent, String url) {
        sendIntent.setAction(Intent.ACTION_SEND);

        //image
        sendIntent.setType("image/*");//IMAGE
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);//??  

        //Uri uri;
        Uri uri;
        try {
            uri = Uri.parse(imgSaved);
        } catch (Exception e) {
            Log.e(logName, "image not found ERROR:", e);
            activity.webView.js("flash(transl('e_imageNotFound'))");
            return null;
        }

        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);

        //text
//        String url = ctx.getResources().getString(R.string.url);
//        sendIntent.putExtra(Intent.EXTRA_TEXT, url + "/" + key);
        sendIntent.putExtra(Intent.EXTRA_TEXT, url);

        return sendIntent;
    }
}
