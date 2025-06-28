// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.gms.google.services) apply false
    // Thêm plugin cho JitPack repository (cho MPAndroidChart)
    id("com.github.ben-manes.versions") version "0.51.0" apply false
}

// Cấu hình cho tất cả projects
//allprojects {
//    repositories {
//        google()
//        mavenCentral()
//        maven { url = uri("https://jitpack.io") } // Cho MPAndroidChart
//        maven { url = uri("https://maven.google.com") } // Backup cho Google Maven
//    }
//}

// Task để clean build directory
tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

// Cấu hình version cho toàn bộ project
ext {
    set("compileSdkVersion", 35)
    set("targetSdkVersion", 35)
    set("minSdkVersion", 26)
    set("buildToolsVersion", "35.0.0")

    // Firebase versions
    set("firebaseBomVersion", "33.1.0")

    // Support library versions
    set("appCompatVersion", "1.7.0")
    set("materialVersion", "1.12.0")
    set("recyclerViewVersion", "1.3.2")
    set("cardViewVersion", "1.0.0")

    // Third-party library versions
    set("picassoVersion", "2.8")
    set("chartVersion", "v3.1.0")
}
