package com.breathein.feature.insights

import com.breathein.core.model.ContentCategory
import com.breathein.core.model.FeedInsights

/** Offline fallback when no API key is set: matches hashtags and common words. Rough, but needs no network. */
internal object KeywordContentClassifier : ContentClassifier {

    override val usesAi = false

    override suspend fun classify(texts: List<String>): List<ContentCategory?> = texts.map(::categoryOf)

    override suspend fun summarize(insights: FeedInsights, topExamples: List<String>): String? {
        val top = insights.categories.firstOrNull() ?: return null
        val sortedSeconds = insights.categories.sumOf { it.seconds }.coerceAtLeast(1)
        return "About ${top.seconds * 100 / sortedSeconds}% of what you watched today was ${top.category.label.lowercase()}."
    }

    fun categoryOf(text: String): ContentCategory {
        val words = text.lowercase().split(NON_WORD).filterTo(HashSet()) { it.isNotEmpty() }
        val (category, hits) = KEYWORDS.map { (category, keywords) -> category to keywords.count(words::contains) }
            .maxBy { it.second }
        return if (hits > 0) category else ContentCategory.Other
    }

    private val NON_WORD = Regex("[^\\p{L}\\p{N}]+")

    private val KEYWORDS: Map<ContentCategory, Set<String>> = mapOf(
        ContentCategory.Fitness to setOf(
            "gym", "workout", "fitness", "legday", "gains", "protein", "lifting", "bodybuilding", "cardio",
            "squat", "deadlift", "pushups", "abs", "calisthenics", "running",
        ),
        ContentCategory.Memes to setOf("meme", "memes", "relatable", "pov", "dankmemes", "shitpost", "meirl"),
        ContentCategory.Comedy to setOf("comedy", "funny", "prank", "sketch", "skit", "lol", "lmao", "joke", "jokes", "humor"),
        ContentCategory.StandUp to setOf("standup", "standupcomedy", "comedian", "comic", "netflixisajoke", "crowdwork", "openmic"),
        ContentCategory.News to setOf(
            "news", "breaking", "politics", "election", "report", "war", "economy", "government", "president",
        ),
        ContentCategory.Cooking to setOf(
            "recipe", "recipes", "cooking", "cook", "baking", "chef", "dinner", "lunch", "breakfast", "pasta",
            "easyrecipe", "kitchen", "homemade",
        ),
        ContentCategory.Music to setOf("music", "song", "cover", "singing", "singer", "rap", "piano", "drums", "musician"),
        ContentCategory.Guitar to setOf("guitar", "guitarist", "guitarcover", "acoustic", "fingerstyle", "riff", "guitarlesson", "fender", "gibson"),
        ContentCategory.Dance to setOf("dance", "dancing", "dancer", "choreography", "dancechallenge", "kpop", "hiphopdance"),
        ContentCategory.Sports to setOf(
            "football", "soccer", "nba", "basketball", "cricket", "goal", "highlights", "nfl", "f1", "tennis", "ufc", "boxing",
        ),
        ContentCategory.Skills to setOf(
            "diy", "skill", "skills", "woodworking", "craft", "crafts", "handmade", "lifehack", "lifehacks", "drawing",
            "painting", "satisfying", "restoration",
        ),
        ContentCategory.Learning to setOf(
            "learn", "tips", "howto", "tutorial", "science", "history", "facts", "education", "study", "explained", "coding",
        ),
        ContentCategory.Travel to setOf("travel", "trip", "vacation", "beach", "wanderlust", "hotel", "travelgram", "roadtrip"),
        ContentCategory.Beauty to setOf("makeup", "skincare", "fashion", "outfit", "ootd", "beauty", "hair", "nails", "grwm"),
        ContentCategory.Gaming to setOf(
            "gaming", "gamer", "minecraft", "fortnite", "gta", "valorant", "roblox", "gameplay", "twitch", "ps5", "xbox",
        ),
        ContentCategory.Babies to setOf("baby", "babies", "toddler", "newborn", "infant", "babygirl", "babyboy", "momlife"),
        ContentCategory.Cats to setOf("cat", "cats", "kitten", "kitty", "catsofinstagram", "meow", "catlover"),
        ContentCategory.Animals to setOf("dog", "dogs", "puppy", "pets", "pet", "animals", "animal", "dogsofinstagram", "wildlife", "birds"),
    )
}
