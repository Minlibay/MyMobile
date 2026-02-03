package com.volovod.alta.ads

import android.app.Activity
import android.util.Log
import com.appodeal.ads.Appodeal
import com.appodeal.ads.initializing.ApdInitializationCallback
import com.appodeal.ads.initializing.ApdInitializationError

object AppodealManager {
    private var initialized = false

    fun initializeIfNeeded(
        activity: Activity,
        appKey: String,
        banner: Boolean,
        interstitial: Boolean,
        rewarded: Boolean,
    ) {
        if (initialized) return
        if (appKey.isBlank()) {
            Log.e("AppodealManager", "Appodeal appKey is blank")
            return
        }

        var adTypes = 0
        if (banner) adTypes = adTypes or Appodeal.BANNER
        if (interstitial) adTypes = adTypes or Appodeal.INTERSTITIAL
        if (rewarded) adTypes = adTypes or Appodeal.REWARDED_VIDEO

        Appodeal.initialize(
            activity,
            appKey,
            adTypes,
            object : ApdInitializationCallback {
                override fun onInitializationFinished(errors: List<ApdInitializationError>?) {
                    initialized = errors.isNullOrEmpty()
                    Log.d("AppodealManager", "Init finished. errors=$errors")
                }
            },
        )
    }

    fun showInterstitial(activity: Activity) {
        if (Appodeal.isLoaded(Appodeal.INTERSTITIAL)) {
            Appodeal.show(activity, Appodeal.INTERSTITIAL)
        } else {
            Appodeal.cache(activity, Appodeal.INTERSTITIAL)
        }
    }

    fun showRewarded(activity: Activity) {
        if (Appodeal.isLoaded(Appodeal.REWARDED_VIDEO)) {
            Appodeal.show(activity, Appodeal.REWARDED_VIDEO)
        } else {
            Appodeal.cache(activity, Appodeal.REWARDED_VIDEO)
        }
    }
}
