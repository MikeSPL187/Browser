package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.data.DownloadEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** What a downloaded file is, for its tile and its line (board W-Downloads). */
internal enum class DownloadKind {
    Pdf,
    Document,
    Spreadsheet,
    Image,
    Video,
    Audio,
    Archive,
    App,
    Other,
}

/** The chips over the list: «All · Documents · Photos · Videos». */
internal enum class DownloadKindFilter {
    All,
    Documents,
    Images,
    Videos,
}

/** When a snoozed tab comes back, as a short label: «Today, 18:00», «Saturday», «6 October». */
internal enum class SnoozeWakeDay {
    Today,
    Tomorrow,
    ThisWeek,
    Later,
}

internal data class DownloadDaySection(
    val date: LocalDate,
    val entries: List<DownloadEntry>,
)

internal object LibraryFileRules {
    private val documentExtensions = setOf("doc", "docx", "odt", "rtf", "txt", "md", "epub", "ppt", "pptx", "odp")
    private val spreadsheetExtensions = setOf("xls", "xlsx", "ods", "csv", "tsv")
    private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "heic", "heif", "bmp", "svg", "avif")
    private val videoExtensions = setOf("mp4", "mkv", "webm", "mov", "avi", "3gp", "m4v")
    private val audioExtensions = setOf("mp3", "m4a", "aac", "ogg", "opus", "flac", "wav")
    private val archiveExtensions = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz")

    fun kind(name: String, mime: String): DownloadKind {
        val type = mime.trim().lowercase(Locale.ROOT)
        val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return when {
            type == "application/pdf" || extension == "pdf" -> DownloadKind.Pdf
            type == "application/vnd.android.package-archive" || extension == "apk" -> DownloadKind.App
            type.startsWith("image/") || extension in imageExtensions -> DownloadKind.Image
            type.startsWith("video/") || extension in videoExtensions -> DownloadKind.Video
            type.startsWith("audio/") || extension in audioExtensions -> DownloadKind.Audio
            "spreadsheet" in type || "excel" in type || extension in spreadsheetExtensions ->
                DownloadKind.Spreadsheet
            extension in archiveExtensions || type in archiveTypes -> DownloadKind.Archive
            type.startsWith("text/") || "document" in type || "msword" in type ||
                "presentation" in type || extension in documentExtensions -> DownloadKind.Document
            else -> DownloadKind.Other
        }
    }

    private val archiveTypes = setOf(
        "application/zip",
        "application/x-zip-compressed",
        "application/x-7z-compressed",
        "application/x-rar-compressed",
        "application/gzip",
        "application/x-tar",
    )

    fun matches(filter: DownloadKindFilter, kind: DownloadKind): Boolean = when (filter) {
        DownloadKindFilter.All -> true
        DownloadKindFilter.Documents ->
            kind == DownloadKind.Pdf || kind == DownloadKind.Document || kind == DownloadKind.Spreadsheet
        DownloadKindFilter.Images -> kind == DownloadKind.Image
        DownloadKindFilter.Videos -> kind == DownloadKind.Video
    }

    fun filter(entries: List<DownloadEntry>, filter: DownloadKindFilter): List<DownloadEntry> =
        if (filter == DownloadKindFilter.All) entries
        else entries.filter { matches(filter, kind(it.name, it.mime)) }

    /** Finished downloads by day, newest first; the ones still running sit above in their own card. */
    fun daySections(entries: List<DownloadEntry>, zoneId: ZoneId): List<DownloadDaySection> =
        entries.filterNot { it.status.isActive }
            .sortedByDescending(DownloadEntry::lastModified)
            .groupBy { Instant.ofEpochMilli(it.lastModified).atZone(zoneId).toLocalDate() }
            .map { (date, dayEntries) -> DownloadDaySection(date, dayEntries) }

    fun wakeDay(wakeAtMillis: Long, nowMillis: Long, zoneId: ZoneId): SnoozeWakeDay {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val day = Instant.ofEpochMilli(wakeAtMillis).atZone(zoneId).toLocalDate()
        return when {
            !day.isAfter(today) -> SnoozeWakeDay.Today
            day == today.plusDays(1) -> SnoozeWakeDay.Tomorrow
            day.isBefore(today.plusDays(DAYS_IN_WEEK)) -> SnoozeWakeDay.ThisWeek
            else -> SnoozeWakeDay.Later
        }
    }

    private const val DAYS_IN_WEEK = 7L
}
