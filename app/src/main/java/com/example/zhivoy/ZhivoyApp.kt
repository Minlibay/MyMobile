package com.example.zhivoy

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.room.Room
import com.example.zhivoy.data.AppDatabase
import com.example.zhivoy.data.session.SessionStore
import com.example.zhivoy.navigation.ZhivoyNavHost

val LocalAppDatabase = staticCompositionLocalOf<AppDatabase> {
    error("AppDatabase is not provided")
}

val LocalSessionStore = staticCompositionLocalOf<SessionStore> {
    error("SessionStore is not provided")
}

private fun provideDatabase(context: Context): AppDatabase {
    return Room.databaseBuilder(context, AppDatabase::class.java, "zhivoy.db")
        .fallbackToDestructiveMigration()
        .build()
}

@Composable
fun ZhivoyApp() {
    val context = LocalContext.current.applicationContext
    val db = provideDatabase(context)
    val sessionStore = SessionStore(context)

    CompositionLocalProvider(
        LocalAppDatabase provides db,
        LocalSessionStore provides sessionStore,
    ) {
        ZhivoyNavHost()
    }
}





