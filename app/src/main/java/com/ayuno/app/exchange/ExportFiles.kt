package com.ayuno.app.exchange

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.ayuno.domain.usecase.IMPORT_MAX_BYTES
import java.io.File

/**
 * Archivos exportados (ADR-SEG-003): se escriben en la caché privada, se entregan con la hoja de compartir y se
 * borran en cada arranque. La app no envía nada por su cuenta.
 */
object ExportFiles {
    private fun dir(context: Context) = File(context.cacheDir, "exports")

    fun clear(context: Context) {
        dir(context).listFiles()?.forEach { it.delete() }
    }

    fun write(context: Context, name: String, bytes: ByteArray): Uri {
        val folder = dir(context).apply { mkdirs() }
        val file = File(folder, name).apply { writeBytes(bytes) }
        return FileProvider.getUriForFile(context, "${context.packageName}.exports", file)
    }

    /** Hoja de compartir con uno o dos archivos (el JSON y, si se pide, las tablas). */
    fun shareIntent(uris: List<Uri>, title: String): Intent {
        val send = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_STREAM, uris.single())
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).setType("*/*").putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
        send.clipData = ClipData.newRawUri(null, uris.first()).apply { uris.drop(1).forEach { addItem(ClipData.Item(it)) } }
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Intent.createChooser(send, title)
    }

    fun displayName(context: Context, uri: Uri): String =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: uri.lastPathSegment.orEmpty()

    /** Lee un archivo elegido por la persona sin pasar del límite (ADR-SEG-006); más grande = nulo. */
    fun readLimited(context: Context, uri: Uri): ByteArray? = context.contentResolver.openInputStream(uri)?.use { input ->
        val bytes = input.readNBytesCompat(IMPORT_MAX_BYTES + 1)
        bytes.takeIf { it.size <= IMPORT_MAX_BYTES }
    }

    private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        while (out.size() < limit) {
            val read = read(buffer, 0, minOf(buffer.size, limit - out.size()))
            if (read < 0) break
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }
}
