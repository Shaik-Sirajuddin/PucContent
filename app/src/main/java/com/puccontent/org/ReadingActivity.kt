package com.puccontent.org


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.link.DefaultLinkHandler
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.puccontent.org.databinding.ActivityReadingBinding
class ReadingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReadingBinding
    private var curPage = 0
    private var nightMode = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReadingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val intent = intent
        val file = intent.getStringExtra("file")
        val url = intent.getStringExtra("url")
        val name = intent.getStringExtra("name")
        val pdf: PDFView = findViewById(R.id.pdfView)
        val backImage: ImageView = findViewById(R.id.backImage)
        val readName:TextView = findViewById(R.id.readName)
        val readToolbar: Toolbar = findViewById(R.id.readToolBar)
        val imageView3:ImageView = findViewById(R.id.imageView3)
        val fullScreen:ImageView = findViewById(R.id.fullScreen)
        backImage.setOnClickListener {
            finish()
        }
        fullScreen.setOnClickListener {
             readToolbar.visibility = View.GONE
            Toast.makeText(this,"Press back button to exit full screen",Toast.LENGTH_SHORT).show()
        }
        imageView3.setOnClickListener {
            if(nightMode){
                pdf.setNightMode(false)
                Toast.makeText(this,"DayMode activated,scroll to see changes",Toast.LENGTH_SHORT).show()
                imageView3.setImageResource(R.drawable.night_mode)
                nightMode = false
            }else{
                pdf.setNightMode(true)
                Toast.makeText(this,"NightMode activated,scroll to see changes",Toast.LENGTH_SHORT).show()
                imageView3.setImageResource(R.drawable.sunny)
                nightMode = true
            }
        }
        readName.text = name
        try {
            if (file != null) {
                pdf.fromFile(getExternalFilesDir(file))
                    .defaultPage(curPage)
                    .pageFitPolicy(FitPolicy.WIDTH)
                    .linkHandler(DefaultLinkHandler(pdf))
                    .scrollHandle(DefaultScrollHandle(this))
                    .load()
            }

        }catch (e:Exception){
            e.printStackTrace()
            Log.e("ReadingActivity",e.message.toString())
        }
    }

    override fun onBackPressed() {
        val readToolbar: Toolbar = findViewById(R.id.readToolBar)
        if(readToolbar.visibility == View.GONE){
            readToolbar.visibility= View.VISIBLE
        }
        else{
            super.onBackPressed()
        }
    }
}