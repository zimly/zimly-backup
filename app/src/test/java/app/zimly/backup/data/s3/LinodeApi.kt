package app.zimly.backup.data.s3

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit

private const val LINODE_API = "https://api.linode.com/v4/object-storage"

class LinodeApi(private val token: String) {

    private val json = Json { ignoreUnknownKeys = true }

    fun createBucket(label: String): BucketResponse {

        val timeout = 20L
        val client = OkHttpClient.Builder()
            .connectTimeout(timeout, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS)
            .writeTimeout(timeout, TimeUnit.SECONDS)
            .build()

        val mediaType: MediaType = "application/json".toMediaType()
        val body = """
                {
                  "acl": "private",
                  "cors_enabled": false,
                  "s3_endpoint": "fr-par-1.linodeobjects.com",
                  "label": "$label",
                  "region": "fr-par"
                }
                """.trimIndent().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("$LINODE_API/buckets")
            .post(body)
            .header("Authorization", "Bearer $token")
            .header("accept", "application/json")
            .header("content-type", "application/json")
            .build()

        val response: Response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Key creation failed. Status: ${response.code}, Body: ${response.body?.string()}")
        }
        val resBody = response.body?.string() ?: ""
        val res = json.decodeFromString<BucketResponse>(resBody)
        return res
    }



    fun createKey(label: String, bucket: String): KeyResponse {

        val client = OkHttpClient()

        val mediaType: MediaType = "application/json".toMediaType()
        val body = """
            {
                "bucket_access": [
                  {
                    "bucket_name": "$bucket",
                    "permissions": "read_write",
                    "region": "fr-par"
                  }
                ],
                "label": "$label",
                "regions": [
                  "fr-par"
                ]
            }
        """.trimIndent().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("$LINODE_API/keys")
            .post(body)
            .header("Authorization", "Bearer $token")
            .header("accept", "application/json")
            .header("content-type", "application/json")
            .build()

        val response: Response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Key creation failed. Status: ${response.code}, Body: ${response.body?.string()}")
        }

        val message = response.body?.string() ?: ""
        val res = json.decodeFromString<KeyResponse>(message)
        return res
    }

    fun deleteBucket(region: String, name: String) {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("$LINODE_API/buckets/$region/$name")
            .delete()
            .header("Authorization", "Bearer $token")
            .header("accept", "application/json")
            .build()

        val response: Response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Bucket deletion failed. Status: ${response.code}, Body: ${response.body?.string()}")
        }
    }

    fun deleteKey(keyId: Int) {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("$LINODE_API/keys/$keyId")
            .delete()
            .header("Authorization", "Bearer $token")
            .header("accept", "application/json")
            .build()

        val response: Response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Key deletion failed. Status: ${response.code}, Body: ${response.body?.string()}")
        }
    }

    fun cancelSubscription() {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("$LINODE_API/cancel")
            .post("".toRequestBody())
            .header("Authorization", "Bearer $token")
            .header("accept", "application/json")
            .header("content-type", "application/json")
            .build()

        val response: Response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Key deletion failed. Status: ${response.code}, Body: ${response.body?.string()}")
        }
    }

    @Serializable
    data class KeyResponse(
        val id: Int,
        val label: String,
        @SerialName("access_key") val accessKey: String,
        @SerialName("secret_key") val secretKey: String
    )

    @Serializable
    data class BucketResponse(
        val hostname: String,
        val label: String,
        val region: String,
        @SerialName("s3_endpoint") val s3Endpoint: String
    )
}