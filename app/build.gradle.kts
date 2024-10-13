plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    compileSdk = 35

    defaultConfig {
        applicationId = "io.zeitmaschine.zimzync"
        minSdk = 29
        targetSdk = 35
        versionCode = 31
        versionName = "1.4.2"

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
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
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

    namespace = "io.zeitmaschine.zimzync"
}

dependencies {
    val composeVersion = "1.7.3"
    val material3Version = "1.3.0"
    val workManagerVersion = "2.9.1"
    val roomVersion = "2.6.1"

    // minio / S3
    implementation("io.minio:minio:8.5.12")

    // needed for okhttp3/minio:
    // Match version from https://github.com/minio/minio-java/blob/master/build.gradle
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.16.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")
    implementation("javax.xml.stream:stax-api:1.0-2")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.compose.material3:material3:$material3Version")
    implementation("androidx.compose.material3:material3-window-size-class:$material3Version")
    implementation("androidx.compose.material:material-icons-extended:$composeVersion")
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.navigation:navigation-compose:2.8.2")
    implementation("com.google.android.material:material:1.12.0")

    // https://developer.android.com/codelabs/android-workmanager#2
    implementation("androidx.work:work-runtime-ktx:$workManagerVersion")

    // room DB
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    // To use Kotlin annotation processing tool (ksp)
    ksp("androidx.room:room-compiler:$roomVersion")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.testcontainers:minio:1.20.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")
    androidTestImplementation("androidx.work:work-testing:2.9.1")
    androidTestImplementation("io.mockk:mockk-android:1.13.13")

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
                else -> includeTestsMatching("*RepositoryTest")
            }
        } else {
            excludeTestsMatching("*RepositoryTest")
        }
    }
}
