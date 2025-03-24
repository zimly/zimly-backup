package app.zimly.backup.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker.Result.Failure
import androidx.work.ListenableWorker.Result.Success
import androidx.work.testing.TestListenableWorkerBuilder
import app.zimly.backup.data.media.ContentObject
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncWorkerTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun syncSuccess() {
        val syncService: SyncService = mockk()

        // https://stackoverflow.com/a/51794655
        mockkObject(SyncWorker)
        coEvery { SyncWorker.Companion.initSyncService(any(), any())} returns syncService

        val locals = listOf(ContentObject("DCIM01213.png", 1234L, "image/png", mockk()))
        every { syncService.diff() } returns Diff(emptyList(), locals, locals, 1234L)
        every { syncService.sync(any(), any()) } returns flowOf(SyncProgress(1234L, 1, 1f, 1024L))

        val worker = TestListenableWorkerBuilder<SyncWorker>(context)
            .build()
        runBlocking {
            val result = worker.doWork()
            assert(result is Success)
            assertThat(result.outputData.getInt(SyncOutputs.PROGRESS_COUNT, -1), `is`(1))
            assertThat(result.outputData.getFloat(SyncOutputs.PROGRESS_PERCENTAGE, -1f), `is`(1f))
            assertThat(result.outputData.getLong(SyncOutputs.PROGRESS_BYTES, -1), `is`(1234L))
            assertThat(result.outputData.getLong(SyncOutputs.PROGRESS_BYTES_PER_SEC, -1), `is`(1024L))
            assertThat(result.outputData.getLong(SyncOutputs.DIFF_BYTES, -1L), `is`(1234L))
            assertThat(result.outputData.getInt(SyncOutputs.DIFF_COUNT, -1), `is`(1))
        }
    }

    @Test
    fun syncFailure() {
        val syncService: SyncService = mockk()
        mockkObject(SyncWorker)
        coEvery { SyncWorker.Companion.initSyncService(any(), any())} returns syncService

        val locals = listOf(ContentObject("DCIM01213.png", 1234L, "image/png", mockk()))
        every { syncService.diff() } returns Diff(emptyList(), locals, locals, 1234L)
        every { syncService.sync(any(), any()) } returns
                flow {
                    emit(SyncProgress(12L, 0, 0.10f, 1024L))
                    error("fml")
                }

        val worker = TestListenableWorkerBuilder<SyncWorker>(context)
            .build()
        runBlocking {
            val result = worker.doWork()
            assert(result is Failure)
            assertThat(result.outputData.getString(SyncOutputs.ERROR), `is`("fml"))
        }
    }
}
