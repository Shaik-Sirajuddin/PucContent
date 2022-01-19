package com.puccontent.org.util

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.content.PackageManagerCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.puccontent.org.activities.ContentActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import androidx.core.content.PackageManagerCompat.LOG_TAG
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.*
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import java.util.*


class AppOpenManager() : Application.ActivityLifecycleCallbacks, LifecycleObserver {
    private var appOpenAd: AppOpenAd? = null
    private lateinit var loadCallback: AppOpenAd.AppOpenAdLoadCallback
    private var activity:Activity? = null
    private var loadTime: Long = 0

    constructor (activity: Activity) : this(){
        this.activity = activity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            this.activity?.registerActivityLifecycleCallbacks(this)
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(this);
    }

    fun fetchAd() {
        if (isAdAvailable) {
            return
        }
        loadCallback = object : AppOpenAd.AppOpenAdLoadCallback() {

            override fun onAdLoaded(ad: AppOpenAd) {
                appOpenAd = ad
                this@AppOpenManager.loadTime = Date().time
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError?) {
                 Log.d(LOG_TAG,loadAdError?.message.toString())
            }
        }
        val request: AdRequest = adRequest
        activity?.let{
            AppOpenAd.load(
                it, AD_UNIT_ID, request,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, loadCallback)
        }
    }
    private val adRequest: AdRequest
    get() = AdRequest.Builder().build()

    val isAdAvailable: Boolean
        get() = appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4);
    private var isShowingAd = false

    private fun showAdIfAvailable() {
        if (!isShowingAd && isAdAvailable) {
            val fullScreenContentCallback: FullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        appOpenAd = null
                        isShowingAd = false
                        fetchAd()
                    }
                    override fun onAdFailedToShowFullScreenContent(adError: AdError?) {}
                    override fun onAdShowedFullScreenContent() {
                        isShowingAd = true
                    }
                }
            appOpenAd?.fullScreenContentCallback = fullScreenContentCallback
            activity?.let{
                appOpenAd?.show(it)
            }
        } else {
            Log.d(LOG_TAG, "Can not show ad.")
            fetchAd()
        }
    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {
        this@AppOpenManager.activity = activity
    }
    override fun onActivityResumed(activity: Activity) {
        this@AppOpenManager.activity = activity
    }

    override fun onActivityPaused(p0: Activity) {}

    override fun onActivityStopped(p0: Activity) {}

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {}

    override fun onActivityDestroyed(aactivity: Activity) {
        this@AppOpenManager.activity = null
    }

    @OnLifecycleEvent(Event.ON_RESUME)
    fun onResume(){
        showAdIfAvailable()
    }
    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference: Long = Date().time - this.loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }
    companion object {
        private const val LOG_TAG = "AppOpenManager"
        var AD_UNIT_ID = "ca-app-pub-3940256099942544/3419835294"
    }
}