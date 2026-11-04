package com.example.ui.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback

class SadhanaHaptics(
    private val context: Context,
    private val composeHaptic: HapticFeedback
) {
    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Tactile click feedback for toggle switches (Yes/No toggles, checkboxes).
     */
    fun toggleClick() {
        try {
            composeHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(20L)
            }
        } catch (e: Exception) {
            // Ignore if vibration fails or is disabled
        }
    }

    /**
     * Stronger tactile feedback for starting or stopping timer sessions (Sleep, Wake up, Reading).
     */
    fun timerAction() {
        try {
            composeHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(40L)
            }
        } catch (e: Exception) {
            // Ignore if vibration fails or is disabled
        }
    }

    fun buttonClick() {
        toggleClick()
    }

    fun itemAdded() {
        toggleClick()
    }

    fun celebration() {
        timerAction()
    }

    fun counterTick() {
        toggleClick()
    }
}

@Composable
fun rememberSadhanaHaptics(): SadhanaHaptics {
    val context = LocalContext.current.applicationContext
    val composeHaptic = LocalHapticFeedback.current
    return remember(context, composeHaptic) {
        SadhanaHaptics(context, composeHaptic)
    }
}
