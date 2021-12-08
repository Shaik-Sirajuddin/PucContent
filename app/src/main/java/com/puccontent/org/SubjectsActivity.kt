package com.puccontent.org

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.puccontent.org.Adapters.SubjectClicked
import com.puccontent.org.Adapters.SubjectsAdapter
import com.puccontent.org.Models.Subject
import com.puccontent.org.databinding.ActivitySubjectsBinding

class SubjectsActivity : AppCompatActivity(), SubjectClicked {
    private lateinit var binding:ActivitySubjectsBinding
    private lateinit var list:ArrayList<Subject>
    private var year = 1
    private var sem = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubjectsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val intent = intent
        year = intent.getIntExtra("year",1)
        sem = intent.getIntExtra("sem",1)
       // list = getData()
        binding.subjectsRecyclerView.adapter = SubjectsAdapter(this,list,this)
        binding.subjectsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.subjectPath.text = "Puc-$year Sem-${sem}"
        binding.backImage.setOnClickListener {
            finish()
        }
    }
    override fun subClicked(position: Int) {
          val intent = Intent(this,ChaptersActivity::class.java)
           intent.putExtra("subject",list[position].name)
           intent.putExtra("year",year)
           intent.putExtra("sem",sem)
           startActivity(intent)
    }
}