package com.puccontent.org

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.browser.customtabs.CustomTabsIntent
import kotlin.Exception

class FileDownloader {

    companion object{
        fun downloadFile(context:Context,url: Uri,path:String,name:String): Long {
            try {
                val request = DownloadManager.Request(url)
                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                request.setDestinationInExternalFilesDir(context,null,path)
                request.setTitle(name)
                request.setDescription("Just a few seconds")
                val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
               return   manager.enqueue(request)
            }catch (e:Exception){
               e.printStackTrace()
                Toast.makeText(context,e.message.toString(),Toast.LENGTH_LONG).show()
            }
            return -1
        }
        fun convertArrayToString(array: ArrayList<String>):String {
            var str = ""
            for (i in array.indices) {
                str += array[i]
                if (i < array.size - 1) {
                    str += ","
                }
            }
            return str
        }
        fun convertStringToArray(str: String): Array<String?> {
            return str.split(",").toTypedArray()
        }
        val fileKey = "thisIsKey"

    }

}

fun Activity.isConnected():Boolean{
    val connectivityManager:ConnectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork) ?: return false
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->    true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->   true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->   true
            else -> false
        }
    }
    else {
        if (connectivityManager.activeNetworkInfo != null && connectivityManager.activeNetworkInfo!!.isConnectedOrConnecting) {
            return true
        }
    }
    return false
}
fun Activity.launchOnlineView(path: String) {
    try {
        val fileId = path.substring(42, 75)
        val url = "https://drive.google.com/file/d/$fileId/view?usp=drivesdk"
        val builder = CustomTabsIntent.Builder()
        builder.setStartAnimations(
            this,
            android.R.anim.slide_in_left,
            android.R.anim.slide_out_right
        );
        builder.setExitAnimations(
            this,
            android.R.anim.slide_out_right,
            android.R.anim.slide_in_left
        );
        val customTabsIntent = builder.build();
        customTabsIntent.launchUrl(this, Uri.parse(url))
    }catch (e:Exception){
        Toast.makeText(this,"Your device doesn't support this action",Toast.LENGTH_SHORT).show()
    }
}
