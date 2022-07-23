package com.puccontent.org.activities


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.link.DefaultLinkHandler
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.puccontent.org.R
import com.puccontent.org.databinding.ActivityReadingBinding
import com.puccontent.org.network.*
import com.puccontent.org.storage.OfflineStorage


class ReadingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReadingBinding
    private var curPage = 0
    private var nightMode = false
    private var mInterstitialAd: InterstitialAd? = null
    private var isAdShown = false
    private var isAdLoaded = false
    private var isInterstitialEnabled = true
    private var isFullScreen = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReadingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val intent = intent
        val file = intent.getStringExtra("file")
        val name = intent.getStringExtra("name")
        val url = intent.getStringExtra("url")
        val pdf: PDFView = findViewById(R.id.pdfView)
        val readName: TextView = findViewById(R.id.readName)
        val imageView3: ImageView = findViewById(R.id.imageView3)
        binding.card.setOnClickListener {
            handleBackClick()
        }
        binding.pdfView.setOnClickListener {
            if(isFullScreen){
                hideFullScreen()
            }
            else{
                showFullScreen()
            }
        }

        binding.nightModeCard.setOnClickListener {
            nightMode = if (nightMode) {
                pdf.setNightMode(false)
                imageView3.setImageResource(R.drawable.night_mode)
                false
            } else {
                pdf.setNightMode(true)
                imageView3.setImageResource(R.drawable.sunny)
                true
            }
            showFullScreen()
        }
        readName.text = name
        try {
            if (file != null) {
                pdf.maxZoom = 10F
                pdf.useBestQuality(true)
                pdf.enableRenderDuringScale(true)
                pdf.enableAnnotationRendering(true)
                pdf.fromFile(getExternalFilesDir(file))
                    .defaultPage(curPage)
                    .pageFitPolicy(FitPolicy.WIDTH)
                    .linkHandler(DefaultLinkHandler(pdf))
                    .scrollHandle(DefaultScrollHandle(this))
                    .onError {
                        Toast.makeText(this,
                            "Pdf Not Downloaded Yet it will be downloaded soon automatically",
                            Toast.LENGTH_LONG).show()
                        if (url != null) {
                            launchOnlineView(url)
                        } else {
                            finish()
                        }
                    }
                    .load()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("ReadingActivity", e.message.toString())
        }
        initAds()

    }

    private fun handleBackClick() {
        if (!isAdShown && isAdLoaded && mInterstitialAd != null) {

            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mInterstitialAd = null
                    finish()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    mInterstitialAd = null
                    finish()
                }

                override fun onAdShowedFullScreenContent() {
                    isAdLoaded = false
                    isAdShown = true
                    mInterstitialAd = null
                }

            }
            if (mInterstitialAd != null) {
                mInterstitialAd?.show(this)
            } else {
                finish()
            }
        } else {
            finish()
        }
    }

    private fun initAds() {
        val storage = OfflineStorage(this)
        Firebase.database.reference.child("Ads")
            .child("PdfInterstitial")
            .get()
            .addOnSuccessListener {
                val id = it.getValue<String>()
                if (id != null) {
                    storage.pdfInterstitialId = id
                }
            }
        Firebase.remoteConfig.fetchAndActivate().addOnCompleteListener {
            isInterstitialEnabled = Firebase.remoteConfig.getBoolean("isInterstitialEnabled")
            if (isInterstitialEnabled) {
                loadAds()
            }
        }
    }

    private fun loadAds() {
        val storage = OfflineStorage(this)
        val adRequest = AdRequest.Builder().build()
        val id = storage.pdfInterstitialId
        InterstitialAd.load(this, id,
            adRequest, object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d("readAd", adError.message)
                    isAdLoaded = false
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d("readAd", "Ad loaded")
                    isAdLoaded = true
                    mInterstitialAd = interstitialAd
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        hideFullScreen()
    }


    private fun hideFullScreen() {
        binding.readToolBar.visibility = View.VISIBLE
        isFullScreen = false
        WindowInsetsControllerCompat(window,
            window.decorView).show(WindowInsetsCompat.Type.navigationBars())
        WindowInsetsControllerCompat(window,
            window.decorView).show(WindowInsetsCompat.Type.systemBars())
    }

    private fun showFullScreen() {
        binding.readToolBar.visibility = View.GONE
        isFullScreen = true
        WindowInsetsControllerCompat(window,
            window.decorView).hide(WindowInsetsCompat.Type.systemBars())
        WindowInsetsControllerCompat(window,
            window.decorView).hide(WindowInsetsCompat.Type.navigationBars())
        WindowInsetsControllerCompat(window, window.decorView).systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}