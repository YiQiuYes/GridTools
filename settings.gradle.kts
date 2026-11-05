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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    // AGP settings plugin: enables R8 execution profiles below.
    id("com.android.settings") version "9.5.0"
}

// R8 over the large Apache POI jars OOMs with the default in-process heap
// (org.gradle.jvmargs does not reach the separate R8 process), so run it in
// its own JVM with an explicit 8G heap.
android {
    execution {
        profiles {
            create("r8-high-memory") {
                r8 {
                    runInSeparateProcess = true
                    // --sun-misc-unsafe-memory-access=allow: this JVM does not inherit
                    // org.gradle.jvmargs; without it JDK 24+ prints terminal-deprecation
                    // warnings for the protobuf copy shaded inside R8.
                    jvmOptions += listOf(
                        "-Xms4096m",
                        "-Xmx16384m",
                        "-XX:+HeapDumpOnOutOfMemoryError",
                        "--sun-misc-unsafe-memory-access=allow",
                    )
                }
            }
        }
        defaultProfile = "r8-high-memory"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ToolKit"
include(":app")
