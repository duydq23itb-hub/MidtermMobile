package com.mobile.midterm.data.model

enum class Role {
    ADMIN,
    USER;

    companion object {
        fun fromString(value: String?): Role {
            return when (value?.trim()?.lowercase()) {
                "admin" -> ADMIN
                else -> USER
            }
        }
    }
}

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val role: Role = Role.USER
)
