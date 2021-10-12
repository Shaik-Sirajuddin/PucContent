
package com.puccontent.org

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import com.puccontent.org.databinding.ActivityChaptersBinding
import java.io.File

class ChaptersActivity : AppCompatActivity() {
    private lateinit var binding:ActivityChaptersBinding
    private val list = ArrayList<String>()
    private var sem:Int = 1
    private var year:Int = 1
    private var subject:String = ""
    private lateinit var adapter:ArrayAdapter<String>
    private var onlineLoaded = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChaptersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val intent = intent
        sem = intent.getIntExtra("sem",1)
        year = intent.getIntExtra("year",1)
        subject = intent.getStringExtra("subject").toString()
        adapter = ArrayAdapter(this,R.layout.chapter_item,R.id.chapterName,list)
        binding.chapterPath.text = subject
        binding.chaptersListView.adapter = adapter
        binding.backImage.setOnClickListener {
            finish()
        }
        binding.chaptersListView.setOnItemClickListener { adapterView, view, pos, l ->
            val intent1 = Intent(this,PdfsActivity::class.java)
            intent1.putExtra("subject",subject)
            intent1.putExtra("year",year)
            intent1.putExtra("sem",sem)
            intent1.putExtra("chapter",list[pos])
            startActivity(intent1)
        }
        if(isConnected()) {
            binding.shrimmer.visibility = View.VISIBLE
            binding.shrimmer.startShimmer()
            binding.chaptersListView.visibility = View.GONE
            fetchOnline()
        }else{
            binding.info.text = getString(R.string.offline)
            fetchOffline()
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            val ref = FirebaseDatabase.getInstance().reference
            ref.child("Puc-$year Sem-$sem").child(subject).child("Chapters")
                .removeEventListener(listener)
        }catch (e:Exception){
            Log.e("removeListener",e.message.toString())
        }
    }
    private fun fetchOffline(){
        binding.shrimmer.startShimmer()
        binding.shrimmer.visibility = View.VISIBLE
        val file: File?= getExternalFilesDir("OfflineData/Puc-$year Sem-$sem/$subject")
        if(file==null || !file.exists())return
        list.clear()
        file.listFiles()?.let{ filesList->
            for(eFile in filesList){
                list.add(eFile.name)
            }
        }
        if(list.isEmpty()){
            binding.info.visibility = View.VISIBLE
        }
        else{
            binding.info.visibility = View.GONE
        }
        adapter.notifyDataSetChanged()
        binding.shrimmer.stopShimmer()
        binding.shrimmer.visibility = View.GONE
    }
    private fun fetchOnline(){
        binding.shrimmer.startShimmer()
        binding.shrimmer.visibility = View.VISIBLE
         val ref = FirebaseDatabase.getInstance().reference
        Handler(Looper.getMainLooper()).postDelayed({
            if(onlineLoaded){
                onlineLoaded = false
                return@postDelayed
            }
            binding.shrimmer.stopShimmer()
            binding.chaptersListView.visibility = View.VISIBLE
            binding.shrimmer.visibility = View.GONE
            fetchOffline()
        },3000)
         ref.child("Puc-$year Sem-$sem").child(subject).child("Chapters").addListenerForSingleValueEvent(listener)
    }
    private val listener = object:ValueEventListener{
        override fun onDataChange(snapshot: DataSnapshot) {
            if(snapshot.exists()){
                list.clear()
                for(snap in snapshot.children){
                    snap.getValue<String>()?.let { list.add(it) }
                }
                if(list.isEmpty()){
                    binding.info.visibility = View.VISIBLE
                }
                else{
                    binding.info.visibility = View.GONE
                }
                onlineLoaded = true
                binding.chaptersListView.visibility = View.VISIBLE
                adapter.notifyDataSetChanged()
                binding.shrimmer.stopShimmer()
                binding.shrimmer.visibility = View.GONE
            }
        }
        override fun onCancelled(error: DatabaseError) {

        }

    }
}