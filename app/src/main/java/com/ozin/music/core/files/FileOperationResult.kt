package com.ozin.music.core.files

import android.content.IntentSender

/**
 * Outcome of a scoped-storage file operation (metadata write, rename,
 * delete). On API 29+, the OS may require user consent via a system dialog
 * before the app (which does not own the file, e.g. it wasn't inserted by
 * this app) can modify or delete it — that consent is surfaced as
 * [NeedsUserConsent] with the [IntentSender] to launch, and the caller
 * should retry the same operation after the consent flow returns OK.
 */
sealed interface FileOperationResult {
    data object Success : FileOperationResult
    data class NeedsUserConsent(val intentSender: IntentSender) : FileOperationResult
    data class Failed(val message: String) : FileOperationResult
}

data class BatchOperationSummary(
    val successCount: Int,
    val failures: List<Pair<String, String>>, // (songTitle/path, error message)
)
