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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConvertTest {

    private val delta = 0.00000001

    data class CalcLineJson
        (
        var desc: String = "",
        var value: Double? = 0.0,
        var lambda: Int = -1,
        var definition: String = "",
        var vector: List<Double>?,
        var matrix: List<List<Double>>?,
    )

    private fun calcLineToStr(calcLine: CalcLine): String {

        var jsonString = ""

        try {
            // Convert to a json string.
            val gson: Gson = GsonBuilder()
                .create()

            val calcLineJson = CalcLineJson(
                desc = calcLine.desc,
                value = if(calcLine.value.isFinite()) calcLine.value else null,
                lambda = calcLine.lambda,
                definition = calcLine.definition,
                vector = null,
                matrix = null,
            )

            if (calcLine.vector != null) {
                val vector1 = mutableListOf<Double>()
                calcLine.vector!!.forEachIndexed { index, d -> vector1.add(d) }
                calcLineJson.vector = vector1.toList()
            }

            if (calcLine.matrix != null) {
                val rows = calcLine.matrix!!.size

                if (rows > 0) {
                    val cols = calcLine.matrix!![0].size
                    val array: MutableList<MutableList<Double>> =
                        MutableList(rows) { MutableList(cols) { 0.0 } }

                    for (row in 0 until rows) {
                        for (col in 0 until cols) {
                            array[row][col] += calcLine.matrix!![row][col]
                        }
                    }
                    calcLineJson.matrix = array
                }
            }

            jsonString = gson.toJson(calcLineJson)

        } catch (e: Exception) {
        }

        return jsonString
    }

    private fun strToCalcLine(
        data: String
    ): CalcLine {

        try {

            val sType = object : TypeToken<CalcLineJson>() {}.type
            val gson = Gson()
            val calcLineJson = gson.fromJson<CalcLineJson>(data, sType)

            val calcLine = CalcLine(
                desc = calcLineJson.desc,
                value = if(calcLineJson.value == null) Double.NaN else calcLineJson.value!!,
                lambda = calcLineJson.lambda,
                definition = calcLineJson.definition,
                vector = null,
                matrix = null,
            )

            if (calcLineJson.vector != null) {
                calcLine.vector =
                    DoubleArray(calcLineJson.vector!!.size) { c -> calcLineJson.vector!![c] }
            }

            if (calcLineJson.matrix != null) {
                val rows = calcLineJson.matrix!!.size

                if (rows > 0) {
                    val cols = calcLineJson.matrix!![0].size
                    calcLine.matrix =
                        Array(rows) { r -> DoubleArray(cols) { c -> calcLineJson.matrix!![r][c] } }
                }
            }

            return calcLine

        } catch (e: Exception) {
        }

        return CalcLine()
    }

    @Test
    @Throws(Exception::class)
    fun calcLineConvertTest() {

        val op1 = CalcLine(
            desc = "test",
            value = 123.0,
            lambda = 10,
            definition = "def",
        )
        op1.vector = DoubleArray(3) { c -> (c + 1).toDouble() }
        val rows = 2
        val cols = 3
        op1.matrix = Array(rows) { r -> DoubleArray(cols) { c -> (r * cols + c + 1).toDouble() } }

        val memStr = calcLineToStr(op1)

        val calcLine = strToCalcLine(memStr)

        assertEquals(op1.desc, calcLine.desc)
        assertEquals(op1.value, calcLine.value, delta)
        assertEquals(op1.lambda, calcLine.lambda)
        assertEquals(op1.definition, calcLine.definition)

        // compare vector
        assertEquals(op1.vector!!.size, calcLine.vector!!.size)
        for (i in 0 until op1.vector!!.size) {
            assertEquals(op1.vector!![i], calcLine.vector!![i], delta)
        }

        // compare matrix
        assertEquals(op1.matrix!!.size, calcLine.matrix!!.size)
        assertEquals(op1.matrix!![0].size, calcLine.matrix!![0].size)
        for (row in 0 until op1.matrix!!.size) {
            for (col in 0 until op1.matrix!![0].size) {
                assertEquals(op1.matrix!![row][col], calcLine.matrix!![row][col], delta)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun calcLineConvertNaNTest() {

        val op1 = CalcLine(
            value = Double.NaN
        )

        op1.vector = DoubleArray(3) { c -> (c + 1).toDouble() }

        val memStr = calcLineToStr(op1)

        val calcLine = strToCalcLine(memStr)

        assertEquals(op1.desc, calcLine.desc)
        assertEquals(op1.value, calcLine.value, delta)
        assertEquals(op1.lambda, calcLine.lambda)
        assertEquals(op1.definition, calcLine.definition)

        // compare vector
        assertEquals(op1.vector!!.size, calcLine.vector!!.size)
        for (i in 0 until op1.vector!!.size) {
            assertEquals(op1.vector!![i], calcLine.vector!![i], delta)
        }
    }
}
