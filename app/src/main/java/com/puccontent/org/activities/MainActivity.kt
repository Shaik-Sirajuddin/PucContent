package com.puccontent.org.activities


import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.setPadding
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.ads.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.auth.ktx.auth
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.puccontent.org.Adapters.SubjectClicked
import com.puccontent.org.Adapters.UpdateClicked
import com.puccontent.org.Adapters.UpdatesAdapter
import com.puccontent.org.Models.Update
import com.puccontent.org.Models.User
import com.puccontent.org.R
import com.puccontent.org.databinding.ActivityMainBinding
import com.puccontent.org.network.*
import com.puccontent.org.storage.OfflineStorage
import com.puccontent.org.util.SwipeGesture
import java.util.*

class MainActivity : AppCompatActivity(), SubjectClicked, UpdateClicked {
    private lateinit var binding: ActivityMainBinding
    private var email: String = ""
    private var inProgress = false
    private var handler: Handler? = null
    private lateinit var quickAccessAdapter: UpdatesAdapter
    private val quickAccessList = ArrayList<Update>()
    private lateinit var adLoader: AdLoader
    private var adLoaded = false
    private val ImmediateRequestCode = 1
    private val FlexibleRequestCode = 1
    private var sharePosition = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            setSupportActionBar(binding.toolBar)
            initAds()
            initRecyclerView()
            getToken()
            initUser()
            initViews()
            checkNotification()
            checkAppUpdate()
            FirebaseMessaging.getInstance().token.addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.d("token", it.result.toString())
                }
            }
        } catch (e: Exception) {
            Log.e("error", e.message.toString())
            e.printStackTrace()
        }
    }

    private fun initRecyclerView() {
        quickAccessAdapter = UpdatesAdapter(this, this)
        binding.quickAccessList.adapter = quickAccessAdapter
        binding.quickAccessList.layoutManager = LinearLayoutManager(this)
        val swipeGesture = object : SwipeGesture(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        removeFromQuickAccess(viewHolder.absoluteAdapterPosition)
                    }
                    ItemTouchHelper.RIGHT -> {
                        shareFile(viewHolder.absoluteAdapterPosition)
                    }
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeGesture)
        itemTouchHelper.attachToRecyclerView(binding.quickAccessList)
    }

    private fun initViews() {
        val contentBox = binding.contentBox
        val aboutBox = binding.aboutBox
        val linksBox = binding.linksBox
        val libraryBox = binding.libraryBox

        contentBox.subName.text = "Content"
        aboutBox.subName.text = "About"
        libraryBox.subName.text = "Library"
        linksBox.subName.text = "Links"

        contentBox.image.setImageResource(R.drawable.content)
        linksBox.image.setImageResource(R.drawable.links)
        libraryBox.image.setImageResource(R.drawable.library)
        aboutBox.image.setImageResource(R.drawable.about)
        contentBox.root.setOnClickListener {
            val intent = Intent(this@MainActivity, ContentActivity::class.java)
            startActivity(intent)
        }
        aboutBox.root.setOnClickListener {
            val intent = Intent(this@MainActivity, AboutActivity::class.java)
            startActivity(intent)
        }

    }

    private fun renderUserDetails() {
        val acct = GoogleSignIn.getLastSignedInAccount(this)
        if (acct != null) {
            val personPhoto = acct.photoUrl
            Glide.with(this)
                .load(personPhoto)
                .placeholder(R.drawable.profile)
                .into(binding.userImage)
        }
        val name = acct?.displayName ?: ""
        binding.name.text = name
    }

    private fun checkNotification() {
        val url = intent.getStringExtra("url")
        url?.let {
            launchUrl(url)
        }
    }

    private fun checkAppUpdate() {
        val appUpdateManager = AppUpdateManagerFactory.create(this)

        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                if ((appUpdateInfo.clientVersionStalenessDays() ?: -1) > 10 &&
                    appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                ) {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.IMMEDIATE,
                        this,
                        ImmediateRequestCode
                    )
                } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.FLEXIBLE,
                        this,
                        FlexibleRequestCode
                    )
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ImmediateRequestCode || requestCode == FlexibleRequestCode) {
            if (resultCode != RESULT_OK) {
                showToast("App update failed")
            } else {
                showToast("App updated successfully")
            }
        }
    }

    private fun launchUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun initAds() {
        val storage = OfflineStorage(this)
        // val id = storage.nativeAdvancedId
        Firebase.database.reference.child("Ads")
            .child("NativeAd")
            .get()
            .addOnSuccessListener {
                it.getValue<String>()?.let { itId ->
                    storage.nativeAdvancedId = itId
                }
            }
        MobileAds.initialize(this)
    }

    private fun initUser() {
        renderUserDetails()
        val intent = intent
        val email = intent.getStringExtra("email")
        val name = intent.getStringExtra("name").toString()
        if (email != null) {
            for (i in email.indices) {
                if (email[i] == '@') {
                    addUser(email.substring(0, i), email, name)
                    break
                }
            }
        }
    }

    private fun addUser(key: String, email: String, name: String) {
        try {
            val token = OfflineStorage(this).userToken
            val database = FirebaseDatabase.getInstance()
            val user = User(
                name,
                email,
                token,
                Date().time
            )
            val map = HashMap<String, Any>()
            map[key] = user
            database.reference.child("Users").updateChildren(map)
        } catch (e: java.lang.Exception) {
            Log.e("addUserError", e.message.toString())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.drop_down_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.addFile -> {
                if (isConnected()) {
                    checkAndAddFile()
                } else {
                    Toast.makeText(this, "Network not available", Toast.LENGTH_SHORT).show()
                }
                return true
            }
            R.id.logOut -> {
                signOut()
                return true
            }
            else -> {
                try {
                    val intent = Intent(this, AboutActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("intentAbout", e.message.toString())
                }
                return true
            }
        }
    }

    private fun signOut() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.clientId))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(this, gso)
        client.signOut().addOnCompleteListener {
            if (it.isSuccessful) {
                Firebase.auth.signOut()
                val intent = Intent(this, SignInActivity::class.java)
                startActivity(intent)
                finishAffinity()
                Toast.makeText(this, "Logout Successful", Toast.LENGTH_SHORT).show()
            } else {
                it.exception?.printStackTrace()
                Toast.makeText(this, it.exception?.message.toString(), Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun onResume() {
        super.onResume()
        try {
            if (sharePosition != -1) {
                quickAccessAdapter.notifyItemChanged(sharePosition)
                sharePosition = -1
            }
            updateQuickAccess()
        } catch (e: Exception) {
            Log.e("rec", e.message.toString())
        }
    }

    override fun openWith(position: Int) {
        try {
            val file =
                this.getExternalFilesDir(quickAccessList[position].path)
            val url = FileProvider.getUriForFile(
                this,
                this.applicationContext?.packageName.toString() + ".provider",
                file!!
            )
            sharePosition = position
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(url, "application/pdf")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        } catch (e: Exception) {
            Firebase.crashlytics.log(e.message.toString())
        }
    }

    private fun updateQuickAccess() {
        val tempList = FileDownloader.convertStringToArray(getQuickAccess())
        quickAccessList.clear()
        tempList.forEach { item ->
            if (!item.isNullOrEmpty()) {
                quickAccessList.add(Update(name = getName(item.toString()), path = item.toString()))
            }
        }
        quickAccessAdapter.updateData(quickAccessList)
        if (quickAccessList.isEmpty()) {
            binding.hideView.visibility = View.VISIBLE
        } else {
            binding.hideView.visibility = View.GONE
        }
    }

    private fun getQuickAccess(): String {
        val sharedPref =
            baseContext?.getSharedPreferences(FileDownloader.fileKey, Context.MODE_PRIVATE)
        return sharedPref?.getString("quick", "").toString()
    }

    private fun getToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("token", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            val storage = OfflineStorage(this)
            storage.userToken = token
            Log.d("token", token)
            initUser()
        }
    }

    private fun getName(str: String): String {
        var lastInd = 0
        for (i in str.indices) {
            if (str[i] == '/') {
                lastInd = i
            }
        }
        if (str.length > lastInd + 1 && str.length - 4 >= 0)
            return str.substring(lastInd + 1, str.length - 4)
        return ""
    }

    private fun checkAndAddFile() {
        try {
            handler?.removeCallbacksAndMessages(null)
            inProgress = true
            binding.mainActivityPBar.visibility = View.VISIBLE
            val database = FirebaseDatabase.getInstance()
            val account = GoogleSignIn.getLastSignedInAccount(this)
            if (account == null) {
                Firebase.auth.signOut()
                Toast.makeText(this, "You haven't Logged In", Toast.LENGTH_LONG).show()
                val intent = Intent(this, SignInActivity::class.java)
                startActivity(intent)
                finishAffinity()
                binding.mainActivityPBar.visibility = View.GONE
                inProgress = false
                return
            }
            email = account.email!!.substring(0, 7)
            database.reference.child("AllowedUsers").child(email)
                .addListenerForSingleValueEvent(listener)
            handler = Handler(Looper.getMainLooper())
            handler?.postDelayed({
                if (binding.mainActivityPBar.visibility == View.VISIBLE) {
                    inProgress = false
                    database.reference.child("AllowedUsers").child(email)
                        .removeEventListener(listener)
                    binding.mainActivityPBar.visibility = View.GONE
                    Toast.makeText(this, "Network Not Available", Toast.LENGTH_LONG).show()
                }
            }, 6000)
        } catch (e: Exception) {
            inProgress = false
            e.printStackTrace()
            Log.e("dd", e.message.toString())
        }
    }

    private val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.exists()) {
                openActivity()
            } else {
                binding.mainActivityPBar.visibility = View.GONE
                Toast.makeText(
                    this@MainActivity,
                    "You Don't Have Rights",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        override fun onCancelled(error: DatabaseError) {
            binding.mainActivityPBar.visibility = View.GONE
            Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_LONG).show()
        }

    }

    fun shareFile(position: Int) {
        sharePosition = position
        val file =
            this.getExternalFilesDir(quickAccessList[position].path)
        val url = FileProvider.getUriForFile(
            this,
            this.applicationContext?.packageName.toString() + ".provider",
            file!!
        )
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "application/pdf"
        intent.putExtra(Intent.EXTRA_STREAM, url)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(
            Intent.createChooser(
                intent,
                "Share " + quickAccessList[position].name + " using ..."
            )
        )

    }

    private fun removeFromQuickAccess(position: Int) {
        var path = quickAccessList[position].path
        val array = FileDownloader.convertStringToArray(getQuickAccess())
        val arr = ArrayList<String>()
        array.forEach {
            if (it != null) {
                arr.add(it)
            }
        }
        arr.remove(path)
        path = FileDownloader.convertArrayToString(arr)
        val sharedPref =
            baseContext?.getSharedPreferences(FileDownloader.fileKey, Context.MODE_PRIVATE)
        if (sharedPref != null) {
            with(sharedPref.edit()) {
                putString("quick", path)
                apply()
            }
            quickAccessList.removeAt(position)
            quickAccessAdapter.removeItem(position)
            if (quickAccessList.isEmpty()) {
                binding.hideView.visibility = View.VISIBLE
            }
        }
    }

    private fun openActivity() {
        if (inProgress && this.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            val intent = Intent(this, AddToDataBase::class.java)
            binding.mainActivityPBar.visibility = View.GONE
            inProgress = false
            startActivity(intent)
        }
    }

    override fun pdfClicked(position: Int) {
        try {
            val path = quickAccessList[position].path
            val name = quickAccessList[position].name
            val inte = Intent(this, ReadingActivity::class.java)
            inte.putExtra("file", path)
            inte.putExtra("name", name)
            startActivity(inte)
        } catch (e: Exception) {
            Log.e("updateItemCLickedError", e.message.toString())
            e.printStackTrace()
        }
    }

    override fun subClicked(position: Int) {}
}