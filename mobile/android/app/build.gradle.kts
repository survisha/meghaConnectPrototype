import java.util.Base64
import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("key.properties")
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

fun signingValue(propertyName: String, envName: String): String? =
    (keystoreProperties[propertyName] as String?)?.takeIf { it.isNotBlank() }
        ?: System.getenv(envName)?.takeIf { it.isNotBlank() }

android {
    namespace = "in.gov.meghalaya.meghaconnect"
    compileSdk = 35
    ndkVersion = "27.0.12077973"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    defaultConfig {
        applicationId = "in.gov.meghalaya.meghaconnect"
        minSdk = flutter.minSdkVersion
        targetSdk = 35
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    signingConfigs {
        create("release") {
            val releaseStoreFile = signingValue("storeFile", "ANDROID_KEYSTORE_PATH")
            keyAlias = signingValue("keyAlias", "ANDROID_KEY_ALIAS") ?: ""
            keyPassword = signingValue("keyPassword", "ANDROID_KEY_PASSWORD") ?: ""
            storePassword = signingValue("storePassword", "ANDROID_STORE_PASSWORD") ?: ""
            if (releaseStoreFile != null) {
                storeFile = file(releaseStoreFile)
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

flutter {
    source = "../.."
}

fun decodedDartDefines(): List<String> {
    val raw = project.findProperty("dart-defines")?.toString()?.takeIf { it.isNotBlank() }
        ?: return emptyList()
    return raw.split(",").mapNotNull { encoded ->
        runCatching { String(Base64.getDecoder().decode(encoded)) }.getOrNull()
    }
}

tasks.register("validateProductionRelease") {
    doLast {
        val missingSigningValues = listOf(
            "storeFile" to "ANDROID_KEYSTORE_PATH",
            "keyAlias" to "ANDROID_KEY_ALIAS",
            "keyPassword" to "ANDROID_KEY_PASSWORD",
            "storePassword" to "ANDROID_STORE_PASSWORD",
        ).filter { (propertyName, envName) -> signingValue(propertyName, envName).isNullOrBlank() }

        if (missingSigningValues.isNotEmpty()) {
            throw GradleException(
                "Release signing is not configured. Copy android/key.properties.example to " +
                    "android/key.properties and fill storeFile, keyAlias, keyPassword, and storePassword; " +
                    "or provide CI env vars: " + missingSigningValues.joinToString { it.second }
            )
        }

        val releaseStoreFile = signingValue("storeFile", "ANDROID_KEYSTORE_PATH")
        if (releaseStoreFile != null && !file(releaseStoreFile).exists()) {
            throw GradleException(
                "Release keystore file was not found at '$releaseStoreFile'. " +
                    "Update android/key.properties storeFile or ANDROID_KEYSTORE_PATH."
            )
        }

        val defines = decodedDartDefines()
        val apiBaseUrl = defines.firstOrNull { it.startsWith("MEGHA_API_BASE_URL=") }
            ?.substringAfter("=")
            ?.trim()
        if (apiBaseUrl.isNullOrBlank()) {
            throw GradleException("MEGHA_API_BASE_URL is required for release builds.")
        }
        if (!apiBaseUrl.startsWith("https://")) {
            throw GradleException("MEGHA_API_BASE_URL must use HTTPS for release builds.")
        }
        if (defines.any { it.equals("ENABLE_DEMO_CREDENTIALS=true", ignoreCase = true) }) {
            throw GradleException("ENABLE_DEMO_CREDENTIALS must not be enabled in release builds.")
        }
    }
}

tasks.matching { it.name.contains("Release") && it.name != "validateProductionRelease" }.configureEach {
    dependsOn("validateProductionRelease")
}
