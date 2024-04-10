package com.carlostorres.uberclone.providers

import android.util.Log
import com.carlostorres.uberclone.models.Booking
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class BookingProvider {

    val db = Firebase.firestore.collection("Bookings")

    val authProvider = AuthProvider()

    fun create ( booking : Booking): Task<Void> {

        return db.document(authProvider.getId()).set(booking).addOnFailureListener {

            Log.d("FIRESTORE", "Error: ${it.message}")

        }

    }

    fun getBooking(): DocumentReference {

        return db.document(authProvider.getId())

    }

    fun remove(): Task<Void> {

        return db.document(authProvider.getId()).delete().addOnFailureListener { exception ->

            Log.d("Firestore", "${exception.message}")

        }

    }

}