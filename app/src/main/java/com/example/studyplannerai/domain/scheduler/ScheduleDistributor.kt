package com.example.studyplannerai.domain.scheduler

import com.example.studyplannerai.data.model.StudyPlanItem
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Distributes a flat list of AI-generated tasks across multiple days,
 * ensuring balanced daily workloads and proper time-slot assignment.
 */
object ScheduleDistributor {

    private const val DEFAULT_MAX_MINUTES_PER_DAY = 240 // 4 hours
    private const val DEFAULT_BREAK_MINUTES = 15
    private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Takes a list of [StudyPlanItem] (possibly all on the same day) and redistributes
     * them across [days] starting from [startDate].
     *
     * @param tasks           Raw task list from AI
     * @param days            Number of days to spread across (e.g., 7)
     * @param startDate       First day of the schedule
     * @param maxMinutesPerDay Max study minutes per day (default 240)
     * @param breakMinutes    Break duration between tasks (default 15)
     * @param startTime       Daily study start time (default 09:00)
     * @return Redistributed task list with corrected day and time_slot values
     */
    fun distribute(
        tasks: List<StudyPlanItem>,
        days: Int = 7,
        startDate: LocalDate = LocalDate.now(),
        maxMinutesPerDay: Int = DEFAULT_MAX_MINUTES_PER_DAY,
        breakMinutes: Int = DEFAULT_BREAK_MINUTES,
        startTime: LocalTime = LocalTime.of(9, 0)
    ): List<StudyPlanItem> {
        if (tasks.isEmpty()) return emptyList()

        // Sort by topic to keep related tasks together, then by original ordering
        val sorted = tasks.sortedWith(
            compareBy<StudyPlanItem> { it.topic }
                .thenBy { it.time_slot }
        )

        val result = mutableListOf<StudyPlanItem>()
        var currentDay = 0
        var currentDayMinutes = 0
        var currentTime = startTime

        for (task in sorted) {
            val taskDuration = task.duration_minutes.coerceIn(15, 180)

            // If adding this task exceeds the daily limit, move to next day
            if (currentDayMinutes + taskDuration > maxMinutesPerDay && currentDayMinutes > 0) {
                currentDay++
                currentDayMinutes = 0
                currentTime = startTime
            }

            // Wrap around if we exceed available days (put extras on last day)
            val actualDay = if (currentDay < days) currentDay else days - 1
            val date = startDate.plusDays(actualDay.toLong())

            // Build time slot string
            val slotStart = currentTime
            val slotEnd = currentTime.plusMinutes(taskDuration.toLong())
            val timeSlot = "${slotStart.format(TIME_FORMAT)}-${slotEnd.format(TIME_FORMAT)}"

            result.add(
                task.copy(
                    day = date.toString(),
                    time_slot = timeSlot,
                    originalDay = if (task.originalDay.isBlank()) date.toString() else task.originalDay
                )
            )

            // Advance time cursor (task duration + break)
            currentTime = slotEnd.plusMinutes(breakMinutes.toLong())
            currentDayMinutes += taskDuration + breakMinutes
        }

        return result
    }

    /**
     * Rebalances today's tasks by recalculating time slots starting from [startTime],
     * accounting for newly carried-forward tasks merged in.
     */
    fun rebalanceDayTimeslots(
        tasks: List<StudyPlanItem>,
        startTime: LocalTime = LocalTime.of(9, 0),
        breakMinutes: Int = DEFAULT_BREAK_MINUTES
    ): List<StudyPlanItem> {
        if (tasks.isEmpty()) return emptyList()

        // Sort: carried-forward tasks first (higher priority), then by existing time
        val sorted = tasks.sortedWith(
            compareByDescending<StudyPlanItem> { it.priority }
                .thenBy { it.time_slot }
        )

        var currentTime = startTime
        return sorted.map { task ->
            val duration = task.duration_minutes.coerceIn(15, 180)
            val slotStart = currentTime
            val slotEnd = currentTime.plusMinutes(duration.toLong())
            val timeSlot = "${slotStart.format(TIME_FORMAT)}-${slotEnd.format(TIME_FORMAT)}"
            currentTime = slotEnd.plusMinutes(breakMinutes.toLong())
            task.copy(time_slot = timeSlot)
        }
    }
}
