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
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.puccontent.org.Adapters.PdfClicked
import com.puccontent.org.Adapters.PdfsAdapter
import com.puccontent.org.Models.MySingleton
import com.puccontent.org.Models.PdfItem
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class PdfsActivity : AppCompatActivity(), PdfClicked {
    private lateinit var binding: ActivityPdfsBinding
    private val list = ArrayList<PdfItem>()
    private var sem:Int = 1
    private var year:Int = 1
    private var subject:String = ""
    private var chapter:String = ""
    private var downloadID:Long=0
    private lateinit var adapter: PdfsAdapter
    private var toast:Toast? = null
    private var downloadList = ArrayList<Long>()
    private var onlineLoaded = false
    private var onlineListened = false
    private var extractPosition = 0
    private lateinit var database: FirebaseDatabase
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val intent = intent
        sem = intent.getIntExtra("sem",1)
        year = intent.getIntExtra("year",1)
        subject = intent.getStringExtra("subject").toString()
        chapter = intent.getStringExtra("chapter").toString()
        database = FirebaseDatabase.getInstance()
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
        val path = "OfflineData/Puc-$year Sem-$sem/$subject/$chapter/${list[position].name}.pdf"
        val file= getExternalFilesDir(path)
        if(file?.isDirectory == false)return true
        file?.delete()
        return false
    }

    override fun checkQuick(position: Int): Boolean {
        val path = "OfflineData/Puc-$year Sem-$sem/$subject/$chapter/${list[position].name}.pdf"
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
        val path = "OfflineData/Puc-$year Sem-$sem/$subject/$chapter/${list[pos].name}.pdf"
        val file= getExternalFilesDir(path)
        if(file?.isDirectory == false){
            deletePdf(file,list[pos].name,pos)
        }
        else{
            file?.delete()
            downloadID = FileDownloader.downloadFile(applicationContext, Uri.parse(list[pos].path),path,list[pos].name+".pdf")
            while(pos>=downloadList.size){
                downloadList.add(-1)
            }
            downloadList[pos] = downloadID
            Toast.makeText(this,"Download queued",Toast.LENGTH_SHORT).show()
        }
    }

    override fun shareFile(position: Int) {
        if (!checkIt(position)) {
          shareLink(position)
        } else {
            val file =
                getExternalFilesDir("OfflineData/Puc-$year Sem-$sem/$subject/$chapter/${list[position].name}.pdf")
            val url = FileProvider.getUriForFile(this,
                applicationContext.packageName.toString() + ".provider",
                file!!)
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "application/pdf"
            intent.putExtra(Intent.EXTRA_STREAM, url)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(intent,
                "Share " + list[position].name + " using ..."))
        }
    }

    private fun shareLink(position: Int) {
        val fileId = list[position].path.substring(42, 75)
        val url = "https://drive.google.com/file/d/$fileId/view?usp=drivesdk"
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT,"${list[position].name} \n $url \n from PucContent App")
        startActivity(Intent.createChooser(intent,"Share url using ..."))
    }
    override fun extractPdf(position: Int) {
        if(!checkIt(position)){
            Toast.makeText(this,"Download the file first",Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, "${list[position].name}.pdf")
        }
        extractPosition = position
        resultLauncher1.launch(intent)
    }
    private fun deletePdf(file:File,name:String,pos:Int) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Delete")
            .setCancelable(true)
            .setMessage("Delete ${name}.pdf?")
            .setPositiveButton("Yes"
            ) { p0, _ ->
                p0.cancel()
                file.delete()
                adapter.notifyItemChanged(pos)
                Toast.makeText(this,"Deleted ${name}.pdf",Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No"){ p0, _ ->
                p0.cancel()
            }
            .show()
    }

    private fun removeFromQuickAccess(position: Int) {
         var path = "OfflineData/Puc-$year Sem-$sem/$subject/$chapter/${list[position].name}.pdf"
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
        var path = "OfflineData/Puc-$year Sem-$sem/$subject/$chapter/${list[pos].name}.pdf"
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
        val path = "OfflineData/Puc-$year Sem-$sem/$subject/$chapter/${list[pos].name}.pdf"
        val file= getExternalFilesDir(path)
        val inte = Intent(this,ReadingActivity::class.java)
        inte.putExtra("name",list[pos].name+".pdf")
        if(file?.isDirectory == false){
            inte.putExtra("file",path)
            inte.putExtra("url",list[pos].path)
            startActivity(inte)
            return
        }
        else{
            file?.delete()
            launchOnlineView(list[pos].path)
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
                list.add(
                    PdfItem(name = name.substring(0,name.length-4)
                        ,path =eFile.absolutePath,size = getFileSize(eFile.length().toString())))
            }
        }
        if(list.isEmpty()){
            binding.info.visibility = View.VISIBLE
        }
        else{
            binding.info.text = ""
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
            if(onlineListened){
                onlineListened = false
                return@postDelayed
            }
            binding.shrimmer.stopShimmer()
            binding.shrimmer.visibility = View.GONE
            showToast("Your Internet connection is slow,it may take time to load items")
            fetchOffline()
        },3000)
        ref.child("Puc-$year Sem-$sem").child(subject).child(chapter)
            .addValueEventListener(listener)
    }
    fun showToast(string:String){
        Toast.makeText(this@PdfsActivity,string,Toast.LENGTH_SHORT).show()
    }
    override fun onStop() {
        super.onStop()
        val ref = FirebaseDatabase.getInstance().reference
        ref.child("Puc-$year Sem-$sem").child(subject).child(chapter)
            .removeEventListener(listener)
        MySingleton.getInstance(this.applicationContext).requestQueue.cancelAll{
            return@cancelAll true
        }
    }
    private val listener = object:ValueEventListener{
        override fun onDataChange(snapshot: DataSnapshot) {
            if(snapshot.exists()){
                list.clear()
               for(snap in snapshot.children){
                   val tItem = PdfItem("","")
                   snap.key?.let { tItem.name = it }
                   snap.getValue<String>()?.let { tItem.path = it }
                   list.add(tItem)
                   database.reference
                       .child("Puc-$year Sem-$sem")
                       .child(subject)
                       .child("Size")
                       .child(tItem.name)
                       .addListenerForSingleValueEvent(SizeListener(list.size-1))
               }
                if(list.isEmpty()){
                    binding.info.visibility = View.VISIBLE
                }
                else{
                    binding.info.visibility = View.GONE
                }
                onlineLoaded = true
                onlineListened = true
                adapter.updateData(list)
                binding.shrimmer.stopShimmer()
                binding.pdfsListView.visibility = View.VISIBLE
                binding.shrimmer.visibility = View.GONE
            }
            else{
                onlineListened = true
                binding.shrimmer.stopShimmer()
                binding.pdfsListView.visibility = View.VISIBLE
                binding.shrimmer.visibility = View.GONE
                binding.info.visibility = View.VISIBLE
                fetchOffline()
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
    private val resultLauncher1 =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            it.data?.data.also { uri ->
                val path = getExternalFilesDir("OfflineData/Puc-$year Sem-$sem/$subject/$chapter/${list[extractPosition].name}.pdf")!!.absolutePath
                if (uri != null) {
                    lifecycleScope.launch(Dispatchers.Default) {
                        copyFile(path, uri){ done->
                                if (done) {
                                    runOnUiThread {
                                        Toast.makeText(applicationContext,
                                            "File saved successfully",
                                            Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    runOnUiThread{
                                        Toast.makeText(applicationContext, "Failed to save file", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                }
                        }
                    }
                }
            }
        }
    private fun getSizeUrl(dUrl: String): String {
        val fileId = dUrl.substring(42, 75)
        val APIKey = "AIzaSyCpn7HmOIq3ddwFB1aFkakNMXKuK0KFbWs"
        return "https://www.googleapis.com/drive/v3/files/${fileId}?fields=size&key=${APIKey}"
    }
    private fun downloadComplete(position: Int) {
        adapter.notifyItemChanged(position)
    }
    private fun getFileSize(sizeInBytes:String): String {
        var size = sizeInBytes
        val s:Float = (size.toFloat()/1000000f)
        size = s.toString()
        for(ind in size.indices){
            if(size[ind]=='.'){
                if(size.length>ind+3)
                    size = size.substring(0,ind+3)
                break
            }
        }
        size = "$size mb"
        return size
    }
    private fun getDownloadSize(i: Int) {
        val queue = MySingleton.getInstance(this.applicationContext).requestQueue
        val url = getSizeUrl(list[i].path)
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                var size = response.get("size") as String
                size = getFileSize(size)
                list[i].size = size
                downloadComplete(i)
                val map = HashMap<String, Any>()
                map[list[i].name] = size
                database.reference
                    .child("Puc-${year} Sem-$sem")
                    .child(subject)
                    .child("Size")
                    .updateChildren(map)
            },
            { error ->
                Log.e("sizeError", error.message.toString())
            }
        )
        MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest)
    }
    inner class SizeListener(private val ind:Int):ValueEventListener{
        override fun onDataChange(snapshot: DataSnapshot) {
            if(snapshot.exists()){
                list[ind].size = snapshot.getValue<String>()
                downloadComplete(ind)
            }else{
                getDownloadSize(ind)
            }
        }
        override fun onCancelled(error: DatabaseError) {
        }
    }
}