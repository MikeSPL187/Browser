package dev.sk2andy.materialbrowser.diagnostics

/**
 * What a native crash report needs from Android's tombstone: the signal, the abort message and the
 * crashing thread's frames. Registers, memory dumps, open files and the log buffers stay out.
 *
 * Android 12+ hands the tombstone over as protobuf (AOSP `debuggerd/proto/tombstone.proto`); this
 * reads the few fields it needs without a protobuf library and skips everything else.
 */
internal data class TombstoneSummary(
    val signalName: String?,
    val signalCode: String?,
    val faultAddress: Long?,
    val abortMessage: String?,
    val threadName: String?,
    val frames: List<String>,
) {
    fun render(): String = buildString {
        val signal = listOfNotNull(signalName, signalCode).joinToString(" / ")
        if (signal.isNotEmpty()) {
            append("Signal: ").append(signal)
            faultAddress?.let { append(", fault address 0x").append(it.toULong().toString(16)) }
            appendLine()
        }
        abortMessage?.takeIf(String::isNotBlank)?.let { appendLine("Abort message: $it") }
        if (frames.isNotEmpty()) {
            appendLine()
            appendLine("Backtrace (thread ${threadName ?: "unknown"}):")
            frames.forEach(::appendLine)
        }
    }

    companion object {
        /** The size read from the trace stream; a tombstone's own fields come well before this. */
        const val MAX_BYTES = 8 * 1024 * 1024
        const val MAX_FRAMES = 40

        // Field numbers from tombstone.proto.
        private const val TOMBSTONE_TID = 6
        private const val TOMBSTONE_SIGNAL = 10
        private const val TOMBSTONE_ABORT_MESSAGE = 14
        private const val TOMBSTONE_THREADS = 16
        private const val SIGNAL_NAME = 2
        private const val SIGNAL_CODE_NAME = 4
        private const val SIGNAL_HAS_FAULT_ADDRESS = 8
        private const val SIGNAL_FAULT_ADDRESS = 9
        private const val MAP_VALUE = 2
        private const val THREAD_ID = 1
        private const val THREAD_NAME = 2
        private const val THREAD_BACKTRACE = 4
        private const val FRAME_REL_PC = 1
        private const val FRAME_FUNCTION_NAME = 4
        private const val FRAME_FUNCTION_OFFSET = 5
        private const val FRAME_FILE_NAME = 6
        private const val FRAME_BUILD_ID = 8

        /** The summary of a tombstone, or null when [bytes] is not one this can read. */
        fun parse(bytes: ByteArray): TombstoneSummary? = runCatching {
            var tid: Long? = null
            var signal: ProtoMessage? = null
            var abortMessage: String? = null
            val threads = mutableListOf<ProtoMessage>()
            ProtoReader(bytes).forEachField { field ->
                when (field.number) {
                    TOMBSTONE_TID -> tid = field.varint
                    TOMBSTONE_SIGNAL -> signal = field.message
                    TOMBSTONE_ABORT_MESSAGE -> abortMessage = field.string
                    // A map entry: the thread id as key, the thread as value.
                    TOMBSTONE_THREADS -> field.message?.child(MAP_VALUE)?.let(threads::add)
                }
            }
            val crashing = threads.firstOrNull { it.varint(THREAD_ID) == tid } ?: threads.firstOrNull()
            val summary = TombstoneSummary(
                signalName = signal?.string(SIGNAL_NAME),
                signalCode = signal?.string(SIGNAL_CODE_NAME),
                faultAddress = signal
                    ?.takeIf { it.varint(SIGNAL_HAS_FAULT_ADDRESS) == 1L }
                    ?.varint(SIGNAL_FAULT_ADDRESS),
                abortMessage = abortMessage,
                threadName = crashing?.string(THREAD_NAME),
                frames = crashing?.children(THREAD_BACKTRACE).orEmpty()
                    .take(MAX_FRAMES)
                    .mapIndexed(::renderFrame),
            )
            summary.takeIf { it.signalName != null || it.abortMessage != null || it.frames.isNotEmpty() }
        }.getOrNull()

        private fun renderFrame(index: Int, frame: ProtoMessage): String = buildString {
            append('#').append(index.toString().padStart(2, '0'))
            append(" pc ").append((frame.varint(FRAME_REL_PC) ?: 0L).toULong().toString(16).padStart(8, '0'))
            // Only the library's name: its install path carries a random directory, not a cause.
            frame.string(FRAME_FILE_NAME)?.let { append("  ").append(it.substringAfterLast('/')) }
            frame.string(FRAME_FUNCTION_NAME)?.takeIf(String::isNotEmpty)?.let { name ->
                append(" (").append(name)
                frame.varint(FRAME_FUNCTION_OFFSET)?.takeIf { it != 0L }?.let { append('+').append(it) }
                append(')')
            }
            frame.string(FRAME_BUILD_ID)?.takeIf(String::isNotEmpty)?.let {
                append(" (BuildId: ").append(it).append(')')
            }
        }
    }
}

/** One decoded field: a varint value, or the bytes of a length-delimited one. */
private class ProtoField(val number: Int, val varint: Long?, val bytes: ByteArray?) {
    val string: String? get() = bytes?.toString(Charsets.UTF_8)
    val message: ProtoMessage? get() = bytes?.let(::ProtoMessage)
}

/** A nested message, decoded on demand. */
private class ProtoMessage(private val bytes: ByteArray) {
    private val fields: List<ProtoField> by lazy {
        buildList { ProtoReader(bytes).forEachField(::add) }
    }

    fun varint(number: Int): Long? = fields.lastOrNull { it.number == number }?.varint
    fun string(number: Int): String? = fields.lastOrNull { it.number == number }?.string
    fun child(number: Int): ProtoMessage? = fields.lastOrNull { it.number == number }?.message
    fun children(number: Int): List<ProtoMessage> =
        fields.filter { it.number == number }.mapNotNull { it.message }
}

/** The protobuf wire format, just enough of it: varints, fixed widths and length-delimited bytes. */
private class ProtoReader(private val bytes: ByteArray) {
    private var position = 0

    fun forEachField(action: (ProtoField) -> Unit) {
        while (position < bytes.size) {
            val key = readVarint()
            val number = (key ushr 3).toInt()
            when ((key and 7).toInt()) {
                WIRE_VARINT -> action(ProtoField(number, readVarint(), null))
                WIRE_FIXED64 -> skip(8)
                WIRE_LENGTH -> {
                    val length = readVarint()
                    require(length >= 0 && length <= bytes.size - position) { "Truncated field" }
                    val start = position
                    skip(length.toInt())
                    action(ProtoField(number, null, bytes.copyOfRange(start, position)))
                }
                WIRE_FIXED32 -> skip(4)
                else -> throw IllegalArgumentException("Unsupported wire type")
            }
        }
    }

    private fun readVarint(): Long {
        var result = 0L
        var shift = 0
        while (shift < 64) {
            require(position < bytes.size) { "Truncated varint" }
            val byte = bytes[position++].toInt()
            result = result or ((byte and 0x7f).toLong() shl shift)
            if (byte and 0x80 == 0) return result
            shift += 7
        }
        throw IllegalArgumentException("Malformed varint")
    }

    private fun skip(count: Int) {
        require(count <= bytes.size - position) { "Truncated field" }
        position += count
    }

    private companion object {
        const val WIRE_VARINT = 0
        const val WIRE_FIXED64 = 1
        const val WIRE_LENGTH = 2
        const val WIRE_FIXED32 = 5
    }
}
