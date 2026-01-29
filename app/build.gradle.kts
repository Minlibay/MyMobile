plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.volovod.alta"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.volovod.alta"
        minSdk = 28
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // OpenRouter API key:
        // 1) gradle.properties / local.properties: OPENROUTER_API_KEY=...
        // 2) или переменная окружения: OPENROUTER_API_KEY
        val openRouterApiKey: String =
            (project.findProperty("OPENROUTER_API_KEY") as String?)
                ?: System.getenv("OPENROUTER_API_KEY")
                ?: ""

        buildConfigField(
            "String",
            "OPENROUTER_API_KEY",
            "\"${openRouterApiKey}\""
        )
    }

    signingConfigs {
        create("release") {
            storeFile = file("../alta-release-key.keystore")
            storePassword = "122344Aa"
            keyAlias = "alta_key"
            keyPassword = "122344Aa"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.runtime.saveable)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation + DataStore
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    
    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation("io.coil-kt:coil-compose:2.6.0")

    implementation("com.appodeal.ads:sdk:3.12.0.1") {
        exclude(group = "io.bidmachine", module = "ads.networks.pangle")
        exclude(group = "com.pangle.global", module = "ads-sdk")

        // Remove AdMob / Google Mobile Ads
        exclude(group = "com.appodeal.ads.sdk.networks", module = "admob")
        exclude(group = "com.google.android.gms", module = "play-services-ads")
        exclude(group = "com.applovin.mediation", module = "google-adapter")
        exclude(group = "com.applovin.mediation", module = "google-ad-manager-adapter")
        exclude(group = "com.unity3d.ads-mediation", module = "admob-adapter")
    }
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
