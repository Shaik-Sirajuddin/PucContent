package com.puccontent.org

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.puccontent.org.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {
    private lateinit var binding:ActivityAboutBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.writeButton.setOnClickListener {
            suggestion()
        }
        binding.backImage.setOnClickListener {
            finish()
        }
    }
    fun suggestion(){
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("mailto:techx2002@gmail.com"))
            intent.putExtra(Intent.EXTRA_EMAIL, "techx2002@gmail.com")
            intent.putExtra(Intent.EXTRA_SUBJECT, "PucContent - Suggestion")
            startActivity(intent)
        }catch(e:Exception){
            Toast.makeText(this,"Your Device Cannot Perform This Action", Toast.LENGTH_SHORT).show()
        }
    }
}