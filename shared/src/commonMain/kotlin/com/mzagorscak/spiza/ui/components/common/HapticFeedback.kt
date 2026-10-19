package com.mzagorscak.spiza.ui.components.common

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

fun performHapticFeedback(hapticFeedback: HapticFeedback) {
    hapticFeedback.performHapticFeedback(hapticFeedbackType = HapticFeedbackType.LongPress)
}
