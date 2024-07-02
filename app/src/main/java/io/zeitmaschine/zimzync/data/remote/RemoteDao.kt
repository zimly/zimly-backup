package io.zeitmaschine.zimzync.data.remote

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RemoteDao {

    @Query("SELECT * FROM remote")
    fun getAll(): Flow<List<Remote>>

    @Query("SELECT * FROM remote WHERE uid IN (:remoteIds)")
    suspend fun loadAllByIds(remoteIds: IntArray): List<Remote>

    @Query("SELECT * FROM remote WHERE uid = :remoteId")
    suspend fun loadById(remoteId: Int): Remote

    @Delete
    suspend fun delete(remote: Remote)

    @Query("DELETE FROM remote WHERE uid = :remoteId")
    suspend fun deleteById(remoteId: Int);

    @Update
    suspend fun update(remote: Remote)

    @Insert
    suspend fun insert(remote: Remote)
}