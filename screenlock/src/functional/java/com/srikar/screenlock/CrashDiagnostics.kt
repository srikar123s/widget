package com.srikar.screenlock

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Lightweight, privacy-safe crash and lifecycle diagnostics tracker.
 *
 * Strictly records:
 * - Timestamps and service lifecycle events (CREATED, CONNECTED, UNBOUND, REBOUND, DESTROYED).
 * - Exception class names and stack traces if an uncaught crash occurs.
 *
 * Strictly DOES NOT record:
 * - Accessibility events, window contents, keystrokes, personal data, or network activity.
 */
object CrashDiagnostics {

    private const val TAG = "ScreenLockDiag"
    private const val MAX_RECENT_EVENTS = 20
    private const val LOG_FILE_NAME = "service_diagnostics.log"

    private val recentEvents = ConcurrentLinkedQueue<String>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    @Volatile
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            recordCrash(context, thread, throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        logEvent(context, "PROCESS_INITIALIZED (pid=${android.os.Process.myPid()})")
    }

    fun logEvent(context: Context?, event: String) {
        val timestamp = dateFormat.format(Date())
        val entry = "[$timestamp] $event"
        Log.i(TAG, entry)

        recentEvents.add(entry)
        while (recentEvents.size > MAX_RECENT_EVENTS) {
            recentEvents.poll()
        }

        if (context != null) {
            try {
                val file = File(context.filesDir, LOG_FILE_NAME)
                file.appendText("$entry\n")
                // Keep file under 64KB
                if (file.length() > 64 * 1024) {
                    val lines = file.readLines()
                    if (lines.size > 100) {
                        file.writeText(lines.takeLast(50).joinToString("\n") + "\n")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to write diagnostic event to disk", e)
            }
        }
    }

    private fun recordCrash(context: Context, thread: Thread, throwable: Throwable) {
        val timestamp = dateFormat.format(Date())
        val crashLog = StringBuilder()
            .append("=== UNCAUGHT EXCEPTION ===\n")
            .append("Time: ").append(timestamp).append("\n")
            .append("Thread: ").append(thread.name).append(" (id=").append(thread.id).append(")\n")
            .append("Exception: ").append(throwable.javaClass.name).append(": ").append(throwable.message).append("\n")
            .append("Stack trace:\n")
            .append(Log.getStackTraceString(throwable))
            .append("==========================\n")
            .toString()

        Log.e(TAG, crashLog)

        try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            file.appendText(crashLog)
        } catch (_: Exception) {
        }
    }

    fun getRecentEvents(): List<String> = recentEvents.toList()

    fun getDiagnosticsLog(context: Context): String {
        return try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists()) file.readText() else "No diagnostic logs recorded yet."
        } catch (e: Exception) {
            "Failed to read diagnostic logs: ${e.message}"
        }
    }

    fun clearLogs(context: Context) {
        recentEvents.clear()
        try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists()) file.delete()
        } catch (_: Exception) {
        }
    }
}

