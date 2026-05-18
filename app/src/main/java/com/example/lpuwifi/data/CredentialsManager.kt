package com.example.lpuwifi.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property to create DataStore
val Context.dataStore by preferencesDataStore(name = "lpu_credentials")

class CredentialsManager(private val context: Context) {

    companion object {
        val REG_NO_KEY = stringPreferencesKey("reg_no")
        val PASSWORD_KEY = stringPreferencesKey("password")
    }

    // Save credentials
    suspend fun saveCredentials(regNo: String, pass: String) {
        context.dataStore.edit { preferences ->
            preferences[REG_NO_KEY] = regNo
            preferences[PASSWORD_KEY] = pass
        }
    }

    // Read Registration Number (Flow updates automatically)
    val getRegNo: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[REG_NO_KEY]
    }

    // Read Password
    val getPassword: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PASSWORD_KEY]
    }
}