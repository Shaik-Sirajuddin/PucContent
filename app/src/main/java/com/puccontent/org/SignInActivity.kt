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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.puccontent.org.databinding.ActivitySignInBinding

class SignInActivity : AppCompatActivity() {

    private lateinit var mGoogleSignInClient:GoogleSignInClient
    private lateinit var binding:ActivitySignInBinding
    private lateinit var mAuth:FirebaseAuth
    private var allEnabled = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivitySignInBinding.inflate(layoutInflater)
            setTheme(R.style.signInActivity)
            setContentView(binding.root)
            supportActionBar?.hide()
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.clientId))
                .requestEmail()
                .build()
            mAuth = Firebase.auth
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
            binding.signInButton.setOnClickListener {
                signIn()
            }
        }catch(e:Exception){
            e.printStackTrace()
            Log.e("signIn",e.message.toString())
        }
    }
    override fun onStart() {
        super.onStart()
        val user = mAuth.currentUser
        if(user!=null) {
            updateUI(user,false)
        }
        else{

            val remoteConfig = Firebase.remoteConfig
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 3600
            }
            remoteConfig.setConfigSettingsAsync(configSettings)
            remoteConfig.fetchAndActivate().addOnCompleteListener {
                allEnabled = remoteConfig.getBoolean("allowAllEmails")
            }
        }
    }
    private fun signIn() {
        val signInIntent = mGoogleSignInClient.signInIntent
        resultLauncher.launch(signInIntent)
    }
    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result->
        val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        binding.pBar.visibility = View.VISIBLE
        binding.signInButton.isEnabled = false
        handleSignInResult(task)
    }
    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account: GoogleSignInAccount = completedTask.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Log.w("FailedSignIn", "signInResult:failed code=" + e.statusCode)
            updateUI(null,true)
        }
        catch(e:Exception) {
            Log.e("cras", e.message.toString())
        }
    }
    private fun firebaseAuthWithGoogle(idToken: String) {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = mAuth.currentUser
                        updateUI(user,true)
                    } else {
                        updateUI(null)
                    }
                }
        }catch (e:Exception){
            updateUI(null)
        }
    }
    private fun updateUI(user:FirebaseUser?,isNewUser:Boolean = false) {
        if(user==null){
            mGoogleSignInClient.signOut()
            binding.pBar.visibility = View.GONE
            Toast.makeText(this,"Sign In Failed",Toast.LENGTH_SHORT).show()
        }else{
           val email = user.email

           if(allEnabled || email?.endsWith("ac.in",false) == true){
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
        binding.signInButton.isEnabled = true
    }
    private fun signOut() {
        Firebase.auth.signOut()
        mGoogleSignInClient.signOut()
    }
}