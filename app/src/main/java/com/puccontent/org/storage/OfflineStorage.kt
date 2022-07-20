package com.puccontent.org.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class OfflineStorage(){
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var context: Context

    var filesScreenId = ""
        get() = field
        set(value) {
            field = value
            sharedPreferences.edit {
                putString(FilesBannerId,value)
                commit()
            }
        }
     var foldersScreenId = ""
        get() = field
        set(value) {
            field = value
            sharedPreferences.edit {
                putString(FolderBannerId,value)
                commit()
            }
        }
     var nativeAdvancedId = ""
        get() = field
        set(value) {
            field = value
            sharedPreferences.edit {
                putString(NativeAdvancedId,value)
                commit()
            }
        }
     var pdfInterstitialId = ""
        get() = field
        set(value) {
            field = value
            sharedPreferences.edit {
                putString(PdfInterstitial,value)
                commit()
            }
        }
     var appOpenId = ""
        get() = field
        set(value) {
            field = value
            sharedPreferences.edit {
                putString(AppOpenId,value)
                commit()
            }
        }
    var rewardAd = ""
        get() = field
        set(value) {
            field = value
            sharedPreferences.edit{
                putString(RewardAd,value)
                commit()
            }
        }
    var userToken = ""
        get(){
            return sharedPreferences.getString(Token,"").toString()
        }
        set(value) {
            field = value
            sharedPreferences.edit{
                putString(Token,value)
                commit()
            }
        }

    constructor(context: Context) : this() {
        this.context = context
        sharedPreferences = context.getSharedPreferences(PrefId,Context.MODE_PRIVATE)
        filesScreenId = sharedPreferences.getString(FilesBannerId,"")?: fileId
        foldersScreenId = sharedPreferences.getString(FolderBannerId,"")?: foldId
        nativeAdvancedId = sharedPreferences.getString(NativeAdvancedId,"")?: natId
        pdfInterstitialId = sharedPreferences.getString(PdfInterstitial,"")?: pdfIntId
        appOpenId = sharedPreferences.getString(AppOpenId,"")?: appOpId
        rewardAd = sharedPreferences.getString(RewardAd,"")?: rewId
    }

    companion object{
        const val PrefId = "com.puccontent.org"
        const val FilesBannerId = "FilesBanner"
        const val FolderBannerId = "FolderBanner"
        const val NativeAdvancedId  = "NativeAdvanced"
        const val PdfInterstitial = "PdfInterstitial"
        const val AppOpenId = "AppOpen"
        const val RewardAd = "RewardVideoAd"
        const val Token = "userToken"
        private const val fileId  = "ca-app-pub-5198941761547304/2235597781"
        private const val foldId = "ca-app-pub-5198941761547304/3310357375"
        private const val natId = "ca-app-pub-5198941761547304/2104252763"
        private const val pdfIntId = "ca-app-pub-5198941761547304/1105695971"
        private const val appOpId = "ca-app-pub-5198941761547304/4874809776"
        private const val rewId = "ca-app-pub-5198941761547304/4931484249"
    }

}