
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.12.3" apply false
    id("com.android.library") version "8.12.3" apply false
    id("org.jetbrains.kotlin.android") version "2.2.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20" apply false
    // https://developer.android.com/build/migrate-to-ksp#add-ksp
    id("com.google.devtools.ksp") version "2.2.20-2.0.3" apply false
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}