package com.puccontent.org

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.puccontent.org.databinding.ActivitySignInBinding

class SignInActivity : AppCompatActivity() {

    private lateinit var mGoogleSignInClient:GoogleSignInClient
    private lateinit var binding:ActivitySignInBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setTheme(R.style.signInActivity)
        setContentView(binding.root)
        supportActionBar?.hide()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        binding.signInButton.setOnClickListener {
            signIn()
        }

    }
    override fun onStart() {
        super.onStart()
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if(account!=null) {
            updateUI(account,false)
        }
    }
    private fun signIn() {
        val signInIntent = mGoogleSignInClient.signInIntent
        resultLauncher.launch(signInIntent)
    }
    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result->
        val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        binding.pBar.visibility = View.VISIBLE
        handleSignInResult(task)
    }
    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account: GoogleSignInAccount = completedTask.getResult(ApiException::class.java)

            updateUI(account,true)
        } catch (e: ApiException) {
            Log.w("FailedSignIn", "signInResult:failed code=" + e.statusCode)
            updateUI(null,true)
        }
    }

    private fun updateUI(account: GoogleSignInAccount?,isNewUser:Boolean) {
        if(account==null){
            binding.pBar.visibility = View.GONE
           Toast.makeText(this,"Sign In Failed",Toast.LENGTH_SHORT).show()
       }else{
           val email = account.email
           if(email?.endsWith("ac.in",false) == true){
               val intent = Intent(this,MainActivity::class.java)
               binding.pBar.visibility = View.GONE
               intent.putExtra("isNewUser",isNewUser)
               intent.putExtra("email",email)
               startActivity(intent)
               finish()
           }
           else{
               signOut()
               binding.pBar.visibility = View.GONE
               Toast.makeText(this,"Invalid Email",Toast.LENGTH_SHORT).show()
           }
       }
    }
    private fun signOut() {
        mGoogleSignInClient.signOut()
    }
}