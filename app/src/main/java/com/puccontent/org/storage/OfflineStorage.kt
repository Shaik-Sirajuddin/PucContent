package com.puccontent.org.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class OfflineStorage(){
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var context: Context

    var adsEnabled = true
        get() = field
        set(value) {
            field = value
            sharedPreferences.edit {
                putBoolean(ADSENABLED,value)
                commit()
            }
        }
    var apiKey = ""
        get() = field
        set(value) {
            field = value
            sharedPreferences.edit {
                putString(API_KEY,value)
                commit()
            }
        }

    var bannerAdId = ""
        get() = field
        set(value) {
            field = value
            sharedPreferences.edit {
                putString(BannerId,value)
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
    var lastOpenHistory = ""
    get(){
        return sharedPreferences.getString(LAST_OPEN,"").toString()
    }
    set(value){
        field  = value
        sharedPreferences.edit {
            putString(LAST_OPEN,value)
            commit()
        }
    }

    var isPdfDark = false
    get(){
        return sharedPreferences.getBoolean(PDF_IS_DARK,false)
    }
    set(value){
        field = value
        sharedPreferences.edit {
            putBoolean(PDF_IS_DARK,value)
            commit()
        }
    }

    constructor(context: Context) : this() {
        this.context = context
        sharedPreferences = context.getSharedPreferences(PrefId,Context.MODE_PRIVATE)
        bannerAdId = sharedPreferences.getString(BannerId,null)?: bannerId
        nativeAdvancedId = sharedPreferences.getString(NativeAdvancedId,null)?: natId
        pdfInterstitialId = sharedPreferences.getString(PdfInterstitial, null)?: pdfIntId
        appOpenId = sharedPreferences.getString(AppOpenId,null)?: appOpId
        rewardAd = sharedPreferences.getString(RewardAd,null)?: rewId
        adsEnabled = sharedPreferences.getBoolean(ADSENABLED,false)
        apiKey  = sharedPreferences.getString(API_KEY,null) ?: ""
    }

    companion object{
        const val API_KEY = "api_key"
        const val ADSENABLED = "ads_enabled"
        const val FBAppOpenAd = "AppOpenAd"
        const val FBContentBannerAd = "ContentActivityBanner"
        const val PrefId = "com.puccontent.org"
        const val BannerId = "Banner_AD"
        const val NativeAdvancedId  = "NativeAdvanced"
        const val PdfInterstitial = "PdfInterstitial"
        const val AppOpenId = "AppOpen"
        const val RewardAd = "RewardVideoAd"
        const val Token = "userToken"
        //history of last page
        const val LAST_OPEN = "last_open"
        //theme of pdf
        const val PDF_IS_DARK = "pdf_theme_is_dark"
        private const val bannerId = "ca-app-pub-3940256099942544/6300978111"
        private const val natId = "ca-app-pub-5198941761547304/2104252763"
        private const val pdfIntId = "ca-app-pub-5198941761547304/1105695971"
        private const val appOpId = "ca-app-pub-3940256099942544/3419835294"
        private const val rewId = "ca-app-pub-5198941761547304/4931484249"
    }

}