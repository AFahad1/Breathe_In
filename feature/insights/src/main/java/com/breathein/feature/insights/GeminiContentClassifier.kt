package com.breathein.feature.insights

import android.util.Log
import com.breathein.core.model.ContentCategory
import com.breathein.core.model.FeedInsights
import com.google.genai.Client
import com.google.genai.types.Content
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.GenerateContentResponse
import com.google.genai.types.Part
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Sorts items and writes the summary with Gemini. Only the on-screen text (captions, creators) is sent. */
internal class GeminiContentClassifier(apiKey: String) : ContentClassifier {

    private val client: Client = Client.builder().apiKey(apiKey).build()

    override val usesAi = true

    override suspend fun classify(texts: List<String>): List<ContentCategory?> = withContext(Dispatchers.IO) {
        val config = GenerateContentConfig.builder()
            .systemInstruction(Content.fromParts(Part.fromText(CLASSIFY_PROMPT)))
            .responseMimeType("application/json")
            .responseJsonSchema(CATEGORIES_SCHEMA)
            .build()
        val prompt = texts.mapIndexed { index, text -> "$index: $text" }.joinToString("\n")

        val result = arrayOfNulls<ContentCategory>(texts.size)
        val answer = textOf(client.models.generateContent(MODEL, prompt, config)) ?: return@withContext result.toList()
        val items = JSONObject(answer).getJSONArray("items")
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            val index = item.getInt("index")
            if (index in result.indices) {
                result[index] = ContentCategory.entries.firstOrNull { it.name == item.getString("category") }
            }
        }
        result.toList()
    }

    override suspend fun summarize(insights: FeedInsights, topExamples: List<String>): String? =
        withContext(Dispatchers.IO) {
            val breakdown = insights.categories.joinToString("\n") { "- ${it.category.label}: ${minutes(it.seconds)}" }
            val examples = topExamples.joinToString("\n") { "- $it" }
            val config = GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(SUMMARY_PROMPT)))
                .build()
            val prompt = "Today's Reels/Shorts time by category:\n$breakdown\n\nSome videos from the top category:\n$examples"
            textOf(client.models.generateContent(MODEL, prompt, config))?.trim()?.ifEmpty { null }
        }

    /** The response text, or null when Gemini blocked the request or returned nothing. */
    private fun textOf(response: GenerateContentResponse): String? {
        val text = response.text()
        if (text.isNullOrBlank()) Log.w(TAG, "No usable answer: ${response.finishReason()}")
        return text?.takeIf { it.isNotBlank() }
    }

    private fun minutes(seconds: Int) = if (seconds < 60) "$seconds s" else "${seconds / 60} min"

    private companion object {
        const val TAG = "GeminiClassifier"
        const val MODEL = "gemini-3.8-flash"

        /** {"items": [{"index": 0, "category": "Fitness"}, …]} */
        val CATEGORIES_SCHEMA = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "items" to mapOf(
                    "type" to "array",
                    "items" to mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "index" to mapOf("type" to "integer"),
                            "category" to mapOf("type" to "string", "enum" to ContentCategory.entries.map { it.name }),
                        ),
                        "required" to listOf("index", "category"),
                    ),
                ),
            ),
            "required" to listOf("items"),
        )

        val CLASSIFY_PROMPT = """
            You sort short-form videos (Instagram Reels, YouTube Shorts) into categories. For each video you only
            get the text that was on screen: caption, hashtags, creator name, audio name, separated by " · ".
            Text can be in any language and is often fragmentary; creator names and hashtags are good clues.
            Categories: ${ContentCategory.entries.joinToString { "${it.name} (${it.label})" }}.
            Memes are relatable or absurd jokes, trends and reaction formats; Comedy is skits and pranks; StandUp
            is stand-up comedy sets and clips. Guitar is anything guitar (playing, lessons, gear); Dance is dancing
            and choreography; Music is the rest of music. Skills is people showing a craft or practical skill (DIY,
            woodworking, art, life hacks); Learning is explainers and facts. Babies is babies and toddlers; Cats is
            cats; Animals is all other animals and pets.
            Use Other only when nothing fits. Return one entry per numbered video, with its number as the index.
        """.trimIndent()

        val SUMMARY_PROMPT = """
            You write the reflection line in Breathe In, an app that helps people notice their doom scrolling.
            Given how today's short-form video time splits across categories, write at most two short sentences:
            say what most of the time went to, then offer one gentle, concrete idea for doing that thing in real
            life (lots of gym content: maybe a short walk or a few push-ups now?). Warm, never judgmental or
            preachy, no guilt, no emoji, no markdown. Plain text only.
        """.trimIndent()
    }
}
