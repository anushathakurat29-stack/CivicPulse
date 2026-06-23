package com.example.data.network

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class Part(val text: String? = null)

@JsonClass(generateAdapter = true)
data class Content(val parts: List<Part>)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val responseMimeType: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(val content: Content? = null)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(val candidates: List<Candidate>? = null)

@JsonClass(generateAdapter = true)
data class GeminiIssueAnalysis(
    val category: String,
    val priority: String,
    val estimatedDaysToResolve: Int,
    val actionPlan: String,
    val citizenTone: String = "NEUTRAL",
    val isImmediateSafetyHazard: Boolean = false,
    val cleanedDescription: String = "",
    val hazardJustification: String = ""
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun analyzeIssue(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: GeminiApiService = retrofit.create(GeminiApiService::class.java)

    suspend fun getIssueIntelligentAnalysis(
        title: String,
        description: String,
        location: String
    ): GeminiIssueAnalysis {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return getFallbackAnalysis(title, description)
        }

        val prompt = """
            You are a helpful expert municipal automated coordinator assistant. 
            Analyze the following citizen-reported community issue and suggest:
            1. An appropriate category (choose exactly from: "Road", "Lighting", "Sanitation", "Safety", "Utilities", "Environment", or "Other")
            2. A calculated priority rating ("LOW", "MEDIUM", "HIGH") based on urgency, danger, and citizen impact.
            3. Estimated number of work days (integer) standard municipality teams take to address this.
            4. A realistic, professional, municipal step-by-step resolution action plan (numbered list, e.g. "1. Inspect... 2. ...") to resolve the issue transparently.
            5. Detected Citizen Emotional Tone (choose exactly from: "ANGRY", "ANXIOUS", "HELPFUL", "URGENT", "NEUTRAL").
            6. Is Immediate Safety Hazard (boolean true/false) - set to true only if there is a severe direct threat to life, limb, or property (such as exposed power cables, active flooding, toxic spills, structural collapses).
            7. Cleaned Description (string) - A translation of the citizen's report into an objective, professionally cleaned, emotionally neutral, and technical description suitable for municipal work orders. Strip out shouting (ALL CAPS), hyperbole, venting, and insults.
            8. Hazard Justification (string) - A short, professional explanation of why this was flagged as an immediate hazard or why the priority was assigned.

            Report Details:
            Title: $title
            Description: $description
            Location: $location

            Return a strict, valid JSON object with matching keys:
            {
               "category": "Sanitation",
               "priority": "MEDIUM",
               "estimatedDaysToResolve": 5,
               "actionPlan": "1. Dispatch sanitation crew to cleanup ...\n2. Safely dispose of hazardous debris.\n3. Wash and disinfect target public grounds.",
               "citizenTone": "ANGRY",
               "isImmediateSafetyHazard": false,
               "cleanedDescription": "Reported dumping of debris on public pathway, blocking access.",
               "hazardJustification": "Debris blocks stroller and wheelchair access, posing a secondary traffic diversion risk."
            }
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                temperature = 0.2f,
                responseMimeType = "application/json"
            )
        )

        return try {
            val response = service.analyzeIssue(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                val adapter = moshi.adapter(GeminiIssueAnalysis::class.java)
                adapter.fromJson(jsonText) ?: getFallbackAnalysis(title, description)
            } else {
                getFallbackAnalysis(title, description)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            getFallbackAnalysis(title, description)
        }
    }

    private fun getFallbackAnalysis(title: String, description: String): GeminiIssueAnalysis {
        val lowerText = "$title $description".lowercase()
        val category = when {
            lowerText.contains("pothole") || lowerText.contains("road") || lowerText.contains("asphalt") || lowerText.contains("pavement") || lowerText.contains("sidewalk") -> "Road"
            lowerText.contains("light") || lowerText.contains("dark") || lowerText.contains("lamp") || lowerText.contains("bulb") || lowerText.contains("blackout") -> "Lighting"
            lowerText.contains("trash") || lowerText.contains("garbage") || lowerText.contains("litter") || lowerText.contains("waste") || lowerText.contains("dump") -> "Sanitation"
            lowerText.contains("leak") || lowerText.contains("water") || lowerText.contains("pipe") || lowerText.contains("sewer") || lowerText.contains("hydrant") -> "Utilities"
            lowerText.contains("tree") || lowerText.contains("park") || lowerText.contains("plant") || lowerText.contains("smoke") || lowerText.contains("pollution") || lowerText.contains("animal") -> "Environment"
            lowerText.contains("dangerous") || lowerText.contains("vandalism") || lowerText.contains("theft") || lowerText.contains("speeding") || lowerText.contains("harass") || lowerText.contains("graffiti") || lowerText.contains("wire") -> "Safety"
            else -> "Other"
        }

        val priority = when {
            lowerText.contains("emergency") || lowerText.contains("danger") || lowerText.contains("broken pipe") || lowerText.contains("wiring") || lowerText.contains("wire") || lowerText.contains("accident") || lowerText.contains("hazard") || lowerText.contains("flood") -> "HIGH"
            lowerText.contains("blocking") || lowerText.contains("foul") || lowerText.contains("broken") || lowerText.contains("smell") -> "MEDIUM"
            else -> "LOW"
        }

        val days = when (priority) {
            "HIGH" -> 2
            "MEDIUM" -> 5
            else -> 10
        }

        // Detect Tone
        val tone = when {
            lowerText.contains("angry") || lowerText.contains("useless") || lowerText.contains("ridiculous") || lowerText.contains("furious") || lowerText.contains("!") || lowerText.contains("terrible") -> "ANGRY"
            lowerText.contains("scared") || lowerText.contains("scary") || lowerText.contains("fear") || lowerText.contains("worry") || lowerText.contains("hazard") -> "ANXIOUS"
            lowerText.contains("please") || lowerText.contains("thanks") || lowerText.contains("thank you") || lowerText.contains("helpful") -> "HELPFUL"
            lowerText.contains("urgent") || lowerText.contains("immediate") || lowerText.contains("spewing") || lowerText.contains("emergency") -> "URGENT"
            else -> "NEUTRAL"
        }

        // Detect Immediate Safety Hazard
        val isImmediateSafetyHazard = lowerText.contains("wire") || lowerText.contains("spewing") || lowerText.contains("leak") || lowerText.contains("flooding") || lowerText.contains("toxic") || lowerText.contains("accident") || lowerText.contains("hazard")

        // Clean Description
        var cleaned = description
            .replace(Regex("(?i)useless city"), "municipality")
            .replace(Regex("(?i)ridiculous"), "concerning")
            .replace(Regex("(?i)terrible"), "substandard")
            .replace(Regex("[!]{2,}"), "!")
            .replace(Regex("(?i)fix this now"), "requires maintenance")
        
        // Remove screaming CAPS if any
        if (cleaned.length > 5 && cleaned == cleaned.uppercase()) {
            cleaned = cleaned.lowercase().replaceFirstChar { it.uppercase() }
        }

        val justification = if (isImmediateSafetyHazard) {
            "Flagged automatically due to high risk of localized environmental contamination, utility disruption, or electrical threat detected in description parameters."
        } else {
            "No immediate life safety or catastrophic structure damage markers detected. Triaged for standard public works ticket queue."
        }

        val plan = """
            1. Report Logged: Issue logged in the CivicResolve automated public ledger.
            2. Team Assigned: Public works team assigned to $category dispatch category.
            3. Site Inspection: Immediate site inspection within 24-48 hours.
            4. Execution: Deploy repair contractors to execute local remediation.
            5. Audit & Sign-off: Verify resolution with photographic proof and update reporter.
        """.trimIndent()

        return GeminiIssueAnalysis(
            category = category,
            priority = priority,
            estimatedDaysToResolve = days,
            actionPlan = plan,
            citizenTone = tone,
            isImmediateSafetyHazard = isImmediateSafetyHazard,
            cleanedDescription = cleaned,
            hazardJustification = justification
        )
    }

    suspend fun generateRepresentativeResponse(
        repName: String,
        category: String,
        location: String,
        userQuestion: String,
        recentHistory: List<String>
    ): String {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return getFallbackRepResponse(repName, category, location, userQuestion)
        }

        val historyText = recentHistory.joinToString("\n")
        val prompt = """
            You are $repName, a polite, professional, action-oriented district municipal representative.
            You are holding a localized digital town hall meeting with residents of $location about a micro-petition regarding systemic "$category" issues in their area.
            
            A resident has asked or said: "$userQuestion"
            
            Context of recent messages:
            $historyText
            
            Provide a realistic, professional, and empathetic response as the representative.
            Address the specific concern, refer to municipal budgets or public works teams where appropriate, and outline a concrete step or timeline (e.g. within 2-3 weeks, scheduling a survey crew, or allocating funds).
            Keep your response concise, conversational, and under 150 words. Do not use JSON formatting; return only the plain text response.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.7f)
        )

        return try {
            val response = service.analyzeIssue(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: getFallbackRepResponse(repName, category, location, userQuestion)
        } catch (e: Exception) {
            getFallbackRepResponse(repName, category, location, userQuestion)
        }
    }

    private fun getFallbackRepResponse(
        repName: String,
        category: String,
        location: String,
        userQuestion: String
    ): String {
        val lower = userQuestion.lowercase()
        return when {
            lower.contains("when") || lower.contains("time") || lower.contains("how long") -> {
                "Thank you for asking about the timeline. I have already submitted a fast-track work order to our $category dispatch crew. For the $location district, we expect a survey team to assess the site this Wednesday, with physical repairs commencing early next week. I will keep you posted on the civic board!"
            }
            lower.contains("cost") || lower.contains("money") || lower.contains("budget") || lower.contains("tax") -> {
                "That's a very fair point regarding municipal allocation. Fortunately, we have active local infrastructure grants that cover $category improvements directly. I am working to prioritize $location in our Q3 budget cycle so residents do not have to wait."
            }
            lower.contains("dangerous") || lower.contains("hazard") || lower.contains("scary") || lower.contains("safety") -> {
                "Resident safety is my absolute top priority. Given the urgency of these $category issues in $location, I am flagging this directly with the chief of safety works today. We will expedite a temporary fix (like barricades or high-intensity warning signals) immediately."
            }
            else -> {
                "I appreciate you bringing this up. As your district representative, I am fully committed to resolving the $category situation in $location. I am coordinating with our municipal team to ensure we have a comprehensive solution in place. Let me know if there are any other specific blocks we should inspect!"
            }
        }
    }
}
