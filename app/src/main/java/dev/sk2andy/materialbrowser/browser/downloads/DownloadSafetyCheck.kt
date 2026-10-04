package dev.sk2andy.materialbrowser.browser.downloads

import java.net.URI
import java.util.Locale

/** Something about a download worth a second look before it is saved, most serious first. */
enum class DownloadSafetyFinding {
    /** The name hides a program behind a document's extension, such as `invoice.pdf.apk`. */
    DisguisedName,

    /** The server says the file is a program, but its name says otherwise. */
    TypeMismatch,

    /** An Android app: only the system installs it, after its own warning. */
    AndroidApp,

    /** A program for a computer; it cannot run on the phone and is often passed on. */
    DesktopProgram,

    /** The file comes over plain HTTP, so anyone on the way could have swapped it. */
    InsecureSource,
}

/**
 * Local checks of a download before it is saved (board W-DownloadCheck). Everything is decided
 * on the phone from the name, the declared type and the address: nothing is sent anywhere.
 * An ordinary file over HTTPS has no findings and downloads without a question.
 */
object DownloadSafetyCheck {
    fun findings(url: String, fileName: String, mimeType: String): List<DownloadSafetyFinding> {
        val extensions = extensionsOf(fileName)
        val last = extensions.lastOrNull()
        val nameIsProgram = last != null && last in programExtensions
        val typeIsProgram = mimeType.lowercase(Locale.ROOT) in programMimeTypes
        return buildList {
            if (nameIsProgram && extensions.dropLast(1).any { it in documentExtensions }) {
                add(DownloadSafetyFinding.DisguisedName)
            }
            if (typeIsProgram && !nameIsProgram) add(DownloadSafetyFinding.TypeMismatch)
            if (last in androidExtensions || mimeType.equals(ANDROID_PACKAGE, ignoreCase = true)) {
                add(DownloadSafetyFinding.AndroidApp)
            } else if (nameIsProgram) {
                add(DownloadSafetyFinding.DesktopProgram)
            }
            if (isPlainHttp(url)) add(DownloadSafetyFinding.InsecureSource)
        }
    }

    /** The extensions of a name, outermost last: `a.tar.gz` gives `tar`, `gz`. */
    private fun extensionsOf(fileName: String): List<String> = fileName
        .lowercase(Locale.ROOT)
        .split('.')
        .drop(1)
        .map(String::trim)
        .filter { it.isNotEmpty() && it.length <= MAX_EXTENSION_LENGTH }

    private fun isPlainHttp(url: String): Boolean =
        runCatching { URI(url.trim()).scheme }.getOrNull().equals("http", ignoreCase = true)

    private const val ANDROID_PACKAGE = "application/vnd.android.package-archive"
    private const val MAX_EXTENSION_LENGTH = 10

    private val androidExtensions = setOf("apk", "apks", "apkm", "xapk", "aab")
    private val desktopExtensions = setOf(
        "exe", "msi", "msix", "bat", "cmd", "com", "scr", "pif", "cpl", "ps1", "vbs", "vbe",
        "wsf", "hta", "jar", "dmg", "pkg", "deb", "rpm", "appimage", "sh", "lnk",
    )
    private val programExtensions = androidExtensions + desktopExtensions
    private val documentExtensions = setOf(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "rtf", "txt", "csv",
        "jpg", "jpeg", "png", "gif", "webp", "heic", "mp3", "mp4", "mov", "avi", "mkv", "zip",
    )
    private val programMimeTypes = setOf(
        ANDROID_PACKAGE,
        "application/x-msdownload",
        "application/x-msdos-program",
        "application/x-msi",
        "application/x-ms-installer",
        "application/vnd.microsoft.portable-executable",
        "application/java-archive",
        "application/x-apple-diskimage",
        "application/x-sh",
    )
}
