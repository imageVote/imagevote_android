package at.imagevote;

import android.content.Context;
import android.util.Log;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.InterstitialAdListener;

public class Interstitial {

    private com.facebook.ads.InterstitialAd interstitialAd_facebook;
    private com.google.android.gms.ads.InterstitialAd interstitialAd_admob;
    private Context ctx;
    private String logName = this.getClass().getName();
    private String facebook_placement_id;

    public Interstitial(Context context) {
        ctx = context;
        double random = Math.random();

        try {
            // FACEBOOK: //
            com.facebook.ads.AdSettings.addTestDevice("59a6986df3fd266d667054b7965927ef"); //my Android:

//        facebook_placement_id = ctx.getResources().getString(at.wouldyourather.R.string.facebook_placement_id);
//        if (random > 0.5) {
            facebook_placement_id = ctx.getResources().getString(at.wouldyourather.R.string.facebook_placement_id2);
//        }
            interstitialAd_facebook = new com.facebook.ads.InterstitialAd(ctx, facebook_placement_id);

            InterstitialAdListener interstitialAdListener = new InterstitialAdListener() {

                @Override
                public void onInterstitialDisplayed(Ad ad) {
                    //
                }

                @Override
                public void onInterstitialDismissed(Ad ad) {
                    requestNewInterstitial_facebook();
                }

                @Override
                public void onError(Ad ad, AdError adError) {
                    Log.i(logName, "interstitialAd_facebook Error: " + adError.getErrorMessage());
                    //if facebook not works try ADMOB
                    requestNewInterstitial_admob();
                }

                @Override
                public void onAdLoaded(Ad ad) {
                    //
                }

                @Override
                public void onAdClicked(Ad ad) {
                    //
                }
            };
            interstitialAd_facebook.setAdListener(interstitialAdListener);

            requestNewInterstitial_facebook();

            // ADMOB: //
            VoteImageActivity act = (VoteImageActivity) ctx;
            interstitialAd_admob = new com.google.android.gms.ads.InterstitialAd(act);
            String banner_ad_unit_id = ctx.getResources().getString(at.wouldyourather.R.string.banner_ad_unit_id);
            if (random > 0.5) {
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

        } catch (Exception e) {
            Log.e(logName, "Interstitial error", e);
        }
    }

    public void requestNewInterstitial_facebook() {
        Log.i(logName, "request Facebook Interstitial");
        try {
            interstitialAd_facebook.loadAd();
        } catch (Exception e) {
            Log.e(logName, "requestNewInterstitial_facebook error", e);
        }
    }

    public void requestNewInterstitial_admob() {
        Log.i(logName, "request ADMOB Interstitial");
        try {
            com.google.android.gms.ads.AdRequest adRequest = new com.google.android.gms.ads.AdRequest.Builder()
                    //                .addTestDevice(com.google.android.gms.ads.AdRequest.DEVICE_ID_EMULATOR)
                    .build();
            interstitialAd_admob.loadAd(adRequest);

            interstitialAd_facebook.loadAd();
        } catch (Exception e) {
            Log.e(logName, "requestNewInterstitial_admob error", e);
        }
    }

    // PUBLIC METHODS:
    public void loadInterstitialAd() {
        Log.i(logName, "Interstitial.loadInterstitialAd()");
        ((VoteImageActivity) ctx).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (interstitialAd_facebook.isAdLoaded()) {
                        interstitialAd_facebook.show();
                    } else if (interstitialAd_admob.isLoaded()) {
                        interstitialAd_admob.show();
                    } else {
                        Log.i(logName, "something wrong happens, no add was loaded, requesting for facebook for next!");
                        requestNewInterstitial_facebook();
                    }
                } catch (Exception e) {
                    Log.e(logName, "loadInterstitialAd error", e);
                }
            }
        });
    }

    public void onDestroy() {
        if (null != interstitialAd_facebook) {
            try {
                interstitialAd_facebook.destroy();
            } catch (Exception e) {
                Log.e(logName, "onDestroy error", e);
            }
        }
    }

}
