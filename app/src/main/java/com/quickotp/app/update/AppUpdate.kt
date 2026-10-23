package com.quickotp.app.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Checks [GitHub Releases](https://github.com/diegoboston/quickotp/releases)
 * for a newer signed APK and hands it to the system package installer.
 *
 * CI uploads a stable asset named [APK_ASSET_NAME] on every main-branch
 * build so [LATEST_APK_URL] always resolves without authentication.
 */
object AppUpdate {

    const val REPO = "diegoboston/quickotp"
    const val APK_ASSET_NAME = "app.apk"
    const val UPDATE_APK_FILENAME = "quickotp-update.apk"
    private val UPDATE_APK_PREFIX = UPDATE_APK_FILENAME.removeSuffix(".apk")
    private const val LATEST_RELEASE_API =
        "https://api.github.com/repos/$REPO/releases/latest"
    const val LATEST_APK_URL =
        "https://github.com/$REPO/releases/latest/download/$APK_ASSET_NAME"

    private val json = Json { ignoreUnknownKeys = true }

    data class ReleaseInfo(
        val versionCode: Long,
        val versionName: String,
        val downloadUrl: String,
    )

    sealed class InstallResult {
        data object Launched : InstallResult()
        data object NeedsPermission : InstallResult()
    }

    fun installedVersionCode(context: Context): Long {
        val pi = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pi.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            pi.versionCode.toLong()
        }
    }

    fun parseVersionCodeFromTag(tagName: String): Long? {
        val stripped = tagName.trim().removePrefix("v").removePrefix("V")
        return stripped.substringAfterLast('.', missingDelimiterValue = stripped)
            .toLongOrNull()
    }

    fun parseRelease(jsonBody: String): ReleaseInfo? {
        val release = json.decodeFromString<GhRelease>(jsonBody)
        val versionCode = parseVersionCodeFromTag(release.tagName) ?: return null
        val asset = release.assets.firstOrNull { it.name == APK_ASSET_NAME }
            ?: release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
            ?: return null
        return ReleaseInfo(
            versionCode = versionCode,
            versionName = release.tagName.removePrefix("v").removePrefix("V"),
            downloadUrl = asset.browserDownloadUrl,
        )
    }

    fun fetchLatestRelease(): ReleaseInfo {
        val conn = openGet(LATEST_RELEASE_API)
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                val detail = conn.errorStream?.bufferedReader()?.readText()?.take(200)
                error("GitHub API HTTP $code${detail?.let { ": $it" } ?: ""}")
            }
            val body = conn.inputStream.bufferedReader().readText()
            return parseRelease(body)
                ?: error("Release has no $APK_ASSET_NAME asset or unparseable tag")
        } finally {
            conn.disconnect()
        }
    }

    fun downloadApk(
        context: Context,
        url: String,
        onProgress: (Float?) -> Unit,
    ): File {
        val conn = openGet(url)
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                error("Download failed (HTTP $code)")
            }
            val total = conn.contentLengthLong.takeIf { it > 0L }
            val dest = File(context.cacheDir, UPDATE_APK_FILENAME)
            dest.outputStream().use { out ->
                conn.inputStream.use { input ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        out.write(buffer, 0, read)
                        downloaded += read
                        onProgress(
                            if (total != null) (downloaded.toFloat() / total).coerceIn(0f, 1f)
                            else null,
                        )
                    }
                }
            }
            return dest
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Returns a previously downloaded update APK when it matches [versionCode]
     * and is still newer than the installed build.
     */
    fun cachedApkForUpdate(context: Context, versionCode: Long): File? {
        val apk = File(context.cacheDir, UPDATE_APK_FILENAME)
        if (!apk.isFile || apk.length() <= 0L) return null
        val archivedVersion = readApkVersionCode(context, apk) ?: return null
        if (archivedVersion != versionCode) return null
        if (archivedVersion <= installedVersionCode(context)) return null
        return apk
    }

    /** Uses a valid cached APK when present; otherwise downloads from [release]. */
    fun resolveApkForUpdate(
        context: Context,
        release: ReleaseInfo,
        onProgress: (Float?) -> Unit,
    ): File {
        cachedApkForUpdate(context, release.versionCode)?.let { return it }
        return downloadApk(context, release.downloadUrl, onProgress)
    }

    /** Removes the downloaded update APK and any related partial files from cache. */
    fun clearUpdateCache(context: Context): Long {
        var reclaimed = 0L
        context.cacheDir.listFiles()?.forEach { file ->
            if (isUpdateCacheFile(file.name) && file.isFile) {
                val bytes = file.length().coerceAtLeast(0L)
                if (file.delete()) reclaimed += bytes
            }
        }
        return reclaimed
    }

    /**
     * Deletes interrupted downloads and update APKs that are no longer needed.
     * Keeps a complete cached APK when it is still newer than the installed build.
     */
    fun clearStaleUpdateCache(context: Context): Long {
        var reclaimed = 0L
        val installed = installedVersionCode(context)
        context.cacheDir.listFiles()?.forEach { file ->
            if (!file.isFile) return@forEach
            val name = file.name
            when {
                name.startsWith("$UPDATE_APK_FILENAME.") ||
                    (name.startsWith(UPDATE_APK_PREFIX) && name.endsWith(".tmp")) -> {
                    val bytes = file.length().coerceAtLeast(0L)
                    if (file.delete()) reclaimed += bytes
                }
                name == UPDATE_APK_FILENAME -> {
                    val version = readApkVersionCode(context, file)
                    if (version == null || version <= installed) {
                        val bytes = file.length().coerceAtLeast(0L)
                        if (file.delete()) reclaimed += bytes
                    }
                }
            }
        }
        return reclaimed
    }

    fun launchInstaller(context: Context, apk: File): InstallResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            return InstallResult.NeedsPermission
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return InstallResult.Launched
    }

    private fun isUpdateCacheFile(name: String): Boolean =
        name == UPDATE_APK_FILENAME ||
            name.startsWith("$UPDATE_APK_FILENAME.") ||
            (name.startsWith(UPDATE_APK_PREFIX) && name.endsWith(".tmp"))

    private fun readApkVersionCode(context: Context, apk: File): Long? {
        val info = context.packageManager.getPackageArchiveInfo(
            apk.absolutePath,
            PackageManager.GET_META_DATA,
        ) ?: return null
        info.applicationInfo?.let { appInfo ->
            appInfo.sourceDir = apk.absolutePath
            appInfo.publicSourceDir = apk.absolutePath
        }
        if (info.packageName != context.packageName) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    }

    private fun openGet(url: String): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 30_000
        conn.readTimeout = 120_000
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("User-Agent", "QuickOtp-Android")
        conn.instanceFollowRedirects = true
        return conn
    }

    @kotlinx.serialization.Serializable
    private data class GhRelease(
        @SerialName("tag_name") val tagName: String,
        val assets: List<GhAsset> = emptyList(),
    )

    @kotlinx.serialization.Serializable
    private data class GhAsset(
        val name: String,
        @SerialName("browser_download_url") val browserDownloadUrl: String,
    )
}
