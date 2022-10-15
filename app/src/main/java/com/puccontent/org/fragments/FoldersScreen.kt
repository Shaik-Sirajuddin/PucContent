package com.puccontent.org.fragments


import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.transition.Transition
import android.transition.TransitionSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.puccontent.org.adapters.FoldersAdapter
import com.puccontent.org.R
import com.puccontent.org.databinding.FragmentFoldersScreenBinding
import com.puccontent.org.network.*
import com.puccontent.org.storage.FirebaseQueryLiveData
import com.puccontent.org.storage.MediaStorage
import java.io.File

class FoldersScreen : Fragment() {
    private lateinit var binding: FragmentFoldersScreenBinding
    private val list = ArrayList<String>()
    private var sem: Int = 1
    private var year: Int = 1
    private var subject: String = ""
    private lateinit var adapter: FoldersAdapter
    private var data : FirebaseQueryLiveData? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentFoldersScreenBinding.inflate(inflater)
        val argsData = requireArguments()
        sem = argsData.getInt("sem")
        year = argsData.getInt("year")
        subject = argsData.getString("subject", "Physics")
        adapter = FoldersAdapter(requireContext(), list) { it, _ ->
            if(it >=0 && it < list.size){
                navigateToFilesScreen(it)
            }
        }
        binding.chapterPath.text = subject
        with(binding.chapterPath) {
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
        Handler(Looper.getMainLooper()).postDelayed({
            fetchOffline()
            fetchOnline()
        }, 400)

        return binding.root
    }

    private fun navigateToFilesScreen(pos: Int) {
        val bundle =
            bundleOf("year" to year, "sem" to sem, "subject" to subject, "chapter" to list[pos])
        Navigation
            .findNavController(binding.root)
            .navigate(
                R.id.action_foldersScreen_to_filesScreen,
                bundle,
            )

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
        data = FirebaseQueryLiveData(ref, FirebaseQueryLiveData.singleType)
        data?.observe(viewLifecycleOwner) {
            try{
                setData(it)
            }
            catch (e:Exception){
                FirebaseCrashlytics.getInstance().log(e.message.toString())
                Log.e("FolderScreen:125" , e.message.toString())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        data?.removeObservers(this)
    }
    private fun setData(snapshot: DataSnapshot) {
        if (snapshot.exists()) {
            list.clear()
            for (snap in snapshot.children) {
                snap.getValue<String>()?.let {
                    list.add(it)
                    val file =  context?.getExternalFilesDir("OfflineData/Puc-$year Sem-$sem/$subject/$it")
//                    if(file?.exists() == true){
//                        file.mkdir()
//                    }
                }

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