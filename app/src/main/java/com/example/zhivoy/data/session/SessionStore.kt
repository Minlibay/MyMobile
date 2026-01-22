package com.example.zhivoy.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "session")

class SessionStore(
    private val context: Context,
) {
    private val KEY_USER_ID = longPreferencesKey("userId")
    private val KEY_ACTIVITY_RECOGNITION_ASKED = booleanPreferencesKey("activityRecognitionAsked")
    private val KEY_POST_NOTIFICATIONS_ASKED = booleanPreferencesKey("postNotificationsAsked")

    val session: Flow<Session?> = context.dataStore.data.map { prefs ->
        val userId = prefs[KEY_USER_ID] ?: return@map null
        Session(userId = userId)
    }

    val activityRecognitionAsked: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACTIVITY_RECOGNITION_ASKED] ?: false
    }

    val postNotificationsAsked: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_POST_NOTIFICATIONS_ASKED] ?: false
    }

    suspend fun setUser(userId: Long) {
        context.dataStore.edit { prefs -> prefs[KEY_USER_ID] = userId }
    }

    suspend fun setActivityRecognitionAsked(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_ACTIVITY_RECOGNITION_ASKED] = value }
    }

    suspend fun setPostNotificationsAsked(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_POST_NOTIFICATIONS_ASKED] = value }
    }

    suspend fun clear() {
        context.dataStore.edit {
            it.remove(KEY_USER_ID)
            it.remove(KEY_ACTIVITY_RECOGNITION_ASKED)
            it.remove(KEY_POST_NOTIFICATIONS_ASKED)
        }
    }
}


