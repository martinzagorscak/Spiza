import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.ksp)
}

kotlin {
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
        }
    }
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

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}
