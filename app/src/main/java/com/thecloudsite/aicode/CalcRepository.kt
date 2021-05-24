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

            // set comment to op if exists, same as add comments if both NaN
            op.value.isNaN() && op.desc.isNotEmpty() -> {
                result.desc = desc + op.desc
                result.value = value
                result.vector = vector
                result.matrix = matrix
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
            // [[Matrix]] / [[Matrix]]
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
                                result.matrix!![row][col] += matrix!![row][col] / op.matrix!![row][col]
                            }
                        }
                    }
                }
            }

            // [Vector] / [Vector]
            vector != null && op.vector != null -> {
                if (vector!!.size == op.vector!!.size) {
                    result.vector =
                        DoubleArray(vector!!.size) { c -> vector!![c] / op.vector!![c] }
                }
            }

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
