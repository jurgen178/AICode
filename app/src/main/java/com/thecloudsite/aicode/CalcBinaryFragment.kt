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

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.thecloudsite.aicode.databinding.FragmentCalcBinaryBinding


class CalcBinaryFragment(stockSymbol: String = "") : CalcBaseFragment(stockSymbol) {

    private var _binding: FragmentCalcBinaryBinding? = null

    private var keys: List<TextView> = listOf()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    companion object {
        fun newInstance(symbol: String) = CalcBinaryFragment(symbol)
    }

    override fun updateCalcAdapter() {
        val lines = calcViewModel.getLines()
        binding.calcIndicatorDepth.text = if (lines > 3) "$lines" else ""

        // scroll to always show last element at the bottom of the list
        // itemcount is numberlist + editline
        binding.calclines.adapter?.itemCount?.minus(1)
            ?.let { binding.calclines.scrollToPosition(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

//    // Crashlytics test
//    throw RuntimeException("Test Crash") // Force a crash

        binaryDisplay = true

        // Inflate the layout for this fragment
        _binding = FragmentCalcBinaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun touchHelperFunction(
        view: View,
        event: MotionEvent
    ) {
        return touchHelper(
            view,
            event,
            requireContext().getColor(R.color.calcFunctionPressed),
            requireContext().getColor(R.color.calcFunction)
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        keys = listOf(
            binding.calc2,
            binding.calc3,
            binding.calc4,
            binding.calc5,
            binding.calc6,
            binding.calc7,
            binding.calc8,
            binding.calc9,
            binding.calcA,
            binding.calcB,
            binding.calcC,
            binding.calcD,
            binding.calcE,
            binding.calcF,
        )

        binding.calclines.adapter = calcAdapter
        binding.calclines.layoutManager = LinearLayoutManager(requireActivity())

        binding.calcHex.setOnTouchListener { view, event -> touchHelper(view, event); false }
        binding.calcHex.setOnClickListener { updateRadix(16) }
        binding.calcDec.setOnTouchListener { view, event -> touchHelper(view, event); false }
        binding.calcDec.setOnClickListener { updateRadix(10) }
        binding.calcOct.setOnTouchListener { view, event -> touchHelper(view, event); false }
        binding.calcOct.setOnClickListener { updateRadix(8) }
        binding.calcBin.setOnTouchListener { view, event -> touchHelper(view, event); false }
        binding.calcBin.setOnClickListener { updateRadix(2) }

        binding.calcEnter.setOnTouchListener { view, event -> touchHelper(view, event); false }
        binding.calcEnter.setOnClickListener { calcViewModel.enter(radix) }

        binding.calcA.setOnTouchListener { view, event -> touchHelper(view, event); false }
        binding.calcA.setOnClickListener { calcViewModel.addNum('a') }
        binding.calcB.setOnTouchListener { view, event -> touchHelper(view, event); false }
        binding.calcB.setOnClickListener { calcViewModel.addNum('b') }
        binding.calcC.setOnTouchListener { view, event -> touchHelper(view, event); false }
        binding.calcC.setOnClickListener { calcViewModel.addNum('c') }
        binding.calcD.setOnTouchListener { view, event -> touchHelper(view, event); false }
        binding.calcD.setOnClickListener { calcViewModel.addNum('d') }
        binding.calcE.setOnTouchListener { view, event -> touchHelper(view, event); false }
        binding.calcE.setOnClickListener { calcViewModel.addNum('e') }
        binding.calcF.setOnTouchListener { view, event -> touchHelper(view, event); false }
        binding.calcF.setOnClickListener { calcViewModel.addNum('f') }

        binding.calc1.setOnTouchListener { view, event -> touchHelper(view, event); false }
        binding.calc1.setOnClickListener { calcViewModel.addNum('1') }
        binding.calc2.setOnTouchListener { view, event -> touchHelper(view, event); false }
        binding.calc2.setOnClickListener { calcViewModel.addNum('2') }
        binding.calc3.setOnTouchListener { view, event -> touchHelper(view, event); false }
        binding.calc3.setOnClickListener { calcViewModel.addNum('3') }
        binding.calc4.setOnTouchListener { view, event -> touchHelper(view, event); false }
        binding.calc4.setOnClickListener { calcViewModel.addNum('4') }
        binding.calc5.setOnTouchListener { view, event -> touchHelper(view, event); false }
        binding.calc5.setOnClickListener { calcViewModel.addNum('5') }
        binding.calc6.setOnTouchListener { view, event -> touchHelper(view, event); false }
        binding.calc6.setOnClickListener { calcViewModel.addNum('6') }
        binding.calc7.setOnTouchListener { view, event -> touchHelper(view, event); false }
        binding.calc7.setOnClickListener { calcViewModel.addNum('7') }
        binding.calc8.setOnTouchListener { view, event -> touchHelper(view, event); false }
        binding.calc8.setOnClickListener { calcViewModel.addNum('8') }
        binding.calc9.setOnTouchListener { view, event -> touchHelper(view, event); false }
        binding.calc9.setOnClickListener { calcViewModel.addNum('9') }
        binding.calc0.setOnTouchListener { view, event -> touchHelper(view, event); false }
        binding.calc0.setOnClickListener { calcViewModel.addNum('0') }

        binding.calcDiv.setOnTouchListener { view, event -> touchHelper(view, event); false }
        binding.calcDiv.setOnClickListener { calcViewModel.opBinary(BinaryArgument.DIV) }
        binding.calcMult.setOnTouchListener { view, event -> touchHelper(view, event); false }
        binding.calcMult.setOnClickListener { calcViewModel.opBinary(BinaryArgument.MULT) }
        binding.calcSub.setOnTouchListener { view, event -> touchHelper(view, event); false }
        binding.calcSub.setOnClickListener { calcViewModel.opBinary(BinaryArgument.SUB) }
        binding.calcAdd.setOnTouchListener { view, event -> touchHelper(view, event); false }
        binding.calcAdd.setOnClickListener { calcViewModel.opBinary(BinaryArgument.ADD) }

        updateShift()
    }

    override fun onPause() {
        super.onPause()

        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(activity /* Activity context */)

        sharedPreferences
            .edit()
            .putBoolean("calc_format_radian", radian == 1.0)
            .apply()

        sharedPreferences
            .edit()
            .putInt("calc_format_radix", radix)
            .apply()
    }

    override fun onResume() {
        super.onResume()

        updateRadix(radix)
    }

    private fun setBackground(textView: TextView, color: Int) {
        // Keep the corner radii and only change the background color.
        val gradientDrawable = textView.background as GradientDrawable
        gradientDrawable.setColor(color)
        textView.background = gradientDrawable
    }

    private fun updateRadix(base: Int) {
        radix = base

        val defaultColor = 0xff444444.toInt()
        val disabledColor = Color.LTGRAY

        keys.forEachIndexed { index, textView ->
            val disabled =
                radix == 2 || radix == 8 && index > 5 || radix == 10 && index > 7

            textView.isEnabled = !disabled

            setBackground(
                textView, if (disabled) {
                    disabledColor
                } else {
                    defaultColor
                }
            )
        }

        binding.calcIndicatorRadix.text = radix.toString()

        // Redraw the valued displayed in the adapter with the new number format.
        calcViewModel.updateData()
    }

    private fun updateShift() {
        when (calcViewModel.shiftLevel) {
            0 -> {
                binding.calcShift.text = "↱"
                binding.calcIndicatorShift.text = ""
            }
            1 -> {
                binding.calcShift.text = "↱  ↱"
                binding.calcIndicatorShift.text = "↱"
            }
            2 -> {
                binding.calcShift.text = "↱  ↱↱"
                binding.calcIndicatorShift.text = "↱↱"
            }
        }

        if (calcViewModel.shiftLevel == 0) {
//      // set 10^x
//      binding.calcZx.text = SpannableStringBuilder()
//        .append("10")
//        .superscript { superscript { scale(0.7f) { bold { append("x") } } } }
//    } else {
//      // set e^x
//      binding.calcZx.text = SpannableStringBuilder()
//        .append("e")
//        .superscript { superscript { scale(0.65f) { bold { append("x") } } } }
        }
    }
}
