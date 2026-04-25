package com.example.studyplannerai.data.repository

import android.util.Log
import com.example.studyplannerai.core.util.Resource
import com.example.studyplannerai.data.model.*
import com.example.studyplannerai.data.remote.GroqService
import com.example.studyplannerai.domain.repository.AiRepository
import com.example.studyplannerai.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import javax.inject.Inject

class AiRepositoryImpl @Inject constructor(
    private val groqService: GroqService
) : AiRepository {
    
    private val rawKey = BuildConfig.GROQ_API_KEY.replace("\"", "").trim()
    private val apiKey = if (rawKey.startsWith("Bearer ")) rawKey else "Bearer $rawKey"

    // DTO for safe parsing
    private data class AiResponseDto(
        val schedule: List<StudyPlanItem>? = null,
        val history: List<StudyPlanItem>? = null,
        val topics: List<String>? = null
    )

    override suspend fun getTopicsForSubject(subject: String): Resource<List<String>> {
        val prompt = """
            {
              "mode": "topics",
              "subject": "$subject"
            }
            Generate 5–10 realistic topics. granular names. Return ONLY JSON.
        """.trimIndent()

        val request = GroqRequest(
            messages = listOf(
                GroqMessage(role = "system", content = "You are a professional API. Return ONLY valid JSON. No explanations."),
                GroqMessage(role = "user", content = prompt)
            )
        )

        return try {
            val response = groqService.getChatCompletion(apiKey, request)
            val responseText = response.choices.firstOrNull()?.message?.content ?: ""
            
            // Clean markdown blocks if LLM ignores rules
            val cleanText = responseText.replace("```json", "").replace("```", "").trim()
            
            val aiResponse = Gson().fromJson(cleanText, AiResponseDto::class.java)
            val topics = aiResponse?.topics ?: throw Exception("Invalid API Response Format: Missing topics")
            
            Resource.Success(topics)
        } catch (e: JsonSyntaxException) {
            Resource.Error("API returned malformed JSON: ${e.message}")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun generateSchedule(
        selectedTopics: List<String>,
        availableHours: Int,
        preferredTime: String,
        breakDuration: Int
    ): Resource<List<StudyPlanItem>> {
        val prompt = """
            {
              "mode": "schedule",
              "selected_topics": ${Gson().toJson(selectedTopics)},
              "hours_per_day": $availableHours,
              "preferred_time": "$preferredTime",
              "break_duration": $breakDuration
            }
            Rules: 24h format, include breaks. 
            - Use ISO dates (YYYY-MM-DD) for 'day' starting from today (${java.time.LocalDate.now()})
            - realistic time slots (HH:mm) for 'time_slot'
            'task' must be a specific actionable instruction (e.g. "Solve 10 recursion problems" NOT "Study recursion").
            MUST include "duration_minutes" (30-120)
            MUST include "status": "pending"
            MUST include "reminder_offset_minutes": 10
            Return ONLY valid JSON with exactly the keys 'schedule' and 'history' (empty array). Do not truncate tasks. No markdown formatting.
        """.trimIndent()

        val request = GroqRequest(
            messages = listOf(
                GroqMessage(role = "system", content = "You are a senior backend API. Return ONLY valid JSON with 'schedule' and 'history' keys. No markdown, no explanations."),
                GroqMessage(role = "user", content = prompt)
            )
        )

        return try {
            val response = groqService.getChatCompletion(apiKey, request)
            val responseText = response.choices.firstOrNull()?.message?.content ?: ""
            
            val cleanText = responseText.replace("```json", "").replace("```", "").trim()
            val aiResponse = Gson().fromJson(cleanText, AiResponseDto::class.java)
            val schedule = aiResponse?.schedule ?: throw Exception("Invalid API Response Format: Missing schedule array")
            
            val finalizedSchedule = schedule.mapIndexed { index, item ->
                item.copy(id = "${System.currentTimeMillis()}_$index")
            }
            Resource.Success(finalizedSchedule)
        } catch (e: JsonSyntaxException) {
            Resource.Error("API returned malformed JSON: ${e.message}")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun generateScheduleForSubject(subject: String): Resource<List<StudyPlanItem>> {
        val prompt = """
            {
              "mode": "generate",
              "subject": "$subject"
            }
            Generate a single study task focused strictly on the subject: "$subject".
            Return ONLY valid JSON with exactly the keys 'schedule' and 'history' (empty array). No markdown formatting or explanation text.
            Rules:
            - The task MUST be directly related to studying or practicing the subject "$subject". No unrelated tasks like checking emails.
            - Use ISO dates (YYYY-MM-DD) for 'day' starting from today (${java.time.LocalDate.now()})
            - realistic time slots (HH:mm)
            - MUST include "duration_minutes" (30-120)
            - MUST include "status": "pending"
            - MUST include "reminder_offset_minutes": 10
            - actionable tasks
            - Include ALL generated tasks (no truncation)
        """.trimIndent()

        val request = GroqRequest(
            messages = listOf(
                GroqMessage(role = "system", content = "You are a senior backend API. Return ONLY valid JSON with 'schedule' and 'history' keys. No markdown, no explanations."),
                GroqMessage(role = "user", content = prompt)
            )
        )

        return try {
            val response = groqService.getChatCompletion(apiKey, request)
            val responseText = response.choices.firstOrNull()?.message?.content ?: ""
            
            val cleanText = responseText.replace("```json", "").replace("```", "").trim()
            val aiResponse = Gson().fromJson(cleanText, AiResponseDto::class.java)
            val schedule = aiResponse?.schedule ?: throw Exception("Invalid API Response Format: Missing schedule array")
            
            val finalizedSchedule = schedule.mapIndexed { index, item ->
                item.copy(id = "${System.currentTimeMillis()}_$index")
            }
            Resource.Success(finalizedSchedule)
        } catch (e: JsonSyntaxException) {
            Resource.Error("API returned malformed JSON: ${e.message}")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
    }
    override suspend fun chatWithAssistant(userMessage: String, historyContext: String): Resource<String> {
        val request = GroqRequest(
            messages = listOf(
                GroqMessage(
                    role = "system",
                    content = """You are an expert AI study assistant embedded in a study planner app.
Help students with:
- Explaining topics clearly (simple language first, then depth)
- Study strategies and time management
- Motivation and overcoming procrastination
- Breaking complex topics into manageable steps
Be concise, friendly, and practical. Use bullet points when listing steps. Max 3 paragraphs."""
                ),
                GroqMessage(role = "user", content = userMessage)
            ),
            response_format = ResponseFormat("text")
        )

        return try {
            val response = groqService.getChatCompletion(apiKey, request)
            val text = response.choices.firstOrNull()?.message?.content?.trim()
                ?: return Resource.Error("No response from AI")
            Resource.Success(text)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Chat error")
        }
    }
}
