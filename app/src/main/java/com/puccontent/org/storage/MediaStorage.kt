package com.puccontent.org.storage

import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.Toast
import java.io.File

class MediaStorage {
    companion object{

    }
    fun getRootDirectory(context: Context)
    {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),"")
        Log.d("files",file.absolutePath)
        Log.d("files","File is directory : "+file.isDirectory)
        Log.d("files","File exists : " + file.exists())
    }
}