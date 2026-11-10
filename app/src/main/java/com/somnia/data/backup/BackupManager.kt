package com.somnia.data.backup

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.somnia.data.db.SomniaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Sauvegarde et restauration de toutes les données (base Room + réglages).
 *
 * Android efface les données d'une app à sa désinstallation. Somnia écrit donc une copie
 * dans Téléchargements/Somnia (qui survit à la désinstallation) et propose de la restaurer
 * au premier lancement. Le fichier reste sur le téléphone : rien n'est envoyé ailleurs.
 */
class BackupManager(private val context: Context, private val database: SomniaDatabase) {

    private val prefs = context.getSharedPreferences("backup", Context.MODE_PRIVATE)
    private val prefFiles = listOf("reminders", "game")

    val lastBackupAt: Long get() = prefs.getLong("last_backup", 0L)

    // ---------- Mode test (outil développeur) ----------
    private val snapshotFile get() = File(context.filesDir, "test_snapshot.zip")

    /** Un instantané de test existe : l'app est en « mode test ». */
    val testModeActive: Boolean get() = snapshotFile.exists()

    /** Photographie tout l'état actuel avant une session de test. */
    suspend fun startTestMode() = withContext(Dispatchers.IO) {
        snapshotFile.outputStream().use { export(it) }
    }

    /** Restaure l'instantané (l'app redémarre). @return une erreur, ou null. */
    suspend fun endTestMode(): String? = withContext(Dispatchers.IO) {
        val f = snapshotFile
        if (!f.exists()) return@withContext "Aucun test en cours."
        val copy = File(context.cacheDir, "snapshot_restore.zip")
        f.copyTo(copy, overwrite = true)
        f.delete()
        // En cas de succès l'app redémarre ; en cas d'échec on remet l'instantané en place.
        val error = copy.inputStream().use { restore(it) }
        if (error != null) copy.copyTo(f, overwrite = true)
        error
    }

    /** Abandonne le mode test en gardant l'état actuel. */
    fun discardTestMode() { snapshotFile.delete() }

    /** Écrit une archive zip de toutes les données dans [out]. */
    suspend fun export(out: OutputStream) = withContext(Dispatchers.IO) {
        // Rapatrie le journal WAL dans le fichier principal pour une copie cohérente.
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry(DB_ENTRY))
            context.getDatabasePath(SomniaDatabase.NAME).inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
            prefFiles.forEach { name ->
                val f = File(context.dataDir, "shared_prefs/$name.xml")
                if (f.exists()) {
                    zip.putNextEntry(ZipEntry("prefs/$name.xml"))
                    f.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
    }

    /**
     * Sauvegarde automatique dans Téléchargements/Somnia (Android 10+, sans permission).
     * Un fichier par jour, les 3 plus récents créés par cette installation sont conservés.
     */
    suspend fun autoBackup(force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext false
        if (testModeActive) return@withContext false // ne pas écraser les vraies sauvegardes avec des données de test
        if (!force && System.currentTimeMillis() - lastBackupAt < 20 * 3_600_000L) return@withContext true
        runCatching {
            val resolver = context.contentResolver
            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val name = "somnia-sauvegarde-${LocalDate.now()}.zip"
            val existing = ownBackups().firstOrNull { it.second == name }?.first
            val uri = existing ?: resolver.insert(collection, ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Somnia")
            }) ?: return@runCatching false
            resolver.openOutputStream(uri, "wt")!!.use { export(it) }
            ownBackups().drop(3).forEach { resolver.delete(it.first, null, null) }
            prefs.edit().putLong("last_backup", System.currentTimeMillis()).apply()
            true
        }.getOrDefault(false)
    }

    /** Sauvegardes créées par cette installation, de la plus récente à la plus ancienne. */
    private fun ownBackups(): List<Pair<Uri, String>> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return emptyList()
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val out = mutableListOf<Pair<Uri, String>>()
        context.contentResolver.query(
            collection,
            arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME),
            "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?", arrayOf("somnia-sauvegarde-%"),
            "${MediaStore.Downloads.DISPLAY_NAME} DESC",
        )?.use { c ->
            while (c.moveToNext()) {
                out += Uri.withAppendedPath(collection, c.getLong(0).toString()) to c.getString(1)
            }
        }
        return out
    }

    /**
     * Restaure une archive. En cas de succès, l'app redémarre d'elle-même.
     * @return un message d'erreur, ou null si tout s'est bien passé.
     */
    suspend fun restore(input: InputStream): String? = withContext(Dispatchers.IO) {
        val tmp = File(context.cacheDir, "restore").apply { deleteRecursively(); mkdirs() }
        try {
            ZipInputStream(input).use { zip ->
                while (true) {
                    val e = zip.nextEntry ?: break
                    val target = File(tmp, e.name).canonicalFile
                    if (!target.path.startsWith(tmp.canonicalPath + File.separator)) continue // protection zip-slip
                    target.parentFile?.mkdirs()
                    target.outputStream().use { zip.copyTo(it) }
                }
            }
            val db = File(tmp, DB_ENTRY)
            if (!db.exists()) return@withContext "Ce fichier n'est pas une sauvegarde Somnia."
            val version = try {
                SQLiteDatabase.openDatabase(db.path, null, SQLiteDatabase.OPEN_READONLY).use { sql ->
                    sql.rawQuery("SELECT count(*) FROM sqlite_master WHERE name = 'sleep_sessions'", null).use { c ->
                        c.moveToFirst(); if (c.getInt(0) == 0) return@withContext "Sauvegarde invalide."
                    }
                    sql.version
                }
            } catch (e: Exception) {
                return@withContext "Sauvegarde illisible."
            }
            if (version > SomniaDatabase.VERSION) {
                return@withContext "Cette sauvegarde vient d'une version plus récente de Somnia : mettez l'app à jour d'abord."
            }

            database.close()
            val dbFile = context.getDatabasePath(SomniaDatabase.NAME)
            listOf("", "-wal", "-shm", "-journal").forEach { File(dbFile.path + it).delete() }
            db.copyTo(dbFile, overwrite = true)
            prefFiles.forEach { name ->
                val f = File(tmp, "prefs/$name.xml")
                if (f.exists()) f.copyTo(File(context.dataDir, "shared_prefs/$name.xml"), overwrite = true)
            }
            restartApp()
            null
        } catch (e: Exception) {
            "Échec de la restauration : ${e.message}"
        } finally {
            tmp.deleteRecursively()
        }
    }

    private fun restartApp() {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        context.startActivity(Intent.makeRestartActivityTask(launch.component))
        Runtime.getRuntime().exit(0)
    }

    companion object {
        private const val DB_ENTRY = "somnia.db"
    }
}
