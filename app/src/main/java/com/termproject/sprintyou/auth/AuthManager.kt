package com.termproject.sprintyou.auth

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await

object AuthManager {

    private val authOrNull: FirebaseAuth?
        get() = runCatching { Firebase.auth }.getOrNull()

    val isFirebaseReady: Boolean
        get() = authOrNull != null

    val isLoggedIn: Boolean
        get() = authOrNull?.currentUser != null

    val currentUserEmail: String?
        get() = authOrNull?.currentUser?.email

    val currentUserId: String?
        get() = authOrNull?.currentUser?.uid

    suspend fun signIn(email: String, password: String) {
        requireAuth().signInWithEmailAndPassword(email, password).await()
    }

    suspend fun signUp(email: String, password: String) {
        requireAuth().createUserWithEmailAndPassword(email, password).await()
    }

    fun signOut() {
        authOrNull?.signOut()
    }

    private fun requireAuth(): FirebaseAuth {
        return authOrNull
            ?: throw IllegalStateException("Firebase configuration is missing.")
    }
}