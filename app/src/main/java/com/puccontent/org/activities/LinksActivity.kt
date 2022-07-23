package com.puccontent.org.activities

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.puccontent.org.Adapters.LinksAdapter
import com.puccontent.org.Models.Link
import com.puccontent.org.R
import com.puccontent.org.databinding.ActivityLinksBinding
import com.puccontent.org.network.launchOnlineView

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