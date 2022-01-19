package com.puccontent.org.storage

import android.util.Log
import androidx.lifecycle.LiveData
import com.google.firebase.database.*
import com.google.firebase.database.DatabaseReference


class FirebaseQueryLiveData() :
    LiveData<DataSnapshot>() {
    private lateinit var query: Query
    private val listener: MyValueEventListener = MyValueEventListener()
    private var isListened = false
    private var type = defaultType

    constructor(query: Query, type: Int = defaultType) : this() {
        this.query = query
        this.type = type
    }

    constructor(ref: DatabaseReference, type: Int = defaultType) : this() {
        query = ref
        this.type = type
    }

    override fun onActive() {
        if (!isListened) {
            query.addValueEventListener(listener)
        }
    }

    override fun onInactive() {
        query.removeEventListener(listener)
    }

    private inner class MyValueEventListener : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            if (type == defaultType) {
                value = dataSnapshot
            } else if (type == singleType && !isListened) {
                value = dataSnapshot
                isListened = true
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Log.e(
                LOG_TAG,
                "Can't listen to query $query", databaseError.toException()
            )
        }
    }

    companion object {
        private const val LOG_TAG = "FirebaseQueryLiveData"
        const val singleType = 1
        const val defaultType = 0
    }
}