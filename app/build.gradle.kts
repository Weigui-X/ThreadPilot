plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.threadpilot.manager"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.threadpilot.manager"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
    }
}

dependencies {
    // Miuix（小米 HyperOS 风格 UI 库）
    implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.4")

    // Compose 基础（由 Miuix 传递引入 foundation 1.12.0，这里只补 setContent 所需）
    implementation("androidx.activity:activity-compose:1.13.0")

    // ViewModel / collectAsStateWithLifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")

    // 图标（纯图标包，不引入 Material 3 主题）
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    implementation("androidx.core:core-ktx:1.17.0")
}
