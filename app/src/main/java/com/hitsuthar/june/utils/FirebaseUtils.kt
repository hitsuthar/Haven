package com.hitsuthar.june.utils

import android.annotation.SuppressLint
import com.google.firebase.firestore.FirebaseFirestore

// FirebaseUtils.kt
object FirebaseUtils {
  @SuppressLint("StaticFieldLeak")
  val firestore = FirebaseFirestore.getInstance()

  // Collections references
  val roomsCollection = firestore.collection("rooms")
  val usersCollection = firestore.collection("users")
  val moviesCollection = firestore.collection("movies")
}