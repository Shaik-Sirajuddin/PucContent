package com.puccontent.org.fragments

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.puccontent.org.*
import com.puccontent.org.Adapters.PdfClicked
import com.puccontent.org.Adapters.PdfsAdapter
import com.puccontent.org.Models.MySingleton
import com.puccontent.org.Models.PdfItem
import com.puccontent.org.activities.ReadingActivity
import com.puccontent.org.databinding.FragmentFilesScreenBinding
import com.puccontent.org.network.*
import com.puccontent.org.storage.FirebaseQueryLiveData
import com.puccontent.org.storage.OfflineStorage
import com.puccontent.org.util.SwipeGesture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


class FilesScreen : Fragment(), PdfClicked {
    private lateinit var binding: FragmentFilesScreenBinding
    private val list = ArrayList<PdfItem>()
    private var sem: Int = 1
    private var year: Int = 1
    private var subject: String = ""
    private var chapter: String = ""
    private var downloadID: Long = 0
    private lateinit var adapter: PdfsAdapter
    private var toast: Toast? = null
    private var downloadList = ArrayList<Long>()
    private var onlineListened = false
    private var extractPosition = 0
    private lateinit var database: FirebaseDatabase
    private var sharePosition: Int = -1
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentFilesScreenBinding.inflate(inflater)
        initData()
        initViews()
        fetchOffline()
        fetchOnline()
        return binding.root
    }

    private fun initViews() {
        database = Firebase.database
        binding.textView8.text = chapter
        with(binding.textView8) {
            setHorizontallyScrolling(true);
            isSingleLine = true;
            marqueeRepeatLimit = -1
            ellipsize = TextUtils.TruncateAt.MARQUEE;
            isSelected = true
        }
        adapter = PdfsAdapter(requireContext(), this)
        binding.pdfsListView.adapter = adapter
        binding.pdfsListView.itemAnimator?.changeDuration = 0
        binding.pdfsListView.layoutManager = LinearLayoutManager(requireContext())
        val swipeGesture = object : SwipeGesture(requireContext(), 15) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        deletePdf(viewHolder.absoluteAdapterPosition)
                    }
                    ItemTouchHelper.RIGHT -> {
                        shareFile(viewHolder.absoluteAdapterPosition)
                    }
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeGesture)
        itemTouchHelper.attachToRecyclerView(binding.pdfsListView)

        requireContext().registerReceiver(
            onDownloadComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
        binding.backImage.setOnClickListener {
            Navigation.findNavController(binding.root).navigateUp()
        }
        if (isConnected()) {
            binding.progressCard.visibility = View.VISIBLE
        } else {
            binding.info.text = getString(R.string.offline)
            binding.info.visibility = View.VISIBLE
            binding.progressCard.visibility = View.GONE
        }
    }

    private fun initAds() {
        context?.let {
            val storage = OfflineStorage(it)
            val id = storage.filesScreenId
            Firebase.database.reference.child("Ads")
                .child("FilesBanner")
                .get()
                .addOnSuccessListener { data ->
                    data.getValue<String>()?.let { itId ->
                        storage.filesScreenId = itId
                    }
                }
            MobileAds.initialize(it)
            val adView = AdView(requireContext())
            adView.adUnitId = id
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
            val params =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            binding.adContainer.addView(adView, params)
        }
    }

    private fun initData() {
        val argsData = requireArguments()
        sem = argsData.getInt("sem")
        year = argsData.getInt("year")
        subject = argsData.getString("subject", "Physics")
        chapter = argsData.getString("chapter", "")
    }

    override fun onDestroy() {
        super.onDestroy()
        requireContext().unregisterReceiver(onDownloadComplete)
    }

    override fun click(position: Int) {
        handleClick(position)
    }

    override fun checkIt(position: Int): Boolean {
        try {
            val path = "OfflineData/Puc-$year Sem-$sem/$subject/$chapter/${list[position].name}.pdf"
            val file = context?.getExternalFilesDir(path)
            if (file?.isDirectory == false) return true
            file?.delete()
            return false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    override fun checkQuick(position: Int): Boolean {
        val path = "OfflineData/Puc-$year Sem-$sem/$subject/$chapter/${list[position].name}.pdf"
        val array = FileDownloader.convertStringToArray(getQuickAccess())
        if (array.contains(path)) return true
        return false
    }

    override fun quickAccess(position: Int) {
        if (checkQuick(position)) {
            removeFromQuickAccess(position)
        } else {
            addToQuickAccess(position)
        }
    }

    override fun downloadOrOpen(position: Int) {
        val path = "OfflineData/Puc-$year Sem-$sem/$subject/$chapter/${list[position].name}.pdf"
        val file = context?.getExternalFilesDir(path)
        if (file?.isDirectory == false) {
            openWith(position)
        } else {
            file?.delete()
            downloadID = FileDownloader.downloadFile(
                requireContext().applicationContext,
                Uri.parse(list[position].path),
                path,
                list[position].name + ".pdf"
            )
            while (position >= downloadList.size) {
                downloadList.add(-1)
            }
            downloadList[position] = downloadID
            Toast.makeText(requireContext(), "Download queued", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareFile(position: Int) {
        sharePosition = position
        if (!checkIt(position)) {
            shareLink(position)
        } else {
            val file =
                context?.getExternalFilesDir("OfflineData/Puc-$year Sem-$sem/$subject/$chapter/${list[position].name}.pdf")
            val url = FileProvider.getUriForFile(
                requireContext(),
                context?.applicationContext?.packageName.toString() + ".provider",
                file!!
            )
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "application/pdf"
            intent.putExtra(Intent.EXTRA_STREAM, url)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(
                Intent.createChooser(
                    intent,
                    "Share " + list[position].name + " using ..."
                )
            )
//            adapter.notifyItemChanged(viewHolder.absoluteAdapterPosition)

        }
    }

    private fun shareLink(position: Int) {
        val fileId = list[position].path.substring(42, 75)
        val url = "https://drive.google.com/file/d/$fileId/view?usp=drivesdk"
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, "${list[position].name} \n $url \n from PucContent App")
        startActivity(Intent.createChooser(intent, "Share url using ..."))
    }


    override fun openWith(position: Int) {
        try {
            if (!checkIt(position)) {
                showToast("Download the pdf")
                return
            }
            val file =
                context?.getExternalFilesDir("OfflineData/Puc-$year Sem-$sem/$subject/$chapter/${list[position].name}.pdf")
            val url = FileProvider.getUriForFile(
                requireContext(),
                context?.applicationContext?.packageName.toString() + ".provider",
                file!!
            )
            sharePosition = position
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(url, "application/pdf")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//            startActivity(Intent.createChooser(intent,
//                "Open " + list[position].name + " using ..."))
            startActivity(intent)
        } catch (e: Exception) {
            Firebase.crashlytics.log(e.message.toString())
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("state", sharePosition.toString())
        if (sharePosition != -1) {
            adapter.notifyItemChanged(sharePosition)
            sharePosition = -1
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("state", sharePosition.toString())
    }

    private fun deletePdf(position: Int) {
        val path = "OfflineData/Puc-$year Sem-$sem/$subject/$chapter/${list[position].name}.pdf"
        val name = list[position].name
        val file = context?.getExternalFilesDir(path)
        if (file?.isDirectory == false) {

            AlertDialog.Builder(requireContext())
                .setTitle("Confirm Delete")
                .setCancelable(true)
                .setMessage("Delete ${name}?")
                .setPositiveButton(
                    "Yes"
                ) { p0, _ ->
                    p0.cancel()
                    file.delete()
                    adapter.notifyItemChanged(position, null)
                    showToast("Deleted ${name}.pdf")
                }
                .setNegativeButton("No") { p0, _ ->
                    p0.cancel()
                    adapter.notifyItemChanged(position, null)
                }
                .setOnCancelListener {
                    adapter.notifyItemChanged(position, null)
                }
                .show()
        } else {
            adapter.notifyItemChanged(position, null)
            showToast("Pdf not downloaded")
        }

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
        val sharedPref =
            requireContext().getSharedPreferences(FileDownloader.fileKey, Context.MODE_PRIVATE)
        if (sharedPref != null) {
            with(sharedPref.edit()) {
                putString("quick", path)
                apply()
            }
            adapter.notifyItemChanged(position, null)
        }
    }

    private fun addToQuickAccess(pos: Int) {
        var path = "OfflineData/Puc-$year Sem-$sem/$subject/$chapter/${list[pos].name}.pdf"
        val file = context?.getExternalFilesDir(path)
        if (file?.isDirectory == true) {
            file.delete()
            showToast("Download the file first")
            return
        }
        val sharedPref =
            requireContext().getSharedPreferences(FileDownloader.fileKey, Context.MODE_PRIVATE)
        val string = getQuickAccess()
        if (string.isNotEmpty()) {
            path = "$string,$path"
        }
        if (sharedPref != null) {
            with(sharedPref.edit()) {
                putString("quick", path)
                apply()
            }
            adapter.notifyItemChanged(pos, null)
        }
    }

    private fun getQuickAccess(): String {
        val sharedPref =
            requireContext().getSharedPreferences(FileDownloader.fileKey, Context.MODE_PRIVATE)
        return sharedPref?.getString("quick", "").toString()
    }

    private fun handleClick(pos: Int) {
        val path = "OfflineData/Puc-$year Sem-$sem/$subject/$chapter/${list[pos].name}.pdf"
        val file = context?.getExternalFilesDir(path)
        val inte = Intent(requireContext(), ReadingActivity::class.java)
        inte.putExtra("name", list[pos].name + ".pdf")
        if (file?.isDirectory == false) {
            inte.putExtra("file", path)
            inte.putExtra("url", list[pos].path)
            startActivity(inte)
            return
        } else {
            file?.delete()
            requireActivity().launchOnlineView(list[pos].path)
        }
    }

    private fun fetchOffline() {
        val file: File? =
            context?.getExternalFilesDir("OfflineData/Puc-$year Sem-$sem/$subject/$chapter")
        if (file == null || !file.exists()) return
        list.clear()
        file.listFiles()?.let { filesList ->
            for (eFile in filesList) {
                verifyFile(eFile)
                val name = eFile.name
                list.add(
                    PdfItem(
                        name = name.substring(0, name.length - 4),
                        path = eFile.absolutePath,
                        size = getFileSize(eFile.length().toString())
                    )
                )
            }
        }
        if (list.isNotEmpty()) {
            binding.info.visibility = View.GONE
        }
        list.sortBy { it.name }
        adapter.updateData(list)
    }

    private fun verifyFile(file: File) {
        if (file.name.endsWith(".download")) {
            val name = file.name
            renameFile(name, name.substring(0, name.length - 9))
        }
    }

    private fun renameFile(oldName: String, newName: String) {
        val dir: File? =
            context?.getExternalFilesDir("OfflineData/Puc-$year Sem-$sem/$subject/$chapter")
        if (dir?.exists() == true) {
            val from = File(dir, oldName)
            val to = File(dir, newName)
            if (from.exists()) from.renameTo(to)
        }
    }

    private fun fetchOnline() {
        val ref = FirebaseDatabase.getInstance().reference
            .child("Puc-$year Sem-$sem").child(subject).child(chapter)
        val data = FirebaseQueryLiveData(ref, FirebaseQueryLiveData.singleType)
        data.observe(viewLifecycleOwner) {
            setData(it)
        }
    }

    private fun setData(snapshot: DataSnapshot) {
        try {
            binding.info.text = resources.getString(R.string.info)
            if (snapshot.exists()) {
                list.clear()
                for (snap in snapshot.children) {
                    val tItem = PdfItem("", "")
                    snap.key?.let { tItem.name = it }
                    snap.getValue<String>()?.let { tItem.path = it }
                    list.add(tItem)
                    database.reference
                        .child("Puc-$year Sem-$sem")
                        .child(subject)
                        .child("Size")
                        .child(tItem.name)
                        .addListenerForSingleValueEvent(SizeListener(list.size - 1))
                }
                if (list.isEmpty()) {
                    binding.info.visibility = View.VISIBLE
                } else {
                    binding.info.visibility = View.GONE
                }
                onlineListened = true
                adapter.updateData(list)
            } else {
                onlineListened = true
                binding.info.visibility = View.VISIBLE
            }
            binding.progressCard.visibility = View.GONE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            for ((counter, a) in downloadList.withIndex()) {
                if (a == id) {
                    toast?.cancel()
                    toast =
                        Toast.makeText(requireContext(), "Download Completed", Toast.LENGTH_SHORT)
                    toast?.show()
                    downloadComplete(counter)
                }
            }
        }
    }
    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (sharePosition != -1) {
                adapter.notifyItemChanged(sharePosition)
                sharePosition = -1
            }
        }

    private fun getSizeUrl(dUrl: String): String {

        val fileId = dUrl.substring(42, 75)
        val APIKey = "AIzaSyCpn7HmOIq3ddwFB1aFkakNMXKuK0KFbWs"
        return "https://www.googleapis.com/drive/v3/files/${fileId}?fields=size&key=${APIKey}"
    }

    private fun downloadComplete(position: Int) {
        adapter.notifyItemChanged(position, null);
    }

    private fun getFileSize(sizeInBytes: String): String {
        var size = sizeInBytes
        val s: Float = (size.toFloat() / 1000000f)
        size = s.toString()
        for (ind in size.indices) {
            if (size[ind] == '.') {
                if (size.length > ind + 3)
                    size = size.substring(0, ind + 3)
                break
            }
        }
        size = "$size mb"
        return size
    }

    private fun getDownloadSize(i: Int) {
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
        context?.let { MySingleton.getInstance(it).addToRequestQueue(jsonObjectRequest) }
    }

    inner class SizeListener(private val ind: Int) : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.exists()) {
                list[ind].size = snapshot.getValue<String>()
                downloadComplete(ind)
            } else {
                getDownloadSize(ind)
            }
        }

        override fun onCancelled(error: DatabaseError) {
        }
    }
}