package com.oscar.smartcontrol.ir

import android.content.Context

enum class IrDeviceCategory(val title: String, val dbCategories: Set<String>) {
    TV("TV", setOf("TVs", "Universal TV Remotes", "TV Tuner", "Monitors")),
    AC("Aire Acondicionado", setOf("ACs", "Heaters")),
    AUDIO("Audio / Barra", setOf("SoundBars", "Audio and Video Receivers", "Speakers", "CD Players", "MiniDisc")),
    PROJECTOR("Proyector", setOf("Projectors")),
    FAN("Ventilador", setOf("Fans", "Air Purifiers", "Humidifiers")),
    LED("Luces LED", setOf("LED Lighting")),
    STREAMING("Deco / Streaming", setOf("Cable Boxes", "Streaming Devices", "DVB-T", "Blu-Ray", "DVD Players", "VCR")),
    OTHER("Otros", emptySet())
}

data class IrDbItem(
    val path: String,
    val category: String,
    val brand: String,
    val model: String
) {
    val displayTitle: String
        get() = if (brand.isNotEmpty() && brand != category) "$brand — $model" else model
}

sealed class IrVariant {
    abstract val label: String
    abstract val brandName: String

    data class BuiltIn(val brand: TvBrand, override val brandName: String, override val label: String) : IrVariant()
    data class DbFile(val item: IrDbItem) : IrVariant() {
        override val label: String get() = item.model
        override val brandName: String get() = item.brand.ifEmpty { item.category }
    }
}

data class SavedIrRemote(
    val id: String,
    val name: String,
    val category: IrDeviceCategory,
    val presetBrandName: String? = null,
    val assetPath: String? = null
)

/**
 * Lazy-loaded offline database backed by bundled Flipper-IRDB assets (CC0 1.0)
 * combined with built-in protocol presets.
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

    fun getBrandsForCategory(category: IrDeviceCategory): List<String> {
        val dbBrands = getItemsForCategory(category)
            .map { it.brand.ifEmpty { it.model } }
            .filter { it.isNotBlank() }

        val builtInBrands = builtInForCategory(category).map { it.second }
        return (builtInBrands + dbBrands)
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }

    fun getVariantsForBrand(category: IrDeviceCategory, brand: String?): List<IrVariant> {
        val variants = mutableListOf<IrVariant>()
        val builtIns = builtInForCategory(category)
            .filter { (_, bName) ->
                brand == null || bName.equals(brand, ignoreCase = true) ||
                    bName.contains(brand, ignoreCase = true) ||
                    brand.contains(bName, ignoreCase = true)
            }
            .map { (preset, bName) -> IrVariant.BuiltIn(preset, bName, preset.displayName) }
        variants.addAll(builtIns)

        val dbItems = getItemsForCategory(category)
            .filter { item ->
                brand == null ||
                    item.brand.equals(brand, ignoreCase = true) ||
                    item.model.contains(brand, ignoreCase = true)
            }
            .map { IrVariant.DbFile(it) }
        variants.addAll(dbItems)

        return variants
    }

    fun loadButtons(item: IrDbItem): List<FlipperIrEntry> = loadButtonsByPath(item.path)

    fun loadButtonsByPath(path: String): List<FlipperIrEntry> {
        return try {
            val content = context.assets.open("irdb/$path").bufferedReader().use { it.readText() }
            IrController.parseFlipperFile(content)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun getItemsForCategory(category: IrDeviceCategory): List<IrDbItem> {
        val all = getAllItems()
        if (category == IrDeviceCategory.OTHER) {
            val known = IrDeviceCategory.entries
                .filter { it != IrDeviceCategory.OTHER }
                .flatMap { it.dbCategories }
                .toSet()
            return all.filter { it.category !in known }
        }
        return all.filter { it.category in category.dbCategories }
    }

    private fun builtInForCategory(category: IrDeviceCategory): List<Pair<TvBrand, String>> {
        return when (category) {
            IrDeviceCategory.TV -> listOf(
                TvBrand.SAMSUNG to "Samsung",
                TvBrand.LG to "LG",
                TvBrand.SONY to "Sony",
                TvBrand.TCL to "TCL",
                TvBrand.HISENSE to "Hisense",
                TvBrand.PHILIPS_RC5 to "Philips",
                TvBrand.PHILIPS_RC6 to "Philips",
                TvBrand.RCA_SMART to "RCA",
                TvBrand.RCA_CLASSIC_56 to "RCA",
                TvBrand.RCA_38 to "RCA",
                TvBrand.XIAOMI to "Xiaomi",
                TvBrand.PANASONIC to "Panasonic",
                TvBrand.SHARP to "Sharp",
                TvBrand.TOSHIBA to "Toshiba",
                TvBrand.ROKU_TV to "Roku",
                TvBrand.VIZIO to "Vizio",
                TvBrand.JVC to "JVC",
                TvBrand.OEM_LATAM_1 to "Noblex",
                TvBrand.OEM_LATAM_1 to "BGH",
                TvBrand.OEM_LATAM_1 to "Admiral",
                TvBrand.OEM_LATAM_2 to "Philco",
                TvBrand.OEM_LATAM_2 to "Sanyo"
            )
            IrDeviceCategory.AUDIO -> listOf(
                TvBrand.AUDIO_JBL_SOUNDBAR to "JBL",
                TvBrand.AUDIO_SAMSUNG_SOUNDBAR to "Samsung",
                TvBrand.AUDIO_LG_SOUNDBAR to "LG",
                TvBrand.AUDIO_SONY_SYSTEM to "Sony"
            )
            IrDeviceCategory.PROJECTOR -> listOf(
                TvBrand.PROJECTOR_EPSON to "Epson",
                TvBrand.PROJECTOR_BENQ to "BenQ"
            )
            IrDeviceCategory.LED -> listOf(
                TvBrand.LED_RGB_24KEY to "LED RGB Universal"
            )
            IrDeviceCategory.STREAMING -> listOf(
                TvBrand.STREAMING_APPLE_TV to "Apple"
            )
            else -> emptyList()
        }
    }
}
