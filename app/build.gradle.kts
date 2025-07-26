plugins {

        id("com.android.application")
        id("org.jetbrains.kotlin.android")

}

android {
    namespace = "com.example.documentation"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.documentation"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
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

    packaging {
        resources {
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation ("com.google.code.gson:gson:2.10.1")






    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev20250701-2.0.0")

    implementation("com.google.api-client:google-api-client-android:2.2.0")
    implementation("com.google.api-client:google-api-client-gson:2.2.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
