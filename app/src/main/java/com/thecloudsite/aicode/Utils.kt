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
import kotlin.math.*

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
        numberOfBits++
        -(value + 1)
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

fun invertBits(value: Long, blockSize: Int): Long {
    var valueInverted = value

    for (i in 0 until blockSize) {
        valueInverted = valueInverted xor (1L shl i)
    }

    return valueInverted
}

fun getDisplayString(value: Long, radix: Int): String {

    if (radix != 2) {
        return value.toString(radix)
    }

    val blockSize = getBlockSize(value)

    val binaryList: MutableList<String> = mutableListOf()
    for (i in 0 until blockSize) {
        binaryList.add(
            if ((value ushr i).and(1L) == 1L) {
                "1"
            } else {
                "0"
            }
        )
    }

    return binaryList.reversed().joinToString(
        separator = "",
    )
}

fun factorial(n: Int): Long {
    // 20! = 2432902008176640000
    // 21! will overflow Long (64bit)
    return if (n >= 21) {
        0
    } else if (n <= 0) {
        1
    } else {
        n * factorial(n - 1)
    }
}

fun factorialDouble(n: Int): Double {
    // Limit to 100!
    return if (n > 100) {
        0.0
    } else if (n <= 0.0) {
        1.0
    } else {
        n * factorialDouble(n - 1)
    }
}

fun binomUsingFactorial(n: Int, k: Int): Long {
    return factorial(n) / (factorial(k) * factorial(n - k))
}

fun binomRecursive(n: Int, k: Int): Long {

//    (n)   (n)
//    (0) = (n) = 1
//
//    (n+1)   (n)   ( n )
//    (k+1) = (k) + (k+1)

    return when {
        k <= 0 -> {
            1
        }
        n <= 0 -> {
            0
        }
        else -> {
            binomRecursive(n - 1, k - 1) + binomRecursive(n - 1, k)
        }
    }

}

// Using product rule.
fun binomLong(n: Int, k: Int): Long {
    var result = 1L

    val kMin = if (2 * k > n) {
        n - k
    } else {
        k
    }
    for (i in 1..kMin) {
        result =
            result * (n + 1 - i) / i // do not use result *= (n + 1 - i) / i, as it gets a wrong result
    }

    return result
}

// Using product rule.
fun binom(n: Int, k: Int): Double {
    var result = 1.0

    val kMin = if (2 * k > n) {
        n - k
    } else {
        k
    }
    for (i in 1..kMin) {
        result =
            result * (n + 1 - i) / i // do not use result *= (n + 1 - i) / i, as it gets a wrong result
    }

    return result
}

fun tau(x: Double): Double {

    // https://de.wikipedia.org/wiki/Fehlerfunktion#Numerische_Berechnung

    val t = 1 / (1 + 0.5 * x.absoluteValue)

    return t * exp(
        -x * x - 1.26551223 + 1.00002368 * t + 0.37409196 * t * t + 0.09678418 * t * t * t
                - 0.18628806 * t * t * t * t + 0.27886807 * t * t * t * t * t
                - 1.13520398 * t * t * t * t * t * t + 1.48851587 * t * t * t * t * t * t * t
                - 0.82215223 * t * t * t * t * t * t * t * t + 0.17087277 * t * t * t * t * t * t * t * t * t
    )
}

fun erf(x: Double): Double {

    // https://de.wikipedia.org/wiki/Fehlerfunktion#Numerische_Berechnung

    return if (x >= 0) {
        1 - tau(x)
    } else {
        tau(-x) - 1
    }
}

// P(X<x)
fun normal(x: Double, mean: Double, sigma: Double): Double {

    // https://de.wikipedia.org/wiki/Normalverteilung#Verteilungsfunktion

    return 0.5 * (1 + erf((x - mean) / sigma / sqrt(2.0)))
}

// P(a<X<b)=P(X<b)-P(X<a)
fun normalcdf(a: Double, b: Double, mean: Double, sigma: Double): Double {

    return normal(b, mean, sigma) - normal(a, mean, sigma)
}
