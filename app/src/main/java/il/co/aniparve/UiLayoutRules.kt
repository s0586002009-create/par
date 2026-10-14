package il.co.aniparve

/** Stable multiline text for home-screen food cards. */
internal object FoodDurationText {
    fun format(hours: Int, minutes: Int): String {
        val hourText = if (hours == 1) "שעה" else "$hours שעות"
        val minuteText = if (minutes == 1) "דקה" else "$minutes דקות"
        return when {
            hours > 0 && minutes > 0 -> "המתנה:\n$hourText\nו־$minuteText"
            hours > 0 -> "המתנה:\n$hourText"
            else -> "המתנה:\n$minuteText"
        }
    }
}

/** Back navigation for the in-activity screens. Null means leave the activity. */
internal object ScreenNavigation {
    fun backTarget(screen: String): String? = when (screen) {
        "help" -> "settings"
        "settings", "timer", "done" -> "home"
        else -> null
    }
}
