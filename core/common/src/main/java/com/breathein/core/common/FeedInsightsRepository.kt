package com.breathein.core.common

import com.breathein.core.model.ContentCategory
import com.breathein.core.model.FeedInsights
import kotlinx.coroutines.flow.StateFlow

/** Today's Reels / Shorts content, sorted into categories. Implemented by :feature:insights. */
interface FeedInsightsRepository {
    val insights: StateFlow<FeedInsights>

    /** Sorts anything not yet classified and refreshes the summary. Safe to call often. */
    fun refresh()

    /** The category the user spent most time on in [packageName] since [sinceMillis] (wall clock), if known. */
    fun topCategorySince(packageName: String, sinceMillis: Long): ContentCategory?
}
