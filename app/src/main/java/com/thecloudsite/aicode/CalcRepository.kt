/*
 * Copyright (C)
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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

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
        val result = CalcLine(value = Double.NaN)

        when {
            // [[Matrix]] + [[Matrix]]
            matrix != null && op.matrix != null -> {
                val rowsA = matrix!!.size
                val rowsB = op.matrix!!.size

                if (rowsA > 0 && rowsA == rowsB) {
                    val colsA = matrix!![0].size
                    val colsB = op.matrix!![0].size

                    if (colsA == colsB) {
                        result.matrix =
                            Array(rowsA) { r -> DoubleArray(colsB) { 0.0 } }

                        for (row in 0 until rowsA) {
                            for (col in 0 until colsB) {
                                result.matrix!![row][col] += matrix!![row][col] + op.matrix!![row][col]
                            }
                        }
                    }
                }
            }

            // [Vector] + [Vector]
            vector != null && op.vector != null -> {
                if (vector!!.size == op.vector!!.size) {
                    result.vector =
                        DoubleArray(vector!!.size) { c -> vector!![c] + op.vector!![c] }
                }
            }

//            // [[Matrix]] + [Vector]
//            matrix != null && op.vector != null-> {
//            }
//
//            // [Vector] + [[Matrix]]
//            vector != null && op.matrix != null-> {
//            }

            // double + [Vector]
            value.isFinite() && op.vector != null -> {
                result.vector = DoubleArray(op.vector!!.size) { c -> value + op.vector!![c] }
            }

            // [Vector] + double
            vector != null && op.value.isFinite() -> {
                result.vector = DoubleArray(vector!!.size) { c -> vector!![c] + op.value }
            }

            // double + [[Matrix]]
            value.isFinite() && op.matrix != null -> {
                val rows = op.matrix!!.size

                if (rows > 0) {
                    val cols = op.matrix!![0].size
                    result.matrix =
                        Array(rows) { r -> DoubleArray(cols) { c -> value + op.matrix!![r][c] } }
                }
            }

            // [[Matrix]] + double
            matrix != null && op.value.isFinite() -> {
                val rows = matrix!!.size

                if (rows > 0) {
                    val cols = matrix!![0].size
                    result.matrix =
                        Array(rows) { r -> DoubleArray(cols) { c -> matrix!![r][c] + op.value } }
                }
            }

            // add comments if both NaN
            value.isNaN() && matrix == null && vector == null && op.value.isNaN() && op.matrix == null && op.vector == null -> {
                result.desc = desc + op.desc
                result.value = value
            }

            // add comment to value
            op.value.isNaN() && op.desc.isNotEmpty() -> {
                result.desc = desc + op.desc
                result.value = value
                result.vector = vector
                result.matrix = matrix
            }

            // add value to comment
            value.isNaN() && desc.isNotEmpty() -> {
                result.desc = desc + op.desc
                result.value = op.value
                result.vector = op.vector
                result.matrix = op.matrix
            }

            // default op, add two numbers
            else -> {
                result.value = value + op.value
            }
        }

        return result
    }

    operator fun minus(op: CalcLine): CalcLine {
        val result = CalcLine(value = Double.NaN)

        when {
            // [[Matrix]] - [[Matrix]]
            matrix != null && op.matrix != null -> {
                val rowsA = matrix!!.size
                val rowsB = op.matrix!!.size

                if (rowsA > 0 && rowsA == rowsB) {
                    val colsA = matrix!![0].size
                    val colsB = op.matrix!![0].size

                    if (colsA == colsB) {
                        result.matrix =
                            Array(rowsA) { r -> DoubleArray(colsB) { 0.0 } }

                        for (row in 0 until rowsA) {
                            for (col in 0 until colsB) {
                                result.matrix!![row][col] += matrix!![row][col] - op.matrix!![row][col]
                            }
                        }
                    }
                }
            }

            // [Vector] - [Vector]
            vector != null && op.vector != null -> {
                if (vector!!.size == op.vector!!.size) {
                    result.vector =
                        DoubleArray(vector!!.size) { c -> vector!![c] - op.vector!![c] }
                }
            }

//            // [[Matrix]] - [Vector]
//            matrix != null && op.vector != null-> {
//            }
//
//            // [Vector] - [[Matrix]]
//            vector != null && op.matrix != null-> {
//            }

            // double - [Vector]
            value.isFinite() && op.vector != null -> {
                result.vector = DoubleArray(op.vector!!.size) { c -> value - op.vector!![c] }
            }

            // [Vector] - double
            vector != null && op.value.isFinite() -> {
                result.vector = DoubleArray(vector!!.size) { c -> vector!![c] - op.value }
            }

            // double - [[Matrix]]
            value.isFinite() && op.matrix != null -> {
                val rows = op.matrix!!.size

                if (rows > 0) {
                    val cols = op.matrix!![0].size
                    result.matrix =
                        Array(rows) { r -> DoubleArray(cols) { c -> value - op.matrix!![r][c] } }
                }
            }

            // [[Matrix]] - double
            matrix != null && op.value.isFinite() -> {
                val rows = matrix!!.size

                if (rows > 0) {
                    val cols = matrix!![0].size
                    result.matrix =
                        Array(rows) { r -> DoubleArray(cols) { c -> matrix!![r][c] - op.value } }
                }
            }

            // default op, subtract two numbers
            else -> {
                result.value = value - op.value
            }
        }

        return result
    }

    operator fun unaryMinus(): CalcLine {
        val result = CalcLine(value = Double.NaN)

        when {
            // -[[Matrix]]
            matrix != null -> {
                val rows = matrix!!.size

                if (rows > 0) {
                    val cols = matrix!![0].size
                    result.matrix = Array(rows) { r -> DoubleArray(cols) { c -> -matrix!![r][c] } }
                }
            }

            // -[Vector]
            vector != null -> {
                result.vector = DoubleArray(vector!!.size) { c -> -vector!![c] }
            }

            // default op
            else -> {
                result.value = -value
            }
        }

        return result
    }

    operator fun times(op: CalcLine): CalcLine {
        val result = CalcLine(value = Double.NaN)

        when {
            // [[Matrix]] * [[Matrix]]
            matrix != null && op.matrix != null -> {
                val rowsA = matrix!!.size
                val rowsB = op.matrix!!.size

                if (rowsA > 0 && rowsB > 0) {
                    val colsA = matrix!![0].size
                    val colsB = op.matrix!![0].size

                    if (rowsB == colsA) {
                        result.matrix =
                            Array(rowsA) { r -> DoubleArray(colsB) { 0.0 } }

                        for (row in 0 until rowsA) {
                            for (col in 0 until colsB) {
                                for (j in 0 until colsA) {
                                    result.matrix!![row][col] += matrix!![row][j] * op.matrix!![j][col]
                                }
                            }
                        }
                    }
                }
            }

            // [[Matrix]] * [Vector]
            matrix != null && op.vector != null -> {
                val rows = matrix!!.size

                if (rows > 0 && matrix!![0].size == op.vector!!.size) {
                    result.vector = DoubleArray(rows) { 0.0 }

                    matrix?.forEachIndexed { row, doubles ->
                        doubles.forEachIndexed { col, a ->
                            result.vector!![row] += op.vector!![col] * a
                        }
                    }
                }
            }

            // [Vector] * [Vector]
            vector != null && op.vector != null -> {
                result.value = 0.0

                if (vector!!.size == op.vector!!.size) {
                    vector!!.forEachIndexed { index, a ->
                        result.value += a * op.vector!![index]
                    }
                }
            }

            // double * [Vector]
            value.isFinite() && op.vector != null -> {
                result.vector = DoubleArray(op.vector!!.size) { c -> value * op.vector!![c] }
            }

            // [Vector] * double
            vector != null && op.value.isFinite() -> {
                result.vector = DoubleArray(vector!!.size) { c -> vector!![c] * op.value }
            }

            // double * [[Matrix]]
            value.isFinite() && op.matrix != null -> {
                val rows = op.matrix!!.size

                if (rows > 0) {
                    val cols = op.matrix!![0].size
                    result.matrix =
                        Array(rows) { r -> DoubleArray(cols) { c -> value * op.matrix!![r][c] } }
                }
            }

            // [[Matrix]] * double
            matrix != null && op.value.isFinite() -> {
                val rows = matrix!!.size

                if (rows > 0) {
                    val cols = matrix!![0].size
                    result.matrix =
                        Array(rows) { r -> DoubleArray(cols) { c -> matrix!![r][c] * op.value } }
                }
            }

            // default op, multiply two numbers
            else -> {
                result.value = value * op.value
            }
        }

        return result
    }

    operator fun div(op: CalcLine): CalcLine {
        val result = CalcLine(value = Double.NaN)

        when {
            // double / [Vector]
            value.isFinite() && op.vector != null -> {
                result.vector = DoubleArray(op.vector!!.size) { c -> value / op.vector!![c] }
            }

            // [Vector] / double
            vector != null && op.value.isFinite() -> {
                result.vector = DoubleArray(vector!!.size) { c -> vector!![c] / op.value }
            }

            // double / [[Matrix]]
            value.isFinite() && op.matrix != null -> {
                val rows = op.matrix!!.size

                if (rows > 0) {
                    val cols = op.matrix!![0].size
                    result.matrix =
                        Array(rows) { r -> DoubleArray(cols) { c -> value / op.matrix!![r][c] } }
                }
            }

            // [[Matrix]] / double
            matrix != null && op.value.isFinite() -> {
                val rows = matrix!!.size

                if (rows > 0) {
                    val cols = matrix!![0].size
                    result.matrix =
                        Array(rows) { r -> DoubleArray(cols) { c -> matrix!![r][c] / op.value } }
                }
            }

            // default op, divide two numbers
            else -> {
                result.value = value / op.value
            }
        }

        return result
    }
}

fun solve(op1: CalcLine, op2: CalcLine): CalcLine {

    var x: DoubleArray? = null

    if (op1.matrix != null && op2.vector != null) {
        val rows = op1.matrix!!.size
        if (rows > 0) {
            val cols = op1.matrix!![0].size
            val v = op2.vector!!.size
            if (cols == rows && cols == v) {
                // Copy of matrix
                val n = cols
                val matrix =
                    Array(n) { r -> DoubleArray(n + 1) { c -> if (c > n - 1) op2.vector!![r] else op1.matrix!![r][c] } }

                // Dreiecksmatrix
                for (i in 0 until n - 1) {
                    for (j in i + 1 until n) {
                        val a = matrix[j][i] / matrix[i][i]
                        for (col in i..n) {
                            matrix[j][col] -= a * matrix[i][col]
                        }
                    }
                }

                x = DoubleArray(n) { 0.0 }

                // Rückwärtseinsetzen
                for (r in n - 1 downTo 0) {
                    var sum = matrix[r][n]
                    for (s in r + 1 until n) {
                        sum -= matrix[r][s] * x[s]
                    }
                    x[r] = sum / matrix[r][r]
                }
            }
        }
    }

    return CalcLine(
        value = Double.NaN,
        vector = x,
    )
}

// 1/x for each element
fun vectorInvers(op: CalcLine): CalcLine {

    if (op.vector != null) {
        val n = op.vector!!.size

        if (n > 0) {
            val vector = op.vector!!.clone()

            for (k in 0 until n) {
                if (vector[k] != 0.0) {
                    vector[k] = 1 / vector[k]
                }
            }

            return CalcLine(
                value = Double.NaN,
                vector = vector
            )
        }
    }

    return CalcLine(value = Double.NaN)
}

fun matrixInvers(op: CalcLine): CalcLine {

    if (op.matrix != null) {
        val rows = op.matrix!!.size

        if (rows > 0) {
            val n = op.matrix!![0].size
            if (rows == n) {
                val matrix =
                    Array(n) { r ->
                        DoubleArray(2 * n) { c ->
                            if (c >= n) {
                                if (r == c - n) {
                                    1.0
                                } else {
                                    0.0
                                }
                            } else {
                                op.matrix!![r][c]
                            }
                        }
                    }

                // https://rosettacode.org/wiki/Gauss-Jordan_matrix_inversion#Kotlin

                var lead = 0
                val rowCount = matrix.size
                val colCount = matrix[0].size
                for (r in 0 until rowCount) {
                    if (colCount <= lead) {
                        return CalcLine(value = Double.NaN)
                    }
                    var i = r

                    while (matrix[i][lead] == 0.0) {
                        i++
                        if (rowCount == i) {
                            i = r
                            lead++
                            if (colCount == lead) {
                                return CalcLine(value = Double.NaN)
                            }
                        }
                    }

                    val temp = matrix[i]
                    matrix[i] = matrix[r]
                    matrix[r] = temp

                    if (matrix[r][lead] != 0.0) {
                        val div = matrix[r][lead]
                        for (j in 0 until colCount) {
                            matrix[r][j] /= div
                        }
                    }

                    for (k in 0 until rowCount) {
                        if (k != r) {
                            val mult = matrix[k][lead]
                            for (j in 0 until colCount) {
                                matrix[k][j] -= matrix[r][j] * mult
                            }
                        }
                    }

                    lead++
                }

                return CalcLine(
                    value = Double.NaN,
                    matrix = Array(n) { r -> DoubleArray(n) { c -> matrix[r][c + n] } }
                )
            }
        }
    }

    return CalcLine(value = Double.NaN)
}

fun clone(op: CalcLine): CalcLine {

    var matrix: Array<DoubleArray>? = null
    if (op.matrix != null) {
        val rows = op.matrix!!.size

        if (rows > 0) {
            val cols = op.matrix!![0].size
            matrix = Array(rows) { r -> DoubleArray(cols) { c -> op.matrix!![r][c] } }
        }
    }

    var vector: DoubleArray? = null
    if (op.vector != null) {
        vector = DoubleArray(op.vector!!.size) { c -> op.vector!![c] }
    }

    return CalcLine(
        desc = op.desc,
        value = op.value,
        lambda = op.lambda,
        definition = op.definition,
        matrix = matrix,
        vector = vector,
    )
}

data class CalcData
    (
    var numberList: MutableList<CalcLine> = mutableListOf(),
    var editMode: Boolean = false,
    var editline: String = "",
    var errorMsg: String = ""
)

object SharedCalcData {
    val calcMutableLiveData = MutableLiveData<CalcData>()
}

class CalcRepository(val context: Context) {

    val calcLiveData: LiveData<CalcData>
        get() = SharedCalcData.calcMutableLiveData

    fun getData(): CalcData =
        SharedCalcData.calcMutableLiveData.value ?: CalcData()

    // Force redraw of the data.
    fun updateData() {
        SharedCalcData.calcMutableLiveData.postValue(getData())
    }

    fun updateData(data: CalcData) {
        SharedCalcData.calcMutableLiveData.postValue(data)
    }
}
