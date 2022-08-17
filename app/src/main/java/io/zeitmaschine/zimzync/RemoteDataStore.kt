package io.zeitmaschine.zimzync

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object RemotesSerializer : Serializer<Remotes> {
    override val defaultValue: Remotes = Remotes.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): Remotes {
        try {
            return Remotes.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: Remotes,
        output: OutputStream
    ) = t.writeTo(output)
}

val Context.remoteDataStore: DataStore<Remotes> by dataStore(
    fileName = "remote.pb",
    serializer = RemotesSerializer,
)

