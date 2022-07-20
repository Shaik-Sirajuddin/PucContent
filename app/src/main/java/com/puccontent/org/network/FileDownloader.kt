package com.puccontent.org.network

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import java.io.FileInputStream
import java.io.FileNotFoundException
import kotlin.Exception

class FileDownloader {

    companion object{
        fun downloadFile(context:Context,url: Uri,path:String,name:String): Long {
            try {
                val request = DownloadManager.Request(url)
                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                request.setDestinationInExternalFilesDir(context,null,path+".download")
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
fun Fragment.isConnected():Boolean{
    val connectivityManager:ConnectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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

fun Activity.copyFile(inputPath: String,outputPath: Uri,onComplete:(isDone:Boolean)->Unit) {
    try {
        val input = FileInputStream(inputPath)
        val out = contentResolver.openOutputStream(outputPath)!!
        val buffer = ByteArray(1024)
        var read: Int
        while (input.read(buffer).also { read = it } != -1) {
            out.write(buffer, 0, read)
        }
        input.close()
        out.flush()
        out.close()
        onComplete(true)
    } catch (fnfe1: FileNotFoundException) {
        onComplete(false)
        Log.e("copyFile", fnfe1.message.toString())
    } catch (e: java.lang.Exception) {
        onComplete(false)
        Log.e("copyFile", e.message.toString())
    }
}
fun Activity.showToast(text: String?){
    Toast.makeText(this,text.toString(),Toast.LENGTH_SHORT).show()
}
fun Fragment.showToast(text:String?){
    Toast.makeText(requireContext(),text.toString(),Toast.LENGTH_SHORT).show()
}
