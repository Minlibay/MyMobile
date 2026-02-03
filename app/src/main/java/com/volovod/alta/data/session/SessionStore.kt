package com.volovod.alta.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.volovod.alta.network.api.AuthApi
import com.volovod.alta.network.dto.RefreshRequest
import com.volovod.alta.network.dto.TokenPair
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "session")

class SessionStore(
    private val context: Context
) {
    private var authApi: AuthApi? = null

    fun setAuthApi(api: AuthApi) {
        this.authApi = api
    }
    private val KEY_USER_ID = longPreferencesKey("userId")
    private val KEY_ACCESS_TOKEN = stringPreferencesKey("accessToken")
    private val KEY_REFRESH_TOKEN = stringPreferencesKey("refreshToken")
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

    suspend fun setTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun getAccessToken(): String? {
        return context.dataStore.data.map { it[KEY_ACCESS_TOKEN] }.first()
    }

    suspend fun getRefreshToken(): String? {
        return context.dataStore.data.map { it[KEY_REFRESH_TOKEN] }.first()
    }

    suspend fun refreshToken(): TokenPair? {
        val refreshToken = getRefreshToken() ?: return null
        if (authApi == null) return null

        return try {
            val deviceId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
            // Используем отдельный API клиент без интерцептора для refresh, чтобы избежать циклической зависимости
            val refreshApi = com.volovod.alta.network.ApiClient.createAuthApiWithoutInterceptor()
            val response = refreshApi.refresh(RefreshRequest(refresh_token = refreshToken, device_id = deviceId))
            setTokens(response.access_token, response.refresh_token)
            response
        } catch (e: Exception) {
            null
        }
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
            it.remove(KEY_ACCESS_TOKEN)
            it.remove(KEY_REFRESH_TOKEN)
            it.remove(KEY_ACTIVITY_RECOGNITION_ASKED)
            it.remove(KEY_POST_NOTIFICATIONS_ASKED)
        }
    }
}


