plugins { id("com.android.application") version "8.9.2"; kotlin("android") version "2.1.20" }
android {
    namespace = "com.example.autoclicker"
    compileSdk = 35
    defaultConfig { applicationId = "com.example.autoclicker"; minSdk = 24; targetSdk = 35; versionCode = 3; versionName = "1.2.0" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildTypes { release { isMinifyEnabled = false } }
}
dependencies { implementation(project(":core")) }
