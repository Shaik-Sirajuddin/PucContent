package com.puccontent.org.activities

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.puccontent.org.adapters.LinksAdapter
import com.puccontent.org.models.Link
import com.puccontent.org.databinding.ActivityLinksBinding

class LinksActivity : AppCompatActivity() {
    private lateinit var binding : ActivityLinksBinding
    private val list = ArrayList<Link>()
    private val namesList = ArrayList<String>()
    private lateinit var adapter: LinksAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLinksBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
    }
    private fun initViews(){
        list.add(Link("Sms","https://intranet.rguktn.ac.in/SMS/"))
        list.add(Link("Examcell","https://examcell.rguktn.ac.in/"))
        list.add(Link("Puc Online Content","https://rgukt-sklm-abccf.firebaseapp.com/"))
        adapter = LinksAdapter(this,list){
            launchUrl(list[it].link)
        }
        binding.listView.layoutManager = LinearLayoutManager(this)
        binding.listView.adapter = adapter
        binding.backImage.setOnClickListener {
            finish()
        }
    }
    private fun launchUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}