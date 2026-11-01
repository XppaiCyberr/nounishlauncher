package com.nounish.launcher.model

data class AppItem(
    val label: String,
    val packageName: String,
    val activityName: String? = null
)
