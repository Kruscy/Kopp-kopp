package hu.apk.kidsapp.data

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Stores the parent PIN as a salted SHA-256 hash in a private SharedPreferences file.
 * Good enough to keep a 4-year-old out of Settings; not meant as high-security storage.
 */
class PinManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("kids_app_lock", Context.MODE_PRIVATE)

    fun isPinSet(): Boolean = prefs.contains(KEY_HASH)

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hash(pin, salt)
        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val saltEncoded = prefs.getString(KEY_SALT, null) ?: return false
        val storedHash = prefs.getString(KEY_HASH, null) ?: return false
        val salt = Base64.decode(saltEncoded, Base64.NO_WRAP)
        val candidate = Base64.encodeToString(hash(pin, salt), Base64.NO_WRAP)
        return candidate == storedHash
    }

    var autoLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_LOCK, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_LOCK, value).apply()

    private fun hash(pin: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return digest.digest(pin.toByteArray(Charsets.UTF_8))
    }

    companion object {
        private const val KEY_SALT = "salt"
        private const val KEY_HASH = "hash"
        private const val KEY_AUTO_LOCK = "auto_lock"
    }
}
