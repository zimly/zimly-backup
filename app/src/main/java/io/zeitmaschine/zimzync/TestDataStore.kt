package io.zeitmaschine.zimzync

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object TestSerializer : Serializer<Test> {
    override val defaultValue: Test = Test.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): Test {
        try {
            return Test.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: Test,
        output: OutputStream
    ) = t.writeTo(output)
}

val Context.testDataStore: DataStore<Test> by dataStore(
    fileName = "test.pb",
    serializer = TestSerializer,
)

