package com.puccontent.org.fragments


import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.puccontent.org.Adapters.FoldersAdapter
import com.puccontent.org.R
import com.puccontent.org.databinding.FragmentFoldersScreenBinding
import com.puccontent.org.network.*
import com.puccontent.org.storage.FirebaseQueryLiveData
import com.puccontent.org.storage.OfflineStorage
import java.io.File

class FoldersScreen : Fragment() {
    private lateinit var binding: FragmentFoldersScreenBinding
    private val list = ArrayList<String>()
    private var sem: Int = 1
    private var year: Int = 1
    private var subject: String = ""
    private lateinit var adapter: FoldersAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentFoldersScreenBinding.inflate(inflater)
        initAds()
        val argsData = requireArguments()
        sem = argsData.getInt("sem")
        year = argsData.getInt("year")
        subject = argsData.getString("subject", "Physics")
        adapter = FoldersAdapter(requireContext(),list){
            navigateToFilesScreen(it)
        }
        binding.chapterPath.text = subject
        with(binding.chapterPath){
            setHorizontallyScrolling(true);
            isSingleLine = true;
            marqueeRepeatLimit = -1
            ellipsize = TextUtils.TruncateAt.MARQUEE;
            isSelected = true
        }
        binding.chaptersListView.layoutManager = LinearLayoutManager(requireContext())
        binding.chaptersListView.adapter = adapter
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
        fetchOffline()
        fetchOnline()
        return binding.root
    }

    private fun initAds() {
        context?.let {
            val storage = OfflineStorage(it)
            val id = storage.foldersScreenId
            Firebase.database.reference.child("Ads")
                .child("FolderBanner")
                .get()
                .addOnSuccessListener { data ->
                    data.getValue<String>()?.let { itId ->
                        storage.foldersScreenId = itId
                    }
                }
            MobileAds.initialize(it)
            val adView = AdView(requireContext())
            adView.adSize = AdSize.BANNER
            adView.adUnitId = id
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
            val params =
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT)
            binding.adContainer.addView(adView, params)
        }
    }

    private fun navigateToFilesScreen(pos: Int) {
        val bundle =
            bundleOf("year" to year, "sem" to sem, "subject" to subject, "chapter" to list[pos])
        Navigation
            .findNavController(binding.root)
            .navigate(R.id.action_foldersScreen_to_filesScreen, bundle)
    }

    private fun fetchOffline() {
        val file: File? = context?.getExternalFilesDir("OfflineData/Puc-$year Sem-$sem/$subject")
        if (file == null || !file.exists()) return
        list.clear()
        file.listFiles()?.let { filesList ->
            for (eFile in filesList) {
                list.add(eFile.name)
            }
        }
        if (list.isNotEmpty()) {
            binding.info.visibility = View.GONE
        }
        list.sortBy { it }
        adapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e("destroy", "kfslf")
    }

    private fun fetchOnline() {
        val ref = Firebase.database.reference
            .child("Puc-$year Sem-$sem")
            .child(subject).child("Chapters")
        val data = FirebaseQueryLiveData(ref, FirebaseQueryLiveData.singleType)
        data.observe(viewLifecycleOwner) {
            setData(it)
        }
    }

    private fun setData(snapshot: DataSnapshot) {
        if (snapshot.exists()) {
            list.clear()
            for (snap in snapshot.children) {
                snap.getValue<String>()?.let { list.add(it) }
            }
            binding.info.text = resources.getString(R.string.info)
            if (list.isEmpty()) {
                binding.info.visibility = View.VISIBLE
            } else {
                binding.info.visibility = View.GONE
            }
            adapter.notifyDataSetChanged()
            binding.progressCard.visibility = View.GONE
        } else {
            binding.info.visibility = View.VISIBLE
            binding.progressCard.visibility = View.GONE
        }
    }
}