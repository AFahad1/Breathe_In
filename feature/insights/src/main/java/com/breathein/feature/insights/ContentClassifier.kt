package com.breathein.feature.insights

import com.breathein.core.model.ContentCategory
import com.breathein.core.model.FeedInsights

/** Sorts watched items into categories and writes the summary line. */
internal interface ContentClassifier {
    /** True when an AI model does the sorting. */
    val usesAi: Boolean

    /** One category per text, in the same order; null where it couldn't decide. */
    suspend fun classify(texts: List<String>): List<ContentCategory?>

    /** A short reflection on today's mix, or null. [topExamples] are texts from the biggest category. */
    suspend fun summarize(insights: FeedInsights, topExamples: List<String>): String?
}
