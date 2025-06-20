//import com.google.protobuf.gradle.GenerateProtoTask
//import com.google.protobuf.gradle.KotlinBuiltIns


plugins {
//    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
//    id("kotlin-kapt")
//    kotlin("jvm") version "2.1.20-RC"
    alias(libs.plugins.google.gms.google.services)
    kotlin("plugin.serialization") version "2.1.21"
}

android {
    namespace = "com.hitsuthar.june"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hitsuthar.june"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }

}

dependencies {
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("androidx.compose.material3:material3-window-size-class:1.3.2")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.4.0-alpha15")
    // https://mvnrepository.com/artifact/androidx.compose.material/material-icons-extended
    implementation("androidx.compose.material:material-icons-extended:1.7.8")


    implementation(libs.libvlc.all)
//    implementation(libs.haze)
//    implementation(libs.haze.materials)
//    implementation(libs.okhttp)
//    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.coil.compose)
    implementation(libs.tmdb.api)
//    implementation (libs.glide)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.jsoup)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
//    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.logging.interceptor)

    // Firebase products
    implementation (libs.firebase.firestore.ktx)
    implementation (libs.firebase.auth.ktx)
    implementation (libs.google.firebase.database.ktx)
    implementation(libs.firebase.database.ktx)
//    implementation(libs.firebase.database)
    implementation(libs.google.firebase.firestore.ktx)
    implementation(libs.google.firebase.auth.ktx)


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}