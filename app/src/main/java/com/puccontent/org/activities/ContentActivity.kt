package com.puccontent.org.activities

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.fragment.NavHostFragment
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.puccontent.org.R
import com.puccontent.org.databinding.ActivitySubjectsBinding
import com.puccontent.org.storage.OfflineStorage
import com.puccontent.org.util.AppOpenManager


class ContentActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySubjectsBinding
    private lateinit var  appOpenManager: AppOpenManager
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
                    bundleOf("year" to year,
                        "sem" to sem,
                        "subject" to subject,
                        "chapter" to chapter)
                navController
                    .navigate(R.id.action_subjectsScreen_to_filesScreen, bundle)
            }
            initAds()
            appOpenManager = AppOpenManager(this)
        }catch(e:IllegalArgumentException){
            e.printStackTrace()
            Log.e("content",e.message.toString())
        }
    }

    private fun initAds() {
        val storage = OfflineStorage(this)
        val id = storage.appOpenId
        Firebase.database.reference.child("Ads")
            .child("AppOpenAd")
            .get()
            .addOnSuccessListener {
                it.getValue<String>()?.let{ itId->
                    storage.appOpenId = itId
                }
            }

        AppOpenManager.AD_UNIT_ID = id

        val anotherId = storage.filesScreenId
        Firebase.database.reference.child("Ads")
            .child("FilesBanner")
            .get()
            .addOnSuccessListener { data ->
                data.getValue<String>()?.let { itId ->
                    storage.filesScreenId = itId
                }
            }
        MobileAds.initialize(this)
        val adView = AdView(this)
        adView.setAdSize(AdSize.BANNER)
        adView.adUnitId = anotherId
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
        val params =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        binding.adContainer.addView(adView, params)
    }
}