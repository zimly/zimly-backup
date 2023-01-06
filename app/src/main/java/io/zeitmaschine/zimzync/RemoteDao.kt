package io.zeitmaschine.zimzync

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query

@Dao
interface RemoteDao {

    @Query("SELECT * FROM remote")
    fun getAll(): List<Remote>

    @Query("SELECT * FROM remote WHERE uid IN (:remoteIds)")
    fun loadAllByIds(remoteIds: IntArray): List<Remote>

    @Query("SELECT * FROM remote WHERE uid = :remoteId")
    fun loadById(remoteId: Int): Remote

    @Delete
    fun delete(remote: Remote)
}