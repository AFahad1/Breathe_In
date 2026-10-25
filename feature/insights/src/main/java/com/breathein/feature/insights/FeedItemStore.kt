package com.breathein.feature.insights

import com.breathein.core.model.ContentCategory
import com.breathein.core.model.FeedItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/** Today's items and summary in a small JSON file, so they survive the process being killed. Call off the main thread. */
internal class FeedItemStore(private val file: File) {

    class Saved(val day: LocalDate, val items: List<FeedItem>, val summary: String?)

    fun load(): Saved? = runCatching {
        val json = JSONObject(file.readText())
        val items = json.getJSONArray("items")
        Saved(
            day = LocalDate.parse(json.getString("day")),
            items = (0 until items.length()).map { i ->
                val item = items.getJSONObject(i)
                val categoryName = item.optString("category")
                FeedItem(
                    packageName = item.getString("package"),
                    text = item.getString("text"),
                    firstSeenMillis = item.getLong("first"),
                    lastSeenMillis = item.getLong("last"),
                    category = ContentCategory.entries.firstOrNull { it.name == categoryName },
                )
            },
            summary = json.optString("summary").ifEmpty { null },
        )
    }.getOrNull()

    fun save(day: LocalDate, items: List<FeedItem>, summary: String?) {
        val itemsJson = JSONArray()
        for (item in items) {
            itemsJson.put(
                JSONObject()
                    .put("package", item.packageName)
                    .put("text", item.text)
                    .put("first", item.firstSeenMillis)
                    .put("last", item.lastSeenMillis)
                    .put("category", item.category?.name.orEmpty()),
            )
        }
        val json = JSONObject()
            .put("day", day.toString())
            .put("summary", summary.orEmpty())
            .put("items", itemsJson)
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(json.toString())
        temp.renameTo(file)
    }
}
