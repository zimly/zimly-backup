package app.zimly.backup.data.remote

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RenameColumn
import androidx.room.RenameTable
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec

@Database(
    entities = [Remote::class],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = ZimDatabase.V3Migration::class),
    ]
)
abstract class ZimDatabase : RoomDatabase() {
    abstract fun remoteDao(): RemoteDao

    @RenameColumn(tableName = "Remote", fromColumnName = "folder", toColumnName = "source_uri")
    class V3Migration : AutoMigrationSpec
}