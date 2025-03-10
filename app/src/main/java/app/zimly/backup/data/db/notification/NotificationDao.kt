package app.zimly.backup.data.db.notification

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notification WHERE type = :type")
    suspend fun loadByType(type: NotificationType): Notification?

    @Update
    suspend fun update(notification: Notification)

    @Insert
    suspend fun insert(notification: Notification)
}