package app.zimly.backup.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RenameColumn
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import app.zimly.backup.data.db.notification.Notification
import app.zimly.backup.data.db.notification.NotificationDao
import app.zimly.backup.data.db.remote.Remote
import app.zimly.backup.data.db.remote.RemoteDao

@Database(
    entities = [Remote::class, Notification::class],
    version = 5,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = ZimlyDatabase.V3Migration::class),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
    ]
)
abstract class ZimlyDatabase : RoomDatabase() {
    abstract fun remoteDao(): RemoteDao
    abstract fun notificationDao(): NotificationDao

    @RenameColumn(tableName = "Remote", fromColumnName = "folder", toColumnName = "source_uri")
    class V3Migration : AutoMigrationSpec
}
