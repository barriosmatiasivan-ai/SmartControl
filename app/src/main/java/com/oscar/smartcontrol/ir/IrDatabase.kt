package com.oscar.smartcontrol.ir

import android.content.Context

data class IrDbItem(
    val path: String,
    val category: String,
    val brand: String,
    val model: String
) {
    val displayTitle: String
        get() = if (brand.isNotEmpty() && brand != category) "$brand — $model" else model
}

/**
 * Lazy-loaded offline database backed by bundled Flipper-IRDB assets (CC0 1.0).
 * Reads `assets/irdb_index.txt` once and parses individual `.ir` files on demand.
 */
class IrDatabase(private val context: Context) {

    private var cachedIndex: List<IrDbItem>? = null

    fun getAllItems(): List<IrDbItem> {
        cachedIndex?.let { return it }
        val items = try {
            context.assets.open("irdb_index.txt").bufferedReader().useLines { lines ->
                lines.mapNotNull { line ->
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) return@mapNotNull null
                    val parts = trimmed.split('/')
                    val category = parts.firstOrNull() ?: "Other"
                    val brand = if (parts.size >= 3) parts[1] else ""
                    val model = parts.last().removeSuffix(".ir").replace('_', ' ')
                    IrDbItem(
                        path = trimmed,
                        category = category.replace('_', ' '),
                        brand = brand.replace('_', ' '),
                        model = model
                    )
                }.toList()
            }
        } catch (_: Exception) {
            emptyList()
        }
        cachedIndex = items
        return items
    }

    fun getCategories(): List<String> {
        return getAllItems().map { it.category }.distinct().sorted()
    }

    fun search(query: String, category: String? = null, limit: Int = 80): List<IrDbItem> {
        val q = query.trim().lowercase()
        return getAllItems()
            .asSequence()
            .filter { item ->
                (category == null || item.category == category) &&
                    (q.isEmpty() ||
                        item.brand.lowercase().contains(q) ||
                        item.model.lowercase().contains(q) ||
                        item.category.lowercase().contains(q))
            }
            .take(limit)
            .toList()
    }

    fun loadButtons(item: IrDbItem): List<FlipperIrEntry> {
        return try {
            val content = context.assets.open("irdb/${item.path}").bufferedReader().use { it.readText() }
            IrController.parseFlipperFile(content)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
