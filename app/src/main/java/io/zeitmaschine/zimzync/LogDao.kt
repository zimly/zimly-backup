package io.zeitmaschine.zimzync

import androidx.room.*

@Dao
interface LogDao {

    @Query("SELECT * FROM log")
    suspend fun getAll(): List<Log>

    @Query("SELECT * FROM log WHERE uid IN (:logIds)")
    suspend fun loadAllByIds(logIds: IntArray): List<Log>

    @Query("SELECT * FROM log WHERE uid = :logId")
    suspend fun loadById(logId: Int): Log

    @Delete
    suspend fun delete(log: Log)

    @Update
    suspend fun update(log: Log)

    @Insert
    suspend fun insert(log: Log)
}