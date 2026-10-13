package xdbot.bitplugg.ru.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates a Baseline Profile for XDDBOT by exercising the main flow once.
 * Run with a connected device/emulator: `./gradlew :baselineprofile:pixel6Api34DebugAndroidTest`
 * or use `generateBaselineProfile`. The output lands in `app/src/main/baseline-prof.txt`.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = "xdbot.bitplugg.ru") {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.text("XDDBOT")), 5_000)
    }
}
