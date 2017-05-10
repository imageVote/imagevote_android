//package at.imagevote;
//
//import android.app.Activity;
//import android.content.Context;
//import android.content.SharedPreferences;
//import android.util.Log;
//import android.view.View;
//
//import com.afollestad.materialdialogs.MaterialDialog;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import angtrim.com.fivestarslibrary.FiveStarsDialog;
//
//import at.wouldyourather.R;
//
//
//public class Stars {
//
//    private final String TAG = this.getClass().getName();
//
//    private Context ctx;
//    private String logName = this.getClass().getName();
//    private MessageLayout edText;
//    private String appName = "";
//    //private String supportEmail = "info@email.com";
//
//    public Stars(Context context, MessageLayout editText) {
//        ctx = context;
//        edText = editText;
//        appName = ctx.getResources().getString(R.string.free_name);
//
//        start();
//    }
//
//    public void start(){
//        Log.i(logName, "Stars.start()");
//        //4 DEBUG:
//        this.clear();
//
//        new FiveStarsDialog.Builder(ctx)
//                .withAppName(appName)
//                //.withSupportEmail(supportEmail)
//                .withUpperBound(4) // Market opened if a rating >= 5 is selected
//                .withReviewListener(new FiveStarsDialog.ReviewListener() {
//                    @Override
//                    public void onReview(int stars) {
//                        Log.i(TAG, "User rated " + stars + " stars");
//                    }
//                }) // Used to listen for reviews (if you want to track them )
//                .withNegativeReviewListener(negativeReviewListener)
//                .showAfter(1);
//    }
//
//    FiveStarsDialog.NegativeReviewListener negativeReviewListener = new FiveStarsDialog.NegativeReviewListener() {
//        @Override
//        public void onNegativeReview(int stars) {
//
//            // Two choices: Send message or send email
//            final String message = ctx.getResources().getString(R.string.rate_message);
//            //final String mail = ctx.getResources().getString(R.string.rate_mail);
//
//            List<String> options = new ArrayList<>();
//            options.add(message);
//            //options.add(mail);
//
//            new MaterialDialog.Builder(ctx)
//                    .title(ctx.getResources().getString(R.string.rate_leave_feedback))
//                    .items(options)
//                    .positiveText(android.R.string.cancel)
//                    .itemsCallback(new MaterialDialog.ListCallback() {
//                        @Override
//                        public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
//                            //if (message.equals(text)) {
//                            //  Toast.makeText(ctx, "Send message not implemented!", Toast.LENGTH_SHORT).show();
//                            //} else if (mail.equals(text)) {
//                            //    sendMail();
//                            //}
//                            sendMessage();
//                        }
//                    })
//                    .show();
//        }
//    };
//
//    /**
//     * Used to avoid clearing app data to test the dialog again
//     */
//    public void clear() {
//        SharedPreferences shared = ((VoteImageActivity) ctx).prefs;
//        SharedPreferences.Editor editor = shared.edit();
//        editor.putBoolean("pref_rating_disabled", false);
//        editor.apply();
//    }
//
//    /*private void sendMail() {
//        Intent emailIntent = new Intent(Intent.ACTION_SEND);
//        emailIntent.setType("text/email");
//        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{supportEmail});
//        emailIntent.putExtra(Intent.EXTRA_SUBJECT, appName + " feedback");
//        emailIntent.putExtra(Intent.EXTRA_TEXT, "");
//        try {
//            ctx.startActivity(Intent.createChooser(emailIntent, ctx.getResources().getString(R.string.rate_mail)));
//        } catch (ActivityNotFoundException e) {
//            //TODO: Handle case where no email app is available
//            Toast.makeText(ctx, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
//        }
//    }*/
//
//    //PARSE SERVER RATE MESSAGE:
//    private void sendMessage() {
//        ((Activity) ctx).runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                edText.showSoftInput();
//            }
//        });
//    }
//
//}
