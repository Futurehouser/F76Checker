package com.futurehouser.f76checker

import android.content.Context
import org.json.JSONObject

enum class Lang { EN, ES }

data class Item(
    val id: String,
    val section: String,
    val en: String,
    val es: String,
    val image: String,
    val wiki: String,
) {
    fun name(lang: Lang) = if (lang == Lang.ES) es else en
}

/** A group of items: a weapon type, or one armor set (with its own picture). */
data class Section(
    val id: String,
    val en: String,
    val es: String,
    val image: String?,
    val wiki: String?,
) {
    fun name(lang: Lang) = if (lang == Lang.ES) es else en
}

class CategoryData(val sections: List<Section>, val items: List<Item>) {
    val sectionById = sections.associateBy { it.id }
}

/**
 * A checklist category backed by a JSON file in assets/.
 * To add a new one: drop a JSON file in app/src/main/assets (same format: "sections" + "items")
 * and add an entry here. It shows up on the home page and in the side menu automatically.
 * [isSets] = each section is a set (shown with its picture and a "check whole set" box).
 */
data class Category(
    val id: String,
    val asset: String,
    val en: String,
    val es: String,
    val isSets: Boolean,
    val cover: String,
) {
    fun name(lang: Lang) = if (lang == Lang.ES) es else en
}

private const val IMG = "https://static.wikia.nocookie.net/fallout/images"

val CATEGORIES = listOf(
    Category("weapons", "weapons.json", "Weapons", "Armas", false,
        "$IMG/5/55/FO76_Enclave_plasma_gun.png/revision/latest/scale-to-width-down/256"),
    Category("power_armor", "power_armor.json", "Power armor sets", "Servoarmaduras", true, ""),
    Category("armor", "armor.json", "Armor sets", "Armaduras", true, ""),
)

object Repository {
    private val cache = mutableMapOf<String, CategoryData>()

    fun load(context: Context, category: Category): CategoryData = cache.getOrPut(category.id) {
        val text = context.assets.open(category.asset).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(text)
        val arr = root.getJSONArray("items")
        val items = List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Item(
                id = o.getString("id"),
                section = o.getString("section"),
                en = o.getString("en"),
                es = o.getString("es"),
                image = o.getString("image"),
                wiki = o.getString("wiki"),
            )
        }
        val secArr = root.optJSONArray("sections")
        val sections = if (secArr != null) List(secArr.length()) { i ->
            val o = secArr.getJSONObject(i)
            Section(
                id = o.getString("id"),
                en = o.getString("en"),
                es = o.getString("es"),
                image = o.optString("image").ifEmpty { null },
                wiki = o.optString("wiki").ifEmpty { null },
            )
        } else items.map { it.section }.distinct().map { Section(it, it, it, null, null) }
        CategoryData(sections, items)
    }

    /** Picture for the home page card: explicit cover, else the first set's picture. */
    fun cover(context: Context, category: Category): String? =
        category.cover.ifEmpty { null } ?: load(context, category).sections.firstNotNullOfOrNull { it.image }
}

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("f76checker", Context.MODE_PRIVATE)

    var lang: Lang
        get() = runCatching { Lang.valueOf(sp.getString("lang", null) ?: "") }.getOrElse {
            if (java.util.Locale.getDefault().language == "es") Lang.ES else Lang.EN
        }
        set(v) = sp.edit().putString("lang", v.name).apply()

    var sound: Boolean
        get() = sp.getBoolean("sound", true)
        set(v) = sp.edit().putBoolean("sound", v).apply()

    fun owned(category: String): Set<String> = sp.getStringSet("owned_$category", emptySet())!!.toSet()

    fun setOwned(category: String, ids: Set<String>) =
        sp.edit().putStringSet("owned_$category", ids).apply()

    var skippedVersion: String?
        get() = sp.getString("skipped_version", null)
        set(v) = sp.edit().putString("skipped_version", v).apply()
}
