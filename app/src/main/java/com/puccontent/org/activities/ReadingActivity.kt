package com.puccontent.org.activities


import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.link.DefaultLinkHandler
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.puccontent.org.R
import com.puccontent.org.databinding.ActivityReadingBinding
import com.puccontent.org.models.LastOpenData
import com.puccontent.org.network.*
import com.puccontent.org.storage.OfflineStorage
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json


class ReadingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReadingBinding
    private var curPage = 0
    private var nightMode = false
    private var isFullScreen = false
    private lateinit var lastOpenData: LastOpenData
    private lateinit var offlineStorage: OfflineStorage
    private lateinit var file: String
    private  var isDefaultNightMode = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReadingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val intent = intent
        file = intent.getStringExtra("file").toString()

        initLastOpenData()
        initViews()
//        isDefaultNightMode  = resources.configuration.uiMode and
//                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
//        if(isDefaultNightMode){
//            toggleNightMode()
//        }
    }

    private fun initLastOpenData() {
        offlineStorage = OfflineStorage(this)
        lastOpenData = if (offlineStorage.lastOpenHistory.isEmpty()) {
            LastOpenData(hashMapOf())
        } else {
            Json.decodeFromString(offlineStorage.lastOpenHistory)
        }
        curPage = lastOpenData.map[file] ?: 0
    }

    private fun saveCurrentPage() {
        lastOpenData.map[file] = binding.pdfView.currentPage
        offlineStorage.lastOpenHistory = lastOpenData.toString()
    }

    private fun initViews() {
        val intent = intent
        file = intent.getStringExtra("file").toString()
        val name = intent.getStringExtra("name")
        val url = intent.getStringExtra("url")
        val pdf: PDFView = findViewById(R.id.pdfView)
        val readName: TextView = findViewById(R.id.readName)
        val imageView3: ImageView = findViewById(R.id.imageView3)

        binding.card.setOnClickListener {
            finish()
        }
        binding.pdfView.setOnClickListener {
            if (isFullScreen) {
                hideFullScreen()
            } else {
                showFullScreen()
            }
        }

        binding.nightModeCard.setOnClickListener {
            toggleNightMode()
        }
        readName.text = name
        try {
            if (file != null) {
                val isDark = offlineStorage.isPdfDark
                nightMode = isDark
                pdf.maxZoom = 10F
                pdf.useBestQuality(true)
                pdf.enableRenderDuringScale(true)
                pdf.enableAnnotationRendering(true)
                pdf.fromFile(getExternalFilesDir(file))
                    .nightMode(nightMode)
                    .defaultPage(curPage)
                    .pageFitPolicy(FitPolicy.WIDTH)
                    .linkHandler(DefaultLinkHandler(pdf))
                    .scrollHandle(DefaultScrollHandle(this))
                    .onError {
                        Toast.makeText(
                            this,
                            "Pdf Not Downloaded Yet it will be downloaded soon automatically",
                            Toast.LENGTH_LONG
                        ).show()
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
    }
    private fun toggleNightMode(){
        nightMode = if (nightMode) {
            binding.pdfView.setNightMode(false)
            binding.imageView3.setImageResource(R.drawable.night_mode)
            false
        } else {
            binding.pdfView.setNightMode(true)
            binding.imageView3.setImageResource(R.drawable.sunny)
            true
        }
        offlineStorage.isPdfDark = nightMode
        showFullScreen()
    }

    override fun onDestroy() {
        super.onDestroy()
        hideFullScreen()
    }

    override fun onPause() {
        super.onPause()
        saveCurrentPage()
    }
    private fun hideFullScreen() {
        binding.readToolBar.visibility = View.VISIBLE
        isFullScreen = false
        WindowInsetsControllerCompat(
            window,
            window.decorView
        ).show(WindowInsetsCompat.Type.navigationBars())
        WindowInsetsControllerCompat(
            window,
            window.decorView
        ).show(WindowInsetsCompat.Type.systemBars())
    }

    private fun showFullScreen() {
        binding.readToolBar.visibility = View.GONE
        isFullScreen = true
        WindowInsetsControllerCompat(
            window,
            window.decorView
        ).hide(WindowInsetsCompat.Type.systemBars())
        WindowInsetsControllerCompat(
            window,
            window.decorView
        ).hide(WindowInsetsCompat.Type.navigationBars())
        WindowInsetsControllerCompat(window, window.decorView).systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}