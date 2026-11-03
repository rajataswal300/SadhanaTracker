package com.example.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.SectionType

class LayoutPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("sadhana_layout_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SECTION_ORDER = "key_section_order"
    }

    fun getSectionOrder(): List<SectionType> {
        val raw = prefs.getString(KEY_SECTION_ORDER, null) ?: return SectionType.DEFAULT_ORDER
        val keys = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val loaded = keys.mapNotNull { SectionType.fromKey(it) }.distinct().toMutableList()

        // Ensure any newly added sections in future updates are included
        SectionType.DEFAULT_ORDER.forEach { s ->
            if (!loaded.contains(s)) {
                loaded.add(s)
            }
        }
        return loaded
    }

    fun saveSectionOrder(order: List<SectionType>) {
        val raw = order.joinToString(",") { it.name }
        prefs.edit().putString(KEY_SECTION_ORDER, raw).apply()
    }

    fun resetToDefault(): List<SectionType> {
        prefs.edit().remove(KEY_SECTION_ORDER).apply()
        return SectionType.DEFAULT_ORDER
    }
}
