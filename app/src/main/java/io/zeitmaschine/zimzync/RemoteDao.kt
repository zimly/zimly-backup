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

    @Query("SELECT * FROM remote JOIN log ON remote.uid = log.remote_id WHERE remote.uid = :remoteId")
    fun loadRemoteAndLogs(remoteId: Int): Map<Remote, List<Log>>

    @Query("SELECT CASE WHEN EXISTS (SELECT * FROM remote JOIN log ON remote.uid = log.remote_id WHERE remote.uid = :remoteId AND log.status = 'IN_PROGRESS') THEN CAST(1 AS BIT) ELSE CAST(0 AS BIT) END")
    fun loadRemoteInProgress(remoteId: Int): Boolean

    @Delete
    suspend fun delete(remote: Remote)

    @Update
    suspend fun update(remote: Remote)

    @Insert
    suspend fun insert(remote: Remote)
}