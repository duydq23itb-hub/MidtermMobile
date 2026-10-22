package com.mobile.midterm.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.mobile.midterm.data.model.Role
import com.mobile.midterm.data.model.UserProfile

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
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
        role: Role,
        onComplete: (Boolean, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email.trim(), pass.trim())
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: ""
                val userMap = hashMapOf(
                    "email" to email.trim(),
                    "role" to role.name.lowercase()
                )
                firestore.collection("users").document(uid).set(userMap)
                    .addOnCompleteListener {
                        onComplete(true, null)
                    }
            }
            .addOnFailureListener { e ->
                onComplete(false, e.localizedMessage)
            }
    }

    fun fetchUserProfile(onResult: (UserProfile?) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onResult(null)
            return
        }

        firestore.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val roleStr = doc.getString("role")
                val role = if (roleStr != null) {
                    Role.fromString(roleStr)
                } else {
                    // Mặc định: nếu email chứa "admin" thì là ADMIN, ngược lại USER
                    if (user.email?.lowercase()?.contains("admin") == true) Role.ADMIN else Role.USER
                }
                onResult(UserProfile(uid = user.uid, email = user.email ?: "", role = role))
            }
            .addOnFailureListener {
                // Nếu không đọc được bảng users, fallback theo email
                val role = if (user.email?.lowercase()?.contains("admin") == true) Role.ADMIN else Role.USER
                onResult(UserProfile(uid = user.uid, email = user.email ?: "", role = role))
            }
    }

    fun updateRole(newRole: Role, onComplete: (Boolean) -> Unit) {
        val user = auth.currentUser ?: return onComplete(false)
        firestore.collection("users").document(user.uid)
            .set(hashMapOf("email" to (user.email ?: ""), "role" to newRole.name.lowercase()))
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun signOut() {
        auth.signOut()
    }
}
