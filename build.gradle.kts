// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    // No need to apply the plugin here, it's applied in the app-level build.gradle.
}

buildscript {
    repositories {
        google()  // Ensure this is included to fetch Firebase and Google services
        mavenCentral()
    }
    dependencies {
        classpath("com.google.gms:google-services:4.3.15")  // Ensure the Google Services plugin is added
    }
}
