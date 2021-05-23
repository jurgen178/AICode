/*
 * Copyright (C) 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thecloudsite.aicode

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import java.io.FileOutputStream
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.sign

fun setBackgroundColor(
    view: View,
    color: Int
) {
    // Keep the corner radii and only change the background color.
    val gradientDrawable = view.background as GradientDrawable
    gradientDrawable.setColor(color)
    view.background = gradientDrawable
}

fun setAppTheme(context: Context) {
    val sharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context /* Activity context */)
    val appTheme = sharedPreferences.getString("app_theme", "0")

    if (appTheme != null) {
        when (appTheme) {
            "0" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            "1" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            "2" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            "3" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
            }
        }
    }
}

fun saveTextToFile(
    text: String,
    msg: String,
    context: Context,
    exportJsonUri: Uri
) {
    // Write the text string.
    try {
        context.contentResolver.openOutputStream(exportJsonUri)
            ?.use { output ->
                output as FileOutputStream
                output.channel.truncate(0)
                output.write(text.toByteArray())
            }

        Toast.makeText(context, msg, Toast.LENGTH_LONG)
            .show()

    } catch (e: Exception) {
        Toast.makeText(
            context, context.getString(R.string.export_error, e.message),
            Toast.LENGTH_LONG
        )
            .show()
        Log.d("Export JSON error", "Exception: $e")
    }
}

// https://begriffs.com/pdf/dec2frac.pdf
fun frac(x: Double): Pair<Int?, Int> {

    val eps = 0.0000001

    if (x.isFinite()) {
        val sign = x.sign.toInt()
        val xAbs = x.absoluteValue

        var z = xAbs

        var n = z.toInt()
        var d0: Int = 0
        var d1: Int = 1
        var x0 = 1.0
        var x1 = 0.0

        while ((z - z.toInt().toDouble()) > eps && (x0 - x1).absoluteValue > eps) {
            z = 1 / (z - z.toInt().toDouble())
            val d = d1 * z.toInt() + d0
            n = (xAbs * d).roundToInt()

            x0 = x1
            x1 = n.toDouble() / d.toDouble()

            d0 = d1
            d1 = d
        }

        if (d1 > 0) {
            return Pair(n * sign, d1)
        }
    }

    return Pair(null, 0)
}

fun getBits(value: Long): Int {
    var numberOfBits = 0
    var x = if (value < 0L) {
        -(value - 1)
    } else {
        value
    }
    while (x != 0L) {
        numberOfBits++
        x = x ushr 1
    }

    return numberOfBits
}

fun getBlockSize(value: Long): Int {

    val numberOfBits = getBits(value)

    return when {
        numberOfBits <= 8 -> {
            8
        }
        numberOfBits <= 16 -> {
            16
        }
        numberOfBits <= 32 -> {
            32
        }
        else -> {
            64
        }
    }
}

fun getDisplayString(value: Long, radix: Int): String {

    if (radix != 2) {
        return value.toLong().toString(radix)
    }

    val blockSize = getBlockSize(value)
    var str = ""

    for (i in 0 until blockSize) {
        str = if ((value ushr i).and(1L) == 1L) {
            "1${str}"
        } else {
            "0${str}"
        }
    }

    return str
}