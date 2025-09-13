package app.zimly.backup.data.db.sync

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncDao {

    @Query("SELECT * FROM sync_profile")
    fun getAll(): Flow<List<SyncProfile>>

    @Query("SELECT * FROM sync_profile WHERE uid IN (:ids)")
    suspend fun loadAllByIds(ids: IntArray): List<SyncProfile>

    @Query("SELECT * FROM sync_profile WHERE uid = :id")
    suspend fun loadById(id: Int): SyncProfile

    @Query("DELETE FROM sync_profile WHERE uid = :id")
    suspend fun deleteById(id: Int);

    @Query("SELECT * FROM sync_path WHERE profileId = :profileId")
    suspend fun loadSyncPathsByProfileId(profileId: Int): List<SyncPath>

    @Update
    suspend fun update(syncProfile: SyncProfile)

    @Insert
    suspend fun insert(syncProfile: SyncProfile)
}