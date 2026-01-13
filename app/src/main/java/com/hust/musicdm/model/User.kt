package com.hust.musicdm.model

data class User(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)