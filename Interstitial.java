package at.imagevote;

import android.content.Context;
import android.util.Log;

import com.facebook.ads.Ad;

public class Interstitial {

    private com.facebook.ads.InterstitialAd interstitialAd_facebook;
    private com.google.android.gms.ads.InterstitialAd interstitialAd_admob;
    private Context ctx;
    private String logName = this.getClass().getName();

    public Interstitial(Context context) {
        ctx = context;
        double random = Math.random() * 2;

        // FACEBOOK: //
        com.facebook.ads.AdSettings.addTestDevice("59a6986df3fd266d667054b7965927ef"); //my Android:
        String facebook_placement_id = ctx.getResources().getString(at.wouldyourather.R.string.facebook_placement_id);
        if (random >= 1) {
            facebook_placement_id = ctx.getResources().getString(at.wouldyourather.R.string.facebook_placement_id2);
        }
        interstitialAd_facebook = new com.facebook.ads.InterstitialAd(ctx, facebook_placement_id);
        interstitialAd_facebook.setAdListener(new com.facebook.ads.InterstitialAdListener() {

            @Override
            public void onInterstitialDisplayed(Ad ad) {
                //
            }

            @Override
            public void onInterstitialDismissed(Ad ad) {
                requestNewInterstitial_facebook();
            }

            @Override
            public void onError(com.facebook.ads.Ad ad, com.facebook.ads.AdError adError) {
                Log.i(logName, "interstitialAd_facebook Error: " + adError.getErrorMessage());
                //if facebook not works try ADMOB
                requestNewInterstitial_admob();
            }

            @Override
            public void onAdLoaded(com.facebook.ads.Ad ad) {
                //
            }

            @Override
            public void onAdClicked(com.facebook.ads.Ad ad) {
                //
            }
        });
        requestNewInterstitial_facebook();

        // ADMOB: //
        VoteImageActivity act = (VoteImageActivity) ctx;
        interstitialAd_admob = new com.google.android.gms.ads.InterstitialAd(act);
        String banner_ad_unit_id = ctx.getResources().getString(at.wouldyourather.R.string.banner_ad_unit_id);
        if (random >= 1) {
            banner_ad_unit_id = ctx.getResources().getString(at.wouldyourather.R.string.banner_ad_unit_id2);
        }
        interstitialAd_admob.setAdUnitId(banner_ad_unit_id);
        interstitialAd_admob.setAdListener(new com.google.android.gms.ads.AdListener() {
            @Override
            public void onAdClosed() {
                //on ADMOB close, request for Facebook again
                requestNewInterstitial_facebook();
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                Log.i(logName, "interstitialAd_admob errorCode: " + errorCode);
            }

            @Override
            public void onAdLeftApplication() {
                //
            }

            @Override
            public void onAdOpened() {
                //
            }

            @Override
            public void onAdLoaded() {
                //
            }
        });
    }

    public void requestNewInterstitial_facebook() {
        Log.i(logName, "request Facebook Interstitial");
        interstitialAd_facebook.loadAd();
    }

    public void requestNewInterstitial_admob() {
        Log.i(logName, "request ADMOB Interstitial");
        com.google.android.gms.ads.AdRequest adRequest = new com.google.android.gms.ads.AdRequest.Builder()
                .addTestDevice(com.google.android.gms.ads.AdRequest.DEVICE_ID_EMULATOR)
                .build();
        interstitialAd_admob.loadAd(adRequest);
    }

    // PUBLIC METHODS:
    public void loadInterstitialAd() {
        Log.i(logName, "Interstitial.loadInterstitialAd()");
        ((VoteImageActivity) ctx).runOnUiThread(new Runnable() {
            @Override public void run() {

                if (interstitialAd_facebook.isAdLoaded() ) {
                   interstitialAd_facebook.show();
                }else if(interstitialAd_admob.isLoaded()){
                    interstitialAd_admob.show();
                }else{
                    Log.i(logName, "something wrong happens, no add was loaded, requesting for facebook for next!");
                    requestNewInterstitial_facebook();
                }

            }
        });
    }

    public void onDestroy(){
        if (interstitialAd_facebook != null) {
            interstitialAd_facebook.destroy();
        }
    }

}
