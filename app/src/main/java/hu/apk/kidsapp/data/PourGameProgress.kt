package hu.apk.kidsapp.data

import android.content.Context

/** Remembers which színöntögető level the child last reached. */
class PourGameProgress(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("pour_game_progress", Context.MODE_PRIVATE)

    var currentLevel: Int
        get() = prefs.getInt(KEY_LEVEL, 0)
        set(value) = prefs.edit().putInt(KEY_LEVEL, value).apply()

    companion object {
        private const val KEY_LEVEL = "level"
    }
}
