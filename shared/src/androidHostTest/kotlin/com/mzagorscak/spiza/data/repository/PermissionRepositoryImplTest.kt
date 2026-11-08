package com.mzagorscak.spiza.data.repository

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.mzagorscak.spiza.domain.model.PermissionStatus
import com.mzagorscak.spiza.domain.model.PermissionType
import com.mzagorscak.spiza.fakes.FakeCrashLogger
import com.mzagorscak.spiza.fakes.FakePermissionsController
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.camera.CAMERA
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

// Android host test: moko's Android PermissionsController adds bind(ComponentActivity), so it cannot be faked in commonTest.
class PermissionRepositoryImplTest {
    private val controller = FakePermissionsController()
    private val crashLogger = FakeCrashLogger()
    private val repository = PermissionRepositoryImpl(permissionsController = controller, crashLogger = crashLogger)

    @Test
    fun checkPermission_mapsTheCameraStateToTheDomainStatus() =
        runTest {
            val expected = mapOf(
                PermissionState.Granted to PermissionStatus.Granted,
                PermissionState.Denied to PermissionStatus.Denied,
                PermissionState.DeniedAlways to PermissionStatus.DeniedAlways,
                PermissionState.NotGranted to PermissionStatus.NotGranted,
                PermissionState.NotDetermined to PermissionStatus.NotGranted,
            )

            val actual =
                expected.keys.associateWith { state ->
                    controller.state = state
                    repository.checkPermission(PermissionType.CAMERA)
                }

            assertThat(actual).isEqualTo(expected)
            assertThat(controller.checkedPermissions).isEqualTo(List<Permission>(expected.size) { Permission.CAMERA })
        }

    @Test
    fun requestPermission_returnsGranted_whenTheUserGrantsTheCamera() =
        runTest {
            val status = repository.requestPermission(PermissionType.CAMERA)

            assertThat(status).isEqualTo(PermissionStatus.Granted)
            assertThat(controller.providedPermissions).containsExactly(Permission.CAMERA)
        }

    // region Error handling

    @Test
    fun requestPermission_returnsDeniedAlways_whenTheUserDeniedPermanently() =
        runTest {
            controller.provideFailure = DeniedAlwaysException(Permission.CAMERA)

            val status = repository.requestPermission(PermissionType.CAMERA)

            assertThat(status).isEqualTo(PermissionStatus.DeniedAlways)
            assertThat(crashLogger.warnings).isEmpty()
        }

    @Test
    fun requestPermission_returnsTheCurrentStateAndLogsAWarning_whenTheUserDenies() =
        runTest {
            controller.provideFailure = DeniedException(Permission.CAMERA)
            controller.state = PermissionState.Denied

            val status = repository.requestPermission(PermissionType.CAMERA)

            assertThat(status).isEqualTo(PermissionStatus.Denied)
            assertThat(crashLogger.warnings).hasSize(1)
            assertThat(crashLogger.exceptions).isEmpty()
        }

    // endregion
}
