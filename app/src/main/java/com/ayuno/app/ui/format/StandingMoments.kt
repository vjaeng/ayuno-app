package com.ayuno.app.ui.format

import android.content.res.Resources
import com.ayuno.app.R

/** Rótulo de un momento de pie (H-053): «1 min», «45 s» o, sin segundos, «sin esperar». */
fun standingMoment(resources: Resources, seconds: Int?): String = when {
    seconds == null -> resources.getString(R.string.standing_moment_none)
    seconds % 60 == 0 -> resources.getString(R.string.standing_moment_minutes, seconds / 60)
    else -> resources.getString(R.string.standing_moment_seconds, seconds)
}
