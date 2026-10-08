plugins { kotlin("jvm") version "2.1.20" }
kotlin { jvmToolchain(21) }
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach { compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
tasks.withType<JavaCompile>().configureEach { options.release.set(17) }
dependencies { testImplementation("junit:junit:4.13.2") }

// These tests read Android UI sources without requiring an Android SDK.
tasks.withType<Test>().configureEach {
    systemProperty("uiContracts.root", rootProject.projectDir.absolutePath)
    inputs.files(rootProject.fileTree("app/src/main/java") { include("**/*.kt") })
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
