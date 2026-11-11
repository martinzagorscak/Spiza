package com.mzagorscak.spiza.ui.clipboard

import android.os.Build

// Android 13+ shows a clipboard overlay on every copy; Google recommends no in-app confirmation there.
actual val showsSystemCopyConfirmation: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
