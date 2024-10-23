package app.zimly.backup.ui.screens.list

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.zimly.backup.data.remote.Remote
import app.zimly.backup.data.remote.RemoteDao
import app.zimly.backup.data.remote.ZimDatabase
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
class ListViewModelTest {
    private lateinit var db: ZimDatabase
    private lateinit var dao: RemoteDao
    private lateinit var viewModel: ListViewModel

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, ZimDatabase::class.java
        ).build()


        this.dao = db.remoteDao()
        this.viewModel = ListViewModel(dao)

        runBlocking {
            dao.insert(
                Remote(
                    null,
                    "Test 1",
                    "https://zimly.cloud",
                    "key",
                    "secret",
                    "bucket",
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
        assertThat(viewModel.numSelected(), `is`(1))
        runBlocking {
            viewModel.copy()
        }

        // THEN
        assertThat(viewModel.numSelected(), `is`(0))
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
        assertThat(viewModel.numSelected(), `is`(1))
        runBlocking {
            viewModel.delete()
        }

        // THEN
        assertThat(viewModel.numSelected(), `is`(0))
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