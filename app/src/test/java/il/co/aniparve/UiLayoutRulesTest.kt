package il.co.aniparve

import org.junit.Assert.assertEquals
import org.junit.Test

class UiLayoutRulesTest {
    @Test fun foodDurationLabelsKeepHoursAndMinutesOnSeparateLines() {
        assertEquals("המתנה:\n6 שעות", FoodDurationText.format(6, 0))
        assertEquals("המתנה:\n5 שעות\nו־30 דקות", FoodDurationText.format(5, 30))
        assertEquals("המתנה:\n4 שעות\nו־59 דקות", FoodDurationText.format(4, 59))
        assertEquals("המתנה:\nשעה\nו־5 דקות", FoodDurationText.format(1, 5))
        assertEquals("המתנה:\n30 דקות", FoodDurationText.format(0, 30))
        assertEquals("המתנה:\nדקה", FoodDurationText.format(0, 1))
    }

    @Test fun backNavigationReturnsHelpToSettingsAndSettingsToHome() {
        assertEquals("settings", ScreenNavigation.backTarget("help"))
        assertEquals("home", ScreenNavigation.backTarget("settings"))
        assertEquals("home", ScreenNavigation.backTarget("timer"))
        assertEquals("home", ScreenNavigation.backTarget("done"))
        assertEquals(null, ScreenNavigation.backTarget("home"))
    }
}
