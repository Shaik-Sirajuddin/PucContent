package com.puccontent.org.activities


import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.denzcoskun.imageslider.models.SlideModel
import com.google.android.ads.nativetemplates.TemplateView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.puccontent.org.Adapters.SubjectClicked
import com.puccontent.org.Adapters.UpdateClicked
import com.puccontent.org.Adapters.UpdatesAdapter
import com.puccontent.org.Models.CustomAd
import com.puccontent.org.Models.Update
import com.puccontent.org.databinding.ActivityMainBinding
import com.puccontent.org.storage.FirebaseQueryLiveData
import java.lang.IndexOutOfBoundsException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import com.google.android.ads.nativetemplates.NativeTemplateStyle
import com.google.android.gms.ads.*
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.database.ktx.database
import com.google.firebase.messaging.FirebaseMessaging
import com.puccontent.org.*
import com.puccontent.org.R
import com.puccontent.org.network.*
import com.puccontent.org.storage.OfflineStorage


class MainActivity : AppCompatActivity(), UpdateClicked, SubjectClicked {
    private lateinit var updatesAdapter: UpdatesAdapter
    private lateinit var binding: ActivityMainBinding
    private var email: String = ""
    private var inProgress = false
    private var handler: Handler? = null
    private lateinit var quickAdapter: UpdatesAdapter
    private val quickList = ArrayList<Update>()
    private val updatesList = ArrayList<Update>()
    private lateinit var adLoader: AdLoader
    private lateinit var template: TemplateView
    private var adLoaded = false
    private val ImmediateRequestCode = 1
    private val FlexibleRequestCode = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            setSupportActionBar(binding.toolBar)
            initAds()
            quickAdapter = UpdatesAdapter(this, this, false)
            updatesAdapter = UpdatesAdapter(this, this, true)
            binding.quickAccessList.adapter = quickAdapter
            binding.quickAccessList.layoutManager = LinearLayoutManager(this)
            binding.recentUpdatesList.adapter = updatesAdapter
            binding.recentUpdatesList.layoutManager = LinearLayoutManager(this)
            initUser()
            val contentBox = binding.contentBox
            val aboutBox = binding.aboutBox
            contentBox.subName.text = "Content"
            aboutBox.subName.text = "About"

            contentBox.subName.setBackgroundResource(R.drawable.content_box_back)
            aboutBox.subName.setBackgroundResource(R.drawable.about_box_back)

            contentBox.root.setOnClickListener {
                val intent = Intent(this@MainActivity, ContentActivity::class.java)
                startActivity(intent)
            }
            aboutBox.root.setOnClickListener {
                val intent = Intent(this@MainActivity, AboutActivity::class.java)
                startActivity(intent)
            }
            if (isConnected()) {
                binding.recentPBar.visibility = View.VISIBLE
            } else {
                binding.recentPBar.visibility = View.GONE
                binding.recentHide.visibility = View.VISIBLE
            }
            val recentLiveData =
                FirebaseQueryLiveData(FirebaseDatabase.getInstance().reference.child("recent"))
            recentLiveData.observe(this, {
                updateRecentUpdates(it)
            })
            binding.discordJoin.setOnClickListener {
                launchUrl("https://discord.gg/Gbvz3d8peP")
            }

            binding.discord.setOnClickListener {
                launchUrl("https://discord.gg/Gbvz3d8peP")
            }
            binding.insta.setOnClickListener {
                launchUrl("https://www.instagram.com/sirajuddinb1/")
            }
            binding.twitter.setOnClickListener {
                launchUrl("https://twitter.com/_siraj_uddin_?t=-SDQjn4k3zEayGDYzTG32Q&s=09")
            }
            checkNotification()
            checkAppUpdate()
            FirebaseMessaging.getInstance().token.addOnCompleteListener {
                if (it.isSuccessful){
                    Log.d("token",it.result.toString())
                }
            }
        } catch (e: Exception) {
            Log.e("error", e.message.toString())
            e.printStackTrace()
        }
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
                        ImmediateRequestCode)
                } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.FLEXIBLE,
                        this,
                        FlexibleRequestCode)
                }
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ImmediateRequestCode || requestCode == FlexibleRequestCode) {
            if (resultCode != RESULT_OK) {
               showToast("App update failed")
            }
            else{
                showToast("App updated successfully")
            }
        }
    }
    private fun launchUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun initAds() {
        val storage = OfflineStorage(this)
        val id = storage.nativeAdvancedId
        Firebase.database.reference.child("Ads")
            .child("NativeAd")
            .get()
            .addOnSuccessListener {
                it.getValue<String>()?.let{ itId->
                    storage.nativeAdvancedId = itId
                }
            }
        MobileAds.initialize(this)
        adLoader = AdLoader.Builder(this, id)
            .forNativeAd {
                val styles =
                    NativeTemplateStyle.Builder()
                        .withMainBackgroundColor(ColorDrawable(Color.WHITE)).build()
                template = findViewById(R.id.nativeAdView)
                template.setStyles(styles)
                template.setNativeAd(it)
                adLoaded = true
                if (isDestroyed) {
                    it.destroy()
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(p0: LoadAdError) {
                    super.onAdFailedToLoad(p0)
                    binding.nativeAdView.visibility = View.GONE
                }
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    binding.nativeAdView.visibility = View.VISIBLE
                }
            })
            .build()
        binding.support.setOnClickListener {
            val intent = Intent(this,LeaderBoardActivity::class.java)
            startActivity(intent)
        }
        val adRequest = AdRequest.Builder().build()
        adLoader.loadAds(adRequest, 3)

    }
    private fun initUser() {
        val intent = intent
        val email = intent.getStringExtra("email")
        val isNewUser = intent.getBooleanExtra("isNewUser", false)
        if (isNewUser) {
            if (email != null) {
                for (i in email.indices) {
                    if (email[i] == '@') {
                        addUser(email.substring(0, i), email)
                        break
                    }
                }
            }
        }
    }

    private fun updateRecentUpdates(snapshot: DataSnapshot?) {
        if (snapshot == null || !snapshot.exists()) {
            updatesList.clear()
            updatesAdapter.updateData(updatesList)
            binding.recentPBar.visibility = View.GONE
            binding.recentHide.visibility = View.VISIBLE
            return
        }
        if (snapshot.exists()) {
            updatesList.clear()
            val tempList = ArrayList<Update>()
            for (snap in snapshot.children) {
                val up = snap.getValue<Update>()
                if (up != null) {
                    tempList.add(up)
                }
            }
            var counter = tempList.size - 1
            while (counter >= 0) {
                updatesList.add(tempList[counter])
                counter--
            }
            binding.recentPBar.visibility = View.GONE
            if (updatesList.isNotEmpty()) {
                binding.recentHide.visibility = View.GONE
            } else {
                binding.recentHide.visibility = View.VISIBLE
            }
            updatesAdapter.updateData(updatesList)
        }
    }

    private fun addUser(key: String, email: String) {
        try {
            val database = FirebaseDatabase.getInstance()
            val map = HashMap<String, Any>()
            map[key] = email
            database.reference.child("Users").updateChildren(map)
        } catch (e: java.lang.Exception) {
            Log.e("addUserError", e.message.toString())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
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
            updateQuickAccess()
        } catch (e: Exception) {
            Log.e("rec", e.message.toString())
        }
    }

    private fun updateQuickAccess() {
        val tempList = FileDownloader.convertStringToArray(getQuickAccess())
        quickList.clear()
        tempList.forEach { item ->
            if (!item.isNullOrEmpty()) {
                quickList.add(Update(name = getName(item.toString()), path = item.toString()))
            }
        }
        quickAdapter.updateData(quickList)
        if (quickList.isEmpty()) {
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

    override fun remove(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Remove From QuickAccess")
            .setCancelable(true)
            .setPositiveButton("Yes"
            ) { p0, _ ->
                p0.cancel()
                removeFromQuickAccess(position)
            }
            .setNegativeButton("No") { p0, _ ->
                p0.cancel()
            }
            .show()
    }

    override fun recentUpdateClicked(position: Int) {
        try {
            val itemName = updatesList[position].path
            var sem = 1
            var year = 1
            var subject = ""
            var chapter = ""
            if (itemName != null) {
                var spaceCounter = 0
                for (i in itemName.indices) {
                    if (itemName[i] == '/') {
                        spaceCounter++
                        when (spaceCounter) {
                            1 -> {
                                year = if (itemName[i - 1] == '1') {
                                    1
                                } else {
                                    2
                                }
                            }
                            2 -> {
                                sem = if (itemName[i - 1] == '1') {
                                    1
                                } else {
                                    2
                                }
                            }
                            3 -> {
                                subject = itemName.substring(4, i)
                                chapter = itemName.substring(i + 1)
                            }
                            else -> {

                            }
                        }
                    }
                }
            }
            val intent1 = Intent(this, ContentActivity::class.java)
            intent1.putExtra("subject", subject)
            intent1.putExtra("year", year)
            intent1.putExtra("sem", sem)
            intent1.putExtra("chapter", chapter)
            intent1.putExtra("recent", true)
            startActivity(intent1)
        } catch (ind: IndexOutOfBoundsException) {
            Toast.makeText(this, "Unexpected Error", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("er", e.message.toString())
            Toast.makeText(this, "Unknown Error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeFromQuickAccess(position: Int) {
        var path = quickList[position].path
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
            quickList.removeAt(position)
            quickAdapter.removeItem(position)
            if (quickList.isEmpty()) {
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


    override fun updateClicked(position: Int) {
        try {
            val path = quickList[position].path
            val name = quickList[position].name
            val inte = Intent(this, ReadingActivity::class.java)
            inte.putExtra("file", path)
            inte.putExtra("name", name)
            startActivity(inte)
        } catch (e: Exception) {
            Log.e("updateItemCLickedError", e.message.toString())
            e.printStackTrace()
        }
    }
    override fun subClicked(position: Int) {

    }
}