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

/**
 * A checklist category backed by a JSON file in assets/.
 * To add armor, power armor, etc. later: drop a new JSON file with the same format into
 * app/src/main/assets and add an entry here (plus its section labels in [Strings.sections]).
 */
data class Category(val id: String, val asset: String, val en: String, val es: String) {
    fun name(lang: Lang) = if (lang == Lang.ES) es else en
}

val CATEGORIES = listOf(
    Category("weapons", "weapons.json", "Weapons", "Armas"),
)

object Repository {
    private val cache = mutableMapOf<String, List<Item>>()

    fun items(context: Context, category: Category): List<Item> = cache.getOrPut(category.id) {
        val text = context.assets.open(category.asset).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val arr = JSONObject(text).getJSONArray("items")
        List(arr.length()) { i ->
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
    }
}

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("f76checker", Context.MODE_PRIVATE)

    var lang: Lang
        get() = runCatching { Lang.valueOf(sp.getString("lang", null) ?: "") }.getOrElse {
            if (java.util.Locale.getDefault().language == "es") Lang.ES else Lang.EN
        }
        set(v) = sp.edit().putString("lang", v.name).apply()

    fun owned(category: String): Set<String> = sp.getStringSet("owned_$category", emptySet())!!.toSet()

    fun setOwned(category: String, ids: Set<String>) =
        sp.edit().putStringSet("owned_$category", ids).apply()

    var skippedVersion: String?
        get() = sp.getString("skipped_version", null)
        set(v) = sp.edit().putString("skipped_version", v).apply()
}
