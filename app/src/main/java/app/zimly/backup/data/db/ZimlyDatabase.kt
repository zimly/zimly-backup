package app.zimly.backup.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RenameColumn
import androidx.room.RenameTable
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import app.zimly.backup.data.db.notification.Notification
import app.zimly.backup.data.db.notification.NotificationDao
import app.zimly.backup.data.db.sync.SyncDao
import app.zimly.backup.data.db.sync.SyncPath
import app.zimly.backup.data.db.sync.SyncProfile

@Database(
    entities = [SyncProfile::class, Notification::class, SyncPath::class],
    version = 9,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = ZimlyDatabase.V3Migration::class),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6, spec = ZimlyDatabase.V6Migration::class),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8, spec = ZimlyDatabase.V8Migration::class),
        AutoMigration(from = 8, to = 9, spec = ZimlyDatabase.V9Migration::class)
    ]
)
abstract class ZimlyDatabase : RoomDatabase() {
    abstract fun syncDao(): SyncDao
    abstract fun notificationDao(): NotificationDao

    @RenameColumn(tableName = "Remote", fromColumnName = "folder", toColumnName = "source_uri")
    class V3Migration : AutoMigrationSpec

    @RenameColumn(
        tableName = "Remote",
        fromColumnName = "source_type",
        toColumnName = "content_type"
    )
    @RenameColumn(tableName = "Remote", fromColumnName = "source_uri", toColumnName = "content_uri")
    class V6Migration : AutoMigrationSpec

    @RenameTable(fromTableName = "Remote", toTableName = "sync_profile")
    // @DeleteTable(tableName = "Remote") TODO
    class V8Migration : AutoMigrationSpec

    class V9Migration : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                    INSERT INTO sync_path (profile_id, path) 
                    SELECT uid, content_uri FROM sync_profile 
                    WHERE content_uri IS NOT NULL
                """.trimIndent()
            )
        }
    }

    companion object {

        @Volatile // ensures thread-safe access to INSTANCE across multiple threads.
        private var INSTANCE: ZimlyDatabase? = null

        fun getInstance(context: Context): ZimlyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZimlyDatabase::class.java,
                    "zim-db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
