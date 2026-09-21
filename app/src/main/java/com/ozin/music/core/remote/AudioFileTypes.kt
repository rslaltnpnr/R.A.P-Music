package com.ozin.music.core.remote

/** Pure, testable extension-based audio detection, consistent with the
 * formats [com.ozin.music.core.data.mediastore.MediaStoreScanner] surfaces
 * for local files. */
object AudioFileTypes {
    val SUPPORTED_EXTENSIONS: Set<String> = setOf("mp3", "flac", "wav", "aac", "m4a", "ogg", "opus")

    fun isAudioFile(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext.isNotEmpty() && ext in SUPPORTED_EXTENSIONS
    }
}
