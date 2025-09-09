pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        maven {
            url = uri("https://prove.jfrog.io/artifactory/libs-public-maven/")
            isAllowInsecureProtocol = true
            content {
                includeGroup("com.prove")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
//    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven {
            url = uri("https://prove.jfrog.io/artifactory/libs-public-maven/")
            isAllowInsecureProtocol = true
            content {
                includeGroup("com.prove.sdk")
            }
        }
        mavenCentral {
            content {
                excludeGroup("com.prove.sdk")
            }
        }
    }
}

rootProject.name = "ProvePassiveIdentityAndroidDemo"
include(":app")
 