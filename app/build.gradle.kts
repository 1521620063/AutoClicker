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

// Keep AGP's original artifacts and also emit a clearly versioned delivery APK.
android.buildTypes.forEach { buildType ->
    val variantName = buildType.name
    val capitalized = variantName.replaceFirstChar { it.uppercaseChar() }
    val versionedApk = tasks.register<Copy>("versioned${capitalized}Apk") {
        dependsOn("package$capitalized")
        inputs.property("deliveryName", "AutoClicker-${android.defaultConfig.versionName}-${variantName}")
        val apkDirectory = layout.buildDirectory.dir("outputs/apk/$variantName")
        from(apkDirectory) { include("app-$variantName*.apk") }
        into(layout.buildDirectory.dir("outputs/distributions/$variantName"))
        rename { name -> name.replaceFirst("app-", "AutoClicker-${android.defaultConfig.versionName}-") }
    }
    tasks.matching { it.name == "assemble$capitalized" }.configureEach {
        dependsOn(versionedApk)
    }
}
