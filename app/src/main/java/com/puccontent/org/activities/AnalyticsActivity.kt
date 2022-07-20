package com.puccontent.org.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.puccontent.org.Models.User
import com.puccontent.org.databinding.ActivityAnalyticsBinding
import java.text.DateFormat
import java.util.*
import kotlin.collections.ArrayList


class AnalyticsActivity : AppCompatActivity() {
    private lateinit var binding:ActivityAnalyticsBinding
    private lateinit var dataBase: FirebaseDatabase
    private val usersList = ArrayList<String>()
    private val cachedList = ArrayList<String>()

    private lateinit var adapter : ArrayAdapter<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        dataBase = Firebase.database
        initViews()
        initData()
    }
    private fun performSearch(text:String){
        val tempList  = cachedList.filter {
            it.contains(text,true)
        }
        usersList.clear()
        usersList.addAll(tempList)
        adapter.notifyDataSetChanged()
        binding.totalUsers.text = "Total Users : ${usersList.size}"
    }
    private fun initViews() {
        adapter = ArrayAdapter(this,android.R.layout.simple_list_item_1,usersList)
        binding.listView.adapter = adapter
        binding.listView.setOnItemClickListener { adapterView, view, i, l ->
            copyToClipboard(usersList[i])
        }
        binding.search.setOnClickListener {
            performSearch(binding.searchBox.editableText.toString().trim())
        }
    }

    private fun initData() {
        dataBase.reference
            .child("Users")
            .addValueEventListener(listener)
    }
    private fun copyToClipboard(text: String){
        val clipboard: ClipboardManager =
            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData = ClipData.newPlainText("token",text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this,"Copied to clipboard",Toast.LENGTH_LONG).show()
    }
    private val listener = object:ValueEventListener{
        override fun onDataChange(snapshot: DataSnapshot) {
            cachedList.clear()
            usersList.clear()
            var counter = 1
             for(item in snapshot.children){
                 var user:User? = null
                 user = try{
                     item.getValue<User>()
                 }catch (e:Exception){
                     null
                 }
1
                 val date = user?.let { Date(it.lastLoginTime) }
                 val dateFormat: DateFormat = DateFormat.getDateTimeInstance(
                     DateFormat.MEDIUM,
                     DateFormat.SHORT,
                     Locale.getDefault()
                 )
                 var time = ""
                 if(date!=null)
                  time =   dateFormat.format(date)
                 var userString = "${counter}) name : ${user?.name} \n email : ${user?.email} \n lastSeen : $time \n token : ${user?.userToken} "

                 if(user==null){
                     userString = item.value.toString()
                 }
                 usersList.add(userString)
                 cachedList.add(userString)

                 counter++
             }
            adapter.notifyDataSetChanged()
        }
        override fun onCancelled(error: DatabaseError) {}
    }
}