package com.somnia.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.somnia.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val versionCode: Int, val title: String, val notes: String, val apkUrl: String)

sealed interface UpdateCheck {
    data class Available(val info: UpdateInfo) : UpdateCheck
    data object UpToDate : UpdateCheck
    data class Failed(val reason: String) : UpdateCheck
}

/**
 * Vérifie la dernière release GitHub du dépôt. Le workflow CI publie chaque build de la
 * branche principale sous le tag `build-<numéro>`, ce numéro étant aussi le versionCode.
 * Seule une requête anonyme vers l'API publique de GitHub est faite : aucune donnée envoyée.
 */
object UpdateChecker {
    private val currentCode get() = BuildConfig.VERSION_CODE

    suspend fun check(): UpdateCheck = withContext(Dispatchers.IO) {
        try {
            val conn = (URL("https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/releases/latest").openConnection() as HttpURLConnection).apply {
                setRequestProperty("Accept", "application/vnd.github+json")
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            val code = conn.responseCode
            if (code == 404) return@withContext UpdateCheck.Failed("Aucune release accessible (dépôt privé ou pas encore de release)")
            if (code != 200) return@withContext UpdateCheck.Failed("Réponse GitHub $code")
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val tag = json.getString("tag_name")
            val version = tag.substringAfterLast('-').toIntOrNull()
                ?: return@withContext UpdateCheck.Failed("Tag inattendu : $tag")
            val assets = json.getJSONArray("assets")
            var url: String? = null
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.getString("name").endsWith(".apk")) { url = a.getString("browser_download_url"); break }
            }
            if (url == null) return@withContext UpdateCheck.Failed("Pas d'APK dans la release $tag")
            if (version <= currentCode) UpdateCheck.UpToDate
            else UpdateCheck.Available(UpdateInfo(version, json.optString("name", tag), json.optString("body", ""), url))
        } catch (e: Exception) {
            UpdateCheck.Failed(e.message ?: "Erreur réseau")
        }
    }
}

object UpdateInstaller {

    /** Télécharge l'APK dans le cache de l'app en rapportant la progression (0..1, ou -1 si inconnue). */
    suspend fun download(context: Context, info: UpdateInfo, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "somnia-${info.versionCode}.apk")
        val conn = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 15_000
            readTimeout = 30_000
        }
        if (conn.responseCode !in 200..299) throw IllegalStateException("Téléchargement refusé (${conn.responseCode})")
        val total = conn.contentLengthLong
        conn.inputStream.use { input ->
            file.outputStream().use { out ->
                val buf = ByteArray(64 * 1024)
                var done = 0L
                var lastReported = -1
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    done += n
                    if (total > 0) {
                        val pct = (done * 100 / total).toInt()
                        if (pct != lastReported) { lastReported = pct; onProgress(pct / 100f) }
                    } else onProgress(-1f)
                }
            }
        }
        file
    }

    fun canInstall(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    fun unknownSourcesSettings(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))

    /** Ouvre l'installateur système sur l'APK téléchargé. */
    fun install(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
