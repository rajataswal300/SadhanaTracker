package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.ui.graphics.vector.ImageVector

enum class SectionType(
    val title: String,
    val description: String,
    val icon: ImageVector
) {
    TOGGLES(
        title = "Daily Sadhana Toggles",
        description = "Chanting & custom habit switches",
        icon = Icons.Default.CheckCircle
    ),
    SLEEP(
        title = "Sleep & Rest",
        description = "Night sleep, naps & bedtime tracking",
        icon = Icons.Default.Bedtime
    ),
    READING(
        title = "Book Reading",
        description = "Srila Prabhupada book reading sessions",
        icon = Icons.Default.MenuBook
    ),
    LECTURE(
        title = "Prabhupada Lecture",
        description = "Daily audio/video lecture attendance",
        icon = Icons.Default.Headphones
    ),
    MAYA_ATTACK(
        title = "Strong Maya Attack",
        description = "Illusion attack logging & reflection",
        icon = Icons.Default.Security
    ),
    NOTES(
        title = "Notes & Learnings",
        description = "Daily spiritual realizations & notes",
        icon = Icons.Default.EditNote
    ),
    SHEETS(
        title = "Google Sheets & Sync",
        description = "Cloud sheet sync & web app connection",
        icon = Icons.Default.TableChart
    ),
    SHARE(
        title = "Share Today's Card",
        description = "Export & send report to devotees",
        icon = Icons.Default.Share
    );

    companion object {
        val DEFAULT_ORDER = listOf(
            TOGGLES,
            SLEEP,
            READING,
            LECTURE,
            MAYA_ATTACK,
            NOTES,
            SHEETS,
            SHARE
        )

        fun fromKey(key: String): SectionType? {
            return entries.find { it.name.equals(key, ignoreCase = true) }
        }
    }
}
