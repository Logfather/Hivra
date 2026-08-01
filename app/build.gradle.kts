plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

android {
    namespace = "de.shopme"
    compileSdk = 35

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    defaultConfig {
        applicationId = "de.shopme"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getProperty("user.home") + "/keystores/shopme/shopme-release-key.keystore")

            storePassword = System.getenv("SHOPME_STORE_PASSWORD")
                ?: project.findProperty("SHOPME_STORE_PASSWORD")?.toString()
                        ?: error("SHOPME_STORE_PASSWORD not found")

            keyAlias = "shopme"

            keyPassword = System.getenv("SHOPME_KEY_PASSWORD")
                ?: project.findProperty("SHOPME_KEY_PASSWORD")?.toString()
                        ?: error("SHOPME_KEY_PASSWORD not found")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
        }
    }

    testOptions {
        unitTests.all {
            it.maxHeapSize =
                System.getenv("OFF_TEST_MAX_HEAP")
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
                    ?: "2g"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {

    implementation(platform(libs.androidx.compose.bom))

    // Compose Core
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.compose.runtime.saveable)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.monitor)


    debugImplementation(libs.androidx.compose.ui.tooling)

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation("androidx.appcompat:appcompat:1.7.0")

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // 🔥 Firebase (SAUBER)
    implementation(platform(libs.firebase.bom))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")

    // JSON
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)


    // Google Play Store SignIn
    //implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.android.gms:play-services-auth:20.6.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.0")
    androidTestImplementation("io.mockk:mockk-android:1.13.5")
    implementation("androidx.compose.animation:animation")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    // WORKER
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("androidx.work:work-runtime-ktx:2.10.1")

    //OPEN FOOD FACTS INTEGRATION
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation(kotlin("test"))

    // APACHE POI
    implementation(libs.apache.poi.ooxml)
}

tasks.withType<Test>().configureEach {
    maxHeapSize = "8g"
}