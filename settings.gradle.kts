pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://artifactory.appodeal.com/appodeal-public/") }
        maven { url = uri("https://artifact.bytedance.com/repository/pangle") }
        maven { url = uri("https://maven.mytracker.ru") }
        maven { url = uri("https://maven.mytracker.ru/repository/mytracker-android-sdk/") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://artifactory.appodeal.com/appodeal-public/") }
        maven { url = uri("https://artifact.bytedance.com/repository/pangle") }
        maven { url = uri("https://maven.mytracker.ru") }
        maven { url = uri("https://maven.mytracker.ru/repository/mytracker-android-sdk/") }
    }
}

rootProject.name = "Alta"
include(":app")