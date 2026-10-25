package com.breathein.feature.insights

import android.content.Context
import android.util.Log
import com.breathein.core.common.FeedContentSink
import com.breathein.core.common.FeedInsightsRepository
import com.breathein.core.model.ContentCategory
import com.breathein.core.model.FeedInsights
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Collects what the user watches in Reels / Shorts (fed by the accessibility service), sorts it into
 * categories with Gemini (or keywords when no API key is set) and keeps today's totals. One per process.
 */
class ContentInsightsRepository(
    context: Context,
    private val scope: CoroutineScope,
) : FeedInsightsRepository, FeedContentSink {

    private val store = FeedItemStore(File(context.filesDir, FILE_NAME))
    private val usesAi = BuildConfig.GEMINI_API_KEY.isNotBlank()

    // Created on first use, on a background thread: setting up the Gemini SDK takes seconds and would
    // otherwise block app startup.
    private val classifier: ContentClassifier by lazy {
        if (usesAi) GeminiContentClassifier(BuildConfig.GEMINI_API_KEY) else KeywordContentClassifier
    }

    // Everything below is guarded by [lock]: the accessibility thread writes, UI and monitor read.
    private val lock = Any()
    private val log = FeedItemLog()
    private var day = LocalDate.now()
    private var summary: String? = null
    private var summaryStale = false
    private var lastPublishMillis = 0L

    private val sorting = AtomicBoolean(false)
    private val saveMutex = Mutex()

    private val _insights = MutableStateFlow(FeedInsights(usesAi = usesAi))
    override val insights: StateFlow<FeedInsights> = _insights.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) {
            val saved = store.load() ?: return@launch
            synchronized(lock) {
                if (saved.day != day) return@launch
                log.restore(saved.items)
                summary = saved.summary
            }
            publish()
        }
    }

    override fun onFeedContent(packageName: String, texts: List<String>) {
        val now = System.currentTimeMillis()
        val (isNew, unsorted) = synchronized(lock) {
            rollOverIfNewDay()
            log.record(packageName, texts, now) to log.all.count { it.category == null }
        }
        // Time on the current item grows with every call; publish that every few seconds, new items right away.
        if (isNew || now - lastPublishMillis > PUBLISH_INTERVAL_MILLIS) publish()
        if (isNew) save()
        if (isNew && unsorted >= AUTO_SORT_BATCH) sort(withSummary = false)
    }

    override fun refresh() = sort(withSummary = true)

    override fun topCategorySince(packageName: String, sinceMillis: Long): ContentCategory? =
        synchronized(lock) { InsightsCalculator.topCategory(log.all, packageName, sinceMillis) }

    /** Classifies everything unsorted, in batches, then (if asked) rewrites the summary. One run at a time. */
    private fun sort(withSummary: Boolean) {
        if (!sorting.compareAndSet(false, true)) return
        publish()
        scope.launch(Dispatchers.IO) {
            try {
                val pending = synchronized(lock) { log.all.filter { it.category == null } }
                for (batch in pending.chunked(SORT_BATCH_SIZE)) {
                    val categories = classifier.classify(batch.map { it.text })
                    synchronized(lock) {
                        log.applyCategories(batch, categories)
                        summaryStale = true
                    }
                    publish()
                }
                if (withSummary) updateSummary()
                save()
            } catch (e: Exception) {
                // Offline, bad key, rate limit…: leave items unsorted and try again on the next refresh.
                Log.w(TAG, "Sorting failed", e)
            } finally {
                sorting.set(false)
                publish()
            }
        }
    }

    private suspend fun updateSummary() {
        val (current, examples) = synchronized(lock) {
            if (!summaryStale && summary != null) return
            val items = log.all
            val current = InsightsCalculator.build(items, summary, isSorting = true, usesAi = usesAi)
            val top = current.categories.firstOrNull()?.category ?: return
            current to InsightsCalculator.examples(items, top, SUMMARY_EXAMPLES)
        }
        val newSummary = classifier.summarize(current, examples) ?: return
        synchronized(lock) {
            summary = newSummary
            summaryStale = false
        }
    }

    private fun rollOverIfNewDay() {
        val today = LocalDate.now()
        if (today == day) return
        day = today
        log.clear()
        summary = null
    }

    private fun publish() {
        _insights.value = synchronized(lock) {
            lastPublishMillis = System.currentTimeMillis()
            InsightsCalculator.build(log.all, summary, sorting.get(), usesAi)
        }
    }

    private fun save() {
        scope.launch(Dispatchers.IO) {
            saveMutex.withLock {
                val (saveDay, items, saveSummary) = synchronized(lock) { Triple(day, log.all, summary) }
                runCatching { store.save(saveDay, items, saveSummary) }.onFailure { Log.w(TAG, "Saving failed", it) }
            }
        }
    }

    private companion object {
        const val TAG = "ContentInsights"
        const val FILE_NAME = "feed_today.json"
        const val PUBLISH_INTERVAL_MILLIS = 5_000L

        /** Sort in the background once this many items are waiting, so the break screen can name the topic. */
        const val AUTO_SORT_BATCH = 5
        const val SORT_BATCH_SIZE = 40
        const val SUMMARY_EXAMPLES = 5
    }
}
