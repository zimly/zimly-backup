package app.zimly.backup.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RenameColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import app.zimly.backup.data.db.notification.Notification
import app.zimly.backup.data.db.notification.NotificationDao
import app.zimly.backup.data.db.remote.Remote
import app.zimly.backup.data.db.remote.RemoteDao

@Database(
    entities = [Remote::class, Notification::class],
    version = 6,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = ZimlyDatabase.V3Migration::class),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6, spec = ZimlyDatabase.V6Migration::class),
    ]
)
abstract class ZimlyDatabase : RoomDatabase() {
    abstract fun remoteDao(): RemoteDao
    abstract fun notificationDao(): NotificationDao

    @RenameColumn(tableName = "Remote", fromColumnName = "folder", toColumnName = "source_uri")
    class V3Migration : AutoMigrationSpec

    @RenameColumn(tableName = "Remote", fromColumnName = "source_type", toColumnName = "content_type")
    @RenameColumn(tableName = "Remote", fromColumnName = "source_uri", toColumnName = "content_uri")
    class V6Migration : AutoMigrationSpec

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
