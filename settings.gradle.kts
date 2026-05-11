pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "MeshVerse"

include(":app")
include(":features:messaging")
include(":features:social")
include(":features:maps")
include(":features:ai")
include(":features:walkie-talkie")
include(":features:payments")
include(":sdk:mesh-sdk")
include(":sdk:mini-app-sdk")
include(":sdk:plugin-framework")
