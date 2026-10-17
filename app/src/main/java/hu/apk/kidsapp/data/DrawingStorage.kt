package hu.apk.kidsapp.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

/**
 * Saves the child's photo sessions privately inside the app: one original (untouched capture)
 * and, once they tap "Kész", one finished (photo + drawing, flattened) PNG per session — both
 * sharing the same [id] so re-editing overwrites rather than piling up duplicate saves.
 */
object DrawingStorage {
    private const val ORIGINALS_DIR = "drawings/originals"
    private const val FINISHED_DIR = "drawings/finished"

    private fun originalsDir(context: Context): File =
        File(context.filesDir, ORIGINALS_DIR).apply { if (!exists()) mkdirs() }

    private fun finishedDir(context: Context): File =
        File(context.filesDir, FINISHED_DIR).apply { if (!exists()) mkdirs() }

    fun newSessionId(): String = System.currentTimeMillis().toString()

    fun originalFileFor(context: Context, id: String): File = File(originalsDir(context), "$id.jpg")

    fun finishedFileFor(context: Context, id: String): File = File(finishedDir(context), "$id.png")

    /** Overwrites any earlier save for this [id] — one photo session, one saved pair. */
    fun saveFinished(context: Context, bitmap: Bitmap, id: String): File {
        val file = finishedFileFor(context, id)
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        return file
    }

    fun listFinished(context: Context): List<File> =
        finishedDir(context).listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()

    fun idOf(finishedFile: File): String = finishedFile.nameWithoutExtension

    fun originalFor(context: Context, finishedFile: File): File = originalFileFor(context, idOf(finishedFile))

    fun loadBitmap(file: File): Bitmap? = BitmapFactory.decodeFile(file.absolutePath)

    fun loadThumbnail(file: File, targetSizePx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= targetSizePx && bounds.outHeight / (sample * 2) >= targetSizePx) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(file.absolutePath, opts)
    }

    /** Deletes both the finished drawing and its original photo for this session. */
    fun deleteSession(context: Context, finishedFile: File) {
        if (finishedFile.exists()) finishedFile.delete()
        val original = originalFor(context, finishedFile)
        if (original.exists()) original.delete()
    }
}
