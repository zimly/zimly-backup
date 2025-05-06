package app.zimly.backup.ui.screens.start

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.zimly.backup.data.db.ZimlyDatabase
import app.zimly.backup.data.db.remote.Remote
import app.zimly.backup.data.db.remote.RemoteDao
import app.zimly.backup.data.media.ContentType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class StartViewModelTest {
    private lateinit var db: ZimlyDatabase
    private lateinit var dao: RemoteDao
    private lateinit var viewModel: StartViewModel

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, ZimlyDatabase::class.java
        ).build()

        this.dao = db.remoteDao()
        this.viewModel = StartViewModel(dao)

        runBlocking {
            dao.insert(
                Remote(
                    null,
                    "Test 1",
                    "https://zimly.cloud",
                    "key",
                    "secret",
                    "bucket",
                    null,
                    ContentType.MEDIA,
                    "Pictures"
                )
            )
            dao.insert(
                Remote(
                    null,
                    "Test 2",
                    "https://zimly.cloud",
                    "key",
                    "secret",
                    "bucket",
                    null,
                    ContentType.MEDIA,
                    "Pictures"
                )
            )
        }
    }

    @Test
    fun copy() {
        // GIVEN
        val remotes = runBlocking {
            dao.getAll().first()
        }
        assertThat(remotes.size, `is`(2))

        // WHEN
        viewModel.select(remotes[0].uid!!.toInt())
        runBlocking {
            viewModel.copy()
        }

        // THEN
        runBlocking {
            assertThat(dao.getAll().first().size, `is`(3))
        }
    }

    @Test
    fun delete() {
        // GIVEN
        val remotes = runBlocking {
            dao.getAll().first()
        }
        assertThat(remotes.size, `is`(2))

        // WHEN
        viewModel.select(remotes[0].uid!!.toInt())
        runBlocking {
            viewModel.delete()
        }

        // THEN
        runBlocking {
            assertThat(dao.getAll().first().size, `is`(1))
        }
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

}