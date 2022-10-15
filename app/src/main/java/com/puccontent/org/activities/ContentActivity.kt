package com.puccontent.org.activities

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.fragment.NavHostFragment
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.RequestConfiguration
import com.puccontent.org.R
import com.puccontent.org.ads.AppOpenAdManager
import com.puccontent.org.databinding.ActivitySubjectsBinding
import com.puccontent.org.storage.OfflineStorage
import java.util.*

class ContentActivity : AppCompatActivity(), LifecycleObserver {

    private lateinit var binding: ActivitySubjectsBinding
    private lateinit var appOpenAdManager: AppOpenAdManager
    private lateinit var offlineStorage: OfflineStorage

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivitySubjectsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            val navHostFragment =
                supportFragmentManager.findFragmentById(R.id.fragmentContainer) as NavHostFragment
            val navController = navHostFragment.navController
            val recent = intent.getBooleanExtra("recent", false)
            if (recent) {
                val year = intent.getIntExtra("year", 1)
                val sem = intent.getIntExtra("sem", 1)
                val subject = intent.getStringExtra("subject") ?: ""
                val chapter = intent.getStringExtra("chapter") ?: ""
                val bundle =
                    bundleOf(
                        "year" to year,
                        "sem" to sem,
                        "subject" to subject,
                        "chapter" to chapter
                    )
                navController
                    .navigate(R.id.action_subjectsScreen_to_filesScreen, bundle)
            }

            initViews()
            initAds()


        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            Log.e("content", e.message.toString())
        }
    }

    private fun initViews(){
        offlineStorage = OfflineStorage(this)
        appOpenAdManager = AppOpenAdManager(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }
    private fun initAds(){
        if(!offlineStorage.adsEnabled)return
        val adView = AdView(this)
        adView.setAdSize(AdSize.BANNER)
        adView.adUnitId = offlineStorage.bannerAdId
        Log.d("Ads",offlineStorage.bannerAdId + "hjh")
        binding.adContainer.addView(adView)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

    }
    /** LifecycleObserver method that shows the app open ad when the app moves to foreground. */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        if(!offlineStorage.adsEnabled)return
        // Show the ad (if available) when the app moves to foreground.
        showAdIfAvailable(this)
    }

    /** Show the ad if one isn't already showing. */
    private fun showAdIfAvailable(activity: Activity) {
        appOpenAdManager.showAdIfAvailable(
            activity,
            object : AppOpenAdManager.OnShowAdCompleteListener {
                override fun onShowAdComplete() {
                    // Empty because the user will go back to the activity that shows the ad.
                }
            }
        )
    }
}