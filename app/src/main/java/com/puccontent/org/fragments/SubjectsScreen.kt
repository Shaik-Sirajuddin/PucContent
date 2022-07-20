package com.puccontent.org.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import com.puccontent.org.Adapters.SubjectClicked
import com.puccontent.org.Adapters.SubjectsAdapter
import com.puccontent.org.Models.Constants
import com.puccontent.org.Models.Subject
import com.puccontent.org.R
import com.puccontent.org.databinding.FragmentSubjectsScreenBinding
import com.puccontent.org.network.*
import android.annotation.SuppressLint
import android.util.Log
import android.widget.Filter
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.puccontent.org.util.SwipeGesture


class SubjectsScreen : Fragment(), SubjectClicked {
    private val subjectsList = ArrayList<Subject>()
    private lateinit var subjectsAdapter: SubjectsAdapter
    private var year = 1
    private var sem = 1
    private lateinit var binding: FragmentSubjectsScreenBinding
    private lateinit var data: SharedPreferences
    private lateinit var semestersList: Array<String>
    private lateinit var semestersAdapter: ArrayAdapter<String>
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSubjectsScreenBinding.inflate(inflater)
        initViews()
        getCurrentSemester()
        setUpAutoCompleteTextView()
        getData()
        return binding.root
    }
    private fun updateOfflineData() {
        data.edit {
            putInt(Constants.year, year)
            putInt(Constants.sem, sem)
        }
    }
    private fun initViews(){
        subjectsAdapter = SubjectsAdapter(requireContext(), subjectsList, this)
        binding.subjectsRecyclerView.adapter = subjectsAdapter
        binding.subjectsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        semestersList = resources.getStringArray(R.array.semesters)
        semestersAdapter =  MyAdapter(requireContext(), android.R.layout.simple_list_item_1, semestersList)
        binding.backImage.setOnClickListener {
            requireActivity().finish()
        }
    }
    private fun getCurrentSemester(){
        data = requireContext().getSharedPreferences(FileDownloader.fileKey, Context.MODE_PRIVATE)
        sem = data.getInt(Constants.sem, 1)
        year = data.getInt(Constants.year, 1)
    }
    private fun setUpAutoCompleteTextView(){
        //Setting selection from offline
        binding.autoCompleteTextView.setText("Puc-$year Sem-$sem", false)
        //AutoComplete TextView
        binding.autoCompleteTextView.setAdapter(semestersAdapter)
        binding.autoCompleteTextView.setOnItemClickListener { _, _, i, _ ->
            when (i) {
                0 -> {
                    year = 1
                    sem = 1
                }
                1 -> {
                    year = 1
                    sem = 2
                }
                2 -> {
                    year = 2
                    sem = 1
                }
                else -> {
                    year = 2
                    sem = 2
                }
            }
            updateOfflineData()
        }
    }
    private fun getData() {
        val arrayList = ArrayList<Subject>()
        arrayList.add(Subject("Telugu",R.drawable.teulgu, R.drawable.telugu_back))
        arrayList.add(Subject("IT",R.drawable.it, R.drawable.it_back))
        arrayList.add(Subject("English",R.drawable.english, R.drawable.english_back))
        arrayList.add(Subject("Maths",R.drawable.maths, R.drawable.maths_back))
        arrayList.add(Subject("Physics",R.drawable.physics,R.drawable.physics_back))
        arrayList.add(Subject("Chemistry",R.drawable.chemistry, R.drawable.chemistry_back))
        arrayList.add(Subject("Biology",R.drawable.biology, R.drawable.biology_back))
        arrayList.add(Subject("Others",R.drawable.others, R.drawable.others_back))
        subjectsList.clear()
        subjectsList.addAll(arrayList)
        subjectsAdapter.notifyDataSetChanged()
    }

    override fun subClicked(position: Int) {
        val bundle =
            bundleOf("year" to year, "sem" to sem, "subject" to subjectsList[position].name)
        Navigation
            .findNavController(binding.root)
            .navigate(R.id.action_subjectsScreen_to_foldersScreen, bundle)
    }
    inner class MyAdapter(context: Context, resource: Int, objects: Array<String>) :
        ArrayAdapter<String>(context, resource, objects) {
        override fun getFilter(): Filter {
            return object:Filter(){
                override fun performFiltering(p0: CharSequence?): FilterResults {
                    return FilterResults()
                }
                override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
                }
            }
        }
    }
}