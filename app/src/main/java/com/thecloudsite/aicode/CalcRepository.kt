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
