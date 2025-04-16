plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "m20.simple.bookkeeping"
    compileSdk = 35

    defaultConfig {
        applicationId = "m20.simple.bookkeeping"
        minSdk = /*24*/26
        targetSdk = 35
        versionCode = 1001
        versionName = "1.0.0 Beta-1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("com.google.android.material:material:1.13.0-alpha12")
    // The view calendar library for Android
    implementation("com.kizitonwose.calendar:view:2.6.2")

    // The compose calendar library for Android
    implementation("com.kizitonwose.calendar:compose:2.6.2")

    implementation("com.google.android.flexbox:flexbox:3.0.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

}