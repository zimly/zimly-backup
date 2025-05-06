package app.zimly.backup.ui.screens.sync

import android.content.ContentResolver
import android.util.Log
import androidx.work.WorkManager
import app.zimly.backup.data.media.ContentType
import app.zimly.backup.data.db.remote.Remote
import app.zimly.backup.data.db.remote.RemoteDao
import app.zimly.backup.permission.PermissionService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.hamcrest.CoreMatchers.hasItems
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test


/**
 * Testing StateFlow is tricky. The weird boilerplate around here is taken from:
 * https://github.com/Kotlin/kotlinx.coroutines/issues/3143#issuecomment-1015372848
 *
 * Here's more:
 * https://www.billjings.com/posts/title/testing-stateflow-is-annoying/
 * https://github.com/cashapp/turbine/issues/204
 *
 * I don't think this is how to write ViewModel tests going forward. Instead write similar tests for
 * Compose and unit test the parts of the ViewModel that do not rely on StateFlow.
 */
@Ignore("Outdated, minio client is lazy loaded, replace with Verify button!")
class SyncViewModelTest {

    private lateinit var viewModel: SyncViewModel

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {

        Dispatchers.setMain(StandardTestDispatcher()) // Weird boilerplate for StateFlow tests

        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        val dao = mockk<RemoteDao>()

        coEvery { dao.loadById(eq(12)) } returns Remote(
            12,
            "zimly 1",
            "https://zimly.s3.eu-central-2.amazonaws.com",
            "CTUFC",
            "GVGVZCZIVGKGC",
            "bucket",
            null,
            ContentType.MEDIA,
            "Images"
        )
        val workManager = mockk<WorkManager>()
        every { workManager.getWorkInfosFlow(any()) } returns emptyFlow()
        val contentResolver = mockk<ContentResolver>()
        val permissionService = mockk<PermissionService>()
        this.viewModel = SyncViewModel(dao, 12, workManager, contentResolver, permissionService)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun teardown() {
        Dispatchers.resetMain() // Weird boilerplate for StateFlow tests
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createDiff() = runTest {

        val res = mutableListOf<String?>()

        // https://github.com/Kotlin/kotlinx.coroutines/issues/3143#issuecomment-1015372848
        // https://www.billjings.com/posts/title/testing-stateflow-is-annoying/
        val job = viewModel.error
            .onEach { res.add(it) }
            .launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        runCurrent() // Boilerplate: Sort of flushes pending tasks on the test scheduler
        viewModel.createDiff()
        runCurrent()

        assertThat(res.size, `is`(2))
        assertThat(res, hasItems(null, "Failed to initialize S3 client: invalid Amazon AWS host zimly.s3.eu-central-2.amazonaws.com"))

        job.cancel()
    }
}