package app.zimly.backup.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PermissionServiceTest {

    private lateinit var context: Context
    private lateinit var permissionService: PermissionService

    @Before
    fun setUp() {
        this.context = mockk<Context>()
        this.permissionService = PermissionService(context, "app.zimly.test") {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.ACCESS_MEDIA_LOCATION
            )
        }
    }

    @Test
    fun allGrants() {


        val grants = arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.ACCESS_MEDIA_LOCATION
        ).associateWith { true }

        val result = permissionService.checkUserGrants(grants)

        assertTrue("All grants should have been given", result)
    }

    @Test
    fun missingGrants() {

        val grants = mapOf(
            Pair(Manifest.permission.READ_MEDIA_IMAGES, true),
            Pair(Manifest.permission.READ_MEDIA_VIDEO, true),
            Pair(Manifest.permission.ACCESS_MEDIA_LOCATION, false)
        )

        val result = permissionService.checkUserGrants(grants)
        assertFalse("All grants should have been given", result)
    }

    @Test
    fun permissionsGranted() {
        mockkStatic(ContextCompat::class)

        every {
            ContextCompat.checkSelfPermission(
                any(),
                any()
            )
        } returns PackageManager.PERMISSION_GRANTED

        val permissionService = PermissionService(context, "app.zimly.test")

        val result = permissionService.isPermissionGranted()

        assertTrue("Permissions should be granted.", result)

    }

    @Test
    fun permissionsMissing() {
        mockkStatic(ContextCompat::class)

        every {
            ContextCompat.checkSelfPermission(
                any(),
                any()
            )
        } returns PackageManager.PERMISSION_GRANTED andThen PackageManager.PERMISSION_DENIED andThen PackageManager.PERMISSION_GRANTED

        val permissionService = PermissionService(context, "app.zimly.test")

        val result = permissionService.isPermissionGranted()

        assertFalse("Permissions missing.", result)
    }
}