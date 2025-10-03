package com.app.saudeemdia

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import kotlin.math.max

object ReminderScheduler {
    fun schedule(context: Context, delayMillis: Long, title: String, message: String) {
        val delay = max(0L, delayMillis)
        val data = workDataOf("title" to title, "message" to message)

        val req = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(req)
    }
}
