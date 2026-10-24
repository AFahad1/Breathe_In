package com.breathein.core.common

const val INSTAGRAM_PACKAGE = "com.instagram.android"
const val YOUTUBE_PACKAGE = "com.google.android.youtube"
const val TIKTOK_PACKAGE = "com.zhiliaoapp.musically"

/** How often the monitor checks the foreground app. 1 s so short visits to a feed add up accurately. */
const val POLL_INTERVAL_MILLIS = 1_000L

/** Leaving the monitored app (or feed) for longer than this ends the session. */
const val SESSION_GAP_MILLIS = 30_000L
