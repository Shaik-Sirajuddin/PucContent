package com.puccontent.org

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import java.lang.Exception

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