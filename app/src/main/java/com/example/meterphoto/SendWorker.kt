package com.example.meterphoto

import android.content.Context
import androidx.work.*
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Фоновая отправка очереди. WorkManager сам:
 *  - ждёт появления интернета (Constraint),
 *  - повторяет попытки с экспоненциальной задержкой 30 сек → 1 мин → 2 мин...
 *  - переживает перезагрузку телефона.
 */
class SendWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = SettingsStore(applicationContext)
        val outbox = OutboxStore(applicationContext)

        if (!settings.isConfigured || outbox.count == 0) return Result.success()

        return try {
            for (item in outbox.all()) {
                val file = File(item.filePath)
                if (!file.exists()) {          // фото удалили — пропускаем
                    outbox.remove(item.filePath)
                    continue
                }
                MailSender.send(
                    to = settings.recipient,
                    from = settings.senderEmail,
                    password = settings.senderPassword,
                    subject = "Счётчик ${item.number}",
                    body = "Фото счётчика №${item.number}",
                    file = file,
                    smtpHost = settings.smtpHost,
                    smtpPort = settings.smtpPort
                )
                outbox.remove(item.filePath)   // отправлено — убираем из очереди
            }
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 6) Result.retry() else Result.failure()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val request = OneTimeWorkRequestBuilder<SendWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("send_outbox", ExistingWorkPolicy.REPLACE, request)
        }
    }
}
