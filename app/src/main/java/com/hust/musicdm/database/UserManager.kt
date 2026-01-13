package com.hust.musicdm.database

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.hust.musicdm.model.User
import androidx.core.content.edit

class UserManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "MusicDM_Prefs"
        private const val KEY_USER = "user"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USERS_DB = "users_database"
        private const val KEY_LAST_ACTIVITY = "last_activity_time"
    }

    // Check if user is logged in
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    // Get current user
    fun getCurrentUser(): User? {
        val userJson = prefs.getString(KEY_USER, null) ?: return null
        return try {
            gson.fromJson(userJson, User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Save user after login/signup
    fun saveUser(user: User) {
        prefs.edit().apply {
            putString(KEY_USER, gson.toJson(user))
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis())
            apply()
        }
    }

    // Logout user
    fun logout() {
        prefs.edit().apply {
            remove(KEY_USER)
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_LAST_ACTIVITY)
            apply()
        }
    }

    // Register new user
    fun registerUser(fullName: String, email: String, password: String): Result<User> {
        val usersMap = getUsersDatabase()

        if (usersMap.containsKey(email)) {
            return Result.failure(Exception("Email already registered"))
        }

        val user = User(
            id = java.util.UUID.randomUUID().toString(),
            fullName = fullName,
            email = email
        )

        usersMap[email] = UserCredential(user, password)
        saveUsersDatabase(usersMap)

        return Result.success(user)
    }

    // Login user
    fun loginUser(email: String, password: String): Result<User> {
        val usersMap = getUsersDatabase()

        val userCredential = usersMap[email]
            ?: return Result.failure(Exception("User not found"))

        if (userCredential.password != password) {
            return Result.failure(Exception("Invalid password"))
        }

        return Result.success(userCredential.user)
    }

    // Helper: Get users database
    private fun getUsersDatabase(): MutableMap<String, UserCredential> {
        val json = prefs.getString(KEY_USERS_DB, null)
        return if (json != null) {
            try {
                val type = object : com.google.gson.reflect.TypeToken<MutableMap<String, UserCredential>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                mutableMapOf()
            }
        } else {
            mutableMapOf()
        }
    }

    // Helper: Save users database
    private fun saveUsersDatabase(usersMap: Map<String, UserCredential>) {
        prefs.edit { putString(KEY_USERS_DB, gson.toJson(usersMap)) }
    }

    private data class UserCredential(
        val user: User,
        val password: String
    )
}