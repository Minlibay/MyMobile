package com.example.zhivoy.feature.ads

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.zhivoy.ads.AppodealManager
import com.example.zhivoy.data.repository.AdsRepository
import com.example.zhivoy.ui.components.ModernButton

@Composable
fun AdsDebugScreen(
    adsRepository: AdsRepository,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val configState = remember { mutableStateOf<com.example.zhivoy.network.dto.AdsConfigResponse?>(null) }
    val errorState = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        adsRepository.getConfig(network = "appodeal").fold(
            onSuccess = {
                configState.value = it
            },
            onFailure = {
                errorState.value = it.message
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Реклама (Appodeal)",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        val cfg = configState.value
        if (cfg != null) {
            Text(
                text = "enabled=${cfg.appodeal_enabled}, banner=${cfg.appodeal_banner_enabled}, inter=${cfg.appodeal_interstitial_enabled}, rewarded=${cfg.appodeal_rewarded_enabled}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        errorState.value?.let {
            Text(text = "Ошибка: $it", color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(8.dp))

        ModernButton(
            text = "Init Appodeal",
            onClick = {
                val c = cfg
                if (activity != null && c != null && c.appodeal_enabled) {
                    AppodealManager.initializeIfNeeded(
                        activity = activity,
                        appKey = c.appodeal_app_key ?: "",
                        banner = c.appodeal_banner_enabled,
                        interstitial = c.appodeal_interstitial_enabled,
                        rewarded = c.appodeal_rewarded_enabled,
                    )
                }
            },
        )

        ModernButton(
            text = "Show Interstitial",
            onClick = { if (activity != null) AppodealManager.showInterstitial(activity) },
        )

        ModernButton(
            text = "Show Rewarded",
            onClick = { if (activity != null) AppodealManager.showRewarded(activity) },
        )
    }
}
