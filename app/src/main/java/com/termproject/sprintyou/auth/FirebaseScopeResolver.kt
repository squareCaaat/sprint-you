package com.termproject.sprintyou.auth

import android.content.Context

data class FirebaseScope(
    val root: String,
    val scopeId: String
)

object FirebaseScopeResolver {
    private const val ANONYMOUS_SCOPE = "anonymous"

    fun resolve(@Suppress("UNUSED_PARAMETER") context: Context): FirebaseScope {
        val userId = AuthManager.currentUserId
        val isUserScope = AuthManager.isLoggedIn && !userId.isNullOrBlank()
        return if (isUserScope) {
            FirebaseScope("users", userId)
        } else {
            FirebaseScope("devices", ANONYMOUS_SCOPE)
        }
    }

    fun ownerId(context: Context): String = resolve(context).scopeId
}

