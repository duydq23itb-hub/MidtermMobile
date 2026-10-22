package com.mobile.midterm.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.mobile.midterm.data.model.UserProfile

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun signIn(
        email: String,
        pass: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email.trim(), pass.trim())
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                onComplete(false, e.localizedMessage)
            }
    }

    fun signUp(
        email: String,
        pass: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email.trim(), pass.trim())
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                onComplete(false, e.localizedMessage)
            }
    }

    fun fetchUserProfile(onResult: (UserProfile?) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onResult(null)
        } else {
            onResult(UserProfile(uid = user.uid, email = user.email ?: ""))
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
