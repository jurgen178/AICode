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
    fun binomTest() {

        assertEquals(120, factorial(5))

        assertEquals(1, binom(0, 0))
        assertEquals(6, binom(4, 2))
        assertEquals(10, binom(5, 2))

        // Test for large numbers
        assertEquals(118264581564861424, binom(60, 30))

        for (n in 0..10) {
            for (k in 0..n) {
                assertEquals(binomUsingFactorial(n, k), binom(n, k))
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun binomRecursiveTest() {

        assertEquals(1, binomRecursive(0, 0))
        assertEquals(6, binomRecursive(4, 2))
        assertEquals(10, binomRecursive(5, 2))

        for (n in 0..10) {
            for (k in 0..n) {
                assertEquals(binomUsingFactorial(n, k), binomRecursive(n, k))
            }
        }
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

    fun parseMatrixTest(words: List<String>): MutableList<CalcLine>? {

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
    fun parseMatrixVector() {
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

    @Test
    @Throws(Exception::class)
    fun linSysMatrix() {
        val words_A: List<String> = listOf(
            "[", "1", "2", "3", "]",
            "[", "1", "1", "1", "]",
            "[", "3", "3", "1", "]",
            "]",
        )
        val words_b: List<String> = listOf(
            "2", "2", "0", "]"
        )
        val b = parseMatrixTest(words_b)!![0].vector!!
        assertEquals(3, b.size)

        val A = parseMatrixTest(words_A)!![0].matrix!!
        assertEquals(3, A.size)
        assertEquals(3, A[0].size)

        // Copy of A
        val n = A.size
        val matrix = Array(n) { r -> DoubleArray(n + 1) { c -> if (c > n - 1) b[r] else A[r][c] } }

        // Dreiecksmatrix
        for (i in 0 until n - 1) {
            for (j in i + 1 until n) {
                val a = matrix[j][i] / matrix[i][i]
                for (col in i..n) {
                    matrix[j][col] -= a * matrix[i][col]
                }
            }
        }

        val x = DoubleArray(n) { 0.0 }

        // Rückwärtseinsetzen
        for (r in n - 1 downTo 0) {
            var sum = matrix[r][n]
            for (s in r + 1 until n) {
                sum -= matrix[r][s] * x[s]
            }
            x[r] = sum / matrix[r][r]
        }

        assertEquals(5.0, x[0], 0.0000001)
        assertEquals(-6.0, x[1], 0.0000001)
        assertEquals(3.0, x[2], 0.0000001)

        val A2 = parseMatrixTest(words_A)?.get(0)!!
        val b2 = parseMatrixTest(words_b)?.get(0)!!

        val x2 = solve(A2, b2).vector!!
        assertEquals(5.0, x2[0], 0.0000001)
        assertEquals(-6.0, x2[1], 0.0000001)
        assertEquals(3.0, x2[2], 0.0000001)
    }

    @Test
    @Throws(Exception::class)
    fun inversMatrix() {

        val words_A: List<String> = listOf(
            "[", "1", "2", "0", "]",
            "[", "2", "4", "1", "]",
            "[", "2", "1", "0", "]",
            "]",
        )

        val A = parseMatrixTest(words_A)?.get(0)!!

        val AI = matrixInvers(A).matrix!!

        // -0.33 0 0.66
        // 0.66 0 -0.33
        // -2 1 0

        assertEquals(-0.33, AI[0][0], 0.01)
        assertEquals(0.0, AI[0][1], 0.01)
        assertEquals(0.66, AI[0][2], 0.01)

        assertEquals(0.66, AI[1][0], 0.01)
        assertEquals(0.0, AI[1][1], 0.01)
        assertEquals(-0.33, AI[1][2], 0.01)

        assertEquals(-2.0, AI[2][0], 0.01)
        assertEquals(1.0, AI[2][1], 0.01)
        assertEquals(0.0, AI[2][2], 0.01)
    }

}
