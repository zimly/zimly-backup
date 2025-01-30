plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.github.triplet.play") version "3.11.0"
}


android {
    compileSdk = 35

    defaultConfig {
        // This gets hot-patched for Google Play releases.
        applicationId = "app.zimly.backup"
        namespace = "app.zimly.backup"
        minSdk = 29
        targetSdk = 35
        versionCode = 50
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Used for mockk in android tests:
        // https://github.com/mockk/mockk/issues/819#issuecomment-1731796944
        testOptions {
            packaging {
                jniLibs {
                    useLegacyPackaging = true
                }
            }
        }
        vectorDrawables {
            useSupportLibrary = true
        }
        // Room schema
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    buildTypes {
        // To debug minified version, copy this block under a debug {} block
        release {
            ndk {
                debugSymbolLevel = "FULL"
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1,LICENSE.md,LICENSE-notice.md}")
        }
    }

    dependenciesInfo {

        // Enabling this will add a metadata block signed with the Play Store key, preventing
        // re-producible builds in F-Droid:
        // https://gitlab.com/fdroid/fdroiddata/-/merge_requests/16193#note_2194340001

        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Keep it in for the Play Store bundle
        includeInBundle = true
    }
}

play {
    // https://github.com/Triple-T/gradle-play-publisher
    track.set("alpha")
    defaultToAppBundles.set(true)
}

dependencies {
    val composeVersion = "1.7.6"
    val material3Version = "1.3.1"
    val workManagerVersion = "2.10.0"
    val roomVersion = "2.6.1"
    val lifecycleVersion = "2.8.7"

    // minio / S3
    implementation("io.minio:minio:8.5.15")

    // needed for okhttp3/minio:
    // Match version from https://github.com/minio/minio-java/blob/master/build.gradle
    //noinspection GradleDependency
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.18.0")
    //noinspection GradleDependency
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.0")
    implementation("javax.xml.stream:stax-api:1.0-2")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.compose.material3:material3:$material3Version")
    implementation("androidx.compose.material3:material3-window-size-class:$material3Version")
    implementation("androidx.compose.material:material-icons-extended:$composeVersion")
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("com.google.android.material:material:1.12.0")

    // https://developer.android.com/codelabs/android-workmanager#2
    implementation("androidx.work:work-runtime-ktx:$workManagerVersion")

    // room DB
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    // To use Kotlin annotation processing tool (ksp)
    ksp("androidx.room:room-compiler:$roomVersion")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.testcontainers:minio:1.20.4")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("io.mockk:mockk:1.13.16")
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")
    androidTestImplementation("androidx.work:work-testing:$workManagerVersion")
    androidTestImplementation("io.mockk:mockk-android:1.13.16")

    debugImplementation("androidx.compose.ui:ui-tooling:$composeVersion")
    debugImplementation("androidx.compose.ui:ui-test-manifest:$composeVersion")
}

tasks.withType<Test> {

    testLogging.showStandardStreams = true

    filter {
        if (project.hasProperty("integrationTests")) {
            val testProfile = project.property("integrationTests") as String

            when (testProfile) {
                "linode" -> includeTestsMatching("LinodeRepositoryTest")
                "aws" -> includeTestsMatching("AwsRepositoryTest")
                "minio" -> includeTestsMatching("MinioRepositoryTest")
                "garage" -> includeTestsMatching("GarageRepositoryTest")
                else -> includeTestsMatching("*RepositoryTest")
            }
        } else {
            excludeTestsMatching("*RepositoryTest")
        }
    }
}

/**
 * Prints value of property to stdout.
 *
 * Usage: ./gradlew -q getVersion
 */
tasks.register("getVersion") {
    val version = "v${android.defaultConfig.versionName}"
    println(version)
}