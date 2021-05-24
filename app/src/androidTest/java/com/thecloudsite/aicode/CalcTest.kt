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
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.text.NumberFormat
import kotlin.math.sqrt

@RunWith(AndroidJUnit4::class)
class CalcTest {

    @Test
    @Throws(Exception::class)
    fun fracTest() {

        assertEquals(Pair(5, 19), frac(5.0 / 19.0))
        assertEquals(Pair(104348, 33215), frac(3.14159265359))
        assertEquals(Pair(37, 61), frac(0.606557377049))
        assertEquals(Pair(2, 1), frac(2.0))
        assertEquals(Pair(1, 3), frac(0.33333333))

        assertEquals(Pair(0, 1), frac(0.0))

        assertEquals(Pair(-2, 1), frac(-2.0))
        assertEquals(Pair(-5, 19), frac(-5.0 / 19.0))
        assertEquals(Pair(-104348, 33215), frac(-3.14159265359))
        assertEquals(Pair(-37, 61), frac(-0.606557377049))
        assertEquals(Pair(-1, 3), frac(-0.33333333))

        // null result

        // infinity
        assertEquals(Pair(null, 1), frac(1 / 0.0))
        // NaN
        assertEquals(Pair(null, 1), frac(sqrt(-1.0)))
        // large number
        assertEquals(Pair(null, 1), frac(100000000000000.0))
    }

    @Test
    @Throws(Exception::class)
    fun bitTest() {

        assertEquals(8, getBlockSize(248))
        assertEquals(8, getBlockSize(127))
        assertEquals(8, getBlockSize(-128))
        assertEquals(16, getBlockSize(-248))

        assertEquals(16, getBlockSize(32767))
        assertEquals(16, getBlockSize(-32767))

        assertEquals(16, getBlockSize(32768))
        assertEquals(16, getBlockSize(-32768))

        assertEquals(16, getBlockSize(65535))
        assertEquals(32, getBlockSize(-65535))

        assertEquals(32, getBlockSize(65536))
        assertEquals(32, getBlockSize(-65536))

    }

    @Test
    @Throws(Exception::class)
    fun displayStringTest() {

        val valueA = 1L
        val s1 = getDisplayString(valueA, 2)
        assertEquals("00000001", s1)

        val blockSizeA = getBlockSize(valueA)
        val valueInvertedA = invertBits(valueA, blockSizeA)

        val s2 = getDisplayString(valueInvertedA, 2)
        assertEquals("11111110", s2)

        val valueB = 3735928559L
        val s3 = getDisplayString(valueB, 2)
        assertEquals("11011110101011011011111011101111", s1)

        val blockSizeB = getBlockSize(valueB)
        val valueInvertedB = invertBits(valueB, blockSizeB)

        val s4 = getDisplayString(valueInvertedB, 2)
        assertEquals("1111111111111111111111111111111100100001010100100100000100010000", s4)

    }

    @Test
    @Throws(Exception::class)
    fun binaryNotTest() {

        val valueA = -2882390955L
        val blockSizeA = getBlockSize(valueA)
        val s1 = getDisplayString(valueA, 2)
        assertEquals("1111111111111111111111111111111101010100001100100011010001010101", s1)

        val valueInvertedA = invertBits(valueA, blockSizeA)

        val s2 = getDisplayString(valueInvertedA, 2)
        assertEquals("10101011110011011100101110101010", s2)

    }

    data class CalcLine
        (
        var desc: String = "",
        var value: Double = 0.0,
        var lambda: Int = -1,
        var definition: String = "",
        var vector: DoubleArray? = null,
        var matrix: Array<DoubleArray>? = null,

        ) {
        operator fun plus(op: CalcLine): CalcLine {
            val result = CalcLine()

            return if (op.value.isNaN() && value.isNaN()) {
                // add comments if both NaN
                CalcLine(
                    desc = desc + op.desc,
                    value = value
                )
            } else {
                if (op.value.isNaN() && op.desc.isNotEmpty()) {
                    // set comment to op2 if exists, same as add comments if both NaN
                    CalcLine(
                        desc = desc + op.desc,
                        value = value
                    )
                } else {
                    // default op, add two numbers
                    CalcLine(
                        desc = "",
                        value = value + op.value
                    )
                }
            }

            return result
        }

        operator fun times(op: CalcLine): CalcLine {
            val result = CalcLine()

            if (matrix != null && op.vector != null) {
                val rows = matrix!!.size

                if (rows > 0 && matrix!![0].size == op.vector!!.size) {
                    result.vector = DoubleArray(rows) { 0.0 }

                    matrix?.forEachIndexed { row, doubles ->
                        doubles.forEachIndexed { col, value ->
                            result.vector!![row] += op.vector!![col] * value
                        }
                    }
                }
            } else {
                return CalcLine(
                    desc = "",
                    value = value * op.value
                )
            }

            return result
        }
    }


    @Test
    @Throws(Exception::class)
    fun matrixTest() {

        val rows = 2
        val cols = 3

        val matrix: Array<DoubleArray>
        matrix = Array(rows) { r -> DoubleArray(cols) { c -> (r * cols + c).toDouble() } }

        val vector: DoubleArray
        vector = DoubleArray(cols) { c -> c.toDouble() }

        val vectorResult: DoubleArray
        vectorResult = DoubleArray(rows) { 0.0 }

        matrix.forEachIndexed { row, doubles ->
            doubles.forEachIndexed { col, value ->
                vectorResult[row] += vector[col] * value
            }
        }

        vectorResult.forEachIndexed { index, d ->
            val result = d
        }
    }

    @Test
    @Throws(Exception::class)
    fun matrixTest2() {

        val rows = 2
        val cols = 3

        val c1 = CalcLine()
        c1.matrix = Array(rows) { r -> DoubleArray(cols) { c -> (r * cols + c).toDouble() } }

        val c2 = CalcLine()
        c2.vector = DoubleArray(cols) { c -> c.toDouble() }

        var c3 = CalcLine()
        c3.vector = DoubleArray(rows) { 0.0 }

        c3 = c1 * c2

        c3.vector!!.forEachIndexed { index, d ->
            val result = d
        }
    }

    fun parseMatrixTest(words: List<String>):MutableList<CalcLine>? {

        val calcData: MutableList<CalcLine> = mutableListOf()
        val numberFormat: NumberFormat = NumberFormat.getNumberInstance()

        var i: Int = 0

        if (i < words.size && words[i] == "[") {
            // Matrix
            i++

            var columnSize = 0
            val rowList: MutableList<MutableList<Double>> = mutableListOf()
            val doubleList: MutableList<Double> = mutableListOf()

            var inVector = true

            while (i < words.size) {
                val nextWord = words[i++]
                if (!inVector && nextWord == "]") {

                    calcData.add(
                        CalcLine(
                            value = Double.NaN,
                            matrix = Array(rowList.size) { r -> DoubleArray(rowList[0].size) { c -> rowList[r][c] } }
                        )
                    )

                    break
                }

                if (inVector && nextWord == "]") {
                    if (columnSize == 0) {
                        columnSize = doubleList.size
                    } else
                        if (columnSize != doubleList.size) {
                            // Error
//                    calcData.errorMsg =
//                        context.getString(R.string.calc_error_parsing_msg, nextWord)
//                    calcRepository.updateData(calcData)

                            // invalid number missing
                            return null
                        }

                    rowList.add(doubleList.toMutableList())
                    doubleList.clear()

                    inVector = false
                    continue
                }

                if (!inVector && nextWord == "[") {

                    inVector = true
                    continue
                }

                if (inVector && nextWord.isNotEmpty()) {
                    try {
                        val value = numberFormat.parse(nextWord)!!
                            .toDouble()

                        doubleList.add(value)
                    } catch (e: Exception) {
                        // Error
//                    calcData.errorMsg =
//                        context.getString(R.string.calc_error_parsing_msg, nextWord)
//                    calcRepository.updateData(calcData)

                        // invalid number missing
                        return null
                    }
                }
            }
        } else {
            // Vector

            val doubleList: MutableList<Double> = mutableListOf()
            while (i < words.size) {
                val nextWord = words[i++]
                if (nextWord == "]") {
                    calcData.add(
                        CalcLine(
                            value = Double.NaN,
                            vector = DoubleArray(doubleList.size) { v -> doubleList[v] }
                        )
                    )

                    break
                }

                if (nextWord.isNotEmpty()) {
                    try {
                        val value = numberFormat.parse(nextWord)!!
                            .toDouble()

                        doubleList.add(value)
                    } catch (e: Exception) {
                        // Error
//                    calcData.errorMsg =
//                        context.getString(R.string.calc_error_parsing_msg, nextWord)
//                    calcRepository.updateData(calcData)

                        // invalid number missing
                        return null
                    }
                }
            }

            if (i == words.size && words[i - 1] != "]") {
                //            calcData.errorMsg =
                //                context.getString(R.string.calc_error_missing_closing_vector_bracket)
                //            calcRepository.updateData(calcData)

                // invalid number missing
                return null
            }
        }

        return calcData
    }

    @Test
    @Throws(Exception::class)
    fun parseMatrixVector()
    {
        val wordsVector1: List<String> = listOf(
            "]"
        )
        val vector1 = parseMatrixTest(wordsVector1)
        assertEquals(0, vector1!![0].vector?.size)

        val wordsMatrix1: List<String> = listOf(
            "[", "]",
            "]",
        )
        val matrix1 = parseMatrixTest(wordsMatrix1)
        assertEquals(1, matrix1!![0].matrix?.size)
        assertEquals(0, matrix1[0].matrix?.get(0)?.size)

        val wordsVector2: List<String> = listOf(
            "1", "2", "3", "]"
        )
        val vector2 = parseMatrixTest(wordsVector2)
        assertEquals(3, vector2!![0].vector?.size)

        val wordsMatrix2: List<String> = listOf(
            "[", "1", "2", "3", "]",
            "[", "4", "5", "6", "]",
            "]",
        )
        val matrix2 = parseMatrixTest(wordsMatrix2)
        assertEquals(2, matrix2!![0].matrix?.size)
        assertEquals(3, matrix2[0].matrix?.get(0)?.size)

    }

}
