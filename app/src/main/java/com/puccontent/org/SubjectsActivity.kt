package com.puccontent.org

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.puccontent.org.Adapters.SubjectClicked
import com.puccontent.org.Adapters.SubjectsAdapter
import com.puccontent.org.Models.Subject
import com.puccontent.org.databinding.ActivitySubjectsBinding

class SubjectsActivity : AppCompatActivity(), SubjectClicked {
    private lateinit var binding: ActivitySubjectsBinding
    private val subjectsList = ArrayList<Subject>()
    private lateinit var subjectsAdapter: SubjectsAdapter
    private var year = 1
    private var sem = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubjectsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        subjectsAdapter = SubjectsAdapter(this, subjectsList, this)
        binding.subjectsRecyclerView.adapter = subjectsAdapter
        binding.subjectsRecyclerView.layoutManager = object : GridLayoutManager(this, 2) {
            override fun canScrollVertically(): Boolean = false
        }
        binding.subjectPath.text = "Puc-$year Sem-${sem}"
        binding.backImage.setOnClickListener {
            finish()
        }
        getData()
    }
    private fun getData() {
        val arrayList = ArrayList<Subject>()
        arrayList.add(Subject("IT", R.drawable.it_back))
        arrayList.add(Subject("Telugu", R.drawable.telugu_back))
        arrayList.add(Subject("English", R.drawable.english_back))
        arrayList.add(Subject("Maths", R.drawable.maths_back))
        arrayList.add(Subject("Physics", R.drawable.physics_back))
        arrayList.add(Subject("Chemistry", R.drawable.chemistry_back))
        arrayList.add(Subject("Biology", R.drawable.biology_back))
        arrayList.add(Subject("Others", R.drawable.others_back))
        subjectsList.clear()
        subjectsList.addAll(arrayList)
        subjectsAdapter.notifyDataSetChanged()
    }

    override fun subClicked(position: Int) {
        val intent = Intent(this, ChaptersActivity::class.java)
        intent.putExtra("subject", subjectsList[position].name)
        intent.putExtra("year", year)
        intent.putExtra("sem", sem)
        startActivity(intent)
    }
}