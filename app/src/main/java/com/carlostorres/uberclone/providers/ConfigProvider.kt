package com.carlostorres.uberclone.providers

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ConfigProvider {

    val db = Firebase.firestore.collection("Config")

    fun getPrices():Task<DocumentSnapshot>{

        return db.document("price").get().addOnFailureListener { exception ->

            Log.d("Firebase", "error ${exception.message}")

        }

    }

}