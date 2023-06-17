package io.zeitmaschine.zimzync

class RemoteRepository(private val remoteDao: RemoteDao, val logDao: LogDao) {

    suspend fun loadById(id: Int): Remote {
        return remoteDao.loadById(id)
    }
}