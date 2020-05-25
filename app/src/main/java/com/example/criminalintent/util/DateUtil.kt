package com.example.criminalintent.util

import android.content.res.Resources
import android.os.Build
import java.text.SimpleDateFormat
import java.util.*

private const val DATE_FORMAT = "EEE, dd MMM yyyy "

fun formatDate(date: Date, resources: Resources): String {
    val dateFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        SimpleDateFormat(DATE_FORMAT, resources.configuration.locales[0])
    } else {
        SimpleDateFormat(DATE_FORMAT, resources.configuration.locale)
    }

    return dateFormat.format(date)
}