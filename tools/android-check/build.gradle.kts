// API compilation check only. This module cannot generate an APK or validate Android resources.
plugins { kotlin("jvm") version "2.1.20" }
kotlin { jvmToolchain(21); sourceSets.main { kotlin.srcDir("../../app/src/main/java") } }
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach { compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
tasks.withType<JavaCompile>().configureEach { options.release.set(17) }
dependencies { implementation(project(":core")); compileOnly("org.robolectric:android-all:15-robolectric-12650502") }
