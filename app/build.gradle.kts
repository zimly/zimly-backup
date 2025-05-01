plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.github.triplet.play") version "3.11.0"
    kotlin("plugin.serialization") version "1.9.22" // Used for API calls in tests
}


android {
    compileSdk = 35

    defaultConfig {
        // This gets hot-patched for Google Play releases.
        applicationId = "app.zimly.backup"
        namespace = "app.zimly.backup"
        minSdk = 29
        targetSdk = 35
        versionCode = 73
        versionName = "2.4.2"

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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,LICENSE.md,LICENSE-notice.md}"
            excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
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

    // https://developer.android.com/develop/ui/compose/bom
    // Not very happy with this, feels like it's not fully thought through, e.g. androidx dependencies.
    val composeBom = platform("androidx.compose:compose-bom:2025.04.00")
    implementation(composeBom)
    testImplementation(composeBom)
    androidTestImplementation(composeBom)
    debugImplementation(platform(composeBom))

    val workManagerVersion = "2.10.0"
    val roomVersion = "2.7.0"
    val lifecycleVersion = "2.8.7"

    // minio / S3
    implementation("io.minio:minio:8.5.17")

    // needed for okhttp3/minio:
    // Match version from https://github.com/minio/minio-java/blob/master/build.gradle
    //noinspection GradleDependency
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.18.2")
    //noinspection GradleDependency
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")
    implementation("javax.xml.stream:stax-api:1.0-2")

    implementation("androidx.core:core-ktx:1.16.0")

    // Managed dependencies
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Pinned, otherwise transient from e.g. work manager
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.navigation:navigation-compose:2.8.9")

    // https://developer.android.com/codelabs/android-workmanager#2
    implementation("androidx.work:work-runtime-ktx:$workManagerVersion")

    // room DB
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    // To use Kotlin annotation processing tool (ksp)
    ksp("androidx.room:room-compiler:$roomVersion")

    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.testcontainers:minio:1.20.6")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("io.mockk:mockk:1.14.0")
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation(kotlin("test"))

    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // https://developer.android.com/jetpack/androidx/releases/test
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.work:work-testing:$workManagerVersion")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("io.mockk:mockk-android:1.14.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

tasks.withType<Test> {

    testLogging.showStandardStreams = true

    filter {
        if (project.hasProperty("integrationTests")) {
            val testProfile = project.property("integrationTests") as String

            when (testProfile) {
                "linode" -> includeTestsMatching("LinodeIntegrationTest")
                "aws" -> includeTestsMatching("AwsIntegrationTest")
                "minio" -> includeTestsMatching("MinioIntegrationTest")
                "garage" -> includeTestsMatching("GarageIntegrationTest")
                else -> includeTestsMatching("*IntegrationTest")
            }
        } else {
            excludeTestsMatching("*IntegrationTest")
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