import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kover)
    alias(libs.plugins.room)
}

kotlin {
    // Applied explicitly: the manual dependsOn edges of `databaseTest` below would otherwise disable the default template.
    applyDefaultHierarchyTemplate()

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    android {
        namespace = "com.mzagorscak.spiza.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            api(libs.firebase.crashlytics)
            api(libs.koin.android)
            implementation(libs.firebase.auth)
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.config)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.playServicesAuth)
            implementation(libs.googleid)
            implementation(libs.ktor.client.android)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
            implementation(libs.mlkit.barcodeScanning)
            implementation(libs.zxing.core)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.lifecycle.viewmodelNavigation3)
            implementation(libs.navigation3.ui)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kermit)
            implementation(libs.koin.core)
            implementation(libs.multiplatform.settings)
            implementation(libs.koin.compose.viewmodel.navigation)
            implementation(libs.compottie.lite)
            implementation(libs.compose.coil)
            implementation(libs.coil.network.ktor)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.mokoPermissionsCompose)
            implementation(libs.mokoPermissionsCamera)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.assertk)
        }
        // Room tests written once and run against a real database on the iOS simulator and an Android device/emulator
        // (Room's Android build needs a Context, so they cannot run as JVM host tests).
        val databaseTest by creating {
            dependsOn(commonTest.get())
            dependencies {
                implementation(libs.androidx.room.testing)
            }
        }
        iosTest.get().dependsOn(databaseTest)
        getByName("androidDeviceTest") {
            dependsOn(databaseTest)
            dependencies {
                implementation(libs.androidx.test.core)
                implementation(libs.androidx.test.runner)
                implementation(libs.androidx.testExt.junit)
            }
        }
    }
}

// Exported schemas (committed) are what migration tests in `databaseTest` open old database versions from.
room {
    schemaDirectory("$projectDir/schemas")
}

// Migration tests on the simulator read the schemas straight from the project (Android gets them as test APK assets).
// `simctl spawn` forwards SIMCTL_CHILD_* variables to the test process without the prefix.
// The schemas are a task input so that a schema-only change re-runs the tests instead of being up to date.
tasks.withType<KotlinNativeSimulatorTest>().configureEach {
    val schemas = layout.projectDirectory.dir("schemas")
    environment("SIMCTL_CHILD_SPIZA_ROOM_SCHEMA_DIR", schemas.asFile.absolutePath)
    inputs.dir(schemas).withPropertyName("roomSchemas").withPathSensitivity(PathSensitivity.RELATIVE)
}

ktlint {
    ignoreFailures = false
    filter {
        exclude { element ->
            element.file.absolutePath.contains("/build/") || element.file.absolutePath.contains("/generated/")
        }
    }
    reporters {
        reporter(ReporterType.PLAIN)
        reporter(ReporterType.CHECKSTYLE)
        reporter(ReporterType.SARIF)
    }
}

// Coverage of the shared logic measured on the Android host tests (`./gradlew :shared:koverHtmlReport`).
// Excluded: composables, theme, DI and generated code (not unit-tested by design, see .claude/rules/testing.md), and the
// Room-backed classes, which are tested on a real database in `databaseTest` (iOS simulator / Android device) that Kover can't see.
kover {
    reports {
        filters {
            excludes {
                annotatedBy("androidx.compose.runtime.Composable", "androidx.compose.ui.tooling.preview.Preview")
                packages(
                    "com.mzagorscak.spiza.ui.theme",
                    "com.mzagorscak.spiza.navigation.di",
                    "com.mzagorscak.spiza.platformDI",
                    "com.mzagorscak.spiza.data.di",
                    "com.mzagorscak.spiza.domain.di",
                    "com.mzagorscak.spiza.ui.screens.di",
                    "spiza.shared.generated.resources",
                )
                classes(
                    "*_Impl",
                    "*_Impl\$*",
                    "*ComposableSingletons*",
                    "*BuildConfig*",
                    "com.mzagorscak.spiza.data.local.SpizaDatabaseConstructor",
                    "com.mzagorscak.spiza.data.local.RoomLocalDataCleaner*",
                    "com.mzagorscak.spiza.data.loyaltyCard.LoyaltyCardRepositoryImpl*",
                    "com.mzagorscak.spiza.data.suggestion.SuggestionRepositoryImpl*",
                )
            }
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}
