package com.puccontent.org.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.puccontent.org.databinding.ActivityAboutBinding
import com.puccontent.org.network.launchOnlineView

class AboutActivity : AppCompatActivity() {
    private lateinit var binding:ActivityAboutBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.suggestCard.setOnClickListener {
            suggestion()
        }
        binding.backImage.setOnClickListener {
            finish()
        }
        binding.otherApps.setOnClickListener {
            openDevPage()
        }
        binding.creditBox.setOnClickListener {
           openAnalyticForAdmin()
        }
        binding.github.setOnClickListener {
            launchUrl("https://github.com/Shaik-Sirajuddin")
        }
        binding.myself.setOnClickListener {
            launchUrl("https://www.instagram.com/sirajuddinb1/")
        }
    }
    private fun launchUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
    private fun openAnalyticForAdmin() {
        val database = FirebaseDatabase.getInstance()
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account == null) {
            Firebase.auth.signOut()
            Toast.makeText(this, "You haven't Logged In", Toast.LENGTH_LONG).show()
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            finishAffinity()
            return
        }
        val email = account.email!!.substring(0, 7)
        if(email.contains("n200224")){
            openActivity()
            return
        }
        database.reference.child("Admin").child(email)
            .addListenerForSingleValueEvent(listener)
    }
    private val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.exists()) {
                openActivity()
            }
        }

        override fun onCancelled(p0: DatabaseError) {

        }
    }

    private fun openActivity() {
        val intent = Intent(this,AnalyticsActivity::class.java)
        startActivity(intent)
    }

    private fun openDevPage() {
        val pack = "Sirajuddin"
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://dev?id=$pack")))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/developer?id=${pack}")))
        }
        catch (e:Exception){

        }
    }

    private fun suggestion(){
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("mailto:techx2002@gmail.com"))
            intent.putExtra(Intent.EXTRA_EMAIL, "techx2002@gmail.com")
            intent.putExtra(Intent.EXTRA_SUBJECT, "PucContent - Suggestion")
            startActivity(intent)
        }catch(e:Exception){
            Toast.makeText(this,"Your Device Cannot Perform This Action", Toast.LENGTH_SHORT).show()
        }
    }
}