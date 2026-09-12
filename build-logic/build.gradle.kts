plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(libs.shadow)
    implementation(libs.indra.git)
    implementation(libs.blossom)
    implementation(libs.minotaur)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    // Gradle's embedded Kotlin compiler cannot target Java 25 yet.
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
