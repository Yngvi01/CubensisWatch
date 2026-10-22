package br.com.cubensiswatch.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class LocalPhotoStore(context: Context) {
    private val appContext = context.applicationContext
    private val photoDirectory = File(appContext.filesDir, "cultivation_photos")

    fun createCaptureFile(): File {
        check(photoDirectory.exists() || photoDirectory.mkdirs()) { "Não foi possível preparar o armazenamento de fotos." }
        return File(photoDirectory, "${UUID.randomUUID()}.jpg").apply { createNewFile() }
    }

    suspend fun import(uri: Uri): File = withContext(Dispatchers.IO) {
        checkNotNull(appContext.contentResolver.openInputStream(uri)) { "Não foi possível abrir a foto selecionada." }
            .use { input ->
                check(photoDirectory.exists() || photoDirectory.mkdirs()) { "Não foi possível preparar o armazenamento de fotos." }
                val file = File(photoDirectory, "${UUID.randomUUID()}.jpg")
                try {
                    file.outputStream().use(input::copyTo)
                    file
                } catch (error: Throwable) {
                    file.delete()
                    throw error
                }
            }
    }

    suspend fun copyIntoStore(source: File): File = withContext(Dispatchers.IO) {
        require(source.isFile) { "O arquivo de foto não existe." }
        check(photoDirectory.exists() || photoDirectory.mkdirs()) { "Não foi possível preparar o armazenamento de fotos." }
        val target = File(photoDirectory, "${UUID.randomUUID()}.jpg")
        try {
            source.inputStream().use { input -> target.outputStream().use(input::copyTo) }
            target
        } catch (error: Throwable) {
            target.delete()
            throw error
        }
    }

    suspend fun delete(path: String) = withContext(Dispatchers.IO) { File(path).takeIf { it.parentFile == photoDirectory }?.delete() }

    suspend fun deleteOrphans(referencedPaths: Set<String>, olderThan: Long = System.currentTimeMillis() - 24L * 60L * 60L * 1000L) = withContext(Dispatchers.IO) {
        photoDirectory.listFiles()?.filter { file ->
            file.isFile && file.absolutePath !in referencedPaths && file.lastModified() < olderThan
        }?.forEach(File::delete)
    }
}
