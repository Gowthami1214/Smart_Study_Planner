package com.example.studyplannerai.reminder

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.studyplannerai.core.util.Resource
import com.example.studyplannerai.domain.repository.StudyRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs once daily (or on app startup) to:
 * 1. Mark yesterday's incomplete tasks as "missed"
 * 2. Carry forward missed tasks to today (up to 3-day limit)
 * 3. Rebalance today's schedule timings
 */
@HiltWorker
class DailyCarryForwardWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val studyRepository: StudyRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("CarryForwardWorker", "Starting daily carry-forward check...")

            // Step 1: Mark old pending tasks as missed
            studyRepository.markOverdueAsMissed()

            // Step 2: Carry forward eligible tasks to today
            when (val result = studyRepository.carryForwardOverdueTasks()) {
                is Resource.Success -> {
                    val count = result.data ?: 0
                    Log.d("CarryForwardWorker", "Carried forward $count tasks")
                }
                is Resource.Error -> {
                    Log.e("CarryForwardWorker", "Carry-forward failed: ${result.message}")
                }
                else -> {}
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("CarryForwardWorker", "Worker failed", e)
            Result.retry()
        }
    }
}
