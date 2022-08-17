package io.zeitmaschine.zimzync

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object RemoteSerializer : Serializer<Remote> {
    override val defaultValue: Remote = Remote.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): Remote {
        try {
            return Remote.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: Remote,
        output: OutputStream
    ) = t.writeTo(output)
}

val Context.remoteDataStore: DataStore<Remote> by dataStore(
    fileName = "remote.pb",
    serializer = RemoteSerializer,
)

