package app.zimly.backup.data.s3

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.MountableFile

/**
 * This test will only pass if the region is configured in `garage.toml` to `s3_region = "us-east-1"`.
 *
 * @ref https://garagehq.deuxfleurs.fr/documentation/connect/cli/#minio-client
 *
 * @see test-container/garage.toml
 */
class GarageIntegrationTest {

    private val bucket = "test-bucket"
    private val region = "garage"
    private val apiKeyName = "test-api-key"
    private val apiKey = "GKd9cde580fadf7f6255e9a3e1"
    private val apiSecret = "cf81ffd289cd56a4ec1fea99e488cfa04537a30fc416769131e14c7237b70348"

    private lateinit var minioRepository: MinioRepository
    private val containerName = "dxflrs/garage:v1.1.0"
    private val s3Port = 3900

    @get:Rule
    val garageContainer: GenericContainer<Nothing> = GenericContainer<Nothing>(containerName)
        .apply {
            withExposedPorts(s3Port)
            withCopyFileToContainer(
                MountableFile.forClasspathResource("/test-container/garage.toml"),
                "/etc/garage.toml"
            )
        }

    @Before
    fun setUp() {

        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        bootstrapGarage()

        val endpoint = "http://${garageContainer.host}:${garageContainer.firstMappedPort}"
        minioRepository = MinioRepository(endpoint, apiKey, apiSecret, bucket, region)
    }

    @Test
    fun put() = runTest {
        val image = "/testdata/test_image.png"
        val stream =
            javaClass.getResourceAsStream(image) ?: throw Error("Could not open test resource.")
        val size = stream.available().toLong()

        val lastProgress =
            minioRepository.put(stream, "testObj", "image/png", size)
                .onEach { println(it) }
                .last()

        assertThat(lastProgress.percentage, `is`(1f))

        val name = minioRepository.get("testObj").`object`()
        assertThat(name, `is`("testObj"))
    }

    /*
     * This is tedious, see https://garagehq.deuxfleurs.fr/documentation/quick-start/ .
     */
    private fun bootstrapGarage() {
        val status = garageContainer.execInContainer("/garage", "status");
        println(status.stdout)
        println(status.stderr)

        // Get node ID
        val node = garageContainer.execInContainer("/garage", "node", "id");
        println(node.stdout)
        println(node.stderr)
        val nodeId = node.stdout.trim()

        // Create layout
        val layout1 = garageContainer.execInContainer(
            "/garage",
            "layout",
            "assign",
            "-z dc1",
            "-c1G",
            nodeId
        );
        println(layout1.stdout)
        println(layout1.stderr)

        // Show layout, the version will be needed next, but hopefully always just 1
        val layout2 = garageContainer.execInContainer("/garage", "layout", "show");
        println(layout2.stdout)
        println(layout2.stderr)

        // Apply layout version 1
        val layout3 =
            garageContainer.execInContainer("/garage", "layout", "apply", "--version", "1");
        println(layout3.stdout)
        println(layout3.stderr)

        // Create bucket
        val bucketout = garageContainer.execInContainer("/garage", "bucket", "create", bucket)
        println(bucketout.stdout)
        println(bucketout.stderr)

        // Import key, otherwise we'd need to parse some random output :sadpanda:
        val keyout = garageContainer.execInContainer(
            "/garage",
            "key",
            "import",
            apiKey,
            apiSecret,
            "-n",
            apiKeyName,
            "--yes"
        )
        println(keyout.stdout)
        println(keyout.stderr)

        // Give permissions for key to bucket
        val allowout = garageContainer.execInContainer(
            "/garage",
            "bucket",
            "allow",
            "--read",
            "--write",
            "--owner",
            bucket,
            "--key",
            apiKeyName
        )
        println(allowout.stdout)
        println(allowout.stderr)
    }


}
