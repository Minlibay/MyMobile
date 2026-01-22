package com.example.zhivoy.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.zhivoy.feature.auth.LoginScreen
import com.example.zhivoy.feature.auth.RegisterScreen
import com.example.zhivoy.feature.main.MainScreen
import com.example.zhivoy.feature.onboarding.AvatarSetupScreen
import com.example.zhivoy.feature.splash.SplashScreen

@Composable
fun ZhivoyNavHost(
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.Splash,
        modifier = modifier,
    ) {
        composable(Routes.Splash) {
            SplashScreen(
                onGoAuth = {
                    navController.navigate(Routes.Login) {
                        popUpTo(Routes.Splash) { inclusive = true }
                    }
                },
                onGoOnboarding = {
                    navController.navigate(Routes.AvatarSetup) {
                        popUpTo(Routes.Splash) { inclusive = true }
                    }
                },
                onGoMain = {
                    navController.navigate(Routes.Main) {
                        popUpTo(Routes.Splash) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.Login) {
            LoginScreen(
                onGoRegister = { navController.navigate(Routes.Register) },
                onNeedOnboarding = {
                    navController.navigate(Routes.AvatarSetup) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                },
                onLoggedIn = {
                    navController.navigate(Routes.Main) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.Register) {
            RegisterScreen(
                onGoLogin = { navController.popBackStack() },
                onRegistered = {
                    navController.navigate(Routes.AvatarSetup) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.AvatarSetup) {
            AvatarSetupScreen(
                onDone = {
                    navController.navigate(Routes.Main) {
                        popUpTo(Routes.AvatarSetup) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.Main) { MainScreen() }
    }
}


