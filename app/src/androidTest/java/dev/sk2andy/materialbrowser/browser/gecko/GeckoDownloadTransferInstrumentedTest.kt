package dev.sk2andy.materialbrowser.browser.gecko

import android.content.Context
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.data.DownloadRetry
import dev.sk2andy.materialbrowser.data.DownloadRetryUnavailable
import dev.sk2andy.materialbrowser.data.DownloadRuntimeRegistry
import dev.sk2andy.materialbrowser.data.DownloadStatus
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.ServerSocket
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoWebExecutor
import org.mozilla.geckoview.WebRequest
import org.mozilla.geckoview.WebResponse

@RunWith(AndroidJUnit4::class)
class GeckoDownloadTransferInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val savedNames = linkedSetOf<String>()

    @After
    fun cleanDownloadedFixture() {
        savedNames.forEach { name ->
            context.contentResolver.delete(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                "${MediaStore.Downloads.DISPLAY_NAME} = ?",
                arrayOf(name),
            )
        }
    }

    @Test
    fun streamsOriginalAuthenticatedStyleRequestIntoMediaStore() {
        val requestBody = "fixture-body".encodeToByteArray()
        val responseBody = "download-result".encodeToByteArray()
        val observedRequest = AtomicReference<String>()
        val server = ServerSocket(0)
        val serverFinished = CountDownLatch(1)
        Thread {
            server.use { socket ->
                socket.accept().use { client ->
                    val input = client.getInputStream().bufferedReader()
                    val requestLine = input.readLine()
                    val headers = linkedMapOf<String, String>()
                    while (true) {
                        val line = input.readLine()
                        if (line.isNullOrEmpty()) break
                        val separator = line.indexOf(':')
                        if (separator > 0) {
                            headers[line.substring(0, separator).lowercase()] =
                                line.substring(separator + 1).trim()
                        }
                    }
                    val length = headers["content-length"]?.toIntOrNull() ?: 0
                    val body = CharArray(length)
                    var offset = 0
                    while (offset < length) {
                        val count = input.read(body, offset, length - offset)
                        if (count < 0) break
                        offset += count
                    }
                    observedRequest.set(
                        listOf(requestLine, headers["x-candy-test"], body.concatToString()).joinToString("|"),
                    )
                    val output = client.getOutputStream()
                    output.write(
                        (
                            "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: application/octet-stream\r\n" +
                                "Content-Disposition: attachment; filename=\"candy-gecko-fixture.bin\"\r\n" +
                                "Content-Length: ${responseBody.size}\r\n" +
                                "Connection: close\r\n\r\n"
                            ).encodeToByteArray(),
                    )
                    output.write(responseBody)
                    output.flush()
                }
            }
            serverFinished.countDown()
        }.start()

        val managerRef = AtomicReference<GeckoDownloadTransferManager>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            managerRef.set(GeckoDownloadTransferManager(context, testExecutor()))
        }
        val manager = checkNotNull(managerRef.get())
        val completed = CountDownLatch(1)
        val failure = AtomicReference<GeckoDownloadFailure?>()
        val started = AtomicReference<GeckoDownloadTransferStart?>()
        val directBody = ByteBuffer.allocateDirect(requestBody.size).apply {
            put(requestBody)
            flip()
        }
        val request = WebRequest.Builder("http://127.0.0.1:${server.localPort}/download")
            .method("POST")
            .header("X-Candy-Test", "preserved")
            .body(directBody)
            .cacheMode(WebRequest.CACHE_MODE_NO_STORE)
            .beConservative(true)
            .build()

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            manager.start(
                GeckoDownloadTransferRequest(
                    owner = GeckoDownloadOwner("test-profile", isPrivate = true, sessionKey = this),
                    request = request,
                    fetchFlags = GeckoWebExecutor.FETCH_FLAGS_NO_REDIRECTS,
                ),
                object : GeckoDownloadTransferListener {
                override fun onStarted(start: GeckoDownloadTransferStart) {
                    started.set(start)
                    savedNames += start.fileName
                }

                override fun onComplete(bytesReceived: Long) {
                    assertEquals(responseBody.size.toLong(), bytesReceived)
                    completed.countDown()
                }

                override fun onFailed(reason: GeckoDownloadFailure) {
                    failure.set(reason)
                    completed.countDown()
                }
                },
            )
        }

        assertTrue("server did not finish", serverFinished.await(20, TimeUnit.SECONDS))
        assertTrue("download did not finish", completed.await(20, TimeUnit.SECONDS))
        assertNull(failure.get())
        assertNotNull(started.get())
        assertEquals("POST /download HTTP/1.1|preserved|fixture-body", observedRequest.get())
        assertArrayEquals(responseBody, storedDownload("candy-gecko-fixture.bin"))

        val externalBody = "original-gecko-response".encodeToByteArray()
        val externalCompleted = CountDownLatch(1)
        val externalFailure = AtomicReference<GeckoDownloadFailure?>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            manager.startResponse(
                owner = GeckoDownloadOwner("test-profile", isPrivate = true, sessionKey = this),
                response = WebResponse.Builder("blob:https://example.com/candy-download")
                    .statusCode(0)
                    .header("Content-Type", "application/octet-stream")
                    .header(
                        "Content-Disposition",
                        "attachment; filename=\"candy-original-response.bin\"",
                    )
                    .header("Content-Length", externalBody.size.toString())
                    .body(ByteArrayInputStream(externalBody))
                    .build(),
                referrer = "https://example.com/private-form",
                listener = object : GeckoDownloadTransferListener {
                    override fun onStarted(start: GeckoDownloadTransferStart) {
                        savedNames += start.fileName
                    }

                    override fun onComplete(bytesReceived: Long) {
                        assertEquals(externalBody.size.toLong(), bytesReceived)
                        externalCompleted.countDown()
                    }

                    override fun onFailed(reason: GeckoDownloadFailure) {
                        externalFailure.set(reason)
                        externalCompleted.countDown()
                    }
                },
            )
        }
        assertTrue("external response did not finish", externalCompleted.await(20, TimeUnit.SECONDS))
        assertNull(externalFailure.get())
        assertArrayEquals(
            externalBody,
            storedDownload("candy-original-response.bin"),
        )
        manager.close()
    }

    @Test
    fun commitOwnsTerminalStateWhenCancelRacesWithPublication() {
        val commitEntered = CountDownLatch(1)
        val allowCommit = CountDownLatch(1)
        val cancelStarted = CountDownLatch(1)
        val cancelReturned = CountDownLatch(1)
        val completed = CountDownLatch(1)
        val committed = AtomicBoolean(false)
        val aborted = AtomicBoolean(false)
        val failure = AtomicReference<GeckoDownloadFailure?>()
        val entry = object : GeckoDownloadStreamEntry {
            override val uri = android.net.Uri.parse("content://media/external/downloads/42")
            override val output = ByteArrayOutputStream()

            override fun commit() {
                commitEntered.countDown()
                check(allowCommit.await(10, TimeUnit.SECONDS))
                committed.set(true)
            }

            override fun abort() {
                aborted.set(true)
            }

            override fun close() = abort()
        }
        val managerRef = AtomicReference<GeckoDownloadTransferManager>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            managerRef.set(
                GeckoDownloadTransferManager(
                    context = context,
                    executor = testExecutor(),
                    sink = object : GeckoDownloadStreamSink {
                        override fun open(
                            fileName: String,
                            mimeType: String,
                        ): GeckoDownloadStreamEntry = entry
                    },
                ),
            )
        }
        val manager = checkNotNull(managerRef.get())
        val cancellation = checkNotNull(
            manager.startResponse(
                owner = GeckoDownloadOwner("test-profile", isPrivate = false, sessionKey = this),
                response = WebResponse.Builder("https://example.com/race.bin")
                    .statusCode(200)
                    .header("Content-Type", "application/octet-stream")
                    .header("Content-Length", "4")
                    .body(ByteArrayInputStream("race".encodeToByteArray()))
                    .build(),
                referrer = null,
                listener = object : GeckoDownloadTransferListener {
                    override fun onStarted(start: GeckoDownloadTransferStart) = Unit

                    override fun onComplete(bytesReceived: Long) {
                        completed.countDown()
                    }

                    override fun onFailed(reason: GeckoDownloadFailure) {
                        failure.set(reason)
                        completed.countDown()
                    }
                },
            ),
        )
        assertTrue("commit did not start", commitEntered.await(10, TimeUnit.SECONDS))

        val cancelThread = Thread {
            cancelStarted.countDown()
            cancellation.cancel()
            cancelReturned.countDown()
        }.apply { start() }
        assertTrue("cancel did not start", cancelStarted.await(10, TimeUnit.SECONDS))
        val blockDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (cancelThread.state != Thread.State.BLOCKED && System.nanoTime() < blockDeadline) {
            Thread.yield()
        }
        assertEquals(Thread.State.BLOCKED, cancelThread.state)
        assertFalse("cancel returned before commit finished", cancelReturned.await(10, TimeUnit.MILLISECONDS))
        allowCommit.countDown()

        assertTrue("download did not finish", completed.await(10, TimeUnit.SECONDS))
        assertTrue("cancel did not return", cancelReturned.await(10, TimeUnit.SECONDS))
        assertTrue(committed.get())
        assertFalse(aborted.get())
        assertNull(failure.get())
        manager.close()
    }

    @Test
    fun finalMetadataWaitsForAnAnswerBeforeAnythingIsWritten() {
        val openedTypes = mutableListOf<String>()
        val bodyClosed = AtomicBoolean(false)
        val events = mutableListOf<String>()
        val metadataRef = AtomicReference<GeckoDownloadTransferMetadata>()
        val decisions = mutableListOf<GeckoDownloadMetadataDecision>()
        val managerRef = AtomicReference<GeckoDownloadTransferManager>()
        fun response(name: String) = WebResponse.Builder("https://files.example/get?id=20")
            .statusCode(200)
            .header("Content-Type", "application/octet-stream")
            .header("Content-Disposition", "attachment; filename=\"$name\"")
            .body(
                object : ByteArrayInputStream("apk".encodeToByteArray()) {
                    override fun close() {
                        bodyClosed.set(true)
                        super.close()
                    }
                },
            )
            .build()
        val listener = object : GeckoDownloadTransferListener {
            override fun onMetadata(
                metadata: GeckoDownloadTransferMetadata,
                decision: GeckoDownloadMetadataDecision,
            ) {
                metadataRef.set(metadata)
                decisions += decision
            }

            override fun onStarted(start: GeckoDownloadTransferStart) {
                events += "started:${start.mimeType}"
            }

            override fun onFailed(reason: GeckoDownloadFailure) {
                events += "failed:$reason"
            }
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val manager = GeckoDownloadTransferManager(
                context = context,
                executor = testExecutor(),
                sink = object : GeckoDownloadStreamSink {
                    override fun open(fileName: String, mimeType: String): GeckoDownloadStreamEntry {
                        openedTypes += mimeType
                        return object : GeckoDownloadStreamEntry {
                            override val uri = android.net.Uri.parse("content://media/external/downloads/48")
                            override val output = ByteArrayOutputStream()
                            override fun commit() = Unit
                            override fun abort() = Unit
                            override fun close() = Unit
                        }
                    }
                },
            )
            managerRef.set(manager)
            val owner = GeckoDownloadOwner("test-profile", isPrivate = true, sessionKey = this)
            manager.startResponse(owner, response("invoice.pdf.apk"), referrer = null, listener = listener)
        }
        val manager = requireNotNull(managerRef.get())
        try {
            assertEquals(
                GeckoDownloadTransferMetadata(
                    fileName = "invoice.pdf.apk",
                    mimeType = "application/vnd.android.package-archive",
                    sourceUrl = "https://files.example/get?id=20",
                ),
                metadataRef.get(),
            )
            assertTrue(openedTypes.isEmpty())
            assertFalse(bodyClosed.get())
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                decisions.single().abort()
                decisions.single().proceed()
            }
            assertTrue(bodyClosed.get())
            assertTrue(openedTypes.isEmpty())
            assertTrue(events.isEmpty())

            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val owner = GeckoDownloadOwner("test-profile", isPrivate = true, sessionKey = this)
                manager.startResponse(owner, response("App.apk"), referrer = null, listener = listener)
                decisions.last().proceed()
                decisions.last().proceed()
            }
            assertEquals(listOf("application/vnd.android.package-archive"), openedTypes)
            assertEquals(listOf("started:application/vnd.android.package-archive"), events)
        } finally {
            manager.close()
        }
    }

    @Test
    fun pausesOriginalResponseBeforeCopyThenResumesWithoutRefetch() {
        val bodyRead = CountDownLatch(1)
        val completed = CountDownLatch(1)
        val started = AtomicReference<GeckoDownloadTransferStart>()
        val savedBytes = ByteArrayOutputStream()
        val body = object : ByteArrayInputStream("paused-body".encodeToByteArray()) {
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                bodyRead.countDown()
                return super.read(buffer, offset, length)
            }
        }
        val entry = object : GeckoDownloadStreamEntry {
            override val uri = android.net.Uri.parse("content://media/external/downloads/45")
            override val output = savedBytes
            override fun commit() = Unit
            override fun abort() = Unit
            override fun close() = Unit
        }
        val managerRef = AtomicReference<GeckoDownloadTransferManager>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val manager = GeckoDownloadTransferManager(
                context = context,
                executor = testExecutor(),
                sink = object : GeckoDownloadStreamSink {
                    override fun open(fileName: String, mimeType: String): GeckoDownloadStreamEntry = entry
                },
            )
            managerRef.set(manager)
            manager.startResponse(
                owner = GeckoDownloadOwner("test-profile", isPrivate = true, sessionKey = this),
                response = WebResponse.Builder("https://example.com/paused.bin")
                    .statusCode(200)
                    .header("Content-Type", "application/octet-stream")
                    .body(body)
                    .build(),
                referrer = null,
                listener = object : GeckoDownloadTransferListener {
                    override fun onStarted(start: GeckoDownloadTransferStart) {
                        started.set(start)
                        assertTrue(DownloadRuntimeRegistry.togglePause(-46L))
                    }

                    override fun onComplete(bytesReceived: Long) {
                        completed.countDown()
                    }
                },
            )
        }
        val manager = requireNotNull(managerRef.get())
        try {
            assertNotNull(started.get())
            assertEquals(DownloadStatus.Paused, DownloadRuntimeRegistry.snapshot().single { it.id == -46L }.status)
            assertFalse("paused response was read", bodyRead.await(100, TimeUnit.MILLISECONDS))
            assertTrue(DownloadRuntimeRegistry.togglePause(-46L))
            assertTrue("resumed transfer did not complete", completed.await(10, TimeUnit.SECONDS))
            assertArrayEquals("paused-body".encodeToByteArray(), savedBytes.toByteArray())
        } finally {
            manager.close()
        }
    }

    @Test
    fun cancelsPausedResponseAndDeletesIncompleteSink() {
        val failed = CountDownLatch(1)
        val aborted = AtomicBoolean(false)
        val failure = AtomicReference<GeckoDownloadFailure>()
        val entry = object : GeckoDownloadStreamEntry {
            override val uri = android.net.Uri.parse("content://media/external/downloads/46")
            override val output = ByteArrayOutputStream()
            override fun commit() = error("Cancelled transfer must not commit")
            override fun abort() {
                aborted.set(true)
            }
            override fun close() = abort()
        }
        val managerRef = AtomicReference<GeckoDownloadTransferManager>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val manager = GeckoDownloadTransferManager(
                context = context,
                executor = testExecutor(),
                sink = object : GeckoDownloadStreamSink {
                    override fun open(fileName: String, mimeType: String): GeckoDownloadStreamEntry = entry
                },
            )
            managerRef.set(manager)
            manager.startResponse(
                owner = GeckoDownloadOwner("test-profile", isPrivate = false, sessionKey = this),
                response = WebResponse.Builder("https://example.com/cancel-paused.bin")
                    .statusCode(200)
                    .header("Content-Type", "application/octet-stream")
                    .body(ByteArrayInputStream("cancel".encodeToByteArray()))
                    .build(),
                referrer = null,
                listener = object : GeckoDownloadTransferListener {
                    override fun onStarted(start: GeckoDownloadTransferStart) {
                        assertTrue(DownloadRuntimeRegistry.togglePause(-47L))
                    }

                    override fun onFailed(reason: GeckoDownloadFailure) {
                        failure.set(reason)
                        failed.countDown()
                    }
                },
            )
        }
        val manager = requireNotNull(managerRef.get())
        try {
            assertTrue(DownloadRuntimeRegistry.cancel(-47L))
            assertTrue("cancel callback missing", failed.await(10, TimeUnit.SECONDS))
            assertEquals(GeckoDownloadFailure.Cancelled, failure.get())
            assertTrue(aborted.get())
            assertFalse(DownloadRuntimeRegistry.togglePause(-47L))
            val cancelled = DownloadRuntimeRegistry.snapshot().single { it.id == -47L }
            assertEquals(DownloadStatus.Cancelled, cancelled.status)
            DownloadRuntimeRegistry.clear(listOf(cancelled.id))
        } finally {
            manager.close()
        }
    }

    @Test
    fun cancelDuringSinkOpenAbortsLateEntryWithoutStartingRuntimeProgress() {
        val openEntered = CountDownLatch(1)
        val allowOpen = CountDownLatch(1)
        val aborted = CountDownLatch(1)
        val finished = CountDownLatch(1)
        val failure = AtomicReference<GeckoDownloadFailure?>()
        val entry = object : GeckoDownloadStreamEntry {
            override val uri = android.net.Uri.parse("content://media/external/downloads/43")
            override val output = ByteArrayOutputStream()

            override fun commit() = Unit

            override fun abort() {
                aborted.countDown()
            }

            override fun close() = abort()
        }
        val server = ServerSocket(0)
        Thread {
            server.use { socket ->
                socket.accept().use { client ->
                    val input = client.getInputStream().bufferedReader()
                    while (true) {
                        if (input.readLine().isNullOrEmpty()) break
                    }
                    client.getOutputStream().use { output ->
                        output.write(
                            (
                                "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: application/octet-stream\r\n" +
                                    "Content-Length: 4\r\n" +
                                    "Connection: close\r\n\r\n" +
                                    "race"
                                ).encodeToByteArray(),
                        )
                    }
                }
            }
        }.start()
        val managerRef = AtomicReference<GeckoDownloadTransferManager>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            managerRef.set(
                GeckoDownloadTransferManager(
                    context = context,
                    executor = testExecutor(),
                    sink = object : GeckoDownloadStreamSink {
                        override fun open(
                            fileName: String,
                            mimeType: String,
                        ): GeckoDownloadStreamEntry {
                            openEntered.countDown()
                            check(allowOpen.await(10, TimeUnit.SECONDS))
                            return entry
                        }
                    },
                ),
            )
        }
        val manager = checkNotNull(managerRef.get())
        val cancellationRef = AtomicReference<GeckoDownloadCancellation?>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            cancellationRef.set(
                manager.start(
                    transfer = GeckoDownloadTransferRequest(
                        owner = GeckoDownloadOwner(
                            "test-profile",
                            isPrivate = false,
                            sessionKey = this,
                        ),
                        request = WebRequest.Builder(
                            "http://127.0.0.1:${server.localPort}/setup-race",
                        ).build(),
                        fetchFlags = GeckoWebExecutor.FETCH_FLAGS_NONE,
                    ),
                    listener = object : GeckoDownloadTransferListener {
                        override fun onStarted(start: GeckoDownloadTransferStart) = Unit

                        override fun onFailed(reason: GeckoDownloadFailure) {
                            failure.set(reason)
                            finished.countDown()
                        }
                    },
                ),
            )
        }
        val cancellation = checkNotNull(cancellationRef.get())
        assertTrue("sink did not open", openEntered.await(10, TimeUnit.SECONDS))

        cancellation.cancel()
        allowOpen.countDown()

        assertTrue("cancel callback missing", finished.await(10, TimeUnit.SECONDS))
        assertTrue("late entry was not aborted", aborted.await(10, TimeUnit.SECONDS))
        assertEquals(GeckoDownloadFailure.Cancelled, failure.get())
        assertTrue(DownloadRuntimeRegistry.snapshot().isEmpty())
        manager.close()
    }

    @Test
    fun cancelledLinkDownloadStartsAgainByGetAndSavesTheSameFile() {
        val body = ByteArray(64 * 1024) { index -> (index % 251).toByte() }
        val server = SequentialFixtureServer(
            // The first answer sends a little and waits until the test has cancelled it.
            { output, release ->
                output.write(fileHeaders("candy-retry-fixture.bin", "application/octet-stream", body.size))
                output.write(body, 0, 1024)
                output.flush()
                release.await(20, TimeUnit.SECONDS)
            },
            { output, _ ->
                output.write(fileHeaders("candy-retry-fixture.bin", "application/octet-stream", body.size))
                output.write(body)
                output.flush()
            },
        )
        val manager = createManager()
        val started = CountDownLatch(1)
        val cancelled = CountDownLatch(1)
        val firstId = AtomicReference<Int>()
        val cancellation = AtomicReference<GeckoDownloadCancellation?>()
        val page = "http://127.0.0.1:${server.port}/page"
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            cancellation.set(
                manager.start(
                    GeckoDownloadTransferRequest(
                        owner = GeckoDownloadOwner("test-profile", isPrivate = false, sessionKey = this),
                        request = WebRequest.Builder("http://127.0.0.1:${server.port}/file")
                            .method("GET")
                            .referrer(page)
                            .build(),
                        fetchFlags = GeckoWebExecutor.FETCH_FLAGS_NONE,
                        retryable = true,
                    ),
                    object : GeckoDownloadTransferListener {
                        override fun onStarted(start: GeckoDownloadTransferStart) {
                            firstId.set(start.id)
                            savedNames += start.fileName
                            started.countDown()
                        }

                        override fun onFailed(reason: GeckoDownloadFailure) {
                            if (reason == GeckoDownloadFailure.Cancelled) cancelled.countDown()
                        }
                    },
                ),
            )
        }
        assertTrue("download did not start", started.await(20, TimeUnit.SECONDS))
        checkNotNull(cancellation.get()).cancel()
        assertTrue("download was not cancelled", cancelled.await(10, TimeUnit.SECONDS))
        server.releaseFirst()
        val failedRow = checkNotNull(DownloadRuntimeRegistry.entryForTransfer(firstId.get()))
        assertEquals(DownloadStatus.Cancelled, failedRow.status)
        assertEquals(DownloadRetry.Available, failedRow.retry)

        assertTrue(DownloadRuntimeRegistry.retry(failedRow.id))

        assertTrue(
            "retried file was not saved",
            waitFor { storedDownload("candy-retry-fixture.bin")?.contentEquals(body) == true },
        )
        assertTrue("the old row stayed", waitFor { DownloadRuntimeRegistry.entryForTransfer(firstId.get()) == null })
        val second = server.requests[1]
        assertTrue(second.requestLine, second.requestLine.startsWith("GET /file "))
        assertEquals(page, second.headers["referer"])
        manager.close()
        server.close()
    }

    @Test
    fun retryThatGetsAnotherFileSavesNothingAndNamesThePage() {
        val server = SequentialFixtureServer(
            // A page's form answered by GET: the server sends its HTML page, not the report.
            { output, _ ->
                val page = "<html><body>Sign in again</body></html>".encodeToByteArray()
                output.write(
                    (
                        "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n" +
                            "Content-Length: ${page.size}\r\nConnection: close\r\n\r\n"
                        ).encodeToByteArray(),
                )
                output.write(page)
                output.flush()
            },
        )
        val manager = createManager()
        val release = CountDownLatch(1)
        val started = CountDownLatch(1)
        val cancelled = CountDownLatch(1)
        val firstId = AtomicReference<Int>()
        val cancellation = AtomicReference<GeckoDownloadCancellation?>()
        val page = "http://127.0.0.1:${server.port}/form"
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            cancellation.set(
                manager.startResponse(
                    owner = GeckoDownloadOwner("test-profile", isPrivate = false, sessionKey = this),
                    response = WebResponse.Builder("http://127.0.0.1:${server.port}/report")
                        .statusCode(200)
                        .header("Content-Type", "application/pdf")
                        .header("Content-Disposition", "attachment; filename=\"candy-retry-report.pdf\"")
                        .body(BlockingInputStream(release))
                        .build(),
                    referrer = page,
                    listener = object : GeckoDownloadTransferListener {
                        override fun onStarted(start: GeckoDownloadTransferStart) {
                            firstId.set(start.id)
                            savedNames += start.fileName
                            started.countDown()
                        }

                        override fun onFailed(reason: GeckoDownloadFailure) {
                            if (reason == GeckoDownloadFailure.Cancelled) cancelled.countDown()
                        }
                    },
                ),
            )
        }
        assertTrue("download did not start", started.await(20, TimeUnit.SECONDS))
        checkNotNull(cancellation.get()).cancel()
        release.countDown()
        assertTrue("download was not cancelled", cancelled.await(10, TimeUnit.SECONDS))
        val failedRow = checkNotNull(DownloadRuntimeRegistry.entryForTransfer(firstId.get()))
        assertEquals(DownloadRetry.Available, failedRow.retry)

        assertTrue(DownloadRuntimeRegistry.retry(failedRow.id))

        assertTrue(
            "the retry did not end",
            waitFor { DownloadRuntimeRegistry.entryForTransfer(firstId.get())?.status == DownloadStatus.Failed },
        )
        assertEquals(
            DownloadRetry.Unavailable(DownloadRetryUnavailable.ResponseChanged, pageUrl = page),
            DownloadRuntimeRegistry.entryForTransfer(firstId.get())?.retry,
        )
        assertFalse(DownloadRuntimeRegistry.retry(failedRow.id))
        assertNull(storedDownload("candy-retry-report.pdf"))
        assertTrue(server.requests.single().requestLine.startsWith("GET /report "))
        DownloadRuntimeRegistry.completed(firstId.get())
        manager.close()
        server.close()
    }

    private fun createManager(): GeckoDownloadTransferManager {
        val managerRef = AtomicReference<GeckoDownloadTransferManager>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            managerRef.set(GeckoDownloadTransferManager(context, testExecutor()))
        }
        return checkNotNull(managerRef.get())
    }

    private fun fileHeaders(name: String, mimeType: String, length: Int): ByteArray = (
        "HTTP/1.1 200 OK\r\n" +
            "Content-Type: $mimeType\r\n" +
            "Content-Disposition: attachment; filename=\"$name\"\r\n" +
            "Content-Length: $length\r\n" +
            "Connection: close\r\n\r\n"
        ).encodeToByteArray()

    private fun waitFor(timeoutMillis: Long = 20_000L, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(100)
        }
        return condition()
    }

    /** Sends a few bytes, then blocks reading until [release]; a transfer stays open until cancelled. */
    private class BlockingInputStream(private val release: CountDownLatch) : java.io.InputStream() {
        private var sent = 0

        override fun read(): Int {
            if (sent < 512) return (sent++ % 251)
            release.await(20, TimeUnit.SECONDS)
            return -1
        }
    }

    private class FixtureRequest(val requestLine: String, val headers: Map<String, String>)

    /** Answers one connection after another with [responders]; each gets the output and a release latch. */
    private class SequentialFixtureServer(
        vararg responders: (java.io.OutputStream, CountDownLatch) -> Unit,
    ) : AutoCloseable {
        private val socket = ServerSocket(0)
        private val firstRelease = CountDownLatch(1)
        val port: Int = socket.localPort
        val requests: MutableList<FixtureRequest> = java.util.concurrent.CopyOnWriteArrayList()

        init {
            Thread {
                runCatching {
                    responders.forEach { respond ->
                        socket.accept().use { client ->
                            val input = client.getInputStream().bufferedReader()
                            val requestLine = input.readLine().orEmpty()
                            val headers = linkedMapOf<String, String>()
                            while (true) {
                                val line = input.readLine()
                                if (line.isNullOrEmpty()) break
                                val separator = line.indexOf(':')
                                if (separator > 0) {
                                    headers[line.substring(0, separator).lowercase()] =
                                        line.substring(separator + 1).trim()
                                }
                            }
                            requests += FixtureRequest(requestLine, headers)
                            respond(client.getOutputStream(), firstRelease)
                        }
                    }
                }
            }.apply { isDaemon = true }.start()
        }

        fun releaseFirst() = firstRelease.countDown()

        override fun close() {
            firstRelease.countDown()
            runCatching(socket::close)
        }
    }

    private fun storedDownload(name: String): ByteArray? = context.contentResolver.query(
        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
        arrayOf(MediaStore.Downloads._ID),
        "${MediaStore.Downloads.DISPLAY_NAME} = ?",
        arrayOf(name),
        null,
    )?.use { cursor ->
        if (!cursor.moveToFirst()) null else {
            val id = cursor.getLong(0)
            context.contentResolver.openInputStream(
                android.content.ContentUris.withAppendedId(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    id,
                ),
            )?.use { input -> input.readBytes() }
        }
    }

    private fun testExecutor(): GeckoWebExecutor {
        val runtime = sharedRuntime ?: GeckoRuntime.create(
            context,
            GeckoRuntimeSettings.Builder().build(),
        ).also { created -> sharedRuntime = created }
        return GeckoWebExecutor(runtime)
    }

    private companion object {
        var sharedRuntime: GeckoRuntime? = null
    }
}
