import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.wizpizz.onepluspluslauncher"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.wizpizz.onepluspluslauncher"
        minSdk = 33
        targetSdk = 37
        versionCode = 4
        versionName = "1.2.1"

        buildConfigField("String", "SUPPORTED_LAUNCHER_VERSION", "\"16.6.9\"")
    }

    signingConfigs {
        val environment = System.getenv()
        val dotEnv = rootProject.file(".env")
            .takeIf(File::exists)
            ?.readLines()
            ?.map(String::trim)
            ?.filter { it.isNotEmpty() && !it.startsWith("#") && '=' in it }
            ?.associate { line ->
                val separator = line.indexOf('=')
                val key = line.substring(0, separator).trim()
                val value = line.substring(separator + 1).trim().removeSurrounding("\"").removeSurrounding("'")
                key to value
            }
            .orEmpty()

        fun secret(name: String): String? = environment[name] ?: dotEnv[name]

        val encodedStore = secret("SIGNING_KEY_STORE_BASE64")
        val storePath = when {
            !encodedStore.isNullOrBlank() -> File.createTempFile("onepluspluslauncher", ".keystore").apply {
                writeBytes(Base64.getDecoder().decode(encodedStore))
                deleteOnExit()
            }
            else -> secret("SIGNING_KEY_STORE_PATH")?.takeIf(String::isNotBlank)?.let(::file)
        }
        val alias = secret("SIGNING_KEY_ALIAS")
        val storePasswordValue = secret("SIGNING_KEY_STORE_PASSWORD")
        val keyPasswordValue = secret("SIGNING_KEY_PASSWORD")

        create("release") {
            if (storePath != null && !alias.isNullOrBlank() &&
                !storePasswordValue.isNullOrBlank() && !keyPasswordValue.isNullOrBlank()
            ) {
                storeFile = storePath
                keyAlias = alias
                storePassword = storePasswordValue
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")?.takeIf { it.storeFile != null }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.fuzzywuzzy)

    compileOnly(libs.libxposed.api)
    implementation(libs.libxposed.service)

    testImplementation(libs.junit)
}
