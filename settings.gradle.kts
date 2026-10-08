pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name = "AutoClicker"
include(":core")
if (!providers.gradleProperty("coreOnly").isPresent) include(":app")

if (providers.gradleProperty("androidCheck").isPresent) include(":tools:android-check")
