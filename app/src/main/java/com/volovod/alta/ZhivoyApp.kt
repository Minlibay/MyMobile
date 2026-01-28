package com.volovod.alta

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.room.Room
import com.volovod.alta.data.AppDatabase
import com.volovod.alta.data.session.SessionStore
import com.volovod.alta.network.ApiClient
import com.volovod.alta.navigation.AltaNavHost

val LocalAppDatabase = staticCompositionLocalOf<AppDatabase> {
    error("AppDatabase is not provided")
}

val LocalSessionStore = staticCompositionLocalOf<SessionStore> {
    error("SessionStore is not provided")
}

private fun provideDatabase(context: Context): AppDatabase {
    return Room.databaseBuilder(context, AppDatabase::class.java, "com.volovod.alta.db")
        .fallbackToDestructiveMigration()
        .build()
}

@Composable
fun AltaApp() {
    val context = LocalContext.current.applicationContext
    val db = provideDatabase(context)
    val sessionStore = remember { SessionStore(context) }
    
    // Инициализируем authApi после создания sessionStore
    val authApi = remember { ApiClient.createAuthApi(sessionStore) }
    sessionStore.setAuthApi(authApi)

    CompositionLocalProvider(
        LocalAppDatabase provides db,
        LocalSessionStore provides sessionStore,
    ) {
        AltaNavHost()
    }
}





