package com.puccontent.org

import android.app.AlertDialog
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import com.puccontent.org.databinding.ActivityPdfsBinding
import java.io.File
import android.app.DownloadManager
import android.content.*
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.puccontent.org.Adapters.PdfClicked
import com.puccontent.org.Adapters.PdfsAdapter


class PdfsActivity : AppCompatActivity(), PdfClicked {
    private lateinit var binding: ActivityPdfsBinding
    private val list = ArrayList<String>()
    private val pathList = ArrayList<String>()
    private var sem:Int = 1
    private var year:Int = 1
    private var subject:String = ""
    private var chapter:String = ""
    private var downloadID:Long=0
    private lateinit var adapter: PdfsAdapter
    private var toast:Toast? = null
    private var downloadList = ArrayList<Long>()
    private var onlineLoaded = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val intent = intent
        sem = intent.getIntExtra("sem",1)
        year = intent.getIntExtra("year",1)
        subject = intent.getStringExtra("subject").toString()
        chapter = intent.getStringExtra("chapter").toString()
        binding.textView8.text = chapter
        adapter = PdfsAdapter(this,this)
        binding.pdfsListView.adapter = adapter
        binding.pdfsListView.layoutManager = LinearLayoutManager(this)
        registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        binding.backImage.setOnClickListener {
            finish()
        }
        if(isConnected()) {
            binding.shrimmer.visibility = View.VISIBLE
            binding.shrimmer.startShimmer()
            binding.pdfsListView.visibility = View.GONE
            fetchOnline()
        }else{
            binding.info.text = getString(R.string.offline)
            fetchOffline()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onDownloadComplete)
    }

    override fun click(position: Int) {
       handleClick(position)
    }

    override fun checkIt(position:Int):Boolean{
        val path = "OfflineData/Puc-$year Sem-$sem/$subject/$chapter/${list[position]}.pdf"
        val file= getExternalFilesDir(path)
        if(file?.isDirectory == false)return true
        file?.delete()
        return false
    }

    override fun checkQuick(position: Int): Boolean {
        val path = "OfflineData/Puc-$year Sem-$sem/$subject/$chapter/${list[position]}.pdf"
        val array = FileDownloader.convertStringToArray(getQuickAccess())
        if(array.contains(path))return true
        return false
    }
    override fun quickAccesss(position: Int) {
      if(checkQuick(position)){
          AlertDialog.Builder(this)
              .setTitle("Remove From QuickAccess")
              .setCancelable(true)
              .setPositiveButton("Yes"
              ) { p0, _ ->
                  p0.cancel()
                  removeFromQuickAccess(position)
              }
              .setNegativeButton("No"){ p0, _ ->
                  p0.cancel()
              }
              .show()
      }
      else{
         AlertDialog.Builder(this)
              .setTitle("Add To QuickAccess")
              .setCancelable(true)
              .setPositiveButton("Yes"
              ) { p0, _ ->
                  p0.cancel()
                  addToQuickAccess(position)
              }
              .setNegativeButton("No"){ p0, _ ->
                  p0.cancel()
              }
             .show()
      }


    }

    override fun downloadOrDelete(pos: Int) {
        val path = "OfflineData/Puc-$year Sem-$sem/$subject/$chapter/${list[pos]}.pdf"
        val file= getExternalFilesDir(path)
        if(file?.isDirectory == false){
            deletePdf(file,list[pos],pos)
        }
        else{
            file?.delete()
            downloadID = FileDownloader.downloadFile(applicationContext, Uri.parse(pathList[pos]),path,list[pos]+".pdf")
            while(pos>=downloadList.size){
                downloadList.add(-1)
            }
            downloadList[pos] = downloadID
            Toast.makeText(this,"Download Started",Toast.LENGTH_SHORT).show()
        }
    }
    private fun deletePdf(file:File,name:String,pos:Int) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Delete")
            .setCancelable(true)
            .setMessage("Delete ${name}.pdf?")
            .setPositiveButton("Yes"
            ) { p0, p1 ->
                p0.cancel()
                file.delete()
                adapter.notifyItemChanged(pos)
                Toast.makeText(this,"Deleted ${name}.pdf",Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No"){ p0, p1 ->
                p0.cancel()
            }
            .show()
    }

    private fun removeFromQuickAccess(position: Int) {
         var path = "OfflineData/Puc-$year Sem-$sem/$subject/$chapter/${list[position]}.pdf"
         val array = FileDownloader.convertStringToArray(getQuickAccess())
         val arr = ArrayList<String>()
         array.forEach {
             if (it != null) {
                 arr.add(it)
             }
         }
        arr.remove(path)
        path = FileDownloader.convertArrayToString(arr)
        val sharedPref =  baseContext?.getSharedPreferences(FileDownloader.fileKey,Context.MODE_PRIVATE)
        if(sharedPref!=null) {
            with(sharedPref.edit()) {
                putString("quick", path)
                apply()
            }
            adapter.notifyItemChanged(position)
        }
    }

    private fun addToQuickAccess(pos: Int){
        var path = "OfflineData/Puc-$year Sem-$sem/$subject/$chapter/${list[pos]}.pdf"
        val file = getExternalFilesDir(path)
        if(file?.isDirectory == true){
            file.delete()
            Toast.makeText(this,"Download the file first",Toast.LENGTH_SHORT).show()
            return
        }
        val sharedPref =  baseContext?.getSharedPreferences(FileDownloader.fileKey,Context.MODE_PRIVATE)
        val string = getQuickAccess()
        if(string.isNotEmpty()){
            path = "$string,$path"
        }
        if(sharedPref!=null) {
            with(sharedPref.edit()) {
                putString("quick", path)
                apply()
            }
            adapter.notifyItemChanged(pos)
        }
    }
    private fun getQuickAccess(): String {
        val sharedPref =
            baseContext?.getSharedPreferences(FileDownloader.fileKey, Context.MODE_PRIVATE)
        return sharedPref?.getString("quick", "").toString()
    }
    private fun handleClick(pos: Int) {
        val path = "OfflineData/Puc-$year Sem-$sem/$subject/$chapter/${list[pos]}.pdf"
        val file= getExternalFilesDir(path)
        val inte = Intent(this,ReadingActivity::class.java)
        inte.putExtra("name",list[pos]+".pdf")
        if(file?.isDirectory == false){
            inte.putExtra("file",path)
            startActivity(inte)
            return
        }
        else{
            file?.delete()
            downloadID = FileDownloader.downloadFile(applicationContext, Uri.parse(pathList[pos]),path,list[pos]+".pdf")
            while(pos>=downloadList.size){
                downloadList.add(-1)
            }
            downloadList[pos] = downloadID
            Toast.makeText(this,"Download Started",Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchOffline(){
        binding.shrimmer.startShimmer()
        binding.shrimmer.visibility = View.VISIBLE
        val file: File?= getExternalFilesDir("OfflineData/Puc-$year Sem-$sem/$subject/$chapter")
        if(file==null || !file.exists())return
        list.clear()
        file.listFiles()?.let{ filesList->
            for(eFile in filesList){
                val name = eFile.name
                list.add(name.substring(0,name.length-4))
                pathList.add(eFile.absolutePath)
            }
        }
        if(list.isEmpty()){
            binding.info.visibility = View.VISIBLE
        }
        else{
            binding.info.visibility = View.GONE
        }
        adapter.updateData(list)
        binding.pdfsListView.visibility = View.VISIBLE
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
            binding.shrimmer.visibility = View.GONE
            fetchOffline()
        },3000)
        ref.child("Puc-$year Sem-$sem").child(subject).child(chapter)
            .addValueEventListener(listener)
    }

    override fun onStop() {
        super.onStop()
        val ref = FirebaseDatabase.getInstance().reference
        ref.child("Puc-$year Sem-$sem").child(subject).child(chapter)
            .removeEventListener(listener)
    }
    private val listener = object:ValueEventListener{
        override fun onDataChange(snapshot: DataSnapshot) {
            if(snapshot.exists()){
                list.clear()
                pathList.clear()
               for(snap in snapshot.children){
                   snap.key?.let { list.add(it) }
                   snap.getValue<String>()?.let { pathList.add(it) }
               }
                if(list.isEmpty()){
                    binding.info.visibility = View.VISIBLE
                }
                else{
                    binding.info.visibility = View.GONE
                }
                onlineLoaded = true
                adapter.updateData(list)
                binding.shrimmer.stopShimmer()
                binding.pdfsListView.visibility = View.VISIBLE
                binding.shrimmer.visibility = View.GONE
            }
        }
        override fun onCancelled(error: DatabaseError) {

        }
    }
    private val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            var coun =0
            for(a in downloadList) {
                if (a == id) {
                    toast?.cancel()
                   toast =  Toast.makeText(this@PdfsActivity, "Download Completed", Toast.LENGTH_SHORT)
                    toast?.show()
                    downloadComplete(coun)
                }
                coun++
            }
        }
    }

    private fun downloadComplete(position: Int) {
        adapter.notifyItemChanged(position)
    }
    private fun getDownloadSize(){

    }
}