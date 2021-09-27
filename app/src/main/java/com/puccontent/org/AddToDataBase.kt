package com.puccontent.org

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import com.puccontent.org.Models.Update
import com.puccontent.org.databinding.ActivityAddToDataBaseBinding
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class AddToDataBase : AppCompatActivity() {
    private lateinit var binding: ActivityAddToDataBaseBinding
    private  var chaptersList = ArrayList<String>()
    private lateinit var semesterList:ArrayList<String>
    private lateinit var subjectsList:ArrayList<String>
    private lateinit var chaptersAdapter: ArrayAdapter<String>
    private var subjectNo = 0
    private var semesterNo = 0
    private var counter = 0
    private var checker = true
    private var chapterNo = 0
    private  var handler1:Handler? = null
    private  var handler:Handler? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddToDataBaseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        semesterList = arrayListOf("Puc-1 Sem-1","Puc-1 Sem-2","Puc-2 Sem-1","Puc-2 Sem-2")
        val semesterAdapter = ArrayAdapter(this,android.R.layout.simple_spinner_item,semesterList)
        semesterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.semesterSpinner.adapter = semesterAdapter
        binding.semesterSpinner.onItemSelectedListener = semesterListener
        subjectsList = arrayListOf("IT","Telugu","English","Maths","Physics","Chemistry")
        val subjectsAdapter = ArrayAdapter(this,android.R.layout.simple_spinner_item,subjectsList)
        subjectsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.subjectSpinner.adapter = subjectsAdapter
        binding.subjectSpinner.onItemSelectedListener = subjectListener
        chaptersList.add("-None-")
        chaptersAdapter = ArrayAdapter(this,android.R.layout.simple_spinner_item,chaptersList)
        chaptersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.chapterSpinner.adapter = chaptersAdapter
        binding.chapterSpinner.onItemSelectedListener = chapterListener
        binding.upload.setOnClickListener {
            checkAndUpload()
        }
    }
    private fun checkAndUpload(){
       var flag = true
        if(binding.pdfTitle.editableText.toString().isEmpty()){
            flag = false
            binding.pdfTitle.error = "Enter A Title"
        }
        if(binding.pdfUrl.editableText.toString().isEmpty()){
            flag = false
            binding.pdfUrl.error = "Enter A Url"
        }
        if((chapterNo==0)&& binding.newChapter.editableText.toString().isEmpty()){
            flag = false
            Toast.makeText(this,"Chapter not selected",Toast.LENGTH_LONG).show()
        }
        if(!flag)return
        upload()
    }

    private fun upload() {
        try {
            handler?.removeCallbacksAndMessages(null)
            binding.submitBar.visibility = View.VISIBLE
            val url = getUrl(binding.pdfUrl.editableText.toString().trim())
            val title = binding.pdfTitle.editableText.toString().trim()
            val sem = semesterList[semesterNo]
            val sub = subjectsList[subjectNo]
            val chapter =
                if (chapterNo == 0) {
                    binding.newChapter.editableText.toString().trim()

                } else {
                    chaptersList[binding.chapterSpinner.selectedItemPosition]
                }
            binding.newChapter.setText("")
            binding.pdfTitle.setText("")
            binding.pdfUrl.setText("")
            val database = FirebaseDatabase.getInstance()
            val map = HashMap<String, Any>()
            map[chapter] = chapter
            checker = false
            val upd = Update(Calendar.getInstance().timeInMillis,"Added $title in $sem",null)
            database.reference
                .child("recent")
                .push()
                .setValue(upd)
                .addOnSuccessListener {
                    completed()
                }.addOnFailureListener {
                    binding.submitBar.visibility = View.GONE
                    Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                }
            database.reference
                .child(sem)
                .child(sub)
                .child("Chapters")
                .updateChildren(map).addOnSuccessListener {
                    completed()
                }.addOnFailureListener {
                    binding.submitBar.visibility = View.GONE
                    Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                }
            val map1 = HashMap<String, Any>()
            map1[title] = url
            database.reference
                .child(sem)
                .child(sub)
                .child(chapter)
                .updateChildren(map1).addOnSuccessListener {
                    completed()
                }.addOnFailureListener {
                    binding.submitBar.visibility = View.GONE
                    Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                }
            handler = Handler(Looper.getMainLooper())
            handler?.postDelayed({
                 if(!checker){
                     Toast.makeText(this,"Network Not Available",Toast.LENGTH_LONG).show()
                     checker = true
                 }
            },10000)

        }
        catch(ex:IndexOutOfBoundsException){
            Toast.makeText(this,"Invalid Url",Toast.LENGTH_SHORT).show()
        }
        catch(e:Exception){
            e.printStackTrace()
            Toast.makeText(this,e.message.toString(),Toast.LENGTH_SHORT).show()
        }
    }

    private fun getUrl(trim: String):String{
            val APIKey = "AIzaSyCpn7HmOIq3ddwFB1aFkakNMXKuK0KFbWs"
            val FileID = trim.substring(32, 65)
            return "https://www.googleapis.com/drive/v3/files/${FileID}?alt=media&key=${APIKey}"
    }

    private fun completed(){
        counter++
        if(counter>=3){
            checker = true
            binding.submitBar.visibility = View.GONE
            Toast.makeText(this,"Upload Completed",Toast.LENGTH_LONG).show()
            getChapters()
            counter = 0
        }
    }
    private fun getChapters(){
        handler1?.removeCallbacksAndMessages(null)
        binding.chapterBar.visibility = View.VISIBLE
        val database = FirebaseDatabase.getInstance()
        database.reference
                .child(semesterList[semesterNo])
                .child(subjectsList[subjectNo])
                .child("Chapters")
                .addListenerForSingleValueEvent(listener)
        handler1 = Handler(Looper.getMainLooper())
        handler1?.postDelayed({
                if(binding.chapterBar.visibility == View.VISIBLE){
                     binding.chapterBar.visibility = View.GONE
                     Toast.makeText(this,"Network Not Available",Toast.LENGTH_LONG).show()
                }
        },6000)
    }
    private val listener = object:ValueEventListener{
        override fun onDataChange(snapshot: DataSnapshot) {
            if(snapshot.exists()){
               chaptersList.clear()
                chaptersList.add("-None-")
               for(snap in snapshot.children){
                   snap.getValue<String>()?.let { chaptersList.add(it) }
               }
               chaptersAdapter.notifyDataSetChanged()
                binding.chapterBar.visibility = View.GONE
           }else{
                chaptersList.clear()
                chaptersAdapter.notifyDataSetChanged()
                binding.chapterBar.visibility = View.GONE
            }
        }

        override fun onCancelled(error: DatabaseError) {
            binding.chapterBar.visibility = View.GONE
        }

    }
    private val chapterListener = object:AdapterView.OnItemSelectedListener{
        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
            chapterNo = p2
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {

        }

    }
    private val semesterListener = object:AdapterView.OnItemSelectedListener{
        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                  semesterNo = p2
                 getChapters()
        }
        override fun onNothingSelected(p0: AdapterView<*>?) {

        }
    }
    private val subjectListener = object:AdapterView.OnItemSelectedListener{
        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                subjectNo = p2
            getChapters()
        }
        override fun onNothingSelected(p0: AdapterView<*>?) {

        }
    }

}