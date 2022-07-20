package com.puccontent.org.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.puccontent.org.Adapters.LeaderBoardAdapter
import com.puccontent.org.Models.Donor
import com.puccontent.org.databinding.ActivityLeaderBoardBinding
import com.puccontent.org.network.showToast
import com.puccontent.org.storage.OfflineStorage

class LeaderBoardActivity : AppCompatActivity() {
    private var mRewardedAd: RewardedAd? = null
    private val TAG = "LeaderBoardActivity"
    private lateinit var binding: ActivityLeaderBoardBinding
    private var account: GoogleSignInAccount? = null
    private val list = ArrayList<Donor>()
    private lateinit var adapter: LeaderBoardAdapter
    private var points = 0
    private var rank = 1
    var name = ""
    var key = ""
    private var rewardRequested = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLeaderBoardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initAds()
        binding.back.setOnClickListener {
            finish()
        }
        binding.reward.setOnClickListener {
            showAd()
        }
        account = GoogleSignIn.getLastSignedInAccount(this)
        if (account == null) {
            Firebase.auth.signOut()
            Toast.makeText(this, "You haven't Logged In", Toast.LENGTH_LONG).show()
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            finishAffinity()
        }
        adapter = LeaderBoardAdapter(this, list)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        val email = account!!.email.toString()
        val ind = email.indexOf("@")
        key = email.substring(0, ind)
        name = account!!.givenName.toString()
        if (key.contains(".")) {
            showToast("Email contains unsupported characters")
        }
        getData(key)
    }

    private fun getData(key: String) {
        Firebase.database.reference.child("Leaderboard")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(it: DataSnapshot) {
                    list.clear()
                    for (doc in it.children) {
                        val donor = doc.getValue<Donor>()
                        if (donor != null) {
                            list.add(donor)
                            if (doc.key == key) {
                                binding.myPoints.text = donor.points.toString()
                                points = donor.points
                                name = donor.name
                            }
                        }
                    }
                    list.sortByDescending { don ->
                        don.points
                    }
                    getRank()
                    adapter.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {}
            })

    }

    private fun getRank() {
        for (i in list.indices) {
            if (list[i].name == name) {
                rank = i + 1
                break
            }
        }
        binding.myRank.text = rank.toString()
        binding.myPoints.text = points.toString()
    }

    private fun showAd() {
        if (mRewardedAd != null) {
            mRewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Log.d("ad", "Ad was shown.")
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d("ad", "Ad failed to show.")
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.d("ad", "Ad was dismissed.")
                    mRewardedAd = null
                    rewardRequested = false
                    initAds()
                }
            }
            mRewardedAd?.show(this) {
                val rewardAmount = it.amount
                Log.d("amount",rewardAmount.toString())
                updateDonation(rewardAmount)
            }
        } else {
            Log.d(TAG, "The rewarded ad wasn't ready yet.")
            rewardRequested = true
            showToast("Loading ad")
        }
    }

    private fun updateDonation(rewardAmount: Int) {
        points += rewardAmount
        val donor = Donor(name, points)
        Firebase.database.reference
            .child("Leaderboard")
            .child(key)
            .setValue(donor)
            .addOnCompleteListener {
                if(it.isSuccessful){
                    showToast("Reward Claimed")
                }
            }
    }

    private fun initAds() {
        val storage = OfflineStorage(this)
        val adId = storage.rewardAd
        //val adId = "ca-app-pub-3940256099942544/5224354917"
        Firebase.database.reference.child("Ads")
            .child("RewardAd")
            .get()
            .addOnSuccessListener {
                val id = it.getValue<String>()
                id?.let { it1 ->
                    storage.rewardAd = it1
                }
            }
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this,
            adId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError.message)
                    mRewardedAd = null
                    if(adError.code==3){
                        showToast("Ad limit reached please comeback after 10 minutes")
                    }
                }

                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    Log.d(TAG, "Ad was loaded.")
                    mRewardedAd = rewardedAd
                    if(rewardRequested){
                        showAd()
                    }
                }
            })

    }
}