package cloud.clausevault.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

fun copyUriToCacheFile(context: Context, uri: Uri): Pair<File, String> {
    val cr = context.contentResolver
    val mime = cr.getType(uri) ?: "application/octet-stream"
    val ext = when {
        mime.contains("pdf") -> "pdf"
        mime.contains("msword") && !mime.contains("openxml") -> "doc"
        mime.contains("wordprocessingml") || mime.contains("officedocument") -> "docx"
        else -> "pdf"
    }
    val f = File(context.cacheDir, "upload_${System.currentTimeMillis()}.$ext")
    cr.openInputStream(uri)?.use { input ->
        f.outputStream().use { output -> input.copyTo(output) }
    } ?: error("Could not read file")
    return Pair(f, mime)
}

fun shareExportBytes(context: Context, bytes: ByteArray, fileName: String, mimeType: String) {
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val f = File(dir, fileName)
    f.writeBytes(bytes)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share"))
}

fun openUrl(context: Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}
