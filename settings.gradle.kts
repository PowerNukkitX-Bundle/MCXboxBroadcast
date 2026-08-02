include(":core")
include(":bootstrap-standalone")
include(":bootstrap-geyser")
include(":bootstrap-pnx")
project(":bootstrap-standalone").projectDir = file("bootstrap/standalone")
project(":bootstrap-geyser").projectDir = file("bootstrap/geyser")
project(":bootstrap-pnx").projectDir = file("bootstrap/pnx")

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    includeBuild("build-logic")
}
