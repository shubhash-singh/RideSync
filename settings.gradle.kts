import java.util.Properties

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

// Load local.properties so we can pass the Mapbox secret token to the Maven repo below.
val localProperties = Properties().apply {
    val f = file("local.properties")
    if (f.exists()) load(f.inputStream())
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        // Mapbox SDK — authenticated private Maven repository.
        // Requires a secret token with DOWNLOADS:READ scope (set in local.properties).
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            authentication {
                create<BasicAuthentication>("basic")
            }
            credentials {
                username = "mapbox"
                // Secret token from local.properties (never committed to VCS).
                password = localProperties.getProperty("MAPBOX_SECRET_TOKEN", "")
            }
        }
    }
}

rootProject.name = "RideSync"
include(":app")
