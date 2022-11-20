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
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.color
import androidx.core.text.scale
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.aicode.CalcAdapter.CalcViewHolder
import com.thecloudsite.aicode.databinding.CalcItemBinding
import java.text.DecimalFormatSymbols
import java.text.NumberFormat

class CalcAdapter internal constructor(
    private val context: Context
) : RecyclerView.Adapter<CalcViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var calcData: CalcData = CalcData()
    private var numberFormat: NumberFormat = NumberFormat.getNumberInstance()
    private var radix = 10
    private var binaryDisplay = false
    private var separatorChar = DecimalFormatSymbols.getInstance().decimalSeparator
    private var sciFormat: Boolean = false

    class CalcViewHolder(
        val binding: CalcItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CalcViewHolder {

        val binding = CalcItemBinding.inflate(inflater, parent, false)
        return CalcViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: CalcViewHolder,
        position: Int
    ) {

        holder.binding.calclineNumber.text =
            if (position == calcData.numberList.size) {

                // edit/error line
                holder.binding.calclineNumber.gravity = Gravity.START

                when {
                    calcData.editline.isNotEmpty() -> {
                        SpannableStringBuilder().color(Color.BLACK) { append(calcData.editline + "‹") }
                    }
                    calcData.errorMsg.isNotEmpty() -> {
                        SpannableStringBuilder().scale(0.8f) { color(Color.RED) { append(calcData.errorMsg) } }
                    }
                    else -> {
                        SpannableStringBuilder()
                    }
                }

            } else
                if (position >= 0 && position < calcData.numberList.size) {

                    // number list
                    holder.binding.calclineNumber.gravity = Gravity.END
                    val current = calcData.numberList[position]
                    if (current.lambda >= 0) {
                        SpannableStringBuilder().color(Color.BLUE) { append("(${current.definition})") }
                    } else {
                        val line =
                            SpannableStringBuilder().color(Color.GRAY) {
                                scale(0.8f) {
                                    append(
                                        current.desc
                                    )
                                }
                            }

                        // No value displayed for Double.NaN if desc is used.
                        if (!(current.desc.isNotEmpty() && current.value.isNaN() && current.vector == null && current.matrix == null)) {
                            line.append(SpannableStringBuilder().color(Color.BLACK) {
                                append(
                                    when {
                                        current.vector != null -> {
                                            current.vector!!.joinToString(
                                                prefix = "[",
                                                separator = " ",
                                                postfix = "]",
                                            ) {
                                                displayValue(
                                                    it
                                                )
                                            }
                                        }
                                        current.matrix != null -> {
                                            // [ [ 1 2 3 ]
                                            //   [ 4 5 6 ]
                                            //   [ 7 8 9 ] ]
                                            current.matrix!!.map { row ->

                                                row.joinToString(
                                                    prefix = "[",
                                                    separator = " ",
                                                    postfix = "]",
                                                ) {
                                                    displayValue(
                                                        it
                                                    )
                                                }
                                            }.joinToString(
                                                prefix = "[ ",
                                                separator = "  \n",
                                                postfix = " ]",
                                            )
                                        }

                                        binaryDisplay -> {
                                            getDisplayString(current.value.toLong(), radix)
                                        }

                                        else -> {
                                            displayValue(current.value)
                                        }
                                    }
                                )
                            })
                        }
                        line
                    }

                } else {

                    holder.binding.calclineNumber.gravity = Gravity.END
                    SpannableStringBuilder().append("")

                }

        holder.binding.calclinePrefix.text =
            if (position >= 0 && position < calcData.numberList.size) {

                // number list
                SpannableStringBuilder().color(Color.BLACK) {
                    append("${calcData.numberList.size - position}:")
                }

            } else {

                SpannableStringBuilder().append("")

            }
    }

    private fun displayValue(value: Double): String {
        return if (sciFormat) {
            value.toString().replace('.', separatorChar)
        } else {
            numberFormat.format(value)
        }
    }

    fun updateData(
        calcData: CalcData,
        numberFormat: NumberFormat,
        radix: Int,
        binaryDisplay: Boolean,
        separatorChar: Char,
        sciFormat: Boolean
    ) {

        // add a copy of the data

        // using calcData.numberList.clear() (or .removeLast()) in the view model causes
        // java.lang.IndexOutOfBoundsException: Inconsistency detected. Invalid view holder adapter positionCalcViewHolder

        this.calcData.numberList.clear()
        this.calcData.numberList.addAll(calcData.numberList)

        this.calcData.errorMsg = calcData.errorMsg
        this.calcData.editMode = calcData.editMode
        this.calcData.editline = calcData.editline
        this.radix = radix
        this.binaryDisplay = binaryDisplay
        this.sciFormat = sciFormat
        this.separatorChar = separatorChar
        this.numberFormat = if (binaryDisplay) {
            val n: NumberFormat = numberFormat.clone() as NumberFormat
            n.minimumFractionDigits = 0
            n.maximumFractionDigits = 0
            n
        } else {
            numberFormat
        }

        notifyDataSetChanged()
    }

    override fun getItemCount() = calcData.numberList.size + 1 // numberlist + editline
}
