package com.breathein.core.common

import android.content.Context
import android.content.Intent

/** Sends the user to the launcher. */
fun Context.goToHomeScreen() {
    val intent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_HOME)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}
