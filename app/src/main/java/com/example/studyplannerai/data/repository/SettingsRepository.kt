package com.example.studyplannerai.data.repository

import android.content.Context
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class UserSettings(
    val weeklyViewEnabled: Boolean = true,
    val autoRescheduleEnabled: Boolean = true
)

class SettingsRepository(context: Context) {

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun observeSettings(): Flow<UserSettings> = callbackFlow {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(readSettings())
        }

        trySend(readSettings())
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    suspend fun updateWeeklyView(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_WEEKLY_VIEW, enabled).apply()
    }

    suspend fun updateAutoReschedule(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTO_RESCHEDULE, enabled).apply()
    }

    private fun readSettings(): UserSettings {
        return UserSettings(
            weeklyViewEnabled = preferences.getBoolean(KEY_WEEKLY_VIEW, true),
            autoRescheduleEnabled = preferences.getBoolean(KEY_AUTO_RESCHEDULE, true)
        )
    }

    private companion object {
        const val PREFS_NAME = "study_planner_settings"
        const val KEY_WEEKLY_VIEW = "weekly_view_enabled"
        const val KEY_AUTO_RESCHEDULE = "auto_reschedule_enabled"
    }
}
