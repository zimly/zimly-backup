package io.zeitmaschine.zimzync

import androidx.room.*

@Dao
interface RemoteDao {

    @Query("SELECT * FROM remote")
    suspend fun getAll(): List<Remote>

    @Query("SELECT * FROM remote WHERE uid IN (:remoteIds)")
    suspend fun loadAllByIds(remoteIds: IntArray): List<Remote>

    @Query("SELECT * FROM remote WHERE uid = :remoteId")
    suspend fun loadById(remoteId: Int): Remote

    @Delete
    suspend fun delete(remote: Remote)

    @Update
    suspend fun update(remote: Remote)

    @Insert
    suspend fun insert(remote: Remote)
}