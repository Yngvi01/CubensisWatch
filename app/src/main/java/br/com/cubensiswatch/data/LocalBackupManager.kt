package br.com.cubensiswatch.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.room.withTransaction
import br.com.cubensiswatch.notifications.ReminderScheduler
import br.com.cubensiswatch.notifications.StockAlertScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class LocalBackupManager(
    context: Context,
    private val database: AppDatabase,
    private val photoStore: LocalPhotoStore,
) {
    private val appContext = context.applicationContext
    private val photoTables = listOf("photos", "culture_photos", "harvest_photos", "note_photos", "material_photos")
    private val tableOrder = listOf(
        "lineages", "incubators", "cultivations", "cultures", "culture_events", "materials", "reminders",
        "incubator_readings", "cultivation_events", "photos", "culture_photos",
        "stock_movements", "shopping_items", "harvests", "harvest_photos", "notes", "note_photos", "material_photos", "cultivation_experiments",
    )
    private val deleteOrder = tableOrder.asReversed()

    suspend fun export(uri: Uri) = withContext(Dispatchers.IO) {
        val resolver = appContext.contentResolver
        val databaseFile = appContext.getDatabasePath("cubensis_watch.db")
        check(databaseFile.isFile) { "O banco local ainda não foi criado." }
        checkpointForBackup()
        val photoRecords = readPhotoRecords()
        photoRecords.forEach { record ->
            check(File(record.localPath).isFile) { "Uma foto vinculada não foi encontrada. O backup foi cancelado para evitar perda de dados." }
        }
        val output = checkNotNull(resolver.openOutputStream(uri, "wt")) { "Não foi possível criar o arquivo de backup." }
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(DATABASE_ENTRY))
            FileInputStream(databaseFile).use { it.copyTo(zip) }
            zip.closeEntry()

            val photoManifest = JSONArray()
            photoRecords.forEach { record ->
                val entryName = "photos/${record.table}/${record.id}.jpg"
                zip.putNextEntry(ZipEntry(entryName))
                FileInputStream(record.localPath).use { it.copyTo(zip) }
                zip.closeEntry()
                photoManifest.put(JSONObject().put("table", record.table).put("id", record.id).put("entry", entryName))
            }

            val packageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            val version = packageInfo.versionName ?: ""
            val manifest = JSONObject()
                .put("backupVersion", BACKUP_VERSION)
                .put("databaseVersion", database.openHelper.writableDatabase.version)
                .put("createdAt", System.currentTimeMillis())
                .put("appVersion", version)
                .put("photos", photoManifest)
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }

    suspend fun restore(uri: Uri) = withContext(Dispatchers.IO) {
        val tempRoot = File(appContext.cacheDir, "restore-${UUID.randomUUID()}").apply { check(mkdirs()) }
        val extracted = File(tempRoot, "archive").apply { check(mkdirs()) }
        val copiedPhotoPaths = mutableListOf<String>()
        var databaseRestored = false
        try {
            val archiveFile = File(tempRoot, "backup.zip")
            checkNotNull(appContext.contentResolver.openInputStream(uri)) { "Não foi possível abrir o backup." }
                .use { input -> archiveFile.outputStream().use(input::copyTo) }
            extractArchive(archiveFile, extracted)

            val manifestFile = File(extracted, MANIFEST_ENTRY)
            require(manifestFile.isFile) { "Este arquivo não contém um manifesto de backup." }
            val manifest = JSONObject(manifestFile.readText(Charsets.UTF_8))
            require(manifest.optInt("backupVersion", -1) == BACKUP_VERSION) { "Versão de backup não suportada." }
            require(manifest.optInt("databaseVersion", -1) == database.openHelper.writableDatabase.version) {
                "A versão do banco no backup não corresponde a esta versão do aplicativo."
            }

            val importedDatabase = File(extracted, DATABASE_ENTRY)
            require(importedDatabase.isFile) { "O arquivo de banco de dados não está no backup." }
            validateDatabase(importedDatabase)

            val photoRecords = parsePhotoManifest(manifest.getJSONArray("photos"), extracted)
            validatePhotoManifest(importedDatabase, photoRecords)
            val remappedPaths = photoRecords.map { record ->
                val restoredFile = photoStore.copyIntoStore(File(extracted, record.entry))
                copiedPhotoPaths += restoredFile.absolutePath
                record.copy(localPath = restoredFile.absolutePath)
            }
            val oldPhotoPaths = readPhotoRecords().map { it.localPath }.toSet()
            val previousReminders = database.reminderDao().observeAll().first()
            replaceDatabaseContents(importedDatabase, remappedPaths)
            databaseRestored = true

            val newPhotoPaths = remappedPaths.map { it.localPath }.toSet()
            (oldPhotoPaths - newPhotoPaths).forEach { photoStore.delete(it) }
            previousReminders.forEach { ReminderScheduler.cancel(appContext, it.id) }
            database.reminderDao().observeAll().first().filter { it.enabled }.forEach { ReminderScheduler.schedule(appContext, it) }
            StockAlertScheduler.schedule(appContext)
        } catch (error: Throwable) {
            if (!databaseRestored) copiedPhotoPaths.forEach { photoStore.delete(it) }
            throw error
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    private fun extractArchive(archive: File, target: File) {
        var entryCount = 0
        var extractedBytes = 0L
        val basePath = target.canonicalPath + File.separator
        ZipInputStream(FileInputStream(archive).buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entryCount += 1
                require(entryCount <= MAX_ARCHIVE_ENTRIES) { "O backup contém arquivos demais." }
                require(!entry.isDirectory) { "Pastas explícitas não são permitidas no backup." }
                val destination = File(target, entry.name)
                require(destination.canonicalPath.startsWith(basePath)) { "O backup contém um caminho inválido." }
                destination.parentFile?.let { require(it.exists() || it.mkdirs()) }
                destination.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = zip.read(buffer)
                        if (read < 0) break
                        extractedBytes += read
                        require(extractedBytes <= MAX_ARCHIVE_BYTES) { "O backup excede o tamanho máximo permitido." }
                        output.write(buffer, 0, read)
                    }
                }
                zip.closeEntry()
            }
        }
    }

    private fun validateDatabase(file: File) {
        val imported = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        try {
            require(imported.version == database.openHelper.writableDatabase.version) { "A versão do banco de dados não é compatível." }
            imported.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                require(cursor.moveToFirst() && cursor.getString(0) == "ok") { "O banco de dados do backup está corrompido." }
            }
            val existingTables = mutableSetOf<String>()
            imported.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table'", null).use { cursor ->
                while (cursor.moveToNext()) existingTables += cursor.getString(0)
            }
            require(tableOrder.all { it in existingTables }) { "O banco de dados do backup não contém todas as tabelas necessárias." }
        } finally {
            imported.close()
        }
    }

    private fun parsePhotoManifest(array: JSONArray, root: File): List<PhotoRecord> {
        val seen = mutableSetOf<Pair<String, Long>>()
        return (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            val table = item.getString("table")
            val id = item.getLong("id")
            val entry = item.getString("entry")
            require(table in photoTables) { "O manifesto referencia uma tabela de fotos inválida." }
            require(seen.add(table to id)) { "O manifesto possui fotos duplicadas." }
            require(entry == "photos/$table/$id.jpg") { "O manifesto possui um caminho de foto inválido." }
            val file = File(root, entry)
            require(file.canonicalPath.startsWith(root.canonicalPath + File.separator) && file.isFile) { "Uma foto do backup está ausente ou fora do arquivo." }
            PhotoRecord(table, id, entry, "")
        }
    }

    private fun validatePhotoManifest(databaseFile: File, records: List<PhotoRecord>) {
        val imported = SQLiteDatabase.openDatabase(databaseFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        try {
            photoTables.forEach { table ->
                val ids = mutableSetOf<Long>()
                imported.rawQuery("SELECT id FROM $table", null).use { cursor -> while (cursor.moveToNext()) ids += cursor.getLong(0) }
                val manifestIds = records.filter { it.table == table }.map { it.id }.toSet()
                require(ids == manifestIds) { "O manifesto não corresponde às fotos registradas no banco." }
            }
        } finally {
            imported.close()
        }
    }

    private fun checkpointForBackup() {
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { cursor ->
            check(cursor.moveToFirst()) { "Não foi possível preparar o banco para o backup." }
            check(cursor.getInt(0) == 0 && cursor.getInt(1) <= cursor.getInt(2)) {
                "O banco está ocupado. Aguarde e tente exportar o backup novamente."
            }
        }
    }

    private suspend fun replaceDatabaseContents(source: File, photoRecords: List<PhotoRecord>) {
        val db = database.openHelper.writableDatabase
        db.query("PRAGMA wal_checkpoint(FULL)").use { }
        db.execSQL("PRAGMA foreign_keys = OFF")
        db.execSQL("ATTACH DATABASE ? AS backup_import", arrayOf(source.absolutePath))
        try {
            database.withTransaction {
                deleteOrder.forEach { table -> db.execSQL("DELETE FROM main.$table") }
                tableOrder.forEach { table -> db.execSQL("INSERT INTO main.$table SELECT * FROM backup_import.$table") }
                photoRecords.forEach { record ->
                    db.execSQL("UPDATE main.${record.table} SET localPath = ? WHERE id = ?", arrayOf(record.localPath, record.id))
                }
                db.query("PRAGMA foreign_key_check").use { cursor ->
                    check(!cursor.moveToFirst()) { "O backup possui relações inválidas e não pode ser restaurado." }
                }
            }
        } finally {
            db.execSQL("DETACH DATABASE backup_import")
            db.execSQL("PRAGMA foreign_keys = ON")
        }
        database.invalidationTracker.refreshVersionsAsync()
    }

    private fun readPhotoRecords(): List<PhotoRecord> {
        val db = database.openHelper.writableDatabase
        return photoTables.flatMap { table ->
            db.query("SELECT id, localPath FROM $table").use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(PhotoRecord(table, cursor.getLong(0), "", cursor.getString(1)))
                }
            }
        }
    }

    private data class PhotoRecord(val table: String, val id: Long, val entry: String, val localPath: String)

    companion object {
        private const val BACKUP_VERSION = 1
        private const val DATABASE_ENTRY = "database/cubensis_watch.db"
        private const val MANIFEST_ENTRY = "manifest.json"
        private const val MAX_ARCHIVE_ENTRIES = 10_000
        private const val MAX_ARCHIVE_BYTES = 1024L * 1024L * 1024L
    }
}
