package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.data.DownloadEntry
import dev.sk2andy.materialbrowser.data.DownloadStatus
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryFileRulesTest {
    @Test
    fun `a file's kind comes from its type, else from its name`() {
        assertEquals(DownloadKind.Pdf, LibraryFileRules.kind("Карта.pdf", ""))
        assertEquals(DownloadKind.Pdf, LibraryFileRules.kind("map", "application/pdf"))
        assertEquals(DownloadKind.Image, LibraryFileRules.kind("olkhon-grotto.JPG", ""))
        assertEquals(DownloadKind.Video, LibraryFileRules.kind("clip", "video/mp4"))
        assertEquals(DownloadKind.Spreadsheet, LibraryFileRules.kind("Отчёт.xlsx", ""))
        assertEquals(DownloadKind.App, LibraryFileRules.kind("F-Droid.apk", ""))
        assertEquals(DownloadKind.Archive, LibraryFileRules.kind("old.zip", ""))
        assertEquals(DownloadKind.Document, LibraryFileRules.kind("notes", "text/plain"))
        assertEquals(DownloadKind.Other, LibraryFileRules.kind("olkhon-route.gpx", ""))
    }

    @Test
    fun `documents take pdfs and spreadsheets, photos and videos only their own`() {
        assertTrue(LibraryFileRules.matches(DownloadKindFilter.Documents, DownloadKind.Pdf))
        assertTrue(LibraryFileRules.matches(DownloadKindFilter.Documents, DownloadKind.Spreadsheet))
        assertFalse(LibraryFileRules.matches(DownloadKindFilter.Documents, DownloadKind.Image))
        assertTrue(LibraryFileRules.matches(DownloadKindFilter.Images, DownloadKind.Image))
        assertFalse(LibraryFileRules.matches(DownloadKindFilter.Videos, DownloadKind.Image))
        assertTrue(LibraryFileRules.matches(DownloadKindFilter.All, DownloadKind.Other))
    }

    @Test
    fun `finished downloads go by day, newest first, without the running ones`() {
        val zone = ZoneOffset.UTC
        val day = LocalDate.of(2026, 10, 4).atStartOfDay(zone).toInstant().toEpochMilli()
        val running = entry(1, "run.pdf", DownloadStatus.Running, day + 5_000)
        val today = entry(2, "today.pdf", DownloadStatus.Successful, day + 4_000)
        val earlier = entry(3, "earlier.pdf", DownloadStatus.Failed, day + 1_000)
        val yesterday = entry(4, "old.pdf", DownloadStatus.Successful, day - 1_000)

        val sections = LibraryFileRules.daySections(listOf(yesterday, earlier, running, today), zone)

        assertEquals(listOf(LocalDate.of(2026, 10, 4), LocalDate.of(2026, 10, 3)), sections.map { it.date })
        assertEquals(listOf(today, earlier), sections.first().entries)
        assertEquals(listOf(yesterday), sections.last().entries)
    }

    @Test
    fun `a snoozed tab says today, tomorrow, a weekday or a date`() {
        val zone = ZoneId.of("UTC")
        val now = LocalDate.of(2026, 10, 4).atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val hour = 3_600_000L
        val day = 24 * hour
        assertEquals(SnoozeWakeDay.Today, LibraryFileRules.wakeDay(now + 6 * hour, now, zone))
        assertEquals(SnoozeWakeDay.Today, LibraryFileRules.wakeDay(now - hour, now, zone))
        assertEquals(SnoozeWakeDay.Tomorrow, LibraryFileRules.wakeDay(now + day, now, zone))
        assertEquals(SnoozeWakeDay.ThisWeek, LibraryFileRules.wakeDay(now + 6 * day, now, zone))
        assertEquals(SnoozeWakeDay.Later, LibraryFileRules.wakeDay(now + 7 * day, now, zone))
    }

    private fun entry(id: Long, name: String, status: DownloadStatus, lastModified: Long) =
        DownloadEntry(
            id = id,
            name = name,
            source = "",
            status = status,
            bytes = 1,
            total = 1,
            lastModified = lastModified,
            mime = "",
        )
}
