package com.threadpilot.manager.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** 把配置文本写成临时文件并通过系统分享出去。 */
fun shareJsonFile(context: Context, fileName: String, content: String) {
    val dir = File(context.filesDir, "shares").apply { mkdirs() }
    val file = File(dir, fileName)
    file.writeText(content, Charsets.UTF_8)

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, fileName)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "分享配置"))
}
